package freestyle
package bench

import scala.annotation.tailrec
import scala.Predef._

object BenchBoiler {

  def main(args: Array[String]): Unit = {
    args.headOption match {
      case Some(path) =>
        val output = generate()
        val file = new java.io.File(path)
        file.getParentFile().mkdirs()
        file.createNewFile()
        val pw = new java.io.FileWriter(file)
        try {
          pw.write(output)
        } finally {
          pw.close()
        }
      case None => println("expected single path argument")
    }
  }

  def generate(): String = generate((1 to 5).toList, (1 to 25).toList)

  def catsFreeAlgebras(algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
        s"""
            |  sealed trait Op_$a[A] extends Product with Serializable
            |  ${ops.map { n => s"case class Op_${a}_$n(x: Int) extends Op_$a[Int]" }.mkString("\n")}
            |
            |""".stripMargin
  }.mkString("\n")

  def catsFreeAlgebraOps(algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
    s"""
       | class CatsOps_$a[F[_]](implicit I: Inject[Op_$a, F]) {
       |   ${ops.map { n => s"def op$n(x: Int): Free[F, Int] = Free.inject[Op_$a, F](Op_${a}_$n(x))" }.mkString("\n")}
       | }
    """.stripMargin
  }.mkString("\n")

  def catsFreeOpsInstances(algebras: List[Int]): String = algebras.map { a =>
    s"val catsOps_$a = new CatsOps_$a[CP_${algebras.size}]"
  }.mkString("\n")

  def catsFreeHandlers(algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
    s"""
       | implicit val catsHandler_$a: FunctionK[Op_$a, cats.Id] = λ[Op_$a ~> Id] {
       |   ${ops.map { n => s"case Op_${a}_$n(x) => x + 1" }.mkString("\n")}
       | }
    """.stripMargin
  }.mkString("\n")

  def catsFreeProgram(algebras: List[Int], ops: List[Int]): String = algebras.flatMap { a =>
    ops.map(n => s"catsOps_$a.op$n(1).foldMap(implicitly[FunctionK[CP_${algebras.size}, Id]])")
  }.mkString("{", "\n", "}")

  def catsFreeCoproduct(algebras: List[Int]): String = {
    def loop(current: List[Int], acc: (Int, String, String)): String = {
       val (index, cp, cpId) = acc
       current match {
         case Nil => cp
         case h :: t if (h == algebras.head) && algebras.size >= 2 =>
            loop(t, (index + 1, List(cp, s"type CP_$index[A] = cats.data.Coproduct[Op_$index, Op_${index + 1}, A]").mkString("\n"), s"CP_$index"))
         case h :: t =>
            loop(t, (index + 1, List(cp, s"type CP_$index[A] = cats.data.Coproduct[Op_$index, $cpId, A]").mkString("\n"), s"CP_$index"))
       }
    }
    loop(algebras, (1, "", ""))
  }

  def freestyleAlgebras(algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
    s"""
       |  @free trait Ops_$a {
       |    ${ops.map { n => s"def op$n(x: Int): FS[Int]" }.mkString("\n")}
       |  }
       |  """.stripMargin
  }.mkString("\n")

  def freestyleHandlers(algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
    s"""
       | implicit val fsHandler_$a = new Ops_$a.Handler[Id] {
       |   ${ops.map { n => s"def op$n(x: Int): cats.Id[Int] = x + 1" }.mkString("\n")}
       | }
    """.stripMargin
  }.mkString("\n")

  def freestyleModule(algebras: List[Int]): String =
    s"""
      |@module trait App {
      |  ${algebras.map(a => s"val a_$a: Ops_$a").mkString("\n")}
      |}
    """.stripMargin

  def freestyleOpsInstances(algebras: List[Int]): String = algebras.map { a =>
    s"val freestyleOps_$a = Ops_$a[App.Op]"
  }.mkString("\n")

  def freestyleProgram(algebras: List[Int], ops: List[Int]): String = algebras.flatMap { a =>
    ops.map(n => s"freestyleOps_$a.op$n(1).foldMap(implicitly[FunctionK[App.Op, Id]])")
  }.mkString("{", "\n", "}")

  def generate(algebras: List[Int], ops: List[Int]): String = {

    s"""
       |  package freestylebench
       |
       |  import java.util.concurrent.TimeUnit
       |  import org.openjdk.jmh.annotations._
       |
       |  import cats._
       |  import cats.arrow._
       |  import cats.free._
       |  import cats.implicits._
       |
       |  ${catsFreeAlgebras(algebras, ops)}
       |
       |  import freestyle._
       |  import freestyle.implicits._
       |
       |  ${freestyleAlgebras(algebras, ops)}
       |
       |  ${freestyleModule(algebras)}
       |
       |  @State(Scope.Thread)
       |  @BenchmarkMode(Array(Mode.Throughput))
       |  @OutputTimeUnit(TimeUnit.SECONDS)
       |  class FreestyleVsCatsFunctionKBench {
       |
       |    ${catsFreeAlgebraOps(algebras, ops)}
       |
       |    ${catsFreeHandlers(algebras, ops)}
       |
       |    ${catsFreeCoproduct(algebras)}
       |
       |    ${catsFreeOpsInstances(algebras)}
       |
       |    @Benchmark def runCatsFree: Any = ${catsFreeProgram(algebras, ops)}
       |
       |    ${freestyleHandlers(algebras, ops)}
       |
       |    ${freestyleOpsInstances(algebras)}
       |
       |    @Benchmark def runFreestyle: Any = ${freestyleProgram(algebras, ops)}
       |
       |  }
    """.stripMargin

  }


}