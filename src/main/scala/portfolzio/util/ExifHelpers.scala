package portfolzio.util

import org.apache.commons.imaging.common.ImageMetadata
import portfolzio.model.ImageInfo
import portfolzio.util.RomanNumerals.digitToRoman

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*
import scala.util.Try

object ExifHelpers {

  private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
  def parseExifDateTime(str: String): Option[LocalDateTime] =
    val stripped = str.stripPrefix("'").stripSuffix("'")
    Try(LocalDateTime.parse(stripped, dateTimeFormatter)).toOption

  def parseSonyModel(exifModel: String): Option[String] = Option.when(exifModel.startsWith("ILCE-")) {
    val suffix = exifModel.stripPrefix("ILCE-")
    val model = "a" + (
      if (suffix.contains('M')) {
        suffix.split("M").toList match {
          case base :: version :: Nil =>
            base.toLowerCase + " " + digitToRoman(version).getOrElse(version)
          case _                      => suffix
        }
      } else suffix
      )
    s"Sony $model"
  }

  extension (info: ImageInfo)
    def populateWithExifMetadata(metadata: ImageMetadata): ImageInfo =
      val items = metadata.getItems.asScala.flatMap(item =>
        item.toString.split(": ").toList match
          case key :: value :: Nil => Some(key -> value)
          case _                   => None
      ).toMap[String, String]
      val exifTime = items.get("DateTimeOriginal").flatMap(parseExifDateTime)
      val exifCamera = items.get("Make").zip(items.get("Model")).map((exifMake, exifModel) =>
        val make = exifMake.filterNot(_ == '\'')
        val model = exifModel.filterNot(_ == '\'')
        Option.when(make == "SONY")(parseSonyModel(model)).flatten
          .getOrElse(s"$make $model")
      )
      val exifAperture = items.get("FNumber").orElse(items.get("ApertureValue")).map { str =>
        if (str.contains('('))
          "F" + str.dropWhile(_ != '(').drop(1).takeWhile(_ != ')')
        else
          str
      }
      val exifFocalLength = items.get("FocalLength").map { str =>
        (
          if (str.contains('('))
            str.dropWhile(_ != '(').drop(1).takeWhile(_ != ')')
          else
            str
          ) + "mm"
      }
      val exifShutterSpeed = items.get("ExposureTime").orElse(items.get("ShutterSpeedValue")).map { speedStr =>
        (
          if (speedStr.contains('('))
            speedStr.takeWhile(c => c != ' ' && c != '(')
          else
            speedStr
          ) + "s"
      }
      val exifISO = items.get("PhotographicSensitivity").map("ISO " + _)

      info.copy(
        time = info.time.orElse(exifTime),
        cameraModel =
          info.cameraModel.orElse(exifCamera),
        aperture = info.aperture.orElse(exifAperture),
        focalLength = info.focalLength.orElse(exifFocalLength),
        shutterSpeed = info.shutterSpeed.orElse(exifShutterSpeed),
        iso = info.iso.orElse(exifISO),
      )

}
