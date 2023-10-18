package portfolzio.model

import portfolzio.*
import portfolzio.model.AlbumEntry.IdSelector
import zio.Scope
import zio.test.Assertion.*
import zio.test.{test, *}

object AlbumEntrySpec extends ZIOSpecDefault {

  val TestEntries = List(
    img("/dir/img"),
    img("/dir/img2"),
    img("/dir2/dir3/img"),
    img("/dir2/dir4/img"),
    img("/dir2/dir4/dir5/img"),
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("IdSelectorSpec")(
    test("IdSelector with an absolute path matches specific IDs") {
      val selector = IdSelector.fromString("/dir/img")
      val matches = IdSelector.findMatches("/otherDir")(List(selector), TestEntries)
      assert(matches)(equalTo(List(img("/dir/img"))))
    },
    test("IdSelector with a relative path matches specific IDs") {
      val selector = IdSelector.fromString("img")
      val matchesDir = IdSelector.findMatches("/dir")(List(selector), TestEntries)
      val matchesDir2 = IdSelector.findMatches("/dir2")(List(selector), TestEntries)
      assert(matchesDir)(equalTo(List(img("/dir/img")))) && assert(matchesDir2)(equalTo(List.empty))
    },
    test("IdSelector with an absolute path with a wildcard matches relevant IDs") {
      val selector = IdSelector.fromString("/dir2/dir4/*")
      val matches = IdSelector.findMatches("/dir")(List(selector), TestEntries)
      assert(matches)(equalTo(List(img("/dir2/dir4/img"), img("/dir2/dir4/dir5/img"))))
    },
    test("IdSelector with a wildcard matches relevant IDs relative to it") {
      val selector = IdSelector.fromString("*")
      val matchesDir2 = IdSelector.findMatches("/dir2")(List(selector), TestEntries)
      val matchesDir4 = IdSelector.findMatches("/dir2/dir4")(List(selector), TestEntries)
      assert(matchesDir2)(equalTo(List(img("/dir2/dir3/img"), img("/dir2/dir4/img"), img("/dir2/dir4/dir5/img")))) &&
        assert(matchesDir4)(equalTo(List(img("/dir2/dir4/img"), img("/dir2/dir4/dir5/img"))))
    },
    test("IdSelector /* matches all IDs regardless of basepath") {
      val selector = IdSelector.fromString("/*")
      val matches = IdSelector.findMatches("/dir2")(List(selector), TestEntries)
      assert(matches)(equalTo(TestEntries))
    },
    test("findMatches() matches items that match the first selector before the rest") {
      val firstItem = img("/dir2/dir3/img")
      val selectors = List(IdSelector.fromString(firstItem.id.value), IdSelector.fromString("*"))
      val matches = IdSelector.findMatches("/")(selectors, TestEntries)
      assert(matches.length)(equalTo(TestEntries.length)) && assert(matches.headOption)(isSome(equalTo(firstItem)))
    },
    test("findMatches() matches items that match with a tag") {
      val item = img("img").copy(info = ImageInfo(tags = Some(List("banana"))))
      val selectors = List(IdSelector.fromString("tag:banana"))
      val matches = IdSelector.findMatches("/")(selectors, List(item))
      assert(matches)(equalTo(List(item)))
    },
  )
}
