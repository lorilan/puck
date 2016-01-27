package puck
package gui
package explorer

import java.awt.event.{ActionEvent, MouseEvent, MouseAdapter}
import javax.swing.{AbstractAction, JPopupMenu, JTree}

import puck.graph._
import javax.swing.tree._


import puck.graph.io.PrintingOptions
import puck.gui.menus.{NodeMenu, ViolationMenu}

import scala.swing.BorderPanel.Position
import scala.swing._
import scala.swing.event.{MouseClicked, Event}

case class FilterSource(filter : NodeId) extends Event
case class FilterTarget(filter : NodeId) extends Event


object ConstraintViolationExplorer {
  def idToNameString(dg : DependencyGraph, nid : NodeId) : String =
    dg.getNode(nid) match {
      case n : ConcreteNode =>
        val name =
          if(n.kind.kindType == ValueDef)
            dg.container(n.id) map {
              idToNameString(dg, _)
            } getOrElse "OrphanDefinition"
          else n.name
        name + ShowDG.typeHolderCord(dg, dg.styp(n.id))
      case vn : VirtualNode => vn.name(dg)
    }
  def edgeToString(e:  DGEdge )(implicit graph : DependencyGraph): String = {
    val nodeName : NodeId => String = idToNameString(graph, _)

    if (e.source == e.target) s"${graph.getNode(e.source).kind} - ${nodeName(e.source)}"
    else {
      val (ksrc, ktgt) = (graph.getNode(e.source).kind, graph.getNode(e.target).kind)
      e.kind match {
        case AbstractEdgeKind =>
          val s = if (ksrc.toString endsWith "s") "es"
          else "s"
          if (ksrc == ktgt) s"$ksrc$s : ${nodeName(e.source)} -> ${nodeName(e.target)}"
          else s"$ksrc : ${nodeName(e.source)} -> $ktgt : ${nodeName(e.target)}"

        case _ =>
          s"${nodeName(e.source)} ($ksrc) - ${e.kind} -> ${nodeName(e.target)} ($ktgt)"
      }
    }
  }

}
import ConstraintViolationExplorer._

class ConstraintViolationExplorer
(publisher : Publisher,
 allViolations : Seq[DGEdge],
 treeIcons : DGTreeIcons,
 printingOptionsControl: PrintingOptionsControl)
( implicit graph : DependencyGraph,
  graphUtils : GraphUtils)
  extends SplitPane {



  def filterViolations
  ( sourceFilter : Option[NodeId],
    targetFilter : Option[NodeId]) : Seq[DGEdge] = {

    val vs = sourceFilter map (srcId =>
      allViolations.filter(e =>
      graph.contains_*(srcId, e.source)
    )) getOrElse allViolations

    targetFilter map (tgtId => vs.filter(e =>
      graph.contains_*(tgtId, e.target)
    )) getOrElse vs
  }

  import swing.Component.wrap
  def sources(violations : Seq[DGEdge]) : Set[NodeId] =
    violations.foldLeft(Set[NodeId]())((s,e) => s + e.source)
  def targets(violations : Seq[DGEdge]) : Set[NodeId] =
    violations.foldLeft(Set[NodeId]())((s,e) => s + e.target)


  trait ViolationTreeHandle {
    def extremities(violations : Seq[DGEdge]) : Set[NodeId]
    def exty(edge : DGEdge) : NodeId
    def event(nodeId : NodeId) : Event
  }

  class ViolationTree(handle : ViolationTreeHandle) extends
    JTree(DGTreeModel.subGraph(graph, handle.extremities(allViolations))) with DGTree {
    def icons : DGTreeIcons = treeIcons

    override def convertValueToText
    (value: AnyRef, selected: Boolean,
     expanded: Boolean, leaf: Boolean,
     row: Int, hasFocus: Boolean) : String =
      value match {
        case null => ""
        case node : DGNode  =>

          val vsCount = allViolations.count (e => graph.contains_*(node.id, handle.exty(e)))
          s"${node.name} ($vsCount)"
        case _ => ""
      }

    addMouseListener( new MouseAdapter {
      override def mouseClicked(e : MouseEvent) : Unit =  {
        val path : TreePath = ViolationTree.this.getPathForLocation(e.getX, e.getY)
        if(path!= null){
          path.getLastPathComponent match {
            case n : DGNode =>
              if(isRightClick(e)) Swing.onEDT {
                val menu = NodeMenu(publisher, ConstraintViolationExplorer.this.graph,
                  graphUtils, List(), None, n.id, printingOptionsControl)
                menu.add(new AbstractAction("Node infos") {
                  def actionPerformed(e: ActionEvent): Unit =
                    ConstraintViolationExplorer.this.publish(NodeClicked(n))
                })
                menu.show(targetTree, e.getX, e.getY)
              } else {
                ConstraintViolationExplorer.this.publish(handle.event(n.id))
                if(n.kind.kindType != NameSpace)
                  ConstraintViolationExplorer.this.publish(NodeClicked(n))
              }
            case _ => ()
          }
        }
      }
    })
  }

  val sourceTree = new ViolationTree(new ViolationTreeHandle {
    def exty(edge: DGEdge): NodeId = edge.source

    def extremities(violations: Seq[DGEdge]): Set[NodeId] = sources(violations)

    def event(nodeId: NodeId): Event = FilterSource(nodeId)
  })

  val targetTree = new ViolationTree(new ViolationTreeHandle {
    def exty(edge: DGEdge): NodeId = edge.target

    def extremities(violations: Seq[DGEdge]): Set[NodeId] = targets(violations)

    def event(nodeId: NodeId): Event = FilterTarget(nodeId)
  })

  def selection(jTree: JTree): Option[NodeId] =
    Option(jTree.getLastSelectedPathComponent.asInstanceOf[DGNode]) map (_.id)

  def selectedSource : Option[NodeId] = selection(sourceTree)
  def selectedTarget : Option[NodeId] = selection(targetTree)


  var sourceFilter0 : Option[NodeId] = None
  var targetFilter0 : Option[NodeId] = None

  def sourceFilter : Option[NodeId] = sourceFilter0
  def sourceFilter_=(sid : Option[NodeId]) = {
    sourceFilter0 = sid
    Swing.onEDT {
      val vs = filterViolations(sourceFilter, targetFilter)
      targetTree.setModel(DGTreeModel.subGraph(graph, targets(vs)))
      sourceLabel.text = sid map graph.fullName getOrElse ""
      updateViolationListPane(vs)
    }
  }
  def targetFilter : Option[NodeId] = targetFilter0
  def targetFilter_=(sid : Option[NodeId]) = {
    targetFilter0 = sid
    Swing.onEDT {
      val vs = filterViolations(sourceFilter, targetFilter)
      sourceTree.setModel(DGTreeModel.subGraph(graph, sources(vs)))
      targetLabel.text = sid map graph.fullName getOrElse ""
      updateViolationListPane(vs)
    }
  }

  val sourceLabel = new Label()
  val targetLabel = new Label()

  reactions += {
    case FilterSource(srcFilter) if srcFilter == graph.rootId =>
      sourceFilter = None

    case FilterSource(srcFilter) =>
      sourceFilter = Some(srcFilter)

    case FilterTarget(tgtFilter) if tgtFilter == graph.rootId =>
      targetFilter = Some(tgtFilter)

    case FilterTarget(tgtFilter) =>
      targetFilter = Some(tgtFilter)
  }

  resizeWeight = 0.4
  this.leftComponent = new SplitPane(Orientation.Vertical){
    resizeWeight = 0.5

    this.leftComponent = new BorderPanel {
        add( new Label("Sources"), Position.North)
        add( new ScrollPane{
           contents = wrap(sourceTree)
          }, Position.Center)
      add(sourceLabel, Position.South)

    }

    this.rightComponent = new BorderPanel {
      add( new Label("Targets"), Position.North)
      add( new ScrollPane{
        contents = wrap(targetTree)
      }, Position.Center)
      add(targetLabel, Position.South)
    }


  }

  val violationListPane = new BoxPanel(Orientation.Vertical)

  this.rightComponent = new ScrollPane() {
    contents = violationListPane
  }


  def updateViolationListPane(violations : Seq[DGEdge]) : Unit = {
    violationListPane.contents.clear()
    violationListPane.contents +=
      new Label(s"${violations.size} / ${allViolations.size} violation" +
        (if(violations.size > 1) "s" else ""))

    violations.foreach {
      edge =>
        violationListPane.contents +=  new Label(edgeToString(edge)) {
          listenTo(mouse.clicks)
          reactions += {
            case mc @ MouseClicked(_,_,_,_,_) =>
              val evt = mc.peer
              if(isRightClick(evt)){
                val menu : JPopupMenu = new ViolationMenu(publisher, edge.target, printingOptionsControl){
                  add( new AbstractAction("Focus in graph explorer") {
                    def actionPerformed(e: ActionEvent): Unit =
                      publisher.publish(GraphFocus(graph, edge))
                  })
                }
                Swing.onEDT(menu.show(this.peer, evt.getX, evt.getY))
              }
              else if(evt.getClickCount > 1)
                publisher.publish(GraphFocus(graph, edge))
          }
        }
    }
    violationListPane.revalidate()
    violationListPane.repaint()

  }
  updateViolationListPane(allViolations)
}


