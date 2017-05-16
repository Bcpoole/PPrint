package test.pprint

import pprint.PPrinter
import utest._

import scala.annotation.tailrec
import scala.collection.SortedMap

object VerticalTests extends TestSuite{

  class C(){
    var counter = 0
    override def toString = {
      counter += 1

      // Make sure the fact that this fella renders ansi colors
      // as part of toString doesn't muck up our computation of width/height
      fansi.Color.Red("C").toString
    }
  }


  val tests = TestSuite{
    'truncatedAttrs{
      def check(input: Iterator[String],
                width: Int,
                height: Int,
                expectedCompletedLineCount: Int,
                expectedLastLineLength: Int) = {

        val t = new pprint.Truncated(
          input.map(fansi.Str(_)),
          width,
          height
        )

        t.foreach(_ => ())

        val completedLineCount = t.completedLineCount
        val lastLineLength = t.lastLineLength
        assert(
          completedLineCount == expectedCompletedLineCount,
          lastLineLength == expectedLastLineLength
        )
      }
      check(
        Iterator("1234567"),
        width = 50, height = 50,
        expectedCompletedLineCount = 0,
        expectedLastLineLength = 7
      )
      check(
        Iterator("1234567"),
        width = 7, height = 50,
        expectedCompletedLineCount = 0,
        expectedLastLineLength = 7
      )
      check(
        Iterator("1234567"),
        width = 6, height = 50,
        expectedCompletedLineCount = 1,
        expectedLastLineLength = 1
      )
      check(
        Iterator("12", "34", "5", "67"),
        width = 6, height = 50,
        expectedCompletedLineCount = 1,
        expectedLastLineLength = 1
      )
    }
    'config{
      val res1 = pprint.apply(List(1, 2, 3)).plainText
      assert(res1 == "List(1, 2, 3)")

      val res2 = pprint.copy(defaultWidth = 6).apply(List(1, 2, 3), width = 6).plainText
      assert(res2 == "List(\n  1,\n  2,\n  3\n)")

      val res3 = pprint.copy(defaultWidth = 6).apply(List(1, 2, 3)).plainText
      assert(res3 == "List(\n  1,\n  2,\n  3\n)")

      val res4 = pprint.copy(defaultWidth = 6).copy(defaultIndent = 4).apply(List(1, 2, 3)).plainText
      assert(res4 == "List(\n    1,\n    2,\n    3\n)")

      val res5 = pprint.copy(additionalHandlers = {
        case x: Int => pprint.Tree.Literal((-x).toString)
      }).apply(List(1, 2, 3)).plainText

      assert(res5 == "List(-1, -2, -3)")
    }

    'Laziness{
      val Check = new Check(width = 20, height = 5)
      'list{
        'Horizontal {
          val C = new C
          Check(
            List.fill(4)(C),
            """List(C, C, C, C)"""
          )
          val counter = C.counter
          // https://github.com/scala-js/scala-js/issues/2953
          if (sys.props("java.vm.name") != "Scala.js") {
            assert(counter == 4)
          }
        }
        'Vertical{
          val C = new C
          Check(
            List.fill(100)(C),
            """List(
              |  C,
              |  C,
              |  C,
              |...""".stripMargin
          )
          //          10        20
          //List(C, C, C, C, C, ) ....

          // 5 horizontal renders before deciding it can't fit,
          // then it re-uses those renders and lays them out
          // vertically, taking the first 3 before being cut off
          val counter = C.counter
          // https://github.com/scala-js/scala-js/issues/2953
          if (sys.props("java.vm.name") != "Scala.js"){
            assert(counter == 5)
          }
        }
      }

      'map{
        'Horizontal{
          val C = new C
          Check(
            SortedMap(List.tabulate(2)(_ -> C):_*),
            """Map(0 -> C, 1 -> C)"""
          )
          val counter = C.counter
          // https://github.com/scala-js/scala-js/issues/2953
          if (sys.props("java.vm.name") != "Scala.js") {
            assert(counter == 2)
          }
        }
        'Vertical{
          val C = new C
          Check(
            SortedMap(List.tabulate(100)(_ -> C):_*),
            """Map(
              |  0 -> C,
              |  1 -> C,
              |  2 -> C,
              |...""".stripMargin
          )
          //          10        20
          //Map(0 -> C, 1 -> C, 2 -> C
          //                    ^ break

          // 2 horizontal renders (and change) before deciding it can't fit
          // 4 vertical renders before overshooting
          val count = C.counter
          // https://github.com/scala-js/scala-js/issues/2953
          if (sys.props("java.vm.name") != "Scala.js") {
            assert(count == 4)
          }
        }
      }
    }
    'Vertical{

      val Check = new Check(width = 25, renderTwice = true)
      'singleNested {
        * - new Check(width = 5)(
          List(1, 2, 3),
          """List(
            |  1,
            |  2,
            |  3
            |)
          """.stripMargin
        )
        * - Check(
          List("12", "12", "12"),
          """List("12", "12", "12")"""
        )
        * - Check(
          List("123", "123", "123"),
          """List("123", "123", "123")"""
        )
        * - Check(
          List("1234", "123", "123"),
          """List(
            |  "1234",
            |  "123",
            |  "123"
            |)""".stripMargin
        )
        * - Check(
          Map(1 -> 2, 3 -> 4),
          """Map(1 -> 2, 3 -> 4)"""
        )
        * - Check(
          Map(List(1, 2) -> List(3, 4), List(5, 6) -> List(7, 8)),
          """Map(
            |  List(1, 2) -> List(3, 4),
            |  List(5, 6) -> List(7, 8)
            |)""".stripMargin
        )

        * - Check(
          Map(
            List(123, 456, 789, 123, 456) -> List(3, 4, 3, 4),
            List(5, 6) -> List(7, 8)
          ),
          """Map(
            |  List(
            |    123,
            |    456,
            |    789,
            |    123,
            |    456
            |  ) -> List(3, 4, 3, 4),
            |  List(5, 6) -> List(7, 8)
            |)""".stripMargin
        )

        * - Check(
          Map(
            List(5, 6) -> List(7, 8),
            List(123, 456, 789, 123, 456) -> List(123, 456, 789, 123, 456)
          ),
          """Map(
            |  List(5, 6) -> List(7, 8),
            |  List(
            |    123,
            |    456,
            |    789,
            |    123,
            |    456
            |  ) -> List(
            |    123,
            |    456,
            |    789,
            |    123,
            |    456
            |  )
            |)""".stripMargin
        )

        * - Check(
          List("12345", "12345", "12345"),
          """List(
            |  "12345",
            |  "12345",
            |  "12345"
            |)""".stripMargin
        )
        * - Check(
          Foo(123, Seq("hello world", "moo")),
          """Foo(
            |  123,
            |  List(
            |    "hello world",
            |    "moo"
            |  )
            |)""".stripMargin
        )
        * - Check(
          Foo(123, Seq("moo")),
          """Foo(123, List("moo"))""".stripMargin
        )

      }
      'doubleNested{

        * - Check(
          List(Seq("omg", "omg"), Seq("mgg", "mgg"), Seq("ggx", "ggx")),
          """List(
            |  List("omg", "omg"),
            |  List("mgg", "mgg"),
            |  List("ggx", "ggx")
            |)""".stripMargin
        )
        * - Check(
          List(Seq("omg", "omg", "omg", "omg"), Seq("mgg", "mgg"), Seq("ggx", "ggx")),
          """List(
            |  List(
            |    "omg",
            |    "omg",
            |    "omg",
            |    "omg"
            |  ),
            |  List("mgg", "mgg"),
            |  List("ggx", "ggx")
            |)""".stripMargin
        )
        * - Check(
          List(
            Seq(
              Seq("mgg", "mgg", "lols"),
              Seq("mgg", "mgg")
            ),
            Seq(
              Seq("ggx", "ggx"),
              Seq("ggx", "ggx", "wtfx")
            )
          ),
          """List(
            |  List(
            |    List(
            |      "mgg",
            |      "mgg",
            |      "lols"
            |    ),
            |    List("mgg", "mgg")
            |  ),
            |  List(
            |    List("ggx", "ggx"),
            |    List(
            |      "ggx",
            |      "ggx",
            |      "wtfx"
            |    )
            |  )
            |)""".stripMargin
        )
        * - Check(
          FooG(Vector(FooG(Array(Foo(123, Nil)), Nil)), Nil),
          """FooG(
            |  Vector(
            |    FooG(
            |      Array(
            |        Foo(123, List())
            |      ),
            |      List()
            |    )
            |  ),
            |  List()
            |)
          """.stripMargin
        )
        * - Check(
          FooG(FooG(Seq(Foo(3, Nil)), Nil), Nil),
          """FooG(
            |  FooG(
            |    List(Foo(3, List())),
            |    List()
            |  ),
            |  List()
            |)""".stripMargin
        )
      }
    }
    'traited {
      val Check = new Check()
      Check(Nested.ODef.Foo(2, "ba"), "Foo(2, \"ba\")")
      Check(Nested.CDef.Foo(2, "ba"), "Foo(2, \"ba\")")
    }
    'Color{
      def count(haystack: Iterator[fansi.Str], needles: (String, Int)*) = {
        val str = haystack.map(_.render).mkString
        for ((needle, expected) <- needles){
          val count = countSubstring(str, needle)

          assert(count == expected)
        }
      }
      def countSubstring(str1:String, str2:String):Int={
        @tailrec def count(pos:Int, c:Int):Int={
          val idx=str1 indexOf(str2, pos)
          if(idx == -1) c else count(idx+str2.size, c+1)
        }
        count(0,0)
      }

      import Console._
      val cReset = fansi.Color.Reset.escape

      * - count(PPrinter.Color.tokenize(123), GREEN -> 1, cReset -> 1)
      * - count(PPrinter.Color.tokenize(""), GREEN -> 1, cReset -> 1)
      * - count(PPrinter.Color.tokenize(Seq(1, 2, 3)), GREEN -> 3, YELLOW -> 1, cReset -> 4)
      * - count(
        PPrinter.Color.tokenize(Map(1 -> Nil, 2 -> Seq(" "), 3 -> Seq("   "))),
        GREEN -> 5, YELLOW -> 4, cReset -> 9
      )
    }

    'Truncation{
//      'test{
//        Check()
//      }
      'longNoTruncation{
        val Check = new Check()
        * - Check("a" * 10000,"\""+"a" * 10000+"\"")
        * - Check(
          List.fill(30)(100),
          """List(
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100,
            |  100
            |)""".stripMargin
        )
      }

      'shortNonTruncated{
        val Check = new Check(height = 15)
        * - Check("a"*1000, "\"" + "a"*1000 + "\"")
        * - Check(List(1,2,3,4), "List(1, 2, 3, 4)")
        * - Check(
          List.fill(13)("asdfghjklqwertz"),
          """List(
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz",
            |  "asdfghjklqwertz"
            |)
          """.stripMargin
        )
      }

      'shortLinesTruncated{
        val Check = new Check(height = 15)
        * - Check(
          List.fill(15)("foobarbaz"),
          """List(
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |...""".stripMargin
        )
        * - Check(
          List.fill(150)("foobarbaz"),
          """List(
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |  "foobarbaz",
            |...""".stripMargin
        )
      }

      'longLineTruncated{
        // These print out one long line, but at the width that the
        // pretty-printer is configured to, it (including any trailing ...)
        // wraps to fit within the desired width and height
        * - {
          val Check = new Check(width = 5, height = 3)
          Check(
            "a" * 13,
            "\"aaaa" +
             "aaaaa" +
             "aaaa\""
          )
        }
        * - {
          val Check = new Check(width = 5, height = 3)
          Check(
            "a" * 1000,
            "\"aaaa" +
             "aaaaa" +
             "..."
          )
        }
        * - {
          val Check = new Check(width = 60, height = 5)
          Check(
            "a" * 1000,
            "\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
             "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
             "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
             "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+
             "..."
          )
        }
      }

      'stream{
        val Check = new Check(height = 5)
        Check(
          Stream.continually("foo"),
          """Stream(
            |  "foo",
            |  "foo",
            |  "foo",
            |...
          """.stripMargin
        )
      }
    }

    'wrappedLines{
      val Check = new Check(width = 8, height = 5)

      Check(
        "1234567890\n"*10,
        "\"\"\"1234567890\n1234567890\n..."
      )
      // The result looks like 10 wide 3 deep, but because of the wrapping
      // (maxWidth = 8) it is actually 8 wide and 5 deep.
    }
  }


}
