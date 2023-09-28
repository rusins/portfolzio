package portfolzio.model

opaque type Id = String

object Id:
  def forImage(directoryPath: String): Id = directoryPath
  def forAlbum(directoryPath: String, albumName: String): Id = directoryPath + "/" + albumName
