package website.html

import zio.http.html._

def template(bodyContent: Html): Html = html(
  head(
    // Include Unpoly before your own stylesheets and JavaScripts
    link(relAttr := "stylesheet", hrefAttr := "css/unpoly.min.css"),
    script( srcAttr := "js/unpoly.min.js"),
  ),
  body(bodyContent),
)
