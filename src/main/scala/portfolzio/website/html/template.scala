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
  ),
  body(bodyContent *),
)

def navigationTemplate(middleContent: Html): Html = headerTemplate(
  div(
    idAttr := "menuToggle",
    onClickAttr := "toggleMenu()",
    styleAttr := Seq(
      "position" -> "absolute",
      "left" -> "1em",
      "top" -> "1em",
      "cursor" -> "pointer",
      "display" -> "none",
    ),
    "☰",
  ),
  header(
    styleAttr := Seq("background-color" -> "lightgrey", "padding" -> "1em"),
    // Left side of header
    nav(
      classAttr := List("navigation", "pure-menu", "pure-menu-horizontal"),
      styleAttr := Seq("position" -> "absolute", "left" -> "1em"),
      ul(
        idAttr := "menu",
        classAttr := List("pure-menu-list"),
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
    span(
      classAttr := List("pure-menu-heading"),
      styleAttr := Seq("text-align" -> "center"),
      "Raitis Kriķis photography",
    ),
  ),
  middleContent,
)
