import freestyle._
import freestyle.implicits._
import cats.implicits._

object pcplodtest {
  @free trait PcplodTestAlgebra {
    def test(n: Int): Oper.Seq[Int]
  }
  implicit val impl = new PcplodTestAlgebra.H@handler@andler[Option] {
    override def test(n:Int): Option[Int] = Some(1)
  }
  def program[F[_]: PcplodTestAlgebra]: FreeS[F, Int] = PcplodTestAlgebra[F].t@test@est(1)

  val r@result@esult = program[PcplodTestAlgebra.Op].exec[Option]
}
