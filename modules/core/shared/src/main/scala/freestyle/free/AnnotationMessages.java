/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.free;

interface AnnotationMessages {

 public static final String handlerNotFoundMsg = "\n-------" +
  "\n\nHandler not found to transform `${F}` to `${G}`" +
  "\n                                               " +
  "\nFor automatic handler resolution to work, ensure you provide an implicit handler for each one of your defined @free algebras." +
  "\nUber handlers considered for evaluating grouped algebras in `@module` or `Coproduct` are automatically derived and implicitly provided" +
  "\nthere is a handler for each one of the `@free` algebras available in the implicit scope. Ensure you have imported `freestyle.free.implicits._`." +
  "\n\n To learn more about `handlers` and how they relate to `@free` algebras visit:" +
  "\n\n http://frees.io/docs/core/interpreters/\n\n" +
  "\n-------";

 public static final String freeSLiftInstanceNotFoundMsg = "\n-------" +
  "\nNo FreeSLift instance found for `${G}`" +
  "\nThe following stub is provided for convenience:" +
  "\n\nimplicit def instance[F[_]]: FreeSLift[F, ${G}] = new FreeSLift[F, ${G}] {" +
  "\n  def liftFSPar[A](ga: ${G}[A]): FreeS.Par[F, A] = ???" +
  "\n}" +
  "\n-------";

 public static final String captureInstanceNotFoundMsg = "\n-------" +
  "\nNo Capture instance found for ${F}" +
  "\nThe following stub is provided for convenience:" +
  "\n\nimplicit val ${F}CaptureInstance: Capture[${F}] = new Capture[${F}] {" +
  "\n  def capture[A](a: => A): ${F}[A] = ???" +
  "\n}" +
  "\n-------";
 

}
