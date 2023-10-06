package portfolzio.website.html

import portfolzio.AppState
import portfolzio.model.AlbumEntry
import portfolzio.model.AlbumEntry.Image
import portfolzio.website.html.Templates.*
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute

object Pages {

  def tagsPage(tags: List[String]): Html = pageWithNavigation(
    div(
      classAttr := List("pure-menu"),
      ul(
        classAttr := List("pure-menu-list"),
        tags.map(tag =>
          li(
            classAttr := List("pure-menu-item"),
            a(classAttr := List("pure-menu-link"), hrefAttr := s"/tag/$tag", tag),
          )
        ),
      ),
    )
  )

  def tagPage(state: AppState)(tag: String): Html = pageWithNavigation(
    centeredTopText(s"Images tagged: $tag"),
    imageGrid(
      state.albumEntries.values.collect {
        case img: Image if img.info.tags.exists(_.contains(tag)) => img
      }.toList
    ),
  )

  def imagePage(image: Image): Html = {
    headerTemplate(main(
      div(
        classAttr := List("photo-box", "center"),
        styleAttr := Seq("max-width" -> "80vh"),
        img(srcAttr := "/preview" + image.id),
      ),
      h1(
        styleAttr := Seq("text-align" -> "center", "font-family" -> "'Raleway', sans-serif", "font-weight" -> "500"),
        image.info.description.getOrElse(""),
      ),
      p(
        styleAttr := Seq("text-align" -> "center", "font-family" -> "'Raleway', sans-serif", "font-weight" -> "200"),
        image.info.time.map(_.toString).getOrElse(""),
        br(),
        image.info.focalLength.getOrElse(""),
        br(),
        image.info.aperture.getOrElse(""),
        br(),
        image.info.cameraModel.getOrElse(""),
      ),
      div(
        classAttr := List("center"),
        div(
          styleAttr := Seq("text-align" -> "center"),
          image.imageFiles.map(downloadPath =>
            a(
              styleAttr := Seq(
                "font-family" -> "'Raleway', sans-serif",
                "font-weight" -> "200",
                "color" -> "black",
              ),
              hrefAttr := s"/download/${ downloadPath.toString }",
              downloadAttr := "Raitis_Krikis" + downloadPath.toString.replaceAll("/", "-"),
              "Download image",
            )
          ).toSeq,
        ),
        div(
          styleAttr := Seq("text-align" -> "center"),
          image.rawFiles.map(downloadPath =>
            a(
              styleAttr := Seq(
                "font-family" -> "'Raleway', sans-serif",
                "font-weight" -> "200",
                "color" -> "black",
              ),
              hrefAttr := s"/download/${ downloadPath.toString }",
              downloadAttr := "Raitis_Krikis" + downloadPath.toString.replaceAll("/", "-"),
              "Download raw file",
            )
          ).toSeq,
        ),
      ),
      div(
        classAttr := List("pure-menu"),
        ul(
          classAttr := List("pure-menu-list"),
          image.info.tags.getOrElse(List.empty).map(tag =>
            li(
              classAttr := List("pure-menu-item"),
              a(classAttr := List("pure-menu-link"), hrefAttr := s"/tag/$tag", tag),
            )
          ),
        ),
      ),
    ))
  }

  def notFoundPage(message: String): Html = headerTemplate(h1(styleAttr := Seq("text-align" -> "center"), message))
}
