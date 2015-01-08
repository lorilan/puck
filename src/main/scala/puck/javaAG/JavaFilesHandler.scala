package puck.javaAG

import java.io.File

import puck.graph.constraints.{Solver, DecisionMaker}
import puck.graph.constraints.search.SolverBuilder
import puck.graph._
import puck.graph.io.{ConstraintSolvingSearchEngineBuilder, FilesHandler}
import puck.graph.transformations.NodeMappingInitialState
import puck.util.PuckLog._


/**
 * Created by lorilan on 11/08/14.
 */

object JavaSolverBuilder extends SolverBuilder{
  def apply(graph : AccessGraph,
            dm : DecisionMaker) : Solver = JavaSolver(graph, dm)
}

class JavaFilesHandler (workingDirectory : File) extends FilesHandler(workingDirectory) {
  def this() = this(new File("."))

  val srcSuffix = ".java"

  var initialRecord : Recording = _

  var sProgram : Option[AST.Program] = None

  def loadGraph(ll : AST.LoadingListener = null) : AccessGraph = {
    import puck.util.FileHelper.{fileLines, findAllFiles, initStringLiteralsMap}

    val sProg = puck.util.Time.time(logger, defaultVerbosity) {
        logger.writeln("Compiling sources ...")
        val sources = findAllFiles(this.srcDirectory.get, srcSuffix, this.outDirectory.get.getName)
        val jars = findAllFiles(this.srcDirectory.get, ".jar", this.outDirectory.get.getName)
        JavaFilesHandler.compile(sources, fileLines(jarListFile.get) ++: jars )

    }
    val g = puck.util.Time.time(logger, defaultVerbosity) {
      logger.writeln("Building Access Graph ...")
      sProg match {
        case None => throw new AGBuildingError("Compilation error, no AST generated")
        case Some(p) => sProgram = Some(p)
          val jGraphBuilder = p.buildAccessGraph(initStringLiteralsMap(decouple.get), ll)
          fileLines(apiNodesFile.get).foreach {
            (l: String) =>
              val tab = l.split(" ")
              jGraphBuilder.addApiNode(tab(0), tab(1), tab(2))
          }

          jGraphBuilder.attachOrphanNodes()

          graphBuilder = jGraphBuilder
          graph = (graphBuilder.g withLogger this.logger).newGraph(nRecording = Recording())

          val (_, transfos) = NodeMappingInitialState.normalizeNodeTransfos(graphBuilder.g.recording(), Seq())
          initialRecord = new Recording(transfos)

          graph
      }
    }

    val (numClass, numItc) = g.nodes.foldLeft((0,0)){ case ((numClass, numItc), n) =>
      val numClass1 = if(n.kind == nodeKind.Class) numClass + 1
          else numClass
        val numItc1 = if(n.kind == nodeKind.Interface) numItc + 1
        else numItc
      (numClass1, numItc1)

    }
    logger.writeln( numClass + " classes and " + numItc + " interfaces parsed")

    g
  }

  val dotHelper = JavaNode

  /*def decisionMaker() = new JavaDefaultDecisionMaker(graph)

  def solver(dm : DecisionMaker[JavaNodeKind]) =
    new JavaSolver(graph, dm)*/

  def applyChangeOnProgram(result : ResultT){

    logger.writeln("applying change !")

    val record = recordOfResult(result)
    val program = sProgram.get
    val applyer = new AG2AST(program, logger)

    record.foldRight((graphOfResult(result), graph)) {
      case (r, (resultGraph, reenactor)) =>
        logger.writeln("/!\\ cache flushed after each transformations, may take some time")
        //println(r)
        //println("before " + program.getNumCUFromSrc + " cus in prog")
        val jreenactor = reenactor.asInstanceOf[JavaAccessGraph]
        val res = applyer(resultGraph, jreenactor, r)
        //println("after " + program.getNumCUFromSrc + " cus in prog")
        program.flushCaches()
        res
    }
    //printCode()
    program.flushCaches()
    program.eliminateLockedNames()
  }

  def printCode() {
    sProgram match {
      case Some(p) => p.printCodeInDirectory(outDirectory.get)
      case None => logger.writeln("no program registered")
    }

  }

  override def searchingStrategies: Seq[ConstraintSolvingSearchEngineBuilder] =
    List(JavaTryAllCSSEBuilder,
      JavaFunneledCSSEBuilder/*,
      //JavaGradedCSSEBuilder,
      JavaFindFirstCSSEBuilder*/)
}


object JavaFilesHandler {

  def apply() = new JavaFilesHandler()
  def apply(file : java.io.File) = new JavaFilesHandler(file)

  def compile(sources: List[String], jars: List[String]): Option[AST.Program] = {
    val arglist = createArglist(sources, jars, List())
    val f = new AST.Frontend {
      protected override def processWarnings(errors: java.util.Collection[_], unit: AST.CompilationUnit) {
      }
    }
    val br = new AST.BytecodeParser
    val jp = new AST.JavaParser {
      def parse(is: java.io.InputStream, fileName: String): AST.CompilationUnit = {
        (new parser.JavaParser).parse(is, fileName)
      }
    }

    if (f.process(arglist, br, jp)){
      Some(f.getProgram)}
    else
      None
  }

  private[puck] def createArglist(sources: List[String],
                                  jars: List[String],
                                  srcdirs:List[String]): Array[String] = {

    if (jars.isEmpty) sources.toArray
    else {
    val args: List[String] = "-classpath" :: jars.mkString("", File.pathSeparator, File.pathSeparator + ".") :: (
        if (srcdirs.isEmpty) sources
        else "-sourcepath" :: srcdirs.mkString("", File.pathSeparator, File.pathSeparator + ".") :: sources)
      args.toArray
    }
  }

}