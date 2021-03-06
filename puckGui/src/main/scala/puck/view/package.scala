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

import java.awt.MouseInfo
import java.awt.event.MouseEvent
import java.io.File
import java.net.URL
import javax.swing._

import scala.swing.Action
import scala.swing._
import scala.swing.event.MouseClicked

/**
  * Created by Loïc Girault on 17/12/15.
  */
package object view {
  def isRightClick(e : MouseEvent) : Boolean = {
    MouseInfo.getNumberOfButtons > 2 && e.getButton == MouseEvent.BUTTON3 ||
      MouseInfo.getNumberOfButtons == 2 && e.getButton == MouseEvent.BUTTON2
  }

  def leftGlued(c : Component) : BoxPanel = {
    new BoxPanel(Orientation.Horizontal) {
      contents += c
      contents += Swing.HGlue
    }
  }

  implicit class LeftGlued(val c : Component) extends AnyVal {
    def leftGlued : BoxPanel = view.leftGlued(c)
  }


  implicit def actionToMenuItem(act : Action) : MenuItem = new MenuItem(act)

  private def chooseFile
  ( currentDir : File,
    chooserMode : JFileChooser => Int) : Option[File] = {
    val chooser = new JFileChooser()
    chooser.setCurrentDirectory(currentDir)
    val returnVal: Int = chooserMode(chooser)
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      Some(chooser.getSelectedFile)
    }
    else None
  }

  def openFile(currentDir : File, parent : java.awt.Component) : Option[File] =
    chooseFile(currentDir, chooser => chooser.showOpenDialog(parent) )
  def saveFile(currentDir : File, parent : java.awt.Component) : Option[File] =
    chooseFile(currentDir, chooser => chooser.showSaveDialog(parent) )

  def button(name:String)
            (action : () => Unit) : Button =
    new Button(new Action(name){
      def apply() = action()
    })

  val addimg = getClass.getResource("/icons/add.png")
  val deleteimg = getClass.getResource("/icons/delete.png")
  val editimg = getClass.getResource("/icons/edit.png")

  def buttonLabel(img : URL)(action : => Unit) = new Label {
    icon = new ImageIcon(img)
    listenTo(mouse.clicks)
    reactions += {
      case mc@MouseClicked(_, _, _, _, _) => action
    }
  }

}
