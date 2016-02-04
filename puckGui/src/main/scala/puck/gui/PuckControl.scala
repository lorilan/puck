package puck.gui

import java.io.File

import puck.{FilesHandlerDG2ASTControllerOps, StackListener, GraphStack, LoadingListener}
import puck.graph._
import puck.graph.io._

import puck.util.{PuckLogger, PuckLog}

import scala.concurrent.Future
import scala.swing.{ProgressBar, Publisher}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.{\/-, -\/}



class PuckControl
( logger0 : PuckLogger,
  val filesHandler : FilesHandler,
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
  var dg2ast: DG2AST = _
  val graphStack: GraphStack = new GraphStack(Bus)

  def graph: DependencyGraph = graphStack.graph

  import PuckLog.defaultVerbosity


  private[this] var printingOptionsControl0: PrintingOptionsControl = _

  def printingOptionsControl = printingOptionsControl0


  def loadCodeAndConstraints() = Future {
    progressBar.visible = true
    progressBar.value = 0

    dg2ast = filesHandler.loadGraph(Some(new LoadingListener {
      override def update(loading: Double): Unit =
        progressBar.value = (loading * 100).toInt
    }))
    progressBar.visible = false


    printingOptionsControl0 = PrintingOptionsControl(dg2ast.initialGraph, Bus)


  } onComplete {
    case Success(_) => loadConstraints(setInitialGraph = true)
    case Failure(exc) =>
      progressBar.visible = false
      exc.printStackTrace()
  }


  def loadConstraints(setInitialGraph : Boolean = false) : Unit = {
      logger.writeln("Loading constraints ...")

      filesHandler.parseConstraints(dg2ast) match {
        case None if setInitialGraph =>
          graphStack.setInitialGraph(dg2ast.initialGraph)
        case Some(cm) =>
          val g = dg2ast.initialGraph.newGraph(constraints = cm)
          logger.writeln(" done:")
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
      dg2ast.printCode(filesHandler.outDirectory !)
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

    case LoadCodeRequest => loadCodeAndConstraints()

    case LoadConstraintRequest => loadConstraints()

    case SaveRecord(f) =>
      saveRecordOnFile(f)

    case LoadRecord(f) =>
      loadRecord(f)
      Bus publish GraphUpdate(graph)

    case ConstraintDisplayRequest(graph) =>
      graph.printConstraints(logger, defaultVerbosity)

    case ApplyOnCodeRequest(searchResult) =>
      applyOnCode(searchResult)

    case pe : PrintingOptionEvent =>
      pe(printingOptionsControl)

    case GenCode(compareOutput) =>
      import FilesHandlerDG2ASTControllerOps._
      deleteOutDirAndapplyOnCode(dg2ast, filesHandler, graphStack.graph)
      if(compareOutput)
        compareOutputGraph(filesHandler, graphStack.graph)

    case PrintCode(nodeId) =>
        logger writeln ("Code :\n" + dg2ast.code(graph, nodeId))

  }


}
