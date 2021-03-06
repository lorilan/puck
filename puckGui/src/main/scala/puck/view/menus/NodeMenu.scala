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

package puck
package view
package menus



import puck.control.{NodeClicked, PuckControl, PushGraph, SetMutability}
import puck.control.actions._
import puck.graph._
import puck.graph.constraints.ConstraintsMaps
import puck.graph.transformations.MutabilitySet.MutabilitySetOps
import puck.graph.transformations.rules.Redirection
import puck.view.svg.actions.{Log, TargetedAutoSolveAction}

import scala.swing._

/**
  * Created by Loïc Girault on 16/12/15.
  */

object NodeMenu{

  type Builder = (DependencyGraph, NodeId, List[NodeId], Option[NodeIdP]) => PopupMenu

  def apply(control : PuckControl,
            graph : DependencyGraph,
            nodeId : NodeId,
            selectedNodes: List[NodeId],
            selectedEdge : Option[NodeIdP])
           (implicit treeIcons: NodeKindIcons,
            cm : Option[ConstraintsMaps]): PopupMenu =
    graph.getNode(nodeId) match {
      case n : ConcreteNode =>
        new ConcreteNodeMenu(control,graph,
          selectedNodes, selectedEdge,
          blurryEdgeSelection = false,
          n, treeIcons)
      case n : VirtualNode =>
        import control.{Bus, graphUtils}
        new VirtualNodeMenu(Bus, graph, graphUtils, n)
    }
}

class ConcreteNodeMenu
(controller : PuckControl,
 implicit val graph : DependencyGraph,
 val selectedNodes: List[NodeId],
 val selectedEdge : Option[NodeIdP],
 blurryEdgeSelection : Boolean,
 node : ConcreteNode,
 implicit val nodeKindIcons: NodeKindIcons)
  extends PopupMenu {

  import controller._
  implicit val graphUtils = controller.graphUtils
  implicit val mutabilitySet = controller.mutabilitySet
  init()



  def init(): Unit = {

    contents += new RenameNodeAction(Bus, graph, node, graphUtils)
    contents += new Separator()

    contents += new MenuItem(s"Abstract ${node.name} as") {
      enabled = false
    }

    abstractionChoices foreach contents.+=

    if (childChoices.nonEmpty) {
      contents += new Separator()
      childChoices foreach contents.+=
    }

    if (node.kind.isWritable) {
      contents += new CreateInitalizerAction(Bus, graph,
        graph.getConcreteNode(graph.hostTypeDecl(node.id)), graphUtils)
    }


    contents += new Separator()
    contents += {
      val mutability = mutabilitySet mutability node.id
      new Action(s"Set $node " + mutability.opposite) {
        def apply(): Unit =
          Bus publish SetMutability(node.id, mutability.opposite)
      }
    }

    contents += new RemoveNodeAction(Bus, graph, node, graphUtils)

    selectedNodes match {
      case Nil => ()
      case List(nid) => addOtherNodeSelectedOption(nid)
      case nodes => addOtherNodesSelectedOption(nodes)
    }

    selectedEdge.toList flatMap {
      case (source, target) =>
        graph.nodePlusDefAndParams(source) map ((_, target))
    } foreach addEdgeSelectedOption


    constraints foreach {
      cm =>
        if (cm.isWronglyContained(graph, node.id)
          || cm.isWronglyUsed(graph, node.id)) {
          contents +=
            new TargetedAutoSolveAction(Bus, cm, mutabilitySet, node,
              printingOptionsControl)(graph, graphUtils, nodeKindIcons)
        }
     }
    contents += new Separator()
    ignore(contents += new Action("Infos"){
      def apply() : Unit = Bus publish NodeClicked(node)
    })
  }

  def abstractionChoices : Seq[MenuItem] =
    node.kind.abstractionChoices.map { case (k, p) =>
      new MenuItem(new AbstractionAction(Bus, node, p, k))
    }

  def childChoices : Seq[MenuItem] = {
    val ks = graph.nodeKinds.filter(node.kind.canContain)
    ks map {k => new MenuItem(new AddNodeAction(Bus, node, k))}
  }

  private def addAddIsaOption(sub: ConcreteNode, sup: ConcreteNode): Unit = {
    if(!graph.isa(sub.id, sup.id) &&  graph.canBe(sub, sup))
      ignore( contents += new AddIsaAction(Bus, sub, sup) )
  }


  private def addOtherNodesSelectedOption(ids: List[NodeId]): Unit = {
    val sContainer = graph.container(ids.head)
    val sameContainer = ids.tail forall (graph.container(_) == sContainer)
    val kt = graph.kindType(ids.head)
    val sameKind = ids.tail forall (graph.kindType(_) == kt)
    if (!sameContainer)
      Bus publish Log("Move multiple only available for nodes with same container")
    else if (!sameKind)
      Bus publish Log("Move multiple only available for nodes with same kind")
    else {
      val selected: ConcreteNode = graph.getConcreteNode(ids.head)
      if (graph.canContain(node, selected))
        ignore(  contents += new MoveAction(Bus, node, ids) )
    }

  }

  private def addOtherNodeSelectedOption(id: NodeId): Unit = {
    val selected: ConcreteNode = graph.getConcreteNode(id)
    if (graph.canContain(node, selected)) {
      contents += new MoveAction(Bus, node, List(id))
    }

    //    val m: MergeMatcher = controller.transfoRules.
    //        mergeMatcherInstances.syntaxicMergeMatcher(selected)
    //
    //    if (m.canBeMergedInto(node, graph))
    if (selected.kind.kindType == node.kind.kindType)
      contents += new MergeAction(Bus, selected, node)


    if (selected.id != node.id) {
      if (selected.kind.canBe(node.kind)) addAddIsaOption(selected, node)
      if (node.kind.canBe(selected.kind)) addAddIsaOption(node, selected)
    }
  }

  private def addEdgeSelectedOption(edge: NodeIdP): Unit = {
    val (source, target) = edge

    def addRedirectAction(uses: NodeIdP) =
      graph.abstractions(target).foreach {
        abs =>
          if (abs.nodes.contains(node.id))
            contents += new RedirectAction(Bus, graph, uses, abs)
      }

    def addChangeInitUsesAction(ctorDef: NodeId) =
      (graph.getRole(node.id), graph.getRole(target)) match {
        case (Some(Factory(ctorId)), Some(Initializer(_)))
          if ctorId == source =>
          contents += new Action("Call to initialization in factory") {
            def apply() : Unit = {
              val g = Redirection.redirectSourceOfInitUseInFactory(graph.mileStone,
                ctorId, ctorDef, target, node.id)
              Bus.publish(PushGraph(g))
            }
          }
        case _ => ()
      }

    if(graph.uses(source, target))
        addRedirectAction(edge)


    graph.definitionOf(source).foreach {
      userDef =>
        if(graph.uses(userDef, target)) {
            addRedirectAction((userDef, target))
            addChangeInitUsesAction(userDef)
        }
    }


  }
}

class VirtualNodeMenu
(controller: Publisher,
 graph : DependencyGraph,
 graphUtils : GraphUtils,
 node : VirtualNode
) extends PopupMenu {

  node.potentialMatches foreach {
    id =>
      val consumer = graph.getConcreteNode(id)
      import graphUtils.{Rules => TR}
      contents += new MenuItem(new Action(s"Concretize as $consumer") {
        def apply() : Unit =
          printErrOrPushGraph(controller,"Concretize action failure") {
            TR.merge.mergeInto(graph.mileStone, node.id, consumer.id)
          }
      })
  }

}