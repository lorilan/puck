name := "PuckConstraintSolver"

version := "1.0"

scalaVersion := "2.11.2"

mainClass in Compile := Some("puck.Front")

puckSvnDir := file(System.getenv("PUCK_SVN_DIR"))

jrrtHome := puckSvnDir.value / "build"

jrrtReadOnly := jrrtHome.value / "jrrt-read-only"

java14frontend := jrrtHome.value / "Java1.4Frontend"

java15frontend := jrrtHome.value / "Java1.5Frontend"

controlFlowGraph := jrrtHome.value / "ControlFlowGraph"

jastaddSrcDir := baseDirectory.value / "src" / "main" / "jrag"

jastaddOutDir := baseDirectory.value / "src" / "main" / "gen"

java15comply := true


libraryDependencies ++= Seq( 
	"org.scala-lang" % "scala-swing" % "2.11.0-M7",
	"org.scala-lang" % "scala-parser-combinators" % "2.11.0-M4",
  "org.scalatest" % "scalatest_2.11" % "2.2.1"
	)

//http://mvnrepository.com/artifact/net.sf.beaver/beaver-cc/0.9.11
//http://mvnrepository.com/artifact/net.sf.beaver/beaver-rt/0.9.11
//http://mvnrepository.com/artifact/de.jflex/jflex/1.6.0