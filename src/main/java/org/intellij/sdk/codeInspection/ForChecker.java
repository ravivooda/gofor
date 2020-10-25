package org.intellij.sdk.codeInspection;

import com.goide.inspections.core.GoInspectionBase;
import com.goide.inspections.core.GoProblemsHolder;
import com.goide.psi.*;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ForChecker extends GoInspectionBase {
    private static final Logger LOG = Logger.getInstance(ForChecker.class);
    public static final String PASSING_REFERENCE_OF_COPIED_RANGE_ELEMENT = "Passing reference of copied range element";

    @Override
    protected @NotNull GoVisitor buildGoVisitor(
            @NotNull GoProblemsHolder holder,
            @NotNull LocalInspectionToolSession session) {
        return new GoVisitor() {
            @Override
            public void visitCallExpr(@NotNull GoCallExpr o) {
                super.visitCallExpr(o);
                PsiFile file = o.getContainingFile();

                GoForStatement forStatement = PsiTreeUtil.getParentOfType(o, GoForStatement.class);
                if (forStatement == null) return;

                PsiElement assignedVariable = getAssignedVariable(forStatement);
                if (assignedVariable == null) return;

                LOG.warn("\n\n\n");

                PsiReference psiReference = o.getExpression().getReference();
                PsiElement resolved = psiReference != null ? psiReference.resolve() : null;
                if (!(resolved instanceof GoFunctionOrMethodDeclaration)) return;

                boolean didFindProblems = false;

                List<GoExpression> expressionList = o.getArgumentList().getExpressionList();
                for (GoExpression goExpression : expressionList) {
                    if (goExpression.isConstant()) continue;

                    LOG.warn("visitCallExpr: expression: " + goExpression + ", text: " + goExpression.getText());

                    if (goExpression instanceof GoUnaryExpr) {
                        GoUnaryExpr unaryExpr = (GoUnaryExpr) goExpression;
                        PsiElement operator = unaryExpr.getOperator();
                        if (operator == null || !operator.getText().equalsIgnoreCase("&")) continue;

                        ArrayList<PsiElement> referenceElements = getReferenceElements(goExpression);
                        for (PsiElement referenceElement : referenceElements) {
                            // Check with current reference
                            if (areEqualReferences(file, assignedVariable, referenceElement)) {
                                holder.registerProblem(referenceElement, () -> PASSING_REFERENCE_OF_COPIED_RANGE_ELEMENT);
                                didFindProblems = true;
                            }

                            // Check with parent reference; but register problem as future
                            PsiElement parentResolvedElement = getParentPsiElement(referenceElement);
                            if (parentResolvedElement != null && areEqualReferences(file, assignedVariable, parentResolvedElement)) {
                                holder.registerProblem(referenceElement, () -> PASSING_REFERENCE_OF_COPIED_RANGE_ELEMENT);
                                didFindProblems = true;
                            }
                        }
                    }
                }

                if (didFindProblems) {
                    holder.registerProblem(assignedVariable, () -> PASSING_REFERENCE_OF_COPIED_RANGE_ELEMENT);
                }
            }
        };
    }

    private boolean areEqualReferences(PsiFile file, PsiElement assignedVariable, PsiElement referenceElement) {
        return file.getManager().areElementsEquivalent(referenceElement, assignedVariable);
    }

    @Nullable
    private PsiElement getParentPsiElement(PsiElement referenceElement) {
        @Nullable PsiReference referenceElementParent = referenceElement.getParent().getReference();
        if (referenceElementParent == null) return null;
        return referenceElementParent.resolve();
    }

    private @NotNull ArrayList<PsiElement> getReferenceElements(@NotNull GoExpression expr) {
        ArrayList<PsiElement> references = new ArrayList<>();
        if (expr instanceof GoUnaryExpr) {
            LOG.warn("getSingleReferences unary: " + expr.getText());
        } else if (expr instanceof GoReferenceExpression) {
            PsiElement identifier = ((GoReferenceExpression) expr).getIdentifier();
            references.add(identifier);
        } else {
            throw new UnsupportedOperationException("unsupported reference type " + expr.getClass().getName());
        }

        @NotNull PsiElement[] children = expr.getChildren();
        for (PsiElement child : children) {
            if (child instanceof GoExpression) {
                references.addAll(getReferenceElements((GoExpression) child));
            }
        }
        return references;
    }

    @Nullable
    private PsiElement getAssignedVariable(@NotNull GoForStatement o) {
        if (o.getRangeClause() == null) {
            return null;
        }
        PsiElement assignedVariable;
        if (o.getRangeClause().getVarAssign() != null) {
            if (o.getRangeClause().getVarDefinitionList().size() <= 1) {
                return null;
            }
            assignedVariable = o.getRangeClause().getVarDefinitionList().get(1);
        } else {
            if (o.getRangeClause().getLeftExpressionsList().size() <= 1) {
                return null;
            }
            assignedVariable = o.getRangeClause().getLeftExpressionsList().get(1);
        }
        return assignedVariable;
    }
}