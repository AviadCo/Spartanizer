package org.spartan.refactoring.wring;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.spartan.refactoring.utils.AncestorSearch;

class ExclusionManager {
  final Set<ASTNode> inner = new HashSet<>();
  boolean isExcluded(final ASTNode n) {
    for (final ASTNode ancestor : AncestorSearch.ancestors(n))
      if (inner.contains(ancestor))
        return true;
    return false;
  }
  void exclude(final ASTNode n) {
    inner.add(n);
  }
  void unExclude(final ASTNode n) {
    inner.remove(n);
  }
}