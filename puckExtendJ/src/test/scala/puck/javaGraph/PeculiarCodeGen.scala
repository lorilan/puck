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

package puck.javaGraph

import puck.graph.comparison.Mapping
import puck.{Settings, AcceptanceSpec}

/**
  * Created by Loïc Girault on 04/02/16.
  */
class PeculiarCodeGen extends AcceptanceSpec {

  feature("Parse lock unlock gen") {

    def makeTest(f: String): Unit = {
      val _ = new ScenarioFactory(f) {
        val recompiledEx = applyChangeAndMakeExample(graph, Settings.outDir)
        assert(Mapping.equals(graph, recompiledEx.graph))
      }
    }

    info("tests generated by finding samples of code that required debugging")

    scenario("Anonymous class") {
      makeTest(
        """package p;
          |
          |interface A{ void ma();}
          |
          |public class Test{
          |    public void main(String[] args){
          |        A a = new A(){
          |           public void ma(){System.out.println("I'm an anonymous A !");}
          |        };
          |    }
          |}
          |
        """.stripMargin)
    }

    scenario("enum") {
      makeTest("""package p;
                 |
                 |enum E { A, B, C; }""")
    }

    scenario("inner enum") {
      makeTest("""package p;
                 |
                 |interface I { enum E { A, B, C; }; }""")
    }

    scenario("method call on anonymous class instantiation") {
      makeTest(
        """package p;
          |
          |interface A{ void ma();}
          |
          |public class Test{
          |    public void main(String[] args){
          |        new A(){
          |           public void ma(){System.out.println("I'm an anonymous A !");}
          |        }.ma();
          |    }
          |}""")
    }
    scenario("empty interface with comments") {
      makeTest("""package p;
                 |
                 |interface Test{
                 |    /** Doc comment */
                 |}""")
    }

    scenario("gen type parameterized with an up-bounded wildcard") {
      makeTest("""class A{}
                 |
                 |interface I0<X>{ X get(); }
                 |
                 |interface I { I0<? extends A> m(); }
                 |
                 |class B{
                 |    void mtest(I i){
                 |        I0<? extends A> i0 = i.m();
                 |        A a = i0.get();
                 |    }
                 |}""")
    }

    scenario("instance initializer") {
      makeTest(
        """class B {
          |    int value(){return 0;}
          |}
          |
          |class A {
          |
          |    //instance initializer
          |    {
          |        b=new B();
          |    }
          |
          |    int j;
          |
          |    A(){ j = b.value(); }
          |
          |    final B b;
          |}""")
    }

    scenario("parameterized class instanciation") {
      makeTest(
        """package p;
          |
          |import java.util.Collection;
          |import java.util.Vector;
          |
          |class C<T> { public C(Collection<T> c){} }
          |
          |public class Test{
          |    public void main(String[] arg){
          |        Vector<String> v = new Vector<String>();
          |        C<String> c = new C<String>(v);
          |    }
          |}""")
    }

    scenario("parameterized class subtyping ") {
      makeTest(
        """package p;
          |
          |import java.util.Collection;
          |import java.util.Vector;
          |
          |public class Test{
          |    public void main(String[] arg){
          |        Collection<String> v1 = new Vector<String>();
          |        Vector<String> v2 = new Vector<String>(v1);
          |    }
          |}""")
    }

    scenario("chained call with more than one argument") {
      makeTest(
        """package p;
          |
          |
          |class A { B ma(int i, int j){ return new B(); } }
          |class B { C mb(){ return new C(); } }
          |class C { void mc(){ System.out.println("hola !"); } }
          |
          |public class Test{
          |
          |    public void main(String[] args){
          |        A a = new A();
          |        a.ma(0, 1).mb().mc();
          |    }
          |}"""
      )
    }

    scenario("overloading with variadic method") {
      makeTest(
        """package p;
          |
          |class A {
          |    void m(double d){ }
          |    void m(double... d){ }
          |}""")
    }

    scenario("wild card usage") {
      makeTest(
        """package p;
          |
          |import java.util.Enumeration;
          |import java.util.Vector;
          |
          |class A {
          |    void m(Vector<Object> v){
          |        for(Enumeration<?> e = v.elements(); e.hasMoreElements();){
          |            Object o = e.nextElement();
          |        }
          |    }
          |}""")
    }

    scenario("chained call in static context") {
      makeTest(
        """package p;
          |
          |class C {
          |    C m0(){ return new C();}
          |
          |    void m1(){}
          |
          |    static void m(C c){
          |        c.m0().m1();
          |    }
          |
          |}"""
      )
    }

    scenario("variable declared final in foreach loop") {
      makeTest(
        """package p;
          |import java.util.List;
          |class C {
          |    void m(List<C> l){ for(final C c : l); }
          |}"""
      )
    }

    scenario("Generic method in gen type - from library") {
      makeTest(
        """package p;
          |import java.util.List;
          |
          |public class Test {
          |    public String[] m(List<String> l) {
          |       return l.toArray(new String[l.size()]);
          |    }
          |}
          |"""
      )
    }

    scenario("Generic method in gen type - from source") {
      makeTest(
        """package p;
          |
          |interface List<E> {
          |   <T> T[] toArray(T[] var1);
          |   int size();
          |}
          |public class Test {
          |    public String[] m(List<String> l) {
          |       return l.toArray(new String[l.size()]);
          |    }
          |}
          |"""
      )
    }

    scenario("Inner Generic type - from library") {
      makeTest(
        """package p;
          |
          |import java.util.Map;
          |import java.util.Map.Entry;
          |
          |public class Test {
          |    public void m(Map<String, Object> m) {
          |         for(Entry<String, Object> e : m.entrySet()){
          |            String s = e.getValue().toString();
          |         }
          |    }
          |}
          |"""
      )
    }

    scenario("Inner Generic type - from source") {
      makeTest(
        """package p;
          |
          |import java.util.Set;
          |
          |public interface Map<K, V> {
          |
          |    Set<Map.Entry<K, V>> entrySet();
          |
          |    public interface Entry<K, V> {
          |        K getKey();
          |
          |        V getValue();
          |    }
          |}
          |
          |public class Test {
          |    public void m(Map<String, Object> m) {
          |          for(Map.Entry<String, Object> e : m.entrySet()){
          |             String s = e.getValue().toString();
          |          }
          |    }
          |}
          |"""
      )
    }

    scenario("Generic method with type variable updounded by a parameterized type") {
      makeTest(
        """package p;
          |
          |interface I<T> {}
          |class C<T> implements I<T> {}
          |
          |class MapOver<R> { public <S extends I<R>> void run(S l) {} }
          |
          |public class Test {
          |    public void m() { new MapOver<String>().run(new C<String>()); }
          |}
          |"""
      )
    }

    scenario("Generic array") {
      makeTest(
      """package p;
        |
        |interface Set<E> { <T> T[] toArray(T[] a); }
        |
        |public class Test {
        |    public void m(Set<Long> ns) { Long[] a = ns.toArray(new Long[0]); }
        |}
        |""")
    }

    scenario("Parenthesized expr in conditionnal expr") {
      makeTest(
        """package p;
          |
          |class C {
          |    C(String str){}
          |
          |    String getText(){return "";}
          |
          |    void init(Object o){ new C(o == null ? (getText()) : ""); }
          |
          |
          |}"""
      )
    }

    scenario("Clone override") {
      makeTest(
        """package p;
          |
          |interface LogEntry {
          |    public LogEntry clone();
          |}
          |
          |abstract class BaseEntry implements LogEntry {
          |    @Override
          |    public LogEntry clone(){
          |        try{
          |            return (LogEntry)super.clone();
          |        } catch (CloneNotSupportedException e) {
          |            throw new Error();
          |        }
          |    }
          |}
          |
          |class INLogEntry extends BaseEntry implements LogEntry {}"""
      )
    }

    scenario("raw generic subtype usage"){
      makeTest(
        """package p;
          |
          |import java.util.Vector;
          |import java.util.Collections;
          |
          |class C {
          |    void m(){
          |        Vector v = new Vector();
          |        Collections.sort(v);
          |    }
          |}"""
      )
    }

  }

}
