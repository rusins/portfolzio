package portfolzio

import portfolzio.model.AlbumEntry
import zio.*

import scala.collection.immutable.HashMap

case class AppState(albumEntries: HashMap[AlbumEntry.Id, AlbumEntry])

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

type Callback = () => UIO[Unit]

/** Business logic core. Holds app state and notifies subscriber services when it changes. */
class AppStateManager private(stateRef: Ref[AppState], subscribers: Ref[HashMap[String, Callback]]) {
  def getState: UIO[AppState] = stateRef.get

  def setState(newState: AppState): UIO[Unit] =
    for
      _ <- stateRef.set(newState)
      callbacks <- subscribers.get
      _ <- ZIO.collectAll(callbacks.values.map(_.apply()))
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
