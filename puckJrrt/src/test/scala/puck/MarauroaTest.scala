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
 * Author of this file : Loïc Giraul
 */

package puck

import java.awt.Dimension
import java.io.File
import javax.swing.UIManager

import puck.util.{PuckFileLogger, PuckLogger}
import LoadAndApply._
import puck.config.Config
import puck.config.Config.{Keys, Root, SingleFile}
import puck.graph.constraints.search.{NoVirtualNodes, WithVirtualNodes}
import puck.gui.PuckMainPanel
import puck.jastadd.{ExtendJGraphUtils, JavaProject}

import scala.swing.MainFrame

object Tests {

  def project(root : String, srcFolder : String, constraint : String) : Project =
    JavaProject.withConfig(conf(root, srcFolder, constraint))

  def conf(root : String, srcFolder : String, constraint : String) : Config.Config =
    (Config.empty
      put (Keys.workspace, SingleFile(root))
      put (Keys.srcs, List(Root(srcFolder, ".java", Seq())))
      put (Keys.classpath, List(Root("libs", ".jar", Seq())))

      put (Keys.out, SingleFile("out"))
      put (Keys.decouple, SingleFile(constraint))
      put (Keys.log, SingleFile("puck-log.txt"))
      )

}

object ResourceTest {

  implicit val logger : PuckLogger = new PuckFileLogger(_ => true,
    new File("/tmp/pucklog"))

  val root = "/home/lorilan/projects/constraintsSolver/puckJrrt/src/test/resources/"

  val path = "nanoPersonne"

  val project = Tests.project(root + path, "nano", "decouple.wld")

  def main(args : Array[String]) : Unit = {
    //        SearchSolution(project, ".",
    //          StrategyChoice.DepthFirst, ControlChoice.Heuristic, NoVirtualNodes)

    ignore(applyRecords(project,
      Seq(root + path +"/heuristic-depthFirst-solution-0_6_0.pck" )))

  }
}
object DistribTest {
  implicit val logger : PuckLogger = new PuckFileLogger(_ => true,
    new File("/tmp/pucklog"))

  val root = "/home/lorilan/projects/constraintsSolver/test_resources/distrib/"

  val path = "bridge/hannemann_simplified"

  val project = Tests.project(root + path, "screen", "decouple.wld")

  def main(args : Array[String]) : Unit = {
    //    SearchSolution(project, ".",
    //      StrategyChoice.DepthFirst, ControlChoice.Heuristic, NoVirtualNodes)

    ignore(applyRecords(project,
      Seq(root + path +"/heuristic-depthFirst-solution-0_44_3_0_0_36_3_0_0_25_3_0_0_2_3_0.pck" )))
  }
}

object MarauroaTest {
  implicit val logger : PuckLogger = new PuckFileLogger(_ => true,
    new File("/tmp/pucklog"))

  val root = "/home/lorilan/projects/arianne-marauroa"

  def project(srcFolder : String, constraint : String = "decouple.wld") : Project =
    Tests.project(root, srcFolder, constraint)
}
import MarauroaTest.{logger, root, project}

object MarauroaLoadRecordAndApply {
  val path = "/constraint-gen/1rule/10/solutionWithHeuristic.pck"
  def main(args : Array[String]) : Unit =
    ignore(applyRecords(project("src.original"),
      Seq(root + path )))

}

object MarauroaLoadRecordAndApplyStepByStep {

  def main(args : Array[String]) : Unit =
    ignore(applyRecursivelyStepByStep(
      project("src.original"),
      root + "/constraint-gen1-05-solution-manual-partial.pck"))

}

object LoadAndSearchSolutions {

  val path = "constraint-gen/1rule/01/"

  def main(args : Array[String]) : Unit =
    SearchSolution(project("src.generated", path + "decouple.wld"), path,
      StrategyChoice.DepthFirst, ControlChoice.Heuristic, NoVirtualNodes)
}

object LaunchUI {



  //val root = "/home/lorilan/projects/constraintsSolver/puckJrrt/src/test/resources/bridge/hannemann_simplified"
  // val srcPath = "screen"
  //val constraintPath = "decouple.wld"

  val constraintPath = "constraint-gen/1rule/07/decouple.wld"
  import MarauroaTest.root
  val srcPath = "src.generated"
  def main(args : Array[String]) : Unit ={
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
    val top = new MainFrame {
      title = "Puck"
      contents = new PuckMainPanel(ExtendJGraphUtils, JavaIcons) {
        control.loadConf(Tests.conf(root, srcPath, constraintPath))
      }
    }
    if (top.size == new Dimension(0,0)) top.pack()
    top.visible = true
  }
}

//object GenConstraintAndSearchSolutions {
//
//  val numConstraint = 1
//  def genBaseName(id : Int) = s"constraint-gen$numConstraint-$id"
//
//  def main(args : Array[String]) : Unit = {
//    var i = 4
//    while(new File(root + File.separator + genBaseName(i)).exists())
//      i = i + 1
//
//    val baseName = genBaseName(i)
//    val p = project("src.generated", baseName+".wld")
//
//    val (dg, names2id, cm, mutability) =  ConstraintGen(p, baseName, numConstraint)
//    SearchSolution(dg, cm, mutability) map (st => (st.uuid(), st.loggedResult)) foreach {
//          case (id, LoggedSuccess(_, (g,_))) =>
//            import puck.util.FileHelper.FileOps
//            val recFile = p.workspace \  s"$baseName-solution$id.pck"
//            Recording.write(recFile.getAbsolutePath, names2id, g)
//        }
//
//  }
//}




