package puck.graph

import puck.graph.constraints.AbstractionPolicy

import scala.collection.mutable

/**
 * Created by lorilan on 11/06/14.
 */

sealed abstract class Transformation {
  def undo() : Unit
}

class CompositeTransformation extends Transformation{

  val sequence = new mutable.Stack[Transformation]()

  def push(t : Transformation) = sequence.push(t)

  def undo(){
    while(sequence.nonEmpty){
      val t = sequence.pop()
      t.undo()
    }
  }
}

class AddNode( node : AGNode) extends Transformation {
  override def toString = "add node %s".format(node)

  def undo(){
    val g = node.graph
    g.remove(node)
  }
}
class RemoveNode( node : AGNode) extends Transformation{
  override def toString = "remove node %s".format(node)
  def undo(){
    val g = node.graph
    g.addNode(node)
  }
}
class AddEdge( edge : AGEdge) extends Transformation {
  override def toString = "add edge " + edge
  def undo(){ edge.delete()}
}
class RemoveEdge( edge : AGEdge) extends Transformation{
  override def toString = "remove edge " + edge
  def undo(){ edge.create()}
}
class AddEdgeDependency(dominant : AGEdge, dominated : AGEdge) extends Transformation{
  def undo(){
    val g = dominant.source.graph
    g.removeUsesDependency(dominant, dominated)
  }
}
class RemoveEdgeDependency(dominant : AGEdge, dominated : AGEdge) extends Transformation{
  def undo(){
    val g = dominant.source.graph
    g.addUsesDependency(dominant, dominated)
  }
}

class RegisterAbstraction( impl : AGNode, abs :AGNode,
                           policy : AbstractionPolicy) extends Transformation {
  def undo(){
    impl.abstractions_-=(abs, policy)
  }
}
class UnregisterAbstraction( impl : AGNode, abs : AGNode,
                             policy : AbstractionPolicy) extends Transformation{
  def undo(){
    impl.abstractions_+=(abs, policy)
  }
}


