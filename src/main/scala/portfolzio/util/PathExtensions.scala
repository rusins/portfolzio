package portfolzio.util

import java.nio.file.Path

extension (basePath: Path)

  /** Avoids directory traversal attacks for file requests */
  def safeResolve(subPath: Path): Option[Path] =
    val res = basePath.resolve(subPath)
    Option.when(res.startsWith(basePath))(res)
