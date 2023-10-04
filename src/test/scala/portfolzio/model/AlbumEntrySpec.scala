package portfolzio.model

import portfolzio.model.AlbumEntry.IdSelector
import zio.Scope
import zio.test.Assertion.*
import zio.test.{test, *}

object AlbumEntrySpec extends ZIOSpecDefault {

  val TestEntries = List(
    "/dir/img",
    "/dir/img2",
    "/dir2/dir3/img",
    "/dir2/dir4/img",
    "/dir2/dir4/dir5/img",
  ).map(AlbumEntry.Id.safe)

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("IdSelectorSpec")(
    test("IdSelector with an absolute path matches specific IDs") {
      val selector = IdSelector.fromString("/dir/img")
      val matches = selector.findMatches("/otherDir", TestEntries)
      assert(matches)(equalTo(List("/dir/img")))
    },
    test("IdSelector with a relative path matches specific IDs") {
      val selector = IdSelector.fromString("img")
      val matchesDir = selector.findMatches("/dir", TestEntries)
      val matchesDir2 = selector.findMatches("/dir2", TestEntries)
      assert(matchesDir)(equalTo(List("/dir/img"))) && assert(matchesDir2)(equalTo(List.empty))
    },
    test("IdSelector with an absolute path with a wildcard matches relevant IDs") {
      val selector = IdSelector.fromString("/dir2/dir4/*")
      val matches = selector.findMatches("/dir", TestEntries)
      assert(matches)(equalTo(List("/dir2/dir4/img", "/dir2/dir4/dir5/img")))
    },
    test("IdSelector with a wildcard matches relevant IDs relative to it") {
      val selector = IdSelector.fromString("*")
      val matchesDir2 = selector.findMatches("/dir2", TestEntries)
      val matchesDir4 = selector.findMatches("/dir2/dir4", TestEntries)
      assert(matchesDir2)(equalTo(List("/dir2/dir3/img", "/dir2/dir4/img", "/dir2/dir4/dir5/img"))) &&
        assert(matchesDir4)(equalTo(List("/dir2/dir4/img", "/dir2/dir4/dir5/img")))
    },
    test("IdSelector /* matches all IDs") {
      val selector = IdSelector.fromString("/*")
      val matches = selector.findMatches("/dir2", TestEntries)
      assert(matches)(equalTo(TestEntries))
    },
  )
}
