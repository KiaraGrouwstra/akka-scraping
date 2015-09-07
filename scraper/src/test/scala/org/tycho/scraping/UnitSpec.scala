package org.tycho.scraping
import org.scalatest._

//abstract class UnitSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors with BeforeAndAfterEach with GivenWhenThen {
//abstract class UnitSpec extends FlatSpec with Matchers {
trait UnitSpec extends FlatSpec with Matchers {

  import org.slf4j.{Logger, LoggerFactory}
  protected lazy val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  
}

/*

test scope:
"my thing" should "work" in {}
Given("my thing")
it should "work" in {}
ignore should "work" in {}

tests:
http://www.scalatest.org/user_guide/matchers_quick_reference
lol should be/equal ("lol")
assert(lol == "lol", "hint")
assertResult(2, "clue") { 1 }
arr should have size/length 3
string should startWith/endWith/include ("Hello")
string should startWith/.../fullyMatch regex "Hel*o".r 
ditto regex "Hel*o".r 
num should (be >= 1 and be <= 10)
sevenDotOh should be (6.9 plusOrMinus 0.2)
num should be (odd)
emptySet should be (a/an) ('empty)  //Accesses boolean .empty or .isEmpty dynamically
temp should be a file
book should have (
  'title ("Programming in Scala"),
  title ("Programming in Scala"),
  ...
)
val beOddAsInt = beOdd compose { (s: String) => s.toInt }
"3" should beOddAsInt
val beOdd = Matcher { (left: Int) =>
  MatchResult(
    left % 2 == 1,
    left + " was not odd",
    left + " was odd"
  )
}
3 should beOdd
intercept[NoSuchElementException] {
  Set.empty.head
}
fail("unimplemented")

custom tags:
object SlowTest extends Tag("com.mycompany.tags.SlowTest")
"a" must "b" taggedAs(SlowTest) in {}

using fixtures to prepare variables:
def fixt = new {
  val a = new A
}
val f = fixt
f.a
or...
import f._
a

or if you need many used in different places...
trait Builder {
  val builder = ...
}
trait Buffer {
  val buffer = ...
}
it should "work" in new Builder with Buffer {}

customize testing process:
override def withFixture(test: NoArgTest) = {
  // Perform setup
  super.withFixture(test)   // Invoke the test function
  match {
    case failed: Failed =>
      val currDir = new File(".")
      val fileNames = currDir.list()
      info("Dir snapshot: " + fileNames.mkString(", "))
      failed
    case other => { other }
  }
}

for argumented fixtures:
object DbServer {
  def createDb(...)
}
import DbServer._

allow importing multiple variables including prep and cleanup:
case class FixtureParam(file: File, writer: FileWriter)
def withFixture(test: OneArgTest) = {
  val file = ...
  val writer = ...
  val theFixture = FixtureParam(file, writer)
  try {
    //prep
    withFixture(test.toNoArgTest(theFixture)) // "loan" the fixture to the test
  }
  finally {
    //cleanup
  }
}
"it" should "work" in { f =>
  f.writer
}

separating prep/cleanup so they can't make the tests fail
... with BeforeAndAfter
val builder = new StringBuilder
before {
  builder.append("ScalaTest is ")
}
after {
  builder.clear()
}
... or put these in a trait to apply selectively

making tests variable: make them parametered functions, call with whatever parameters you want.
def nonFullStack(newStack: => Stack[Int]) {}
it should behave like nonFullStack(stackWithOneItem)

mock objects: check if methods on an object get invoked or something

test for a range of parameter inputs:
forAll { (n: Int, d: Int) =>
  ...
}
val invalidCombos = Table(
  ("n",               "d"),
  ...
)
forAll (invalidCombos) { (n: Int, d: Int) =>
  ...
}

selenium browser spoofing:
"org.seleniumhq.selenium" % "selenium-java" % "2.35.0" % "test"
with WebBrowser
implicit val webDriver: WebDriver = new HtmlUnitDriver
go to url
http://www.scalatest.org/user_guide/using_selenium

entering case classes:
inside (name) { case Name(first, middle, last) =>
  first should be ("Sally")
}

with OptionValues trait: .value method for Option
opt.value should be > 9
assert(opt.value > 9)

with EitherValues trait: .left.value and .right.value methods for Either
either1.right.value should be > 9
either2.left.value should be ("Muchas problemas")
assert(either1.right.value > 9)
assert(either2.left.value === "Muchas problemas")

with PartialFunctionValues trait: .valueAt method for PartialFunction
pf.valueAt("IV") should equal (4)
assert(pf.valueAt("IV") === 4)

with PrivateMethodTester
val decorateToStringValue = PrivateMethod[String]('decorateToStringValue)
targetObject invokePrivate decorateToStringValue(1)

*/
