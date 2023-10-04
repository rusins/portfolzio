package portfolzio

import portfolzio.model.AlbumEntry
import portfolzio.model.AlbumEntry.*
import zio.*

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
      case Album(id, children) => children.foreach(selector =>
        val parentPath = id.value.reverse.dropWhile(_ != '/').drop(1).reverse
        val resolvedChildren = selector.findMatches(parentPath, albumEntries.keys.toList)
        resolvedChildren.foreach(child => hasParents.addOne(child, true))
        childMap.addOne(id, resolvedChildren.flatMap(albumEntries.get))
      )
    }
    val orphans = albumEntries.values.filter(entry => !hasParents.getOrElse(entry.id, false)).toSet
    (childMap.toMap, orphans)
  }
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
