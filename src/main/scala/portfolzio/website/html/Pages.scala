package portfolzio.website.html

import portfolzio.AppState
import portfolzio.model.AlbumEntry
import portfolzio.model.AlbumEntry.Image
import portfolzio.website.html.CustomTags.OGTags
import zio.http.template.*
import zio.http.template.IsAttributeValue.{instanceList, instanceString, instanceTuple2Seq}

import java.time.format.DateTimeFormatter

class Pages(titleText: String, rootUrl: Option[String], showLicense: Boolean) {

  private val templates = Templates(titleText, rootUrl)
  import templates.*

  def tagsPage(tags: List[String]): Html = pageWithNavigation()(
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

  def tagPage(state: AppState)(tag: String): Html = pageWithNavigation()(
    centeredTopText(s"Images tagged: $tag"),
    imageGrid(
      state.albumEntries.values.collect {
        case img: Image if img.info.tags.exists(_.contains(tag)) => img
      }.toList
    ),
  )

  def imagePage(image: Image): Html = {
    headerTemplate(rootUrl.map(rootUrl =>
      OGTags(
        title = image.id.lastPart + " | " + titleText,
        staticUrl = rootUrl + "/image" + image.id,
        imageUrl = rootUrl + "/preview" + image.id,
        description = image.info.description,
      )
    ))(main(
      div(
        classAttr := List("photo-box", "center"),
        styleAttr := Seq("max-width" -> "100%", "max-height" -> "75vh"),
        img(srcAttr := "/preview" + image.id),
      ),
      h1(
        styleAttr := Seq("text-align" -> "center", "font-family" -> "'Raleway', sans-serif", "font-weight" -> "500"),
        image.info.description.getOrElse(""),
      ),
      p(
        styleAttr := Seq("text-align" -> "center", "font-family" -> "'Raleway', sans-serif", "font-weight" -> "200"),
        image.info.time.map(_.format(DateTimeFormatter.ofPattern("E, MMMM d. yyyy, HH:mm:ss"))).getOrElse(""),
        br(),
        image.info.focalLength.getOrElse(""),
        br(),
        image.info.aperture.getOrElse(""),
        br(),
        image.info.shutterSpeed.getOrElse(""),
        br(),
        image.info.iso.getOrElse(""),
        br(),
        image.info.cameraModel.getOrElse(""),
      ),
      div(
        classAttr := List("center"),
        div(
          styleAttr := Seq("text-align" -> "center"),
          image.imageFiles.map(downloadPath =>
            div(a(
              styleAttr := Seq(
                "font-family" -> "'Raleway', sans-serif",
                "font-weight" -> "200",
                "font-size" -> "2em",
                "color" -> "black",
              ),
              hrefAttr := s"/download/${ downloadPath.toString }",
              downloadAttr := "Raitis_Krikis" + downloadPath.toString.replaceAll("/", "-"),
              "Download Photo",
            ))
          ).toSeq,
        ),
        div(
          styleAttr := Seq("text-align" -> "center"),
          image.rawFiles.map(downloadPath =>
            div(a(
              styleAttr := Seq(
                "font-family" -> "'Raleway', sans-serif",
                "font-weight" -> "200",
                "font-size" -> "2em",
                "color" -> "black",
              ),
              hrefAttr := s"/download/${ downloadPath.toString }",
              downloadAttr := "Raitis_Krikis" + downloadPath.toString.replaceAll("/", "-"),
              "Download Raw File",
            ))
          ).toSeq,
        ),
        if (showLicense) div(
          styleAttr := Seq("text-align" -> "center", "margin" -> "0.5em"),
          span("By downloading, you agree to the the ", a(hrefAttr := "/license", "License Terms")),
        )
        else div(),
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

  def licensePage(licenseContents: String): Html = pageWithNavigation()(
    div(
      styleAttr := Seq(
        "text-align" -> "center",
        "font-size" -> "4em",
        "font-family" -> "'Raleway', sans-serif",
        "font-weight" -> "200",
      ),
      "License",
    ),
    div(
      classAttr := List("center"),
      styleAttr := Seq("max-width" -> "80em"),
      licenseContents,
    ),
  )

  def notFoundPage(message: String): Html =
    headerTemplate(ogTags = None)(h1(styleAttr := Seq("text-align" -> "center"), message))
}
