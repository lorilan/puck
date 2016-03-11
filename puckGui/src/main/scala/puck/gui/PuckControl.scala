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

package puck.gui

import java.io.File

import puck._
import puck.config.{Config, ConfigParser}
import puck.graph._

import puck.util.{PuckLogger, PuckLog}

import scala.concurrent.Future
import scala.swing.{ProgressBar, Publisher}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.{\/-, -\/}


object FrontVars {
// // val root = "/home/lorilan/test_cases_for_puck"
//  // val system = "/jhotdraw/JHotDraw 7.0.6"
//  //val system = "/jhotdraw/jhotdraw-7.5.1"
//  //val system = "dspace-1.5.1"
//
//  //val root = "/home/lorilan/puck_svn/examples/QualitasCorpus-20130901r/Systems"
//  val root = "/home/lorilan/test_cases_for_puck/QualitasCorpus/Systems"
//  //val system = "freecs/freecs-1.3.20100406"
//  //val system = "freemind/freemind-0.9.0"
//  val system = "freemind/freemind-1.0.1"
//
//  //val workspace = s"$root/$system/puck_test"
//  //val workspace = s"/home/lorilan/projects/constraintsSolver/test_resources/distrib/bridge/hannemann_simplified"
//    val workspace = "/home/lorilan/freemind-0.9.0_example"
//  //val workspace = "/home/lorilan/puck_svn/examples/dspace-1.5.1-src-release"
//  val workspace = "/home/lorilan/test"
  val workspace = "."
}

class PuckControl
(logger0 : PuckLogger,
 val graphUtils: GraphUtils)
  extends Publisher {

  val progressBar = new ProgressBar {
    min = 0
    max = 100
    value = 0
    labelPainted = true
    visible = false
  }

  object Bus extends Publisher
  this listenTo Bus

  implicit val logger: PuckLogger = logger0

  var project : Project = _
  var dg2ast: DG2AST = _
  val graphStack: GraphStack = new GraphStack(Bus)


  {
    val workspace = "."
    if (Config.defaultConfFile(new File(FrontVars.workspace)).exists())
      loadConf(Config.defaultConfFile(new File(FrontVars.workspace)))
  }




  def loadConf(file : File) : Unit = {
    project = new Project(ConfigParser(file),
      graphUtils.dg2astBuilder)

    val sf : Option[File]= project.someFile(Config.Keys.workspace)
    val path = sf map (_.getAbsolutePath) getOrElse "No directory selected"
    logger writeln  s"Workspace directory :\n$path"
    loadCodeAndConstraints()
  }

  def graph: DependencyGraph = graphStack.graph

  import PuckLog.defaultVerbosity


  private[this] var printingOptionsControl0: PrintingOptionsControl = _

  def printingOptionsControl = printingOptionsControl0


  def loadCodeAndConstraints() = Future {
    progressBar.visible = true
    progressBar.value = 0
    if(project.pathList(Config.Keys.srcs).isEmpty) {
      throw NoSourceDetected
    }

    dg2ast = project.loadGraph(Some(new LoadingListener {
      override def update(loading: Double): Unit =
        progressBar.value = (loading * 100).toInt
    }))
    progressBar.visible = false


    printingOptionsControl0 = PrintingOptionsControl(dg2ast.initialGraph, Bus)


  } onComplete {
    case Success(_) =>
      logger writeln s"Graph builded : ${dg2ast.initialGraph.nodes.size} nodes"
      loadConstraints(setInitialGraph = true)
    case Failure(exc) =>
      progressBar.visible = false

      if(exc.getCause == null ) {
        if(exc.getMessage == null)
          logger write (exc.getStackTrace mkString "\n")
        else {
          logger writeln exc.getMessage
          exc.printStackTrace()
        }
      }
      else {
          if (exc.getCause != NoSourceDetected)
            exc.printStackTrace()
        logger writeln exc.getCause.getMessage
      }

  }


  def loadConstraints(setInitialGraph : Boolean = false) : Unit = {
      logger.writeln("Loading constraints ...")

      project.parseConstraints(dg2ast) match {
        case None =>
          if(setInitialGraph)
            graphStack.setInitialGraph(dg2ast.initialGraph)
        case Some(cm) =>
          val g = dg2ast.initialGraph.newGraph(constraints = cm)
          logger writeln " done:"
          graphStack.setInitialGraph(g)
          g.printConstraints(logger, defaultVerbosity)
      }
    }


  def printRecording() : Unit = {
    import ShowDG._
    graph.recording.reverseIterator.foreach(r => logger writeln (graph, r).shows)
  }


  def applyOnCode(record : DependencyGraph) : Unit =
    Future {
      logger.write("generating code ...")
      dg2ast(record)
      dg2ast.printCode(project.outDirectory get)
      logger.writeln(" done")
    } onComplete {
      case Success(_) => ()
      case Failure(exc) => exc.printStackTrace()
    }

  def saveRecordOnFile(file : File) : Unit = {
    Recording.write(file.getAbsolutePath, dg2ast.nodesByName, graph)
  }

  def loadRecord(file : File) : Unit = {
    try graphStack.load(Recording.load(file.getAbsolutePath, dg2ast.nodesByName))
    catch {
      case Recording.LoadError(msg, m) =>
        logger writeln ("Record loading error " + msg)
        logger writeln ("cannot bind loaded map " + m.toList.sortBy(_._1).mkString("\n"))
        logger writeln ("with " + dg2ast.nodesByName.toList.sortBy(_._1).mkString("\n"))
    }

  }


  reactions += {

    case PushGraph(g) =>
      graphStack.pushGraph(g)

    case PrintErrOrPushGraph(msg, lgt) =>
      lgt.value match {
        case -\/(err) =>
          logger.writeln(s"$msg\n${err.getMessage}\nLog : ${lgt.log}")
        case \/-(g) =>
          logger.writeln(lgt.log)
          graphStack.pushGraph(g)
      }

    case RewriteHistory(r) =>
      graphStack.rewriteHistory(r)

    case Log(msg) =>
      logger.writeln(msg)

    case gf @ GraphFocus(g, e) =>
      printingOptionsControl.focus(g, e)

    case LoadCodeRequest =>
      if(project == null)
        logger writeln "select a project first"
      else
        loadCodeAndConstraints()

    case LoadConstraintRequest => loadConstraints()

    case SaveRecord(f) =>
      saveRecordOnFile(f)

    case LoadRecord(f) =>
      loadRecord(f)


    case ConstraintDisplayRequest(graph) =>
      graph.printConstraints(logger, defaultVerbosity)

    case ApplyOnCodeRequest(searchResult) =>
      applyOnCode(searchResult)

    case pe : PrintingOptionEvent =>
      pe(printingOptionsControl)

    case GenCode(compareOutput) =>
      import ProjectDG2ASTControllerOps._
      deleteOutDirAndapplyOnCode(dg2ast, project, graphStack.graph)
      if(compareOutput)
        compareOutputGraph(project, graphStack.graph)

    case PrintCode(nodeId) =>
        logger writeln ("Code :\n" + dg2ast.code(graph, nodeId))

  }


}
