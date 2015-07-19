package org.spartan.refactoring.spartanizations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.spartan.hamcrest.CoreMatchers.is;
import static org.spartan.hamcrest.MatcherAssert.assertThat;
import static org.spartan.refactoring.spartanizations.TESTUtils.collect;
import static org.spartan.refactoring.spartanizations.TESTUtils.p;
import static org.spartan.refactoring.utils.Funcs.asBooleanLiteral;
import static org.spartan.refactoring.utils.Funcs.asNot;
import static org.spartan.refactoring.utils.Funcs.getCore;

import java.util.Collection;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.spartan.refactoring.spartanizations.AbstractWringTest.Noneligible;
import org.spartan.refactoring.spartanizations.AbstractWringTest.OutOfScope;
import org.spartan.refactoring.spartanizations.AbstractWringTest.Wringed;
import org.spartan.refactoring.utils.Is;
import org.spartan.utils.Utils;

/**
 * Unit tests for {@link Wrings#ADDITION_SORTER}.
 *
 * @author Yossi Gil
 * @since 2014-07-13
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING) //
@SuppressWarnings({ "javadoc", "static-method" }) //
public class PUSHDOWN_NOT_Test {
  /** The {@link Wring} under test */
  static final Wring WRING = Wrings.PUSHDOWN_NOT.inner;

  @Test public void notOfFalse() {
    final PrefixExpression e = p("!false");
    assertThat(e, is(notNullValue()));
    assertThat(WRING.scopeIncludes(e), is(true));
    assertThat(WRING.eligible(e), is(true));
    assertThat(Wrings.hasOpportunity(e), is(true));
    assertThat(asNot(e), is(notNullValue()));
    final Expression inner = getCore(e.getOperand());
    assertThat(inner, is(notNullValue()));
    assertThat(inner.toString(), is("false"));
    assertThat(Is.booleanLiteral(inner), is(true));
    assertThat(Wrings.perhapsNotOfLiteral(e, inner), is(notNullValue()));
    assertThat(Wrings.notOfLiteral(e, asBooleanLiteral(inner)), is(notNullValue()));
    assertThat(Wrings.pushdownNot(e, inner), is(notNullValue()));
    assertThat(Wrings.pushdownNot(asNot(e)), is(notNullValue()));
    assertThat(WRING.replacement(e), is(notNullValue()));
  }

  @RunWith(Parameterized.class) //
  public static class OutOfScope extends AbstractWringTest.OutOfScope {
    static String[][] cases = Utils.asArray(//
        Utils.asArray("Summation", "a+b"), //
        Utils.asArray("Multiplication", "a*b"), //
        Utils.asArray("OR", "a||b"), //
        Utils.asArray("END", "a&&b"), //
        null);

    /** Instantiates the enclosing class ({@link OutOfScope}) */
    public OutOfScope() {
      super(WRING);
    }
    /**
     * Generate test cases for this parameterized class.
     *
     * @return a collection of cases, where each case is an array of three
     *         objects, the test case name, the input, and the file.
     */
    @Parameters(name = DESCRIPTION) //
    public static Collection<Object[]> cases() {
      return collect(cases);
    }
  }

  @RunWith(Parameterized.class) //
  public static class Noneligible extends AbstractWringTest.Noneligible {
    static String[][] cases = Utils.asArray(//
        // Literal
        Utils.asArray("Simple not", "!a"), //
        Utils.asArray("Simple not of function", "!f(a)"), //
        null);

    /**
     * Generate test cases for this parameterized class.
     *
     * @return a collection of cases, where each case is an array of three
     *         objects, the test case name, the input, and the file.
     */
    @Parameters(name = DESCRIPTION) //
    public static Collection<Object[]> cases() {
      return collect(cases);
    }
    /** Instantiates the enclosing class ({@link Noneligible}) */
    public Noneligible() {
      super(WRING);
    }
  }

  @RunWith(Parameterized.class) //
  @FixMethodOrder(MethodSorters.NAME_ASCENDING) //
  public static class Wringed extends AbstractWringTest.Wringed {
    private static String[][] cases = Utils.asArray(//
        Utils.asArray("not of AND", "!(f() && f(5))", "(!f() || !f(5))"), //
        Utils.asArray("not of EQ", "!(3 == 5)", "3 != 5"), //
        Utils.asArray("not of AND nested", "!(f() && (f(5)))", "(!f() || !f(5))"), //
        Utils.asArray("not of EQ nested", "!((((3 == 5))))", "3 != 5"), //
        Utils.asArray("not of GE", "!(3 >= 5)", "3 < 5"), //
        Utils.asArray("not of GT", "!(3 > 5)", "3 <= 5"), //
        Utils.asArray("not of NE", "!(3 != 5)", "3 == 5"), //
        Utils.asArray("not of LE", "!(3 <= 5)", "3 > 5"), //
        Utils.asArray("not of LT", "!(3 < 5)", "3 >= 5"), //
        Utils.asArray("not of AND", "!(a && b && c)", "(!a || !b || !c)"), //
        Utils.asArray("not of OR", "!(a || b || c)", "(!a && !b && !c)"), //
        Utils.asArray("double not", "!!f()", "f()"), //
        Utils.asArray("double not nested", "!(!f())", "f()"), //
        Utils.asArray("double not deeply nested", "!(((!f())))", "f()"), //
        Utils.asArray("not of false", "!false", "true"), //
        Utils.asArray("not of true", "!true", "false"), //
        null);

    /**
     * Generate test cases for this parameterized class.
     *
     * @return a collection of cases, where each case is an array of three
     *         objects, the test case name, the input, and the file.
     */
    @Parameters(name = DESCRIPTION) //
    public static Collection<Object[]> cases() {
      return collect(cases);
    }
    /**
     * Instantiates the enclosing class ({@link Wringed})
     */
    public Wringed() {
      super(WRING);
    }
    @Test public void inputIsPrefixExpression() {
      final PrefixExpression e = asPrefixExpression();
      assertNotNull(e);
    }
  }
}