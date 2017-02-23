import freestyle._
import freestyle.implicits._
import cats.implicits._

object pcplodtest {
  @free trait PcplodTestAlgebra[F[_]] {
    def test(n: Int): FreeS[F, Int]
  }
  implicit val impl = new PcplodTestAlgebra.I@interpreter@nterpreter[Option] {
    override def testImpl(n:Int): Option[Int] = Some(1)
  }
  def program[F[_]: PcplodTestAlgebra]: FreeS[F, Int] = PcplodTestAlgebra[F].t@test@est(1)

  val r@result@esult = program[PcplodTestAlgebra.T].exec[Option]
}
