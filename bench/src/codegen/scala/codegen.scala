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

  def generate(): String = generate((1 to 10).toList)


  def generate(ops: List[Int]): String = {

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
       |  sealed trait Op[A] extends Product with Serializable
       |  ${ops.map { n => s"case class Op$n(x: Int) extends Op[Int]" }.mkString("\n")}
       |
       |  import freestyle._
       |  import freestyle.implicits._
       |
       |  @free trait Ops {
       |    ${ops.map { n => s"def op$n(x: Int): FS[Int]" }.mkString("\n")}
       |  }
       |
       |  @State(Scope.Thread)
       |  @BenchmarkMode(Array(Mode.Throughput))
       |  @OutputTimeUnit(TimeUnit.SECONDS)
       |  class FreestyleVsCatsFunctionKBench {
       |
       |    object CatsOps {
       |       ${ops.map { n => s"def op$n(x: Int): Free[Op, Int] = Free.liftF[Op, Int](Op$n(x))" }.mkString("\n")}
       |    }
       |
       |    val catsHandler: FunctionK[Op, cats.Id] = λ[Op ~> Id] {
       |      ${ops.map { n => s"case Op$n(x) => x + 1" }.mkString("\n")}
       |    }
       |
       |    val catsFreeProgram = ${ops.map(n => s"CatsOps.op$n(1)").mkString("(", " |@| ", ").tupled")}
       |
       |    @Benchmark def runCatsFree: Any = catsFreeProgram.foldMap(catsHandler)
       |
       |    implicit val fsHandler = new Ops.Handler[Id] {
       |      ${ops.map { n => s"def op$n(x: Int): cats.Id[Int] = x + 1" }.mkString("\n")}
       |    }
       |
       |    val FreestyleOps = Ops[Ops.Op]
       |
       |    val freestyleProgram = ${ops.map(n => s"FreestyleOps.op$n(1)").mkString("(", " |@| ", ").tupled")}
       |
       |    @Benchmark def runFreestyle: Any = Ops[Ops.Op].op1(1).interpret[Id]
       |
       |  }
    """.stripMargin

  }


}