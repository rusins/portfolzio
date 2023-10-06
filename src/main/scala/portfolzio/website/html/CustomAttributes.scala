package portfolzio.website.html

import zio.http.html.Attributes.PartialAttribute
import zio.http.html.{Dom, Html}

object CustomAttributes {

  // Unpoly attributes
  val upFollowAttr: Dom = Dom.attr("up-follow", "")
  val upInstantAttr: Dom = Dom.attr("up-instant", "")
  val upPreloadAttr: Dom = Dom.attr("up-preload", "")

  val upLayerAttr: PartialAttribute[String] = PartialAttribute("up-layer")
  val upLayerNewAttr: Html = upLayerAttr := "new"
  val upSizeAttr: PartialAttribute[String] = PartialAttribute("up-size")

  val upTransitionAttr: PartialAttribute[String] = PartialAttribute("up-transition")

  val upExpandAttr: PartialAttribute[String] = PartialAttribute("up-expand")
}
