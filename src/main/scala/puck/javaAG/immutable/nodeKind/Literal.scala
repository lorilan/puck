package puck.javaAG.immutable.nodeKind

import puck.graph.constraints.{AbstractionPolicy, DelegationAbstraction}

/**
 * Created by lorilan on 31/07/14.
 */
case object Literal extends JavaNodeKind {
  override val toString = "Literal"

  def canContain(k : JavaNodeKind) = false
  //TODO in case of method abstraction cf field comment
  override def abstractionPolicies = Seq(DelegationAbstraction)
  def abstractKinds(p : AbstractionPolicy) = ???
    ///List(Field(puck.graph.dummyId, typ), Method())
}