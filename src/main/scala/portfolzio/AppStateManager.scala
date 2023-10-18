package portfolzio

import portfolzio.model.AlbumEntry
import portfolzio.model.AlbumEntry.*
import zio.*
import zio.prelude.EqualOps

import scala.collection.immutable.HashMap
import scala.collection.mutable

case class AppState(albumEntries: HashMap[AlbumEntry.Id, AlbumEntry]) {

  /** children is a map from an album ID to its entries (subalbums and images)
    * orphans is a list of all album entries that are not contained within an album, making them top-level entries
    */
  val (children, orphans): (Map[AlbumEntry.Id, List[AlbumEntry]], Set[AlbumEntry]) = {
    val hasParents = mutable.HashMap[AlbumEntry.Id, Boolean]()
    val childMap = mutable.HashMap[AlbumEntry.Id, List[AlbumEntry]]()
    albumEntries.values.collect {
      case Album(albumId, childSelectors) =>
        val parentPath = albumId.value.reverse.dropWhile(_ != '/').drop(1).reverse
        var children = IdSelector.findMatches(parentPath)(childSelectors, albumEntries.values)
          .filterNot(_.id === albumId)
        children.foreach(child => hasParents.addOne(child.id, true))
        if (children.nonEmpty)
          childMap.addOne(albumId, children)
    }
    val orphans = albumEntries.values.filter(entry => !hasParents.getOrElse(entry.id, false)).toSet
    (childMap.toMap, orphans)
  }

  def bestAlbum: Option[AlbumEntry.Album] = albumEntries.values.collectFirst {
    case alb @ Album(AlbumEntry.Id("/best"), _) => alb
    case alb @ Album(AlbumEntry.Id("/Best"), _) => alb
  }

  def resolveCoverImage(album: AlbumEntry.Id, visited: Set[AlbumEntry.Id] = Set.empty): Option[AlbumEntry.Id] =
    if (visited.contains(album)) None
    else
      albumEntries.get(album) match
        case None             => None
        case Some(img: Image) => Some(img.id)
        case Some(alb: Album) => children.get(alb.id)
          .flatMap(_.headOption.flatMap(child => resolveCoverImage(child.id, visited + album)))

  val tags: List[String] = albumEntries.collect {
    case (_, img: Image) => img.info.tags.getOrElse(List.empty)
  }.flatten.toSet.toList.sortBy(_.toLowerCase)

  val images: List[Image] = albumEntries.values.collect {
    case img: Image => img
  }.toList
}

object AppState {
  def empty: AppState = AppState(HashMap.empty)
  def fromRawEntries(albumEntries: List[AlbumEntry]): UIO[AppState] =
    val grouped = albumEntries.groupBy(_.id)
    val duplicates = grouped.collect { case (id, entries) if entries.length > 1 => id }
    ZIO.when(duplicates.nonEmpty)(
      ZIO.logWarning(
        s"Duplicate album entries detected! Please check for duplicates for the following IDs: ${
          duplicates.map(_.value)
            .mkString("", ", ", "")
        }"
      )
    ).as(AppState(HashMap.from(grouped.view.mapValues(_.head))))
}

type Callback = UIO[Unit]

/** Business logic core. Holds app state and notifies subscriber services when it changes. */
class AppStateManager private(stateRef: Ref[AppState], subscribers: Ref[HashMap[String, Callback]]) {
  def getState: UIO[AppState] = stateRef.get

  def setState(newState: AppState): UIO[Unit] =
    for
      _ <- stateRef.set(newState)
      callbacks <- subscribers.get
      runningCallbacks <- ZIO.collectAll(callbacks.values.map(_.fork))
      _ <- ZIO.collectAll(runningCallbacks.map(_.join))
    yield ()

  /** @param subscriberId unique ID used for unsubscribing if needed */
  def subscribeToUpdates(subscriberId: String, callback: Callback): UIO[Unit] = subscribers
    .update(_ + (subscriberId -> callback))

  def unsubscribeFromUpdates(subscriberId: String): UIO[Unit] = subscribers.update(_ - subscriberId)
}

object AppStateManager {
  def make: UIO[AppStateManager] =
    for
      stateRef <- Ref.make(AppState.empty)
      subscribers <- Ref.make(HashMap.empty[String, Callback])
    yield AppStateManager(stateRef, subscribers)
}
