package il.org.spartan.refactoring.wring;

import static il.org.spartan.refactoring.utils.Funcs.*;
import org.eclipse.jdt.core.dom.*;
import il.org.spartan.refactoring.preferences.PluginPreferencesResources.*;

/** A {@link Wring} to remove the parentheses from annotations that do not take
 * arguments, converting <code><pre>@Override()</pre></code> to
 * <code><pre>@Override</pre></code>
 * @author Daniel Mittelman <code><mittelmania [at] gmail.com></code>
 * @since 2016-04-02 */
public class AnnotationRemoveEmptyParentheses extends Wring.ReplaceCurrentNode<NormalAnnotation> {
  @Override String description(final NormalAnnotation a) {
    return "Remove redundant parentheses from the @" + a.getTypeName().getFullyQualifiedName() + " annotation";
  }
  @Override ASTNode replacement(final NormalAnnotation a) {
    if (a.values().size() > 0)
      return null;
    final MarkerAnnotation $ = a.getAST().newMarkerAnnotation();
    $.setTypeName(duplicate(a.getTypeName()));
    return $;
  }
  @Override WringGroup wringGroup() {
    return WringGroup.REMOVE_SYNTACTIC_BAGGAGE;
  }
}