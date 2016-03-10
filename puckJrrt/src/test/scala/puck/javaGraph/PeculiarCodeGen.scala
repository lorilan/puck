/*
 * Puck is a dependency analysis and refactoring tool.
 * Copyright (C) 2016 Loïc Girault loic.girault@gmail.com
 *               2016 Mikal Ziane  mikal.ziane@lip6.fr
 *               2016 Cédric Besse cedric.besse@lip6.fr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Additional Terms.
 * Author attributions in that material or in the Appropriate Legal
 * Notices displayed by works containing it is required.
 *
 * Author of this file : Loïc Girault
 */

package puck.javaGraph

import puck.graph.comparison.Mapping
import puck.{Settings, AcceptanceSpec}

/**
  * Created by Loïc Girault on 04/02/16.
  */
class PeculiarCodeGen extends AcceptanceSpec {
  val examplesPath = Settings.testExamplesPath + "/codeGen"

  feature("Parse lock unlock gen") {

    def makeTest(f : String) : Unit = {
      val _ = new ScenarioFactory(f) {
        val recompiledEx = applyChangeAndMakeExample(graph, Settings.outDir)
        assert( Mapping.equals(graph, recompiledEx.graph) )
      }
    }

    info("tests generated by finding samples of code that required debugging")

    scenario("Anonymous class") {
      makeTest(s"$examplesPath/anonymousClass.java")
    }

    scenario("enum") {
      makeTest(s"$examplesPath/enum.java")
    }

    scenario("inner enum") {
      makeTest(s"$examplesPath/innerEnum.java")
    }

    scenario("method call on anonymous class instantiation") {
      makeTest(s"$examplesPath/anonymousClassInstCall.java")
    }
    scenario("empty interface with comments") {
      makeTest(s"$examplesPath/emptyInterfaceWithComment.java")
    }

    scenario("gen type parameterized with an up-bounded wildcard") {
      makeTest(s"$examplesPath/genTypeUpBound.java")
    }

    scenario("instance initializer") {
      makeTest(s"$examplesPath/instanceInit.java")
    }

    scenario("parameterized class instanciation") {
      makeTest(s"$examplesPath/parClassInstanciation.java")
    }

    scenario("parameterized class subtyping ") {
      makeTest(s"$examplesPath/parClassSubtyping.java")
    }

    scenario("chained call with more than one argument") {
      makeTest(s"$examplesPath/chainedCallWithArgs.java")
    }

    scenario("overloading with variadic method"){
      makeTest(s"${Settings.testExamplesPath}/graphBuilding/variadicMethod/A.java")
    }

    scenario("wild card usage") {
      makeTest(s"$examplesPath/wild.java")
    }

    scenario("chained call in static context") {
      makeTest(s"$examplesPath/chainedCallInStaticContext.java")
    }

  }

}
