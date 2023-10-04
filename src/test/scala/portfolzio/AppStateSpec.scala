package portfolzio

import portfolzio.model.AlbumEntry.IdSelector
import portfolzio.model.{AlbumEntry, ImageInfo}
import zio.Scope
import zio.prelude.NonEmptyList
import zio.test.Assertion.*
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, test, *}

import java.nio.file.Paths

object AppStateSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("AppStateSpec")(
    test("AppState correctly identifies orphans and builds the child tree") {
      def img(id: String) = AlbumEntry.Image(
        AlbumEntry.Id.unsafe(id),
        ImageInfo(),
        imageFiles = NonEmptyList(Paths.get("/")),
        rawFiles = List.empty,
      )

      def album(id: String, children: List[String]) = AlbumEntry.Album(
        AlbumEntry.Id.unsafe(id),
        children.map(IdSelector.fromString),
      )

      val img1 = img("/dir/img1")
      val img2 = img("/dir/img2")
      val img3 = img("/dir2/img3")
      val alb1 = album("/alb1", List("/dir/*"))
      val alb2 = album("/dir/alb2", List("/dir/img1"))
      val entries = List(img1, img2, img3, alb1, alb2)
      for appState <- AppState.fromRawEntries(entries)
        yield assertTrue(appState.orphans.iterator.sameElements(List(img3, alb1).map(_.id))) &&
          assert(appState.children.get(alb1.id))(isSome(hasSameElementsDistinct(List(img1, img2, alb2)))) &&
          assert(appState.children.get(alb2.id))(isSome(hasSameElementsDistinct(List(img1))))
    }
  )
}
