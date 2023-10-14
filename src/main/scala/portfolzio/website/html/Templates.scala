package portfolzio.website.html

import portfolzio.AppState
import portfolzio.model.AlbumEntry
import portfolzio.model.AlbumEntry.*
import portfolzio.util.Regex.DelimiterRegex
import portfolzio.website.html.CustomAttributes.*
import zio.http.html.*

class Templates(titleText: String):

  def altTextFromId(imageId: AlbumEntry.Id): Html =
    val text = "An image of " + imageId.lastPart.replaceAll(DelimiterRegex.regex, " ") + "."
    altAttr := text

  def headerTemplate(bodyContent: Html*): Html = html(
    head(
      meta(charsetAttr := "utf-8"),
      meta(nameAttr := "viewport", contentAttr := "width=device-width, initial-scale=1.0"),
      title(titleText),
      link(relAttr := "icon", hrefAttr := "/img/favicon.svg"),
      // Include Unpoly before your own stylesheets and JavaScripts
      link(relAttr := "stylesheet", hrefAttr := "/css/unpoly.min.css"),
      link(relAttr := "stylesheet", hrefAttr := "/css/pure-min.css"),
      link(relAttr := "stylesheet", hrefAttr := "/css/custom.css"),
      link(relAttr := "stylesheet", hrefAttr := "https://fonts.googleapis.com/css?family=Raleway"),
      // Include Unpoly before your own stylesheets and JavaScripts
      script(srcAttr := "/js/unpoly.min.js"),
      script(srcAttr := "/js/main.js"),
    ),
    body(bodyContent *),
  )

  def pageWithNavigation(content: Html*): Html = headerTemplate(
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
            a(
              classAttr := List("pure-menu-link"),
              upInstantAttr,
              hrefAttr := "/recent",
              onClickAttr := "toggleMenu()",
              "Recent",
            ),
          ),
          li(
            classAttr := List("pure-menu-item"),
            a(
              classAttr := List("pure-menu-link"),
              upInstantAttr,
              hrefAttr := "/albums",
              onClickAttr := "toggleMenu()",
              "Albums",
            ),
          ),
          li(
            classAttr := List("pure-menu-item"),
            a(
              classAttr := List("pure-menu-link"),
              upPreloadAttr,
              hrefAttr := "/tags",
              onClickAttr := "toggleMenu()",
              "Tags",
            ),
          ),
        ),
      ),
      // Middle of header
      span(
        classAttr := List("pure-menu-heading"),
        styleAttr := Seq("text-align" -> "center"),
        titleText,
      ),
      // Right side of header
    ),
    main(content: _*),
  )

  def photoBoxes(images: List[Image]): Html =
    images.map(image =>
      div(
        classAttr := List("photo-box", "animated-padding"),
        a(
          hrefAttr := "/image" + image.id,
          upLayerNewAttr,
          upSizeAttr := "large",
          img(srcAttr := "/preview" + image.id, altTextFromId(image.id)),
        ),
      ),
    )

  def imageGrid(images: List[AlbumEntry.Image]): Html =
    div(
      styleAttr := Seq("margin" -> "1em"),
      div(
        classAttr := List("columns"),
        photoBoxes(images),
      ),
    )

  def centeredTopText(text: String): Html = h1(
    styleAttr := Seq("text-align" -> "center", "font-family" -> "'Raleway', sans-serif", "font-weight" -> "300"),
    text,
  )

  def albumView(state: AppState)(rootAlbum: Option[Album], entries: List[AlbumEntry]): Html =
    val MaxWidth = "120em"
    val (albums, images) = entries.partitionAlbumEntries
    div(
      centeredTopText(rootAlbum.map(_.name).getOrElse("")),
      div(
        classAttr := List("center"),
        styleAttr := Seq("max-width" -> MaxWidth),
        div(
          albums.sortBy(_.name).zipWithIndex.map { case (album, index) =>
            val coverId = state.resolveCoverImage(album.id)

            def imagePart(align: "left" | "right") = div(
              styleAttr := Seq("align" -> align, "flex-shrink" -> "1"),
              classAttr := List("photo-box"),
              img(
                styleAttr := Seq("max-height" -> "36em"),
                srcAttr := s"/preview${ coverId.getOrElse("/not-found") }",
                coverId.fold(altAttr := "Image not found.")(altTextFromId),
              ),
            )

            val textPart = div(
              styleAttr := Seq(
                "width" -> "48em",
                "flex-grow" -> "1",
                "text-align" -> "center",
                "font-family" -> "'Raleway', sans-serif",
              ),
              upTransitionAttr := "move-left",
              h1(
                classAttr := List("album-link-text"),
                album.name,
              ),
            )
            val containerStyles =
              styleAttr := Seq(
                "display" -> "flex",
                "align-items" -> "center",
                "background-color" -> "lightgrey",
                "margin" -> "1em",
              )
            a(
              upTransitionAttr := "move-left",
              styleAttr := Seq(
                "color" -> "black",
                "text-decoration" -> "none",
              ),
              hrefAttr := "/album" + album.id,
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
                ),
            )
          } *
        ),
      ),
      div(
        classAttr := List("center"),
        styleAttr := Seq("max-width" -> MaxWidth),
        photoBoxes(images.sortBy(_.info.time)),
      ),
    )
