package portfolzio

import zio.http.Http

object FileDownloading:
  def apply() =
    Http.empty
