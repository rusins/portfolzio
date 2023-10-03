package portfolzio.website.html

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
    link(relAttr := "stylesheet", hrefAttr := "css/custom.css"),
  ),
  body(bodyContent *),
)

def navigationTemplate(middleContent: Html): Html = headerTemplate(
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
  middleContent,
  script(srcAttr := "js/main.js"),
)
