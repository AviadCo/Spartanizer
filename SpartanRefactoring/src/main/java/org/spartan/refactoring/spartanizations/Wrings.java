package org.spartan.refactoring.spartanizations;

import static org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_AND;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_OR;
import static org.eclipse.jdt.core.dom.PrefixExpression.Operator.NOT;
import static org.spartan.refactoring.utils.Funcs.asAndOrOr;
import static org.spartan.refactoring.utils.Funcs.asBooleanLiteral;
import static org.spartan.refactoring.utils.Funcs.asComparison;
import static org.spartan.refactoring.utils.Funcs.asInfixExpression;
import static org.spartan.refactoring.utils.Funcs.asNot;
import static org.spartan.refactoring.utils.Funcs.asPrefixExpression;
import static org.spartan.refactoring.utils.Funcs.duplicate;
import static org.spartan.refactoring.utils.Funcs.flip;
import static org.spartan.refactoring.utils.Funcs.getCore;
import static org.spartan.refactoring.utils.Funcs.makeParenthesizedExpression;
import static org.spartan.refactoring.utils.Funcs.makePrefixExpression;
import static org.spartan.utils.Utils.in;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.spartan.refactoring.utils.All;
import org.spartan.refactoring.utils.Are;
import org.spartan.refactoring.utils.Is;

/**
 * @author Yossi Gil
 * @since 2015-07-17
 *
 */
public enum Wrings {
  /**
   * A {@link Wring} that eliminates redundant comparison with the two boolean
   * literals: <code><b>true</b></code> and <code><b>false</b></code>.
   *
   * @author Yossi Gil
   * @since 2015-07-17
   *
   */
  COMPARISON_WITH_BOOLEAN(new Wring.OfInfixExpression() {
    @Override public final boolean scopeIncludes(final InfixExpression e) {
      return in(e.getOperator(), Operator.EQUALS, Operator.NOT_EQUALS)
          && (Is.booleanLiteral(e.getRightOperand()) || Is.booleanLiteral(e.getLeftOperand()));
    }
    @Override boolean _eligible(final InfixExpression e) {
      assert scopeIncludes(e);
      return true;
    }
    @Override Expression _replacement(final InfixExpression e) {
      Expression nonliteral;
      BooleanLiteral literal;
      if (Is.booleanLiteral(e.getLeftOperand())) {
        literal = (BooleanLiteral) e.getLeftOperand();
        nonliteral = duplicate(e.getRightOperand());
      } else {
        literal = (BooleanLiteral) e.getRightOperand();
        nonliteral = duplicate(e.getLeftOperand());
      }
      return nonNegating(e, literal) ? nonliteral : negate(nonliteral);
    }
    private PrefixExpression negate(final ASTNode e) {
      return makePrefixExpression(makeParenthesizedExpression((Expression) e), PrefixExpression.Operator.NOT);
    }
    private boolean nonNegating(final InfixExpression e, final BooleanLiteral literal) {
      return literal.booleanValue() == (e.getOperator() == Operator.EQUALS);
    }
  }), //
  /**
   * A {@link Wring} that sorts the arguments of a {@link Operator#PLUS}
   * expression.
   *
   * Extra care is taken to leave intact the use of {@link Operator#PLUS} for
   * the concatenation of {@link String}s.
   *
   * @author Yossi Gil
   * @since 2015-07-17
   *
   */
  ADDITION_SORTER(new Wring.OfInfixExpression() {
    @Override boolean scopeIncludes(final InfixExpression e) {
      return e.getOperator() == Operator.PLUS;
    }
    @Override boolean _eligible(final InfixExpression e) {
      return Are.notString(All.operands(flatten(e))) && tryToSort(e);
    }
    private boolean tryToSort(final InfixExpression e) {
      return tryToSort(All.operands(flatten(e)));
    }
    private boolean tryToSort(final List<Expression> es) {
      return Wrings.tryToSort(es, ExpressionComparator.ADDITION);
    }
    @Override Expression _replacement(final InfixExpression e) {
      final List<Expression> operands = All.operands(flatten(e));
      return !Are.notString(operands) || !tryToSort(operands) ? null : refit(e, operands);
    }
  }), //
  /**
   * A {@link Wring} that sorts the arguments of an expression using the same
   * sorting order as {@link Operator#PLUS} expression, except that we do not
   * worry about commutativity. Unlike {@link #ADDITION_SORTER}, we know that
   * the reordering is always possible.
   *
   * @see #ADDITION_SORTER
   * @author Yossi Gil
   * @since 2015-07-17
   *
   */
  PSEUDO_ADDITION_SORTER(new Wring.OfInfixExpression() {
    @Override boolean scopeIncludes(final InfixExpression e) {
      return in(e.getOperator(), Operator.OR);
    }
    @Override boolean _eligible(final InfixExpression e) {
      return tryToSort(e);
    }
    private boolean tryToSort(final InfixExpression e) {
      return tryToSort(All.operands(flatten(e)));
    }
    private boolean tryToSort(final List<Expression> es) {
      return Wrings.tryToSort(es, ExpressionComparator.ADDITION);
    }
    @Override Expression _replacement(final InfixExpression e) {
      final List<Expression> operands = All.operands(flatten(e));
      if (!tryToSort(operands))
        return null;
      return refit(e, operands);
    }
  }), //
  /**
   * A {@link Wring} that sorts the arguments of a {@link Operator#PLUS}
   * expression.
   *
   * Extra care is taken to leave intact the use of {@link Operator#PLUS} for
   * the concatenation of {@link String}s.
   *
   * @author Yossi Gil
   * @since 2015-07-17
   *
   */
  MULTIPLICATION_SORTER(new Wring.OfInfixExpression() {
    @Override boolean scopeIncludes(final InfixExpression e) {
      return in(e.getOperator(), Operator.TIMES, Operator.XOR, Operator.AND);
    }
    @Override boolean _eligible(final InfixExpression e) {
      return tryToSort(e);
    }
    private boolean tryToSort(final InfixExpression e) {
      return tryToSort(All.operands(flatten(e)));
    }
    private boolean tryToSort(final List<Expression> es) {
      return Wrings.tryToSort(es, ExpressionComparator.MULTIPLICATION);
    }
    @Override Expression _replacement(final InfixExpression e) {
      final List<Expression> operands = All.operands(flatten(e));
      if (!tryToSort(operands))
        return null;
      return refit(e, operands);
    }
  }), //
  /**
   * A {@link Wring} that reorder comparisons so that the specific value is
   * placed on the right. Specific value means a literal, or any of the two
   * keywords <code><b>this</b></code> or <code><b>null</b></code>.
   *
   * @author Yossi Gil
   * @since 2015-07-17
   *
   */
  COMPARISON_WITH_SPECIFIC(new Wring.OfInfixExpression() {
    @Override public boolean scopeIncludes(final InfixExpression e) {
      return Is.comparison(e) && (hasThisOrNull(e) || hasOneSpecificArgument(e));
    }
    @Override boolean _eligible(final InfixExpression e) {
      return Is.specific(e.getLeftOperand());
    }
    @Override Expression _replacement(final InfixExpression e) {
      return flip(e);
    }
    boolean hasThisOrNull(final InfixExpression e) {
      return Is.thisOrNull(e.getLeftOperand()) || Is.thisOrNull(e.getRightOperand());
    }
    private boolean hasOneSpecificArgument(final InfixExpression e) {
      // One of the arguments must be specific, the other must not be.
      return Is.specific(e.getLeftOperand()) != Is.specific(e.getRightOperand());
    }
  }), //
  /**
   * A {@link Wring} that pushes down "<code>!</code>", the negation operator as
   * much as possible, using the de-Morgan and other simplification rules.
   *
   * @author Yossi Gil
   * @since 2015-7-17
   *
   */
  PUSHDOWN_NOT(new Wring.OfPrefixExpression() {
    @Override public boolean scopeIncludes(final PrefixExpression e) {
      return e != null && asNot(e) != null;
    }
    @Override boolean _eligible(final PrefixExpression e) {
      return hasOpportunity(asNot(e));
    }
    @Override Expression _replacement(final PrefixExpression e) {
      return pushdownNot(asNot(e));
    }
  }),
  //
  ;
  final Wring inner;

  Wrings(final Wring inner) {
    this.inner = inner;
  }
  /**
   * Find the first {@link Wring} appropriate for an {@link InfixExpression}
   *
   * @param e
   *          JD
   * @return the first {@link Wring} for which the parameter is eligible, or
   *         <code><b>null</b></code>i if no such {@link Wring} is found.
   */
  public static Wring find(final Expression e) {
    Wring $;
    return ($ = find(asInfixExpression(e))) != null//
        || ($ = find(asPrefixExpression(e))) != null//
            //
            ? $ : null;
  }
  /**
   * Find the first {@link Wring} appropriate for an {@link InfixExpression}
   *
   * @param e
   *          JD
   * @return the first {@link Wring} for which the parameter is eligible, or
   *         <code><b>null</b></code>i if no such {@link Wring} is found.
   */
  public static Wring find(final InfixExpression e) {
    if (e == null)
      return null;
    for (final Wrings s : values())
      if (s.inner.scopeIncludes(e))
        return s.inner;
    return null;
  }
  /**
   * Find the first {@link Wring} appropriate for a {@link PrefixExpression}
   *
   * @param e
   *          JD
   * @return the first {@link Wring} for which the parameter is eligible, or
   *         <code><b>null</b></code>i if no such {@link Wring} is found.
   */
  public static Wring find(final PrefixExpression e) {
    if (e == null)
      return null;
    for (final Wrings s : Wrings.values())
      if (s.inner.scopeIncludes(e))
        return s.inner;
    return null;
  }
  static boolean tryToSort(final List<Expression> es, final java.util.Comparator<Expression> c) {
    boolean $ = false;
    // Bubble sort, duplicating in each case of swap
    for (int i = 0, size = es.size(); i < size; i++)
      for (int j = 0; j < size - 1; j++) {
        final Expression e0 = es.get(j);
        final Expression e1 = es.get(j + 1);
        if (c.compare(e0, e1) <= 0)
          continue;
        es.get(j + 1);
        es.remove(j);
        es.remove(j);
        es.add(j, e0);
        es.add(j, e1);
        $ = true;
      }
    return $;
  }
  public static InfixExpression flatten(final InfixExpression $) {
    return refit(duplicate($), flattenInto($.getOperator(), All.operands($), new ArrayList<Expression>()));
  }
  private static List<Expression> flattenInto(final Operator o, final List<Expression> es, final List<Expression> $) {
    for (final Expression e : es)
      flattenInto(o, e, $);
    return $;
  }
  private static List<Expression> flattenInto(final Operator o, final Expression e, final List<Expression> $) {
    final InfixExpression inner = asInfixExpression(getCore(e));
    return inner == null || inner.getOperator() != o ? flattenInto(e, $) : flattenInto(o, All.operands(inner), $);
  }
  private static List<Expression> flattenInto(final Expression e, final List<Expression> $) {
    $.add(e);
    return $;
  }
  static InfixExpression refit(final InfixExpression e, final List<Expression> operands) {
    assert operands.size() >= 2;
    final InfixExpression $ = e.getAST().newInfixExpression();
    $.setOperator(e.getOperator());
    $.setLeftOperand(duplicate(operands.get(0)));
    $.setRightOperand(duplicate(operands.get(1)));
    operands.remove(0);
    operands.remove(0);
    if (!operands.isEmpty())
      for (final Expression operand : operands)
        $.extendedOperands().add(duplicate(operand));
    return $;
  }
  static Expression pushdownNot(final PrefixExpression e) {
    return e == null ? null : pushdownNot(e, getCore(e.getOperand()));
  }
  static Expression pushdownNot(final PrefixExpression e, final Expression inner) {
    Expression $;
    return ($ = perhapsNotOfLiteral(e, inner)) != null//
        || ($ = perhapsDoubleNegation(e, inner)) != null//
        || ($ = perhapsDeMorgan(e, inner)) != null//
        || ($ = perhapsComparison(e, inner)) != null //
            ? $ : null;
  }
  static Expression perhapsNotOfLiteral(final PrefixExpression e, final Expression inner) {
    return !Is.booleanLiteral(inner) ? null : notOfLiteral(e, asBooleanLiteral(inner));
  }
  static Expression notOfLiteral(final PrefixExpression e, final BooleanLiteral l) {
    final BooleanLiteral $ = duplicate(l);
    $.setBooleanValue(!l.booleanValue());
    return $;
  }
  static Expression perhapsDoubleNegation(final Expression e, final Expression inner) {
    return perhapsDoubleNegation(e, asNot(inner));
  }
  static Expression perhapsDoubleNegation(final Expression e, final PrefixExpression inner) {
    return inner == null ? null : inner.getOperand();
  }
  static Expression perhapsDeMorgan(final Expression e, final Expression inner) {
    return perhapsDeMorgan(e, asAndOrOr(inner));
  }
  static Expression perhapsDeMorgan(final Expression e, final InfixExpression inner) {
    return inner == null ? null : deMorgan(e, inner, getCoreLeft(inner), getCoreRight(inner));
  }
  static Expression deMorgan(final Expression e, final InfixExpression inner, final Expression left, final Expression right) {
    return deMorgan1(e, inner, parenthesize(left), parenthesize(right));
  }
  static Expression deMorgan1(final Expression e, final InfixExpression inner, final Expression left, final Expression right) {
    return parenthesize( //
        addExtendedOperands(inner, //
            makeInfixExpression(not(left), conjugate(inner.getOperator()), not(right))));
  }
  static InfixExpression addExtendedOperands(final InfixExpression from, final InfixExpression $) {
    if (from.hasExtendedOperands())
      addExtendedOperands(from.extendedOperands(), $.extendedOperands());
    return $;
  }
  static void addExtendedOperands(final List<Expression> from, final List<Expression> to) {
    for (final Expression e : from)
      to.add(not(e));
  }
  static Expression perhapsComparison(final Expression e, final Expression inner) {
    return perhapsComparison(e, asComparison(inner));
  }
  static Expression perhapsComparison(final Expression e, final InfixExpression inner) {
    return inner == null ? null : comparison(e, inner);
  }
  static Expression comparison(final Expression e, final InfixExpression inner) {
    return cloneInfixChangingOperator(inner, ShortestBranchFirst.negate(inner.getOperator()));
  }
  static InfixExpression cloneInfixChangingOperator(final InfixExpression e, final Operator o) {
    return e == null ? null : makeInfixExpression(getCoreLeft(e), o, getCoreRight(e));
  }
  static Expression parenthesize(final Expression e) {
    if (isSimple(e))
      return duplicate(e);
    final ParenthesizedExpression $ = e.getAST().newParenthesizedExpression();
    $.setExpression(duplicate(getCore(e)));
    return $;
  }
  static boolean isSimple(final Expression e) {
    return isSimple(e.getClass());
  }
  static boolean isSimple(final Class<? extends Expression> c) {
    return in(c, BooleanLiteral.class, //
        CharacterLiteral.class, //
        NullLiteral.class, //
        NumberLiteral.class, //
        StringLiteral.class, //
        TypeLiteral.class, //
        Name.class, //
        QualifiedName.class, //
        SimpleName.class, //
        ParenthesizedExpression.class, //
        SuperMethodInvocation.class, //
        MethodInvocation.class, //
        ClassInstanceCreation.class, //
        SuperFieldAccess.class, //
        FieldAccess.class, //
        ThisExpression.class, //
        null);
  }
  static PrefixExpression not(final Expression e) {
    final PrefixExpression $ = e.getAST().newPrefixExpression();
    $.setOperator(NOT);
    $.setOperand(parenthesize(e));
    return $;
  }
  static InfixExpression makeInfixExpression(final Expression left, final Operator o, final Expression right) {
    final InfixExpression $ = left.getAST().newInfixExpression();
    $.setLeftOperand(duplicate(left));
    $.setOperator(o);
    $.setRightOperand(duplicate(right));
    return $;
  }
  static Expression getCoreRight(final InfixExpression e) {
    return getCore(e.getRightOperand());
  }
  static Expression getCoreLeft(final InfixExpression e) {
    return getCore(e.getLeftOperand());
  }
  static Operator conjugate(final Operator o) {
    assert Is.deMorgan(o);
    return o.equals(CONDITIONAL_AND) ? CONDITIONAL_OR : CONDITIONAL_AND;
  }
  static boolean hasOpportunity(final PrefixExpression e) {
    return e == null ? false : hasOpportunity(getCore(e.getOperand()));
  }
  static boolean hasOpportunity(final Expression inner) {
    return Is.booleanLiteral(inner) || asNot(inner) != null || asAndOrOr(inner) != null || asComparison(inner) != null;
  }
}
