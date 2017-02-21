import freestyle._

object pcplodtest {
  @free trait PcplodTestAlgebra[F[_]] {
    def test(n: Int): FreeS[F, Int]
  }
  implicit val impl = new PcplodTestAlgebra.I@interpreter@nterpreter[Option] {
    override def testImpl(n:Int): Option[Int] = Some(1)
  }
  PcplodTestAlgebra[PcplodTestAlgebra.T].exec[Option]
}
