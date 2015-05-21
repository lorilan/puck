package puck.javaGraph

import puck.PuckError
import puck.graph.ShowDG._
import puck.graph._
import puck.graph.constraints.SupertypeAbstraction
import puck.util.Collections._

import scalaz.{-\/, \/-}

object JavaType{
  def findOverridedMethodIn(g : DependencyGraph,
                          absName : String, absSig : MethodType,
                          candidates : List[ConcreteNode]) : Option[(ConcreteNode, List[ConcreteNode])] = {
    import puck.util.Collections.SelectList
    candidates.select( c => c.styp.nonEmpty &&
        c.name == absName &&
        c.styp.exists(absSig.canOverride(g, _)))
  }


  def findAndRegisterOverridedMethods
  ( g : DependencyGraph,
    className : String,
    absMeths : List[ConcreteNode],
    candidates : List[ConcreteNode]): Try[DependencyGraph] =
  //    Foldable[List].foldLeftM[Option, ConcreteNode, (DependencyGraph, List[ConcreteNode])](absMeths, (g, candidates.toList)){
    traverse(absMeths, (g, candidates) ){
      case ((g0, cs), supMeth) =>
        supMeth.styp match {
          case Some(mt : MethodType ) =>
            findOverridedMethodIn(g0, supMeth.name, mt, cs) match {
              case Some((subMeth, newCandidates)) =>
                \/-( (g0.addAbstraction(subMeth.id, (supMeth.id, SupertypeAbstraction)), newCandidates) )
              case None => -\/(new PuckError(s"$className do not have an implementation of ${showDG[ConcreteNode](g).shows(supMeth)}"))
            }
          case _ => -\/(new PuckError(s"${showDG[ConcreteNode](g).shows(supMeth)} has not a correct type"))
        }
    } map(_._1)

}

class JavaNamedType(n : NodeId) extends NamedType(n){

 override def copy(n : NodeId = n) = new JavaNamedType(n)

 /*def hasMethodThatCanOverride(name : String, sig : MethodType) : Boolean =
    n.content.exists{ (childThis : AGNode[JavaNodeKind]) =>
      childThis.name == name &&
        (childThis.kind match {
          case m @ Method() => m.`type`.canOverride(sig)
          case _ => false
        })
    }*/
/*
  // compute structural subtyping in addition to registered named subtyping
  override def subtypeOf(other : Type) : Boolean = super.subtypeOf(other) ||
    ( other match {
      case NamedType(othern) =>
        (n.kind, othern.kind) match {
          case (Interface(), Interface())
            | (Class(), Interface())
            | (Class(), Class()) =>
            /* TODO Find what to do about fields
               for now let's consider only the methods */
            othern.content forall { (childOther : AGNode) =>
                childOther.kind match {
                case m @ Method() => hasMethodThantCanOverride(childOther.name, m.`type`)
                case am @ AbstractMethod() => hasMethodThantCanOverride(childOther.name, am.`type`)
                case _ => true
              }
            }
          case _ => false
        }
      case _ => false
    })*/
}

object MethodType{
 def unapply( mt : MethodType) : Option[(Tuple, NamedType)] =
   Some((mt.input, mt.output))

  /*def unapply( t : Type) : Option[(Tuple, NamedType)] =
    t match {
      case mt : MethodType => Some((mt.input, mt.output))
      case _ => None
    }*/
}

class MethodType(override val input : Tuple,
                 override val output : NamedType)
  extends Arrow(input, output){

  /*override def equals(other : Any) = other match {
    case that : MethodType => that.canEqual(this) &&
      that.input == this.input && that.output == this.output
    case _ => false
  }

  def canEqual( that : MethodType ) = true

  override def hashCode = 41 * input.hashCode + output.hashCode() + 41*/
  override def toString = "MethodType(" + input +" -> " + output +")"

  override def canOverride(graph : DependencyGraph, other : Type) : Boolean =
    other match {
      case om : MethodType => om.input == input &&
        output.subtypeOf(graph, om.output)
      case _ => false
    }

  override def changeNamedType(oldUsee : NodeId, newUsee: NodeId) : MethodType =
    copy(input.changeNamedType(oldUsee, newUsee),
      output.changeNamedType(oldUsee, newUsee))

  override def changeNamedTypeContravariant(oldUsee : NodeId, newUsee: NodeId) =
    copy(input.changeNamedType(oldUsee, newUsee), output)

  def copy(i : Tuple, o : NamedType) : MethodType = new MethodType(i, o)

  override def copy(i : Type = input, o : Type = output) : MethodType =
    (i,o) match {
      case (t @ Tuple(_), nt @ NamedType(_)) => copy(t, nt)
      case _ => throw new PuckError("Trying to create ad malformed method type")
    }


  def createReturnAccess(graph : DependencyGraph,
                         id2Decl : Map[NodeId, ASTNodeLink]) =
    id2Decl(output.id) match {
    case tk : TypedKindDeclHolder => tk.decl.createLockedAccess()
    case _ => throw new JavaAGError("need a typekind as output node")
  }

  def createASTParamList(graph : DependencyGraph,
                         id2Decl : Map[NodeId, ASTNodeLink]) : Seq[AST.ParameterDeclaration] = {
    input.types.map {
      case ty : NamedType =>

        val node = graph.getConcreteNode(ty.id)
        id2Decl(ty.id) match {
          case tk : TypedKindDeclHolder =>

            new AST.ParameterDeclaration(new AST.Modifiers, tk.decl.createLockedAccess(), node.name.toLowerCase)

          case _ => throw new JavaAGError("need type kind for param list !")
        }
      case _ => throw new PuckError("Unexpected not NamedType in param list")

    }
  }
}
