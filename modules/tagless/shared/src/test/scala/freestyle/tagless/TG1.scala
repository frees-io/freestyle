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

// /*
//  * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *     http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// abstract trait TG1[FF$2[_] >: [_]Nothing <: [_]Any] extends AnyRef with freestyle.tagless.internal.TaglessEffectLike[FF$2] {
//   def x(a: Int): TG1.this.FS[Int];
//   def y(a: Int): TG1.this.FS[Int]
// };
// object TG1 extends scala.AnyRef {
//   def apply[MM$3[_] >: [_]Nothing <: [_]Any](implicit ev$9: freestyle.tagless.algebras.TG1[MM$3]): freestyle.tagless.algebras.TG1[MM$3] = ev$9;
//   private[this] val functorKInstance$7: mainecoon.FunctorK[[α[_]]freestyle.tagless.algebras.TG1[α]] = {
//     final class $anon extends AnyRef with mainecoon.FunctorK[[α[_]]freestyle.tagless.algebras.TG1[α]] {
//       def <init>(): <$anon: mainecoon.FunctorK[[α[_]]freestyle.tagless.algebras.TG1[α]]> = {
//         $anon.super.<init>();
//         ()
//       };
//       def mapK[MM$3[_] >: [_]Nothing <: [_]Any, NN$4[_] >: [_]Nothing <: [_]Any](hh$5: freestyle.tagless.algebras.TG1[MM$3])(fk: cats.arrow.FunctionK[MM$3,NN$4]): freestyle.tagless.algebras.TG1[NN$4] = {
//         final class $anon extends AnyRef with freestyle.tagless.algebras.TG1[NN$4] {
//           def <init>(): <$anon: freestyle.tagless.algebras.TG1[NN$4]> = {
//             $anon.super.<init>();
//             ()
//           };
//           def x(a: Int): NN$4[Int] = fk.apply[Int](hh$5.x(a));
//           def y(a: Int): NN$4[Int] = fk.apply[Int](hh$5.y(a))
//         };
//         new $anon()
//       }
//     };
//     new $anon()
//   };
//   implicit <stable> <accessor> def functorKInstance$7: mainecoon.FunctorK[[α[_]]freestyle.tagless.algebras.TG1[α]] = TG1.this.functorKInstance$7;
//   implicit def derive[MM$3[_] >: [_]Nothing <: [_]Any, NN$4[_] >: [_]Nothing <: [_]Any](implicit h: freestyle.tagless.algebras.TG1[MM$3], FK: mainecoon.FunctorK[freestyle.tagless.algebras.TG1], fk: cats.arrow.FunctionK[MM$3,NN$4]): freestyle.tagless.algebras.TG1[NN$4] = FK.mapK[MM$3, NN$4](h)(fk);
//   abstract trait StackSafe[FF$67[_] >: [_]Nothing <: [_]Any] extends AnyRef with freestyle.internal.EffectLike[FF$67] {
//     def x(a: Int): StackSafe.this.FS[Int];
//     def y(a: Int): StackSafe.this.FS[Int]
//   };
//   @SuppressWarnings(value = ["org.wartremover.warts.Any", "org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Throw"]) object StackSafe extends scala.AnyRef {
//     def <init>(): freestyle.tagless.algebras.TG1.StackSafe.type = {
//       StackSafe.super.<init>();
//       ()
//     };
//     sealed abstract trait Op[_] extends AnyRef with Product with java.io.Serializable {
//       <stable> <accessor> val FSAlgebraIndex66: Int
//     };
//     final case class XOp extends _root_.scala.AnyRef with freestyle.tagless.algebras.TG1.StackSafe.Op[Int] with Product with Serializable {
//       <caseaccessor> <paramaccessor> private[this] val a: Int = _;
//       <stable> <caseaccessor> <accessor> <paramaccessor> def a: Int = XOp.this.a;
//       def <init>(a: Int): freestyle.tagless.algebras.TG1.StackSafe.XOp = {
//         XOp.super.<init>();
//         ()
//       };
//       private[this] val FSAlgebraIndex66: Int = 0;
//       override <stable> <accessor> def FSAlgebraIndex66: Int = XOp.this.FSAlgebraIndex66;
//       <synthetic> def copy(a: Int = a): freestyle.tagless.algebras.TG1.StackSafe.XOp = new XOp(a);
//       <synthetic> def copy$default$1: Int = XOp.this.a;
//       override <synthetic> def productPrefix: String = "XOp";
//       <synthetic> def productArity: Int = 1;
//       <synthetic> def productElement(x$1: Int): Any = x$1 match {
//         case 0 => XOp.this.a
//         case _ => throw new IndexOutOfBoundsException(x$1.toString())
//       };
//       override <synthetic> def productIterator: Iterator[Any] = scala.runtime.ScalaRunTime.typedProductIterator[Any](XOp.this);
//       <synthetic> def canEqual(x$1: Any): Boolean = x$1.$isInstanceOf[freestyle.tagless.algebras.TG1.StackSafe.XOp]();
//       override <synthetic> def hashCode(): Int = {
//         <synthetic> var acc: Int = -889275714;
//         acc = scala.runtime.Statics.mix(acc, a);
//         scala.runtime.Statics.finalizeHash(acc, 1)
//       };
//       override <synthetic> def toString(): String = scala.runtime.ScalaRunTime._toString(XOp.this);
//       override <synthetic> def equals(x$1: Any): Boolean = XOp.this.eq(x$1.asInstanceOf[Object]).||(x$1 match {
// case (_: freestyle.tagless.algebras.TG1.StackSafe.XOp) => true
// case _ => false
// }.&&({
//         <synthetic> val XOp$1: freestyle.tagless.algebras.TG1.StackSafe.XOp = x$1.asInstanceOf[freestyle.tagless.algebras.TG1.StackSafe.XOp];
//         XOp.this.a.==(XOp$1.a)
//       }))
//     };
//     <synthetic> object XOp extends scala.runtime.AbstractFunction1[Int,freestyle.tagless.algebras.TG1.StackSafe.XOp] with Serializable {
//       def <init>(): freestyle.tagless.algebras.TG1.StackSafe.XOp.type = {
//         XOp.super.<init>();
//         ()
//       };
//       final override <synthetic> def toString(): String = "XOp";
//       case <synthetic> def apply(a: Int): freestyle.tagless.algebras.TG1.StackSafe.XOp = new XOp(a);
//       case <synthetic> def unapply(x$0: freestyle.tagless.algebras.TG1.StackSafe.XOp): Option[Int] = if (x$0.==(null))
//         scala.None
//       else
//         Some.apply[Int](x$0.a);
//       <synthetic> private def readResolve(): Object = freestyle.tagless.algebras.TG1.StackSafe.XOp
//     };
//     final case class YOp extends _root_.scala.AnyRef with freestyle.tagless.algebras.TG1.StackSafe.Op[Int] with Product with Serializable {
//       <caseaccessor> <paramaccessor> private[this] val a: Int = _;
//       <stable> <caseaccessor> <accessor> <paramaccessor> def a: Int = YOp.this.a;
//       def <init>(a: Int): freestyle.tagless.algebras.TG1.StackSafe.YOp = {
//         YOp.super.<init>();
//         ()
//       };
//       private[this] val FSAlgebraIndex66: Int = 1;
//       override <stable> <accessor> def FSAlgebraIndex66: Int = YOp.this.FSAlgebraIndex66;
//       <synthetic> def copy(a: Int = a): freestyle.tagless.algebras.TG1.StackSafe.YOp = new YOp(a);
//       <synthetic> def copy$default$1: Int = YOp.this.a;
//       override <synthetic> def productPrefix: String = "YOp";
//       <synthetic> def productArity: Int = 1;
//       <synthetic> def productElement(x$1: Int): Any = x$1 match {
//         case 0 => YOp.this.a
//         case _ => throw new IndexOutOfBoundsException(x$1.toString())
//       };
//       override <synthetic> def productIterator: Iterator[Any] = scala.runtime.ScalaRunTime.typedProductIterator[Any](YOp.this);
//       <synthetic> def canEqual(x$1: Any): Boolean = x$1.$isInstanceOf[freestyle.tagless.algebras.TG1.StackSafe.YOp]();
//       override <synthetic> def hashCode(): Int = {
//         <synthetic> var acc: Int = -889275714;
//         acc = scala.runtime.Statics.mix(acc, a);
//         scala.runtime.Statics.finalizeHash(acc, 1)
//       };
//       override <synthetic> def toString(): String = scala.runtime.ScalaRunTime._toString(YOp.this);
//       override <synthetic> def equals(x$1: Any): Boolean = YOp.this.eq(x$1.asInstanceOf[Object]).||(x$1 match {
// case (_: freestyle.tagless.algebras.TG1.StackSafe.YOp) => true
// case _ => false
// }.&&({
//         <synthetic> val YOp$1: freestyle.tagless.algebras.TG1.StackSafe.YOp = x$1.asInstanceOf[freestyle.tagless.algebras.TG1.StackSafe.YOp];
//         YOp.this.a.==(YOp$1.a)
//       }))
//     };
//     <synthetic> object YOp extends scala.runtime.AbstractFunction1[Int,freestyle.tagless.algebras.TG1.StackSafe.YOp] with Serializable {
//       def <init>(): freestyle.tagless.algebras.TG1.StackSafe.YOp.type = {
//         YOp.super.<init>();
//         ()
//       };
//       final override <synthetic> def toString(): String = "YOp";
//       case <synthetic> def apply(a: Int): freestyle.tagless.algebras.TG1.StackSafe.YOp = new YOp(a);
//       case <synthetic> def unapply(x$0: freestyle.tagless.algebras.TG1.StackSafe.YOp): Option[Int] = if (x$0.==(null))
//         scala.None
//       else
//         Some.apply[Int](x$0.a);
//       <synthetic> private def readResolve(): Object = freestyle.tagless.algebras.TG1.StackSafe.YOp
//     };
//     type OpTypes = iota.TConsK[freestyle.tagless.algebras.TG1.StackSafe.Op,iota.TNilK];
//     abstract trait Handler[MM$73[_] >: [_]Nothing <: [_]Any] extends AnyRef with freestyle.FSHandler[freestyle.tagless.algebras.TG1.StackSafe.Op,MM$73] {
//       def /*Handler*/$init$(): Unit = {
//         ()
//       };
//       protected[this] def x(a: Int): MM$73[Int];
//       protected[this] def y(a: Int): MM$73[Int];
//       override def apply[AA$74](fa$75: freestyle.tagless.algebras.TG1.StackSafe.Op[AA$74]): MM$73[AA$74] = (fa$75.FSAlgebraIndex66: Int @scala.annotation.switch) match {
// case 0 => {
// val fresh76: freestyle.tagless.algebras.TG1.StackSafe.XOp = fa$75.asInstanceOf[freestyle.tagless.algebras.TG1.StackSafe.XOp];
// Handler.this.x(fresh76.a)
// }
// case 1 => {
// val fresh77: freestyle.tagless.algebras.TG1.StackSafe.YOp = fa$75.asInstanceOf[freestyle.tagless.algebras.TG1.StackSafe.YOp];
// Handler.this.y(fresh77.a)
// }
// case (i @ _) => throw new java.lang.Exception("freestyle internal error: index ".+(i.toString()).+(" out of bounds for ").+(this.toString()))
// }.asInstanceOf[MM$73[AA$74]]
//     };
//     class To[LL$69[_] >: [_]Nothing <: [_]Any] extends AnyRef with freestyle.tagless.algebras.TG1.StackSafe[LL$69] {
//       implicit <paramaccessor> private[this] val ii$70: freestyle.InjK[freestyle.tagless.algebras.TG1.StackSafe.Op,LL$69] = _;
//       def <init>()(implicit ii$70: freestyle.InjK[freestyle.tagless.algebras.TG1.StackSafe.Op,LL$69]): freestyle.tagless.algebras.TG1.StackSafe.To[LL$69] = {
//         To.super.<init>();
//         ()
//       };
//       private[this] val toInj71: freestyle.tagless.algebras.TG1.StackSafe.Op ~> [β$5$]cats.free.FreeApplicative[LL$69,β$5$] = freestyle.`package`.FreeS.inject[freestyle.tagless.algebras.TG1.StackSafe.Op, LL$69](To.this.ii$70);
//       override def x(a: Int): To.this.FS[Int] = To.this.toInj71.apply[Int](StackSafe.this.XOp.apply(a));
//       override def y(a: Int): To.this.FS[Int] = To.this.toInj71.apply[Int](StackSafe.this.YOp.apply(a))
//     };
//     implicit def to[LL$69[_] >: [_]Nothing <: [_]Any](implicit ii$70: freestyle.InjK[freestyle.tagless.algebras.TG1.StackSafe.Op,LL$69]): freestyle.tagless.algebras.TG1.StackSafe.To[LL$69] = new freestyle.tagless.algebras.TG1.StackSafe.To[LL$69]()(ii$70);
//     def apply[LL$69[_] >: [_]Nothing <: [_]Any](implicit ev$72: freestyle.tagless.algebras.TG1.StackSafe[LL$69]): freestyle.tagless.algebras.TG1.StackSafe[LL$69] = ev$72
//   };
//   implicit def stackSafeHandler$6[MM$3[_] >: [_]Nothing <: [_]Any](implicit ev$8: cats.Monad[MM$3], hh$5: freestyle.tagless.algebras.TG1.Handler[MM$3]): freestyle.tagless.algebras.TG1.StackSafe.Handler[MM$3] = {
//     final class $anon extends AnyRef with freestyle.tagless.algebras.TG1.StackSafe.Handler[MM$3] {
//       def <init>(): <$anon: freestyle.tagless.algebras.TG1.StackSafe.Handler[MM$3]> = {
//         $anon.super.<init>();
//         ()
//       };
//       def x(a: Int): MM$3[Int] = hh$5.x(a);
//       def y(a: Int): MM$3[Int] = hh$5.y(a)
//     };
//     new $anon()
//   };
//   abstract trait Handler[MM$3[_] >: [_]Nothing <: [_]Any] extends AnyRef with freestyle.tagless.algebras.TG1[MM$3] {
//     def x(a: Int): MM$3[Int];
//     def y(a: Int): MM$3[Int]
//   }
// };