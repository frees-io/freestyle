/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle

@free
trait SCtors1 {
  def x(a: Int): FS[Int]
  def y(a: Int): FS[Int]
}

@free
trait SCtors2 {
  def i(a: Int): FS[Int]
  def j(a: Int): FS[Int]
}

@free
trait SCtors3 {
  def o(a: Int): FS[Int]
  def p(a: Int): FS[Int]
}

@free
trait SCtors4 {
  def k(a: Int): FS[Int]
  def m(a: Int): FS[Int]
}

@free
trait MixedFreeS {
  def x: FS[Int]
  def y: FS[Int]
  def z: FS[Int]
}

@free
trait S1 {
  def x(n: Int): FS[Int]
}

@free
trait S2 {
  def y(n: Int): FS[Int]
}


@module
trait M1 {
  val sctors1: SCtors1
  val sctors2: SCtors2
}

@module
trait M2 {
  val sctors3: SCtors3
  val sctors4: SCtors4
}

@module
trait O1 {
  val m1: M1
  val m2: M2
}

@module
trait O2 {
  val o1: O1
  val x = 1
  def y = 2
}

@module
trait O3 {
  def x = 1
  def y = 2
}

@module
trait StateProp {
  val s1: S1
  val s2: S2
}

object comp {

  @module
  trait FSMod {

    val sCtors1: SCtors1

    def x(a: Int): FS.Seq[Int] = sCtors1.x(a)
    def y(b: Int): FS.Seq[Int] = sCtors1.y(b)
  }

}


object interps {

  implicit val optionHandler1: FSHandler[SCtors1.Op, Option] = new SCtors1.Handler[Option] {
    def x(a: Int): Option[Int] = Some(a)
    def y(a: Int): Option[Int] = Some(a)
  }

  implicit val listHandler1: FSHandler[SCtors1.Op, List] = new SCtors1.Handler[List] {
    def x(a: Int): List[Int] = List(a)
    def y(a: Int): List[Int] = List(a)
  }

  implicit val optionHandler2: FSHandler[SCtors2.Op, Option] = new SCtors2.Handler[Option] {
    def i(a: Int): Option[Int] = Some(a)
    def j(a: Int): Option[Int] = Some(a)
  }

  implicit val listHandler2: FSHandler[SCtors2.Op, List] = new SCtors2.Handler[List] {
    def i(a: Int): List[Int] = List(a)
    def j(a: Int): List[Int] = List(a)
  }

  implicit val optionHandler3: FSHandler[SCtors3.Op, Option] = new SCtors3.Handler[Option] {
    def o(a: Int): Option[Int] = Some(a)
    def p(a: Int): Option[Int] = Some(a)
  }

  implicit val listHandler3: FSHandler[SCtors3.Op, List] = new SCtors3.Handler[List] {
    def o(a: Int): List[Int] = List(a)
    def p(a: Int): List[Int] = List(a)
  }

  implicit val optionHandler4: FSHandler[SCtors4.Op, Option] = new SCtors4.Handler[Option] {
    def k(a: Int): Option[Int] = Some(a)
    def m(a: Int): Option[Int] = Some(a)
  }

  implicit val listHandler4: FSHandler[SCtors4.Op, List] = new SCtors4.Handler[List] {
    def k(a: Int): List[Int] = List(a)
    def m(a: Int): List[Int] = List(a)
  }

}
