package freestyle.free
package bench

import scala.annotation.tailrec
import scala.Predef._
import scala.collection.immutable

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

  def catsFreeAlgebras(id: String, algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
        s"""
            |  sealed trait Op_${a}_$id[A] extends Product with Serializable
            |  ${ops.map { n => s"case class Op_${a}_${n}_$id(x: Int) extends Op_${a}_$id[Int]" }.mkString("\n")}
            |
            |""".stripMargin
  }.mkString("\n")

  def catsFreeAlgebraOps(id: String, algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
    s"""
       | class CatsOps_${a}_$id[F[_]](implicit I: Inject[Op_${a}_$id, F]) {
       |   ${ops.map { n => s"def op${n}_$id(x: Int): Free[F, Int] = Free.inject[Op_${a}_$id, F](Op_${a}_${n}_$id(x))" }.mkString("\n")}
       | }
    """.stripMargin
  }.mkString("\n")

  def catsFreeOpsInstances(id: String, algebras: List[Int]): String = algebras.map { a =>
    s"val catsOps_${a}_$id = new CatsOps_${a}_$id[CP_${algebras.size}_$id]"
  }.mkString("\n")

  def catsFreeHandlers(id: String, algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
    s"""
       | implicit val catsHandler_${a}_$id: FunctionK[Op_${a}_$id, cats.Id] = λ[Op_${a}_$id ~> Id] {
       |   ${ops.map { n => s"case Op_${a}_${n}_$id(x) => x + 1" }.mkString("\n")}
       | }
    """.stripMargin
  }.mkString("\n")

  def catsFreeProgram(id: String, algebras: List[Int], ops: List[Int]): String = algebras.flatMap { a =>
    ops.map(n => s"catsOps_${a}_$id.op${n}_$id(1).foldMap(implicitly[FunctionK[CP_${algebras.size}_$id, Id]])")
  }.reverse.take(1).mkString("{", "\n", "}")

  def catsFreeCoproduct(id: String, algebras: List[Int]): String = {
    def loop(current: List[Int], acc: (Int, String, String)): String = {
       val (index, cp, cpId) = acc
       current match {
         case Nil => cp
         case h :: t if (h == algebras.head) && algebras.size >= 2 =>
            loop(t, (index + 1, List(cp, s"type CP_${index}_$id[A] = cats.data.Coproduct[Op_${index}_$id, Op_${index + 1}_$id, A]").mkString("\n"), s"CP_${index}_$id"))
         case h :: t =>
            loop(t, (index + 1, List(cp, s"type CP_${index}_$id[A] = cats.data.Coproduct[Op_${index}_$id, $cpId, A]").mkString("\n"), s"CP_${index}_$id"))
       }
    }
    loop(algebras, (1, "", ""))
  }

  def freestyleAlgebras(id: String, algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
    s"""
       |  @free trait Ops_${a}_$id {
       |    ${ops.map { n => s"def op${n}_$id(x: Int): FS[Int]" }.mkString("\n")}
       |  }
       |  """.stripMargin
  }.mkString("\n")

  def freestyleHandlers(id: String, algebras: List[Int], ops: List[Int]): String = algebras.map { a =>
    s"""
       | implicit val fsHandler_${a}_$id = new Ops_${a}_$id.Handler[Id] {
       |   ${ops.map { n => s"def op${n}_$id(x: Int): cats.Id[Int] = x + 1" }.mkString("\n")}
       | }
    """.stripMargin
  }.mkString("\n")

  def freestyleModule(id: String, algebras: List[Int]): String =
    s"""
      |@module trait App_$id {
      |  ${algebras.map(a => s"val a_${a}_$id: Ops_${a}_$id").mkString("\n")}
      |}
    """.stripMargin

  def freestyleOpsInstances(id: String, algebras: List[Int]): String = algebras.map { a =>
    s"val freestyleOps_${a}_$id = Ops_${a}_$id[App_$id.Op]"
  }.mkString("\n")

  def freestyleProgram(id: String, algebras: List[Int], ops: List[Int]): String = algebras.flatMap { a =>
    ops.map(n => s"freestyleOps_${a}_$id.op${n}_$id(1).interpret[Id]")
  }.reverse.take(1).mkString("{", "\n", "}")

  def imports: String =
    """
      |  package freestyle.free.bench
      |
      |  import java.util.concurrent.TimeUnit
      |  import org.openjdk.jmh.annotations._
      |
      |  import cats._
      |  import cats.arrow._
      |  import cats.free._
      |  import cats.implicits._
      |
      |  import freestyle.free._
      |  import freestyle.free.implicits._
      |
    """.stripMargin

  def outterScope(id: String, algebras: List[Int], ops: List[Int]): String =
    s"""
      |  ${catsFreeAlgebras(id, algebras, ops)}
      |
      |  ${freestyleAlgebras(id, algebras, ops)}
      |
      |  ${freestyleModule(id, algebras)}
    """.stripMargin

  def benchMarkClass(id: String, algebras: List[Int], ops: List[Int]): String =
    s"""
       |  @State(Scope.Thread)
       |  @BenchmarkMode(Array(Mode.Throughput))
       |  @OutputTimeUnit(TimeUnit.SECONDS)
       |  @Fork(jvmArgsAppend = Array("-Xms3g", "-Xmx3g"))
       |  class _$id {
       |
       |    ${catsFreeAlgebraOps(id, algebras, ops)}
       |
       |    ${catsFreeHandlers(id, algebras, ops)}
       |
       |    ${catsFreeCoproduct(id, algebras)}
       |
       |    ${catsFreeOpsInstances(id, algebras)}
       |
       |    @Benchmark def bench_cats: Any = ${catsFreeProgram(id, algebras, ops)}
       |
       |    ${freestyleHandlers(id, algebras, ops)}
       |
       |    ${freestyleOpsInstances(id, algebras)}
       |
       |    @Benchmark def bench_freestyle: Any = ${freestyleProgram(id, algebras, ops)}
       |
       |  }
    """.stripMargin

  def template(numAlgebras: Int, numOps: Int): String = {
    val algebras = (1 to numAlgebras).toList
    val ops = (1 to numOps).toList
    val id = f"$numAlgebras%02d_adts_$numOps%02d_ops"
    s"""
    |  ${outterScope(id, algebras, ops)}
    |  ${benchMarkClass(id, algebras, ops)}
    |""".stripMargin
  }

  def generate(): String = {
    s"""
       |$imports
       |${template(2, 10)}
       |${template(2, 20)}
       |${template(2, 30)}
       |${template(2, 40)}
       |${template(2, 50)}
       |${template(2, 60)}
       |${template(2, 80)}
       |${template(2, 90)}
       |
     """.stripMargin
  }


}