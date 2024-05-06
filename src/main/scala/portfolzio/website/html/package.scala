package portfolzio.website

import zio.http.template.*

package object html {

  implicit def optionToHtml(o: Option[Html]): Html = o.getOrElse(Html.fromUnit(()))

  implicit def sequenceToHtml(seq: Seq[Html]): Html = seq.headOption match
    case Some(head) => head ++ seq.tail
    case None       => Html.fromUnit(())
}
