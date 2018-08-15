package main

import sigmastate.Values._
import sigmastate._
import sigmastate.lang.Terms._
import sigmastate.lang._
import sigmastate.lang.syntax.ParserException

object Main extends App {

  val compiler = new SigmaCompiler()

  var env = Map(
    "timeout" -> 100,
    "minToRaise" -> 1000,
    "backerPubKey" -> "Sigma",
    "projectPubKey" -> "Sigma"
  )

  var text = """{
           let c1 = HEIGHT >= timeout && backerPubKey
           let c2 = allOf(Array(
             HEIGHT < timeout,
             projectPubKey,
             OUTPUTS.exists(fun (out: Box) = {
               out.value >= minToRaise && out.propositionBytes == projectPubKey.propBytes
             })
           ))
           c1 || c2
    }"""

  println("Next...")
  println(text)
  println()
  io.StdIn.readLine("Press to compile...")
  println()

  compiler.Analyze(env, text)

  text = """{
           let c1 = HEIGHT >=== timeout && backerPubKey
           let c2 = allOf(Array(
             HEIGHT < timeout,
             projectPubKey,
             OUTPUTS.exists(fun (out: Box) = {
               out.value >= minToRaise && out.propositionBytes == projectPubKey.propBytes
             })
           ))
           c1 || c2
    }"""

  println("Next...unknown operator")
  println(text)
  println()
  io.StdIn.readLine("Press to compile...")
  println()

  compiler.Analyze(env, text)

  text = """{
           let c1 = HEIGHT >= timeout22 && backerPubKey
           let c2 = allOf(Array(
             HEIGHT < timeout,
             projectPubKey,
             OUTPUTS.exists(fun (out: Box) = {
               out.value >= minToRaise && out.propositionBytes == projectPubKey.propBytes
             })
           ))
           c1 || c2
    }"""

  println("Next... undefined variable")
  println(text)
  println()
  io.StdIn.readLine("Press to compile...")
  println()

  compiler.Analyze(env, text)

  text = """{
           let c1 = HEIGHT >= timeout && backerPubKey
           let c2 = allOf(Array
             HEIGHT < timeout,
             projectPubKey,
             OUTPUTS.exists(fun (out: Box) = {
               out.value >= minToRaise && out.propositionBytes == projectPubKey.propBytes
             })
           ))
           c1 || c2
    }"""

  println("Next... missing bracket")
  println(text)
  println()
  io.StdIn.readLine("Press to compile...")
  println()

  compiler.Analyze(env, text)
}

class SigmaCompiler {

  // TODO: Parse output and show error location

  val err = Seq("Unknown binary operation")

  def Analyze(params: Map[String, Any], script: String): Unit = {

    var map: Map[String, Any] = Map()

    // TODO: Temp stub for Sigma parameter

    params foreach ((e: (String, Any)) => {
      e._2 match {
        case "Sigma" =>
          val prover = new ErgoProvingInterpreter
          map += (e._1 -> prover.dlogSecrets.head.publicImage)
        case _ => map += (e._1 -> e._2)
      }
    })

    println()
    println("Starting to compile...")
    println()

    try {
      compile(map, script.stripMargin)
    }
    catch {
      case e: ParserException =>
        println("Error: " + e.getMessage)
        println()
        return
      case e: TyperException =>
        println("Error: " + e.getMessage)
        println()
        return
    }

    println()
    println("Compiled successfully")
    println()
  }

  def compile(env: Map[String, Any], code: String): Value[SType] = {
    val parsed = parse(code)
    val binder = new SigmaBinder(env)
    val bound = binder.bind(parsed)
    val st = new SigmaTree(bound)
    val typer = new SigmaTyper
    val typed = typer.typecheck(bound)
    val spec = new SigmaSpecializer
    val ir = spec.specialize(typed)
    ir
  }

  def parse(x: String): SValue = SigmaParser(x).get.value
}
