package il.ac.technion.cs.ssdl.spartan.refactoring;

import static il.ac.technion.cs.ssdl.spartan.utils.Utils.sort;
import il.ac.technion.cs.ssdl.spartan.utils.Occurrences;
import il.ac.technion.cs.ssdl.spartan.utils.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 * @author Artium Nihamkin (original)
 * @author Boris van Sosin <code><boris.van.sosin [at] gmail.com></code> (v2)
 * 
 * @since 2013/01/01
 */
public class RenameReturnToDollar extends Spartanization {
	/** Instantiates this class */
	public RenameReturnToDollar() {
		super("Rename returned variable to '$'", "Rename the variable returned by a function to '$'");
	}

	@Override protected final void fillRewrite(final ASTRewrite $, final AST t, final CompilationUnit cu, final IMarker m) {
		cu.accept(new ASTVisitor() {
			@Override public boolean visit(final MethodDeclaration n) {
				final VariableDeclarationFragment returnVar = getOnlyReturnVariable(n);
				if (returnVar != null) {
					if (!inRange(m, n))
						return true;
					for (final Expression e : Occurrences.BOTH_LEXICAL.of(returnVar.getName()).in(n))
						$.replace(e, t.newSimpleName("$"), null);
				}
				return true;
			}
		});
	}

	static List<VariableDeclarationFragment> getCandidates(final ASTNode container) {
		final List<VariableDeclarationFragment> $ = new ArrayList<VariableDeclarationFragment>();
		container.accept(new ASTVisitor() {
			/**
			 * 
			 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
			 *      AnonymousClassDeclaration)
			 * 
			 * @param _
			 *          ignored, we don't want to visit declarations inside anonymous
			 *          classes
			 */
			@Override public boolean visit(@SuppressWarnings("unused") final AnonymousClassDeclaration _) {
				return false;
			}

			@Override public boolean visit(final VariableDeclarationFragment node) {
				$.add(node);
				return true;
			}
		});
		return $;
	}

	static List<ReturnStatement> getReturnStatements(final ASTNode container) {
		final List<ReturnStatement> $ = new ArrayList<ReturnStatement>();
		container.accept(new ASTVisitor() {
			/**
			 * 
			 * 
			 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
			 *      AnonymousClassDeclaration)
			 * 
			 * @param _
			 *          ignored, we don't want to visit declarations inside anonymous
			 *          classes
			 */
			@Override public boolean visit(@SuppressWarnings("unused") final AnonymousClassDeclaration _) {
				return false;
			}

			@Override public boolean visit(final ReturnStatement node) {
				$.add(node);
				return true;
			}
		});
		return $;
	}

	static VariableDeclarationFragment getOnlyReturnVariable(final MethodDeclaration n) {
		final List<VariableDeclarationFragment> $ = getCandidates(n);
		// check if we already have $
		for (final VariableDeclaration d : $)
			if (d.getName().getIdentifier().equals("$"))
				return null;
		final List<ReturnStatement> returnStatements = getReturnStatements(n);
		int usesOfLastCondidate = 0;
		for (final Iterator<VariableDeclarationFragment> iter = $.iterator(); iter.hasNext();) {
			final VariableDeclarationFragment currDecl = iter.next();
			for (final ReturnStatement returnStmt : returnStatements) {
				// this demands explanation: returnStmt.getExpression() only if the
				// statement if "return;"
				// if such statement exists, then the current method is void, and there
				// can be no return variable.
				// in this case we can return null immediately, without checking more
				// return statements
				if (returnStmt.getExpression() == null)
					return null;
				if (Arrays.binarySearch(literals, returnStmt.getExpression().getNodeType()) >= 0)
					continue;
				final int nUses = Occurrences.BOTH_LEXICAL.of(currDecl.getName()).in(returnStmt).size();
				if (nUses == 0) {
					iter.remove();
					break;
				}
				usesOfLastCondidate = nUses;
			}
		}
		return $.size() == 1 && returnStatements.size() > 0 && usesOfLastCondidate > 0 ? $.get(0) : null;
	}

	private static final int[] literals = sort(new int[] { //
	ASTNode.NULL_LITERAL, //
	    ASTNode.CHARACTER_LITERAL, //
	    ASTNode.NUMBER_LITERAL, //
	    ASTNode.STRING_LITERAL, //
	    ASTNode.BOOLEAN_LITERAL, //
	});

	@Override protected ASTVisitor fillOpportunities(final List<Range> opportunities) {
		return new ASTVisitor() {
			@Override public boolean visit(final MethodDeclaration n) {
				final VariableDeclarationFragment v = getOnlyReturnVariable(n);
				if (v != null)
					opportunities.add(new Range(v));
				return true;
			}
		};
	}
}
