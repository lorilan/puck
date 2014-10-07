package puck.graph.constraints.search

import puck.graph.backTrack.Recording
import puck.graph.{AccessGraph, NodeKind}
import puck.search.FindFirstSearchEngine
import puck.util.Logger

/**
 * Created by lorilan on 25/09/14.
 */
//CSSE : Constraint Solving Search Engine
class FindFirstCSSE[Kind <: NodeKind[Kind]](val logger : Logger[Int],
                                            val violationsKindPriority : List[Kind],
                                            val graph : AccessGraph[Kind],
                                             sb : SolverBuilder[Kind],
                                             solverLogger : Logger[Int])
  extends ConstraintSolvingSearchEngineDecisionMaker[Kind](sb, solverLogger)
  with FindFirstSearchEngine[Recording[Kind]]