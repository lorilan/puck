package puck.javaGraph

import puck.graph.{NodeId, DependencyGraph}
import puck.graph.transformations.Transformation
import puck.util.{PuckFileLogger, PuckNoopLogger, PuckLogger}

case class ExampleSample
( program : AST.Program,
  graph : DependencyGraph,
  initialRecord : Seq[Transformation],
  fullName2id : Map[String, NodeId],
  dg2astMap : Map[NodeId, ASTNodeLink]){

  def this(t : (AST.Program,
                DependencyGraph,
                Seq[Transformation],
                Map[String, NodeId],
                Map[NodeId, ASTNodeLink])) =
        this(t._1, t._2, t._3, t._4, t._5)

  def this(filePath : String) =
    this(CompileHelper.compileSrcsAndbuildGraph(List(filePath), List()))
  def this(filesPath : String*) =
    this(CompileHelper.compileSrcsAndbuildGraph(filesPath.toList, List()))

  implicit var logger : PuckLogger = new PuckFileLogger(_ => true, new java.io.File("/tmp/comparisonLog"))
  def compare: (DependencyGraph, DependencyGraph) => Boolean =
    (g1, g2) => DependencyGraph.areEquivalent(initialRecord,g1,g2, logger)
}