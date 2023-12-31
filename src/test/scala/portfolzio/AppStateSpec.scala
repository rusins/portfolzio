package portfolzio

import portfolzio.model.AlbumEntry
import zio.Scope
import zio.test.Assertion.*
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, test, *}

object AppStateSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("AppStateSpec")(
    test("AppState correctly identifies orphans and builds the child tree") {
      val img1 = img("/dir/img1")
      val img2 = img("/dir/img2")
      val img3 = img("/dir2/img3")
      val img4 = img("/img4")
      val alb1 = album("/alb1", List("/dir/*", "/img4"))
      val alb2 = album("/dir/alb2", List("/dir/img1"))
      val entries = List(img1, img2, img3, img4, alb1, alb2)
      for appState <- AppState.fromRawEntries(entries)
        yield assertTrue(appState.orphans.iterator.sameElements(List(img3, alb1))) &&
          assert(appState.children.get(alb1.id))(isSome(hasSameElementsDistinct(List(img1, img2, alb2, img4)))) &&
          assert(appState.children.get(alb2.id))(isSome(hasSameElementsDistinct(List(img1))))
    },
    test("AppState does not include albums matching themselves") {
      val alb1 = album("/alb1", List("*"))
      val entries = List(alb1)
      for appState <- AppState.fromRawEntries(entries)
        yield assert(appState.children.get(alb1.id))(isNone)
    },
    test("AppState does not duplicate child entries") {
      val img1 = img("/dir/img1")
      val alb1 = album("/alb1", List("/dir/*", "/dir/img1"))
      val entries = List(img1, alb1)
      for appState <- AppState.fromRawEntries(entries)
        yield assert(appState.children.get(alb1.id))(isSome(equalTo(List(img1))))
    },
    test(".resolveCoverImage correctly dives head first into a tree of albums to find the first image entry") {
      val img1 = img("/img1")
      val alb1 = album("/alb1", List("alb2"))
      val alb2 = album("/alb2", List("/dir/*"))
      val alb3 = album("/dir/alb3", List("/img1"))
      val entries = List(img1, alb1, alb2, alb3)
      for appState <- AppState.fromRawEntries(entries)
        yield assert(appState.resolveCoverImage(alb3.id))(isSome(equalTo(img1.id)))
    },
    test(".resolveCoverImage does not get stuck in a loop") {
      val alb1 = album("/alb1", List("alb2"))
      val alb2 = album("/alb2", List("alb1"))
      val alb3 = album("/alb3", List("/alb1"))
      val entries = List(alb1, alb2, alb3)
      for appState <- AppState.fromRawEntries(entries)
        yield assert(appState.resolveCoverImage(alb3.id))(isNone)
    },
  )
}
