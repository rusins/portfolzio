package portfolzio.website.html

import portfolzio.AppState
import portfolzio.model.AlbumEntry
import portfolzio.model.AlbumEntry.*
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute

object Templates:

  def headerTemplate(bodyContent: Html*): Html = html(
    head(
      meta(charsetAttr := "utf-8"),
      meta(nameAttr := "viewport", contentAttr := "width=device-width, initial-scale=1.0"),
      // Include Unpoly before your own stylesheets and JavaScripts
      link(relAttr := "stylesheet", hrefAttr := "/css/unpoly.min.css"),
      script(srcAttr := "/js/unpoly.min.js"),
      link(relAttr := "stylesheet", hrefAttr := "/css/pure-min.css"),
      link(relAttr := "stylesheet", hrefAttr := "/css/custom.css"),
    ),
    body(bodyContent *),
  )

  def navigationTemplate(middleContent: Html*): Html = headerTemplate(
    header(
      // Left side of header
      div(
        idAttr := "menuToggle",
        onClickAttr := "toggleMenu()",
        "☰",
      ),
      nav(
        idAttr := "menu",
        classAttr := List("navigation", "pure-menu"),
        ul(
          classAttr := List("pure-menu-list"),
          li(
            idAttr := "closeMenu",
            classAttr := List("pure-menu-item"),
            a(classAttr := List("pure-menu-link"), hrefAttr := "#", onClickAttr := "toggleMenu()", "Close Menu"),
          ),
          li(
            classAttr := List("pure-menu-item"),
            a(classAttr := List("pure-menu-link"), hrefAttr := "/recent", "Recent"),
          ),
          li(
            classAttr := List("pure-menu-item"),
            a(classAttr := List("pure-menu-link"), hrefAttr := "/albums", "Albums"),
          ),
          li(classAttr := List("pure-menu-item"), a(classAttr := List("pure-menu-link"), hrefAttr := "/tags", "Tags")),
        ),
      ),
      // Middle of header
      span(
        classAttr := List("pure-menu-heading"),
        styleAttr := Seq("text-align" -> "center"),
        "Raitis Kriķis photography",
      ),
      // Right side of header
    ),
    div(middleContent: _*),
    script(srcAttr := "js/main.js"),
  )

  def pageWithNavigation(content: Html*) = headerTemplate(navigationTemplate(content *))

  def photoBoxes(images: List[Image]): Html =
    images.map(image =>
      div(
        classAttr := List("photo-box", "animated-padding"),
        a(hrefAttr := "/image" + image.id, img(srcAttr := "/preview" + image.id)),
      ),
    )

  def imageGrid(images: List[AlbumEntry.Image]): Html =
    div(
      styleAttr := Seq("margin" -> "1em"),
      section(
        classAttr := List("columns"),
        photoBoxes(images),
      ),
    )

  def albumView(state: AppState)(rootAlbum: Option[Album], entries: List[AlbumEntry]): Html =
    val (albums, images) = entries.partitionAlbumEntries
    div(
      h1(rootAlbum.map(_.name).getOrElse("")),
      div(
        styleAttr := Seq("max-width" -> "120em"),
        div(
          albums.zipWithIndex.map { case (album, index) =>
            val coverId = state.resolveCoverImage(album.id).getOrElse("/not-found")
            val imagePart = div(
              classAttr := List("photo-box", "pure-u-2-3"),
              a(
                hrefAttr := "/album" + album.id,
                img(styleAttr := Seq("max-width" -> "60em"), srcAttr := s"/preview$coverId"),
              ),
            )
            val textPart = div(
              classAttr := List("pure-u-1-3"),
              a(hrefAttr := "/album" + album.id, h1(album.name)),
            )
            if (index % 2 == 0)
              div(styleAttr := Seq("background-color" -> "lightgrey"), imagePart, textPart)
            else
              div(styleAttr := Seq("background-color" -> "lightgrey"), textPart, imagePart)
          } *
        ),
      ),
      photoBoxes(images),
    )
