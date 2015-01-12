package puck.javaAG.nodeKind

import puck.graph.{AGError, NodeKind}
import puck.graph.constraints.AbstractionPolicy

/**
 * Created by lorilan on 31/07/14.
 */
case object Primitive extends TypeKind {
  def canContain(k: NodeKind) = false
  def abstractKinds(p : AbstractionPolicy) =
    throw new AGError("do not know how to abstract primitive kind")
}