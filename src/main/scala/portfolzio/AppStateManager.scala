package portfolzio

import portfolzio.model.AlbumEntry
import zio.*

import scala.collection.immutable.HashMap

type Path = String
case class AppState(albums: HashMap[Path, AlbumEntry])

object AppState {
  def empty: AppState = AppState(HashMap.empty)
}

class AppStateManager private (ref: Ref[AppState]) {
  def getState: UIO[AppState] = ref.get
  def setState(state: AppState): UIO[Unit] = ref.set(state)
}

object AppStateManager {
  def make: UIO[AppStateManager] =
    Ref.make(AppState.empty).map(new AppStateManager(_))
}
