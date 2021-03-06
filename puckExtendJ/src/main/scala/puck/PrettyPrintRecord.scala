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

import java.io.File

import org.extendj.JavaProject
import puck.graph.ShowDG
import puck.graph.transformations.Recording
import puck.util.PuckSystemLogger
//object PrettyPrintRecord {
//  def main (args: Array[String]) : Unit = {
//
//    val recFileName = args.head
//    val recFile = new File(recFileName)
//    val (_,_,r) = Recording.read(recFile.getAbsolutePath)
//
//    r.reverseIterator foreach println
//
//  }
//}

object PrettyPrintRecord {

  def main (args: Array[String]) : Unit = {

    if(args.isEmpty) {
      println("PrintRecord recFile [projectConfFile]")
      System exit 1
    }
    val recFileName = args.head
    val recFile = new File(recFileName)

    val fh =
      if(args.length >= 2)
        JavaProject withConfig args(1)
      else JavaProject()
    implicit val logger = new PuckSystemLogger(_ => true)

    val dg2ast = fh.loadGraph()

    val r = Recording.load(recFile.getAbsolutePath, dg2ast.nodesByName)

    import ShowDG._
    val _ = r.reverse.foldLeft(dg2ast.initialGraph){(g0, t) =>
      (g0, t).println
      t.redo(g0)
    }

  }

}