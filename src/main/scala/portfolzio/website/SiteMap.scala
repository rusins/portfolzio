package portfolzio.website

import portfolzio.AppState
import portfolzio.model.AlbumEntry.{Album, Image}

object SiteMap {
  def genSiteMap(baseUrl: String)(state: AppState): String =
    val xmlBegin =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""".stripMargin
    val xmlEnd = "</urlset>"
    val mainPages = List("", "/random", "/recent", "/albums", "/tags")
    val albumsAndImages = state.albumEntries.values.map {
      case alb: Album => s"/album${alb.id}"
      case img: Image => s"/image${img.id}"
    }
    val entries = mainPages ++ albumsAndImages

    def xmlEntry(url: String) = s"""  <url>
                                   |    <loc>$url</loc>
                                   |    <changefreq>monthly</changefreq>
                                   |  </url>""".stripMargin

    val xmlEntries = entries.map(urlSuffix => xmlEntry(baseUrl + urlSuffix))

    xmlBegin + "\n" + xmlEntries.mkString("\n") + "\n" + xmlEnd
}
