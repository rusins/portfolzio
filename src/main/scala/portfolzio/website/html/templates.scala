package portfolzio.website.html

import portfolzio.model.AlbumEntry
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute

def headerTemplate(bodyContent: Html*): Html = html(
  head(
    meta(charsetAttr := "utf-8"),
    meta(nameAttr := "viewport", contentAttr := "width=device-width, initial-scale=1.0"),
    // Include Unpoly before your own stylesheets and JavaScripts
    link(relAttr := "stylesheet", hrefAttr := "css/unpoly.min.css"),
    script(srcAttr := "js/unpoly.min.js"),
    link(relAttr := "stylesheet", hrefAttr := "css/pure-min.css"),
    link(relAttr := "stylesheet", hrefAttr := "css/pure-grids-responsive.css"),
    link(relAttr := "stylesheet", hrefAttr := "css/custom.css"),
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

def imageMozaic(images: List[AlbumEntry.Image]): Html =
  div(
    classAttr := List("pure-g"),
    styleAttr := Seq("margin" -> "1em"),
    images.map(image =>
      div(
        classAttr := List("pure-u-1", "pure-u-lg-1-2", "pure-u-xxl-1-3"),
        div(
          classAttr := List("photo-box", "animated-padding"),
          a(
            hrefAttr := "/image" + image.id.toString,
            img(classAttr := List("pure-img"), srcAttr := "/preview" + image.id.toString),
          ),
        ),
      )
    ),
  )
