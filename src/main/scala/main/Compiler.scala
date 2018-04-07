package main

import sigmastate._
import sigmastate.Values._
import sigmastate.lang.{SigmaBinder, SigmaParser, SigmaSpecializer, SigmaTyper}
import sigmastate.utxo._
import main.BoxHelpers.{fakeMessage, fakeSelf}
import sigmastate.lang.Terms._

object Main extends App {

  val compiler = new Compiler()

  compiler.sample()

}

class Compiler {

  def sample(): Unit = {

    println("Starting to compile...")

    val verifier = new ErgoInterpreter

    //backer's prover with his private key
    val backerProver = new ErgoProvingInterpreter
    //project's prover with his private key
    val projectProver = new ErgoProvingInterpreter
    val backerPubKey = backerProver.dlogSecrets.head.publicImage
    val projectPubKey = projectProver.dlogSecrets.head.publicImage

    val timeout = IntConstant(100)
    val minToRaise = IntConstant(1000)

    val env = Map(
      "timeout" -> 100,
      "minToRaise" -> 1000,
      "backerPubKey" -> backerPubKey,
      "projectPubKey" -> projectPubKey
    )

    val compiledScript = compile(env,
      """{
        | let c1 = HEIGHT >= timeout && backerPubKey
        | let c2 = allOf(Array(
        |   HEIGHT < timeout,
        |   projectPubKey,
        |   OUTPUTS.exists(fun (out: Box) = {
        |     out.value >= minToRaise && out.propositionBytes == projectPubKey.propBytes
        |   })
        | ))
        | c1 || c2
        | }
      """.stripMargin).asBoolValue

    println("Simpe output")
    println("{")
    println("\t%s".format(compiledScript))
    println("\topCode: %s".format(compiledScript.opCode))
    println("\tpropBytes: %s".format(compiledScript.propBytes))
    println("\tcost: %s".format(compiledScript.cost))
    println("\tevaluated: %s".format(compiledScript.evaluated))
    println("}")

    val tx1Output1 = ErgoBox(minToRaise.value, projectPubKey)
    val tx1Output2 = ErgoBox(1, projectPubKey)

    val tx1 = ErgoTransaction(IndexedSeq(), IndexedSeq(tx1Output1, tx1Output2))

    val crowdFundingScript = OR(
      AND(GE(Height, timeout), backerPubKey),
      AND(
        Seq(
          LT(Height, timeout),
          projectPubKey,
          Exists(Outputs, 21,
            AND(
              GE(ExtractAmount(TaggedBox(21)), minToRaise),
              EQ(ExtractScriptBytes(TaggedBox(21)), ByteArrayConstant(projectPubKey.propBytes))
            )
          )
        )
      )
    )

    val outputToSpend = ErgoBox(10, crowdFundingScript)

    val ctx1 = ErgoContext(
      currentHeight = timeout.value - 1,
      lastBlockUtxoRoot = AvlTreeData.dummy,
      boxesToSpend = IndexedSeq(),
      spendingTransaction = tx1,
      self = outputToSpend)

    val result = backerProver.prove(compiledScript, ctx1, fakeMessage)
    println("Prove result: %s".format(result))
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
