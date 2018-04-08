package main

class Script(val text: String) {

  val params : String = {
    val first = text.indexOf("(")
    val second = text.indexOf(")")
    val pars = text.substring(first, second).split(",")
    // TODO: Parse scripts arguments
    ""
  }
}
