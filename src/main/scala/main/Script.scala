package main

class Script(val text: String) {

  val isOdd: PartialFunction[Int, String] = {
    case x if x % 2 == 1 => x + " is odd"
  }

  val params : Array[(String, String)] = {

    val first = text.indexOf("(")
    val second = text.indexOf(")")

    val pairs: Array[(String, String)] =
      text substring(first, second) split "," map (_.split(":")) map { case Array(x, y) => (x, y) }

    pairs
  }
}
