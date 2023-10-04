package portfolzio.website.html

import portfolzio.model.AlbumEntry
import portfolzio.website.html.Templates.*
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute

object Pages {

  def tagsPage(tags: List[String]) = pageWithNavigation(
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
}
