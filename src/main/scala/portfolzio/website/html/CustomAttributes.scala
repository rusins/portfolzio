package portfolzio.website.html

import zio.http.template.Attributes.PartialAttribute
import zio.http.template.{Dom, Html}

object CustomAttributes {

  // For open graph meta tag
  val propertyAttr: PartialAttribute[String] = PartialAttribute("property")

  // Unpoly attributes
  val upFollowAttr: Dom = Dom.attr("up-follow", "")
  val upIgnoreHistoryAttr: Dom = Dom.attr("up-history", "false")
  val upDontCacheAttr: Dom = Dom.attr("up-cache", "false")
  val upInstantAttr: Dom = Dom.attr("up-instant", "")
  val upPreloadAttr: Dom = Dom.attr("up-preload", "")

  val upLayerAttr: PartialAttribute[String] = PartialAttribute("up-layer")
  val upLayerNewAttr: Html = upLayerAttr := "new"
  val upSizeAttr: PartialAttribute[String] = PartialAttribute("up-size")

  val upTransitionAttr: PartialAttribute[String] = PartialAttribute("up-transition")

  val upExpandAttr: PartialAttribute[String] = PartialAttribute("up-expand")
}
