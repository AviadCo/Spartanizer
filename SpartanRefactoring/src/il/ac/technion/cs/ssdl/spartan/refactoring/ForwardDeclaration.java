package il.ac.technion.cs.ssdl.spartan.refactoring;

import il.ac.technion.cs.ssdl.spartan.utils.Occurrences;
import il.ac.technion.cs.ssdl.spartan.utils.Range;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

/**
 * @author Artium Nihamkin (original)
 * @author Boris van Sosin <code><boris.van.sosin [at] gmail.com></code> (v2)
 * 
 * @since 2013/01/01
 */
public class ForwardDeclaration extends Spartanization {
	/** Instantiates this class */
	public ForwardDeclaration() {
		super("Forward declaration", "Forward declaration of a variable just prior to first use");
	}

	@Override protected final void fillRewrite(final ASTRewrite r, final AST t, final CompilationUnit cu, final IMarker m) {
		cu.accept(new ASTVisitor() {
			@Override public boolean visit(final VariableDeclarationFragment n) {
				if (!inRange(m, n))
					return true;
				final SimpleName varName = n.getName();
				final ASTNode containingNode = n.getParent().getParent();
				if (!(containingNode instanceof Block))
					return true;
				final Block block = (Block) containingNode;
				final int declaredIdx = block.statements().indexOf(n.getParent());
				final int firstUseIdx = findFirstUse(block, varName);
				if (firstUseIdx < 0)
					return true;
				final int beginingOfDeclarationsBlockIdx = findBeginingOfDeclarationBlock(block, declaredIdx, firstUseIdx);
				if (beginingOfDeclarationsBlockIdx > declaredIdx) {
					final ASTNode declarationNode = (ASTNode) block.statements().get(declaredIdx);
					if (((VariableDeclarationStatement) declarationNode).fragments().size() == 1) {
						final ListRewrite lstRewrite = r.getListRewrite(block, Block.STATEMENTS_PROPERTY);
						lstRewrite.remove(declarationNode, null);
						lstRewrite.insertAt(ASTNode.copySubtree(t, declarationNode), beginingOfDeclarationsBlockIdx + 1, null);
					} else {
						r.getListRewrite(block, Block.STATEMENTS_PROPERTY).insertAt(
						    t.newVariableDeclarationStatement((VariableDeclarationFragment) ASTNode.copySubtree(t, n)),
						    beginingOfDeclarationsBlockIdx + 1, null);
						r.remove(n, null);
					}
				}
				return true;
			}
		});
	}

	@Override protected ASTVisitor fillOpportunities(final List<Range> oppportunities) {
		return new ASTVisitor() {
			@Override public boolean visit(final VariableDeclarationFragment n) {
				final ASTNode containingNode = n.getParent().getParent();
				if (!(containingNode instanceof Block))
					return true;
				return moverForward(n, (Block) containingNode);
			}

			private boolean moverForward(final VariableDeclarationFragment n, final Block b) {
				final int firstUseIdx = findFirstUse(b, n.getName());
				if (firstUseIdx < 0)
					return true;
				final int declaredIdx = b.statements().indexOf(n.getParent());
				if (findBeginingOfDeclarationBlock(b, declaredIdx, firstUseIdx) > declaredIdx)
					oppportunities.add(new Range(n));
				return true;
			}
		};
	}

	static int findFirstUse(final Block b, final SimpleName name) {
		final ASTNode declarationFragment = name.getParent();
		final ASTNode declarationStmt = declarationFragment.getParent();
		for (int i = b.statements().indexOf(declarationStmt) + 1; i < b.statements().size(); ++i)
			if (Occurrences.BOTH_LEXICAL.of(name).in((ASTNode) b.statements().get(i)).size() > 0)
				return i; // first use!
		return -1; // that means unused
	}

	static int findBeginingOfDeclarationBlock(final Block b, final int declaredIdx, final int firstUseIdx) {
		int beginingOfDeclarationsBlockIdx = firstUseIdx - 1;
		for (int i = firstUseIdx - 1; i > declaredIdx; --i) {
			if (!(b.statements().get(i) instanceof VariableDeclarationStatement))
				break;
			final VariableDeclarationStatement declarations = (VariableDeclarationStatement) b.statements().get(i);
			boolean foundUsedVariable = false;
			for (final Object item : declarations.fragments()) {
				final int firstUseOfCurr = findFirstUse(b, ((VariableDeclarationFragment) item).getName());
				if (firstUseOfCurr == firstUseIdx) {
					beginingOfDeclarationsBlockIdx = i - 1;
					foundUsedVariable = true;
				}
			}
			if (!foundUsedVariable)
				break;
		}
		return beginingOfDeclarationsBlockIdx;
	}
}
