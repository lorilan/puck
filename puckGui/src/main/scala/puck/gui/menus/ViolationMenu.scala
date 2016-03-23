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

package puck.gui.menus

import javax.swing.JPopupMenu

import puck.graph.constraints.ConstraintsMaps
import puck.graph.{DependencyGraph, GraphUtils, NodeId}
import puck.gui.PrintingOptionsControl
import puck.gui.svg.actions.AutoSolveAction

import scala.swing.Publisher

/**
  * Created by Loïc Girault on 17/12/15.
  */
class ViolationMenu
(publisher : Publisher,
 target : NodeId,
 printingOptionsControl: PrintingOptionsControl,
 constraints : ConstraintsMaps)
(implicit graph : DependencyGraph,
  graphUtils : GraphUtils)
  extends JPopupMenu {

  val targetNode = graph.getConcreteNode(target)
  //add(new ManualSolveAction(publisher, targetNode))
  add(new AutoSolveAction(publisher, constraints, targetNode, printingOptionsControl))

}
