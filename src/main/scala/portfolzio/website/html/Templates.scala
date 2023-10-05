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
      link(relAttr := "stylesheet", hrefAttr := "https://fonts.googleapis.com/css?family=Raleway"),
      script(srcAttr := "/js/main.js"),
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
  )

  def pageWithNavigation(content: Html*): Html = headerTemplate(navigationTemplate(content *))

  def photoBoxes(images: List[Image]): Html =
    images.map(image =>
      div(
        classAttr := List("photo-box", "animated-padding"),
        a(
          hrefAttr := "/image" + image.id,
          img(srcAttr := "/preview" + image.id),
        ),
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
    val MaxWidth = "120em"
    val (albums, images) = entries.partitionAlbumEntries
    div(
      h1(
        styleAttr := Seq("text-align" -> "center", "font-family" -> "'Raleway', sans-serif", "font-weight" -> "300"),
        rootAlbum.map(_.name).getOrElse(""),
      ),
      div(
        classAttr := List("center"),
        styleAttr := Seq("max-width" -> MaxWidth),
        div(
          albums.zipWithIndex.map { case (album, index) =>
            val coverId = state.resolveCoverImage(album.id).getOrElse("/not-found")

            def imagePart(align: "left" | "right") = div(
              styleAttr := Seq("align" -> align, "flex-shrink" -> "1"),
              classAttr := List("photo-box"),
              a(
                hrefAttr := "/album" + album.id,
                img(
                  styleAttr := Seq("max-height" -> "36em"),
                  srcAttr := s"/preview$coverId",
                ),
              ),
            )

            val textPart = div(
              styleAttr := Seq(
                "width" -> "48em",
                "flex-grow" -> "1",
                "text-align" -> "center",
                "font-family" -> "'Raleway', sans-serif",
              ),
              a(
                styleAttr := Seq("color" -> "black", "text-decoration" -> "none"),
                hrefAttr := "/album" + album.id,
                h1(
                  styleAttr := Seq("font-size" -> "4em"),
                  album.name,
                ),
              ),
            )
            val containerStyles =
              styleAttr := Seq(
                "display" -> "flex",
                "align-items" -> "center",
                "background-color" -> "lightgrey",
                "margin" -> "1em",
              )
            if (index % 2 == 0)
              div(
                containerStyles,
                imagePart(align = "left"),
                textPart,
              )
            else
              div(
                containerStyles,
                textPart,
                imagePart(align = "right"),
              )
          } *
        ),
      ),
      div(
        classAttr := List("center"),
        styleAttr := Seq("max-width" -> MaxWidth),
        photoBoxes(images),
      ),
    )
