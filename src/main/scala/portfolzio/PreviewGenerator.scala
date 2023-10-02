package portfolzio

import zio.*

trait PreviewGenerator {
  def generatePreviewsForAllImages: Task[Unit]
}


