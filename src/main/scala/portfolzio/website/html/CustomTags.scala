package portfolzio.website.html

import portfolzio.website.html.CustomAttributes.propertyAttr
import zio.http.html.*
object CustomTags {

  /** Open Graph metadata. https://ogp.me/ */
  case class OGTags(
    title: String,
    staticUrl: String,
    imageUrl: String,
    description: Option[String] = None,
  ) {
    def tags: Html = Seq[Html](
      meta(propertyAttr := "og:title", contentAttr := title),
      meta(propertyAttr := "og:type", contentAttr := "website"),
      meta(propertyAttr := "og:url", contentAttr := staticUrl),
      meta(propertyAttr := "og:image", contentAttr := imageUrl),
      description.map(descriptionText => meta(propertyAttr := "og:description", contentAttr := descriptionText)),
      meta(nameAttr := "twitter:card", contentAttr := "summary_large_image"),
      meta(nameAttr := "twitter:title", contentAttr := title),
      description.map(descriptionText => meta(nameAttr := "twitter:description", contentAttr := descriptionText)),
      meta(nameAttr := "twitter:image", contentAttr := imageUrl),
    )
  }

}
