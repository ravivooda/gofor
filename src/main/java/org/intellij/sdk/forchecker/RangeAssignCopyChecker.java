package org.intellij.sdk.forchecker;

import com.goide.inspections.core.GoInspectionBase;
import com.goide.inspections.core.GoProblemsHolder;
import com.goide.psi.*;
import com.goide.psi.impl.GoPsiImplUtil;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RangeAssignCopyChecker extends GoInspectionBase {
    private static final Logger LOG = Logger.getInstance(RangeAssignCopyChecker.class);
    public static final String PASSING_REFERENCE_OF_COPIED_RANGE_ELEMENT = "Passing reference of copied range element";
    public static final String CALLING_MODIFYING_METHOD_ON_COPIED_RANGE_ELEMENT = "Need to complete";

    @Override
    protected @NotNull GoVisitor buildGoVisitor(
            @NotNull GoProblemsHolder holder,
            @NotNull LocalInspectionToolSession session) {
        return new GoVisitor() {
            @Override
            public void visitCallExpr(@NotNull GoCallExpr o) {
                super.visitCallExpr(o);

                GoForStatement forStatement = PsiTreeUtil.getParentOfType(o, GoForStatement.class);
                if (forStatement == null) return;

                PsiElement assignedVariable = getAssignedVariable(forStatement);
                if (assignedVariable == null) return;

                LOG.warn("\n\n\n");

                PsiReference psiReference = o.getExpression().getReference();
                PsiElement resolved = psiReference != null ? psiReference.resolve() : null;
                if (!(resolved instanceof GoFunctionOrMethodDeclaration)) return;

                evaluateReferenceIssues(o, assignedVariable, holder);

                evaluateMethodInvocations(o, assignedVariable, holder);
            }
        };
    }

    private void evaluateMethodInvocations(GoCallExpr o, PsiElement assignedVariable, GoProblemsHolder holder) {
        GoExpression expression = o.getExpression();
        if (!(expression instanceof GoReferenceExpression)) return;
        if (!isReceiverPointer(o)) return;

        GoReferenceExpression referenceExpression = (GoReferenceExpression) expression;
        PsiElement qualifier = referenceExpression.getFirstChild();
        if (qualifier == null) return;

        boolean foundProblems = false;
        @NotNull Collection<PsiReference> search = ReferencesSearch.search(assignedVariable).findAll();
        for (PsiReference psiReference : search) {
            if (areEqualReferences(psiReference.getElement(), qualifier)) {
                holder.registerProblem(qualifier, () -> CALLING_MODIFYING_METHOD_ON_COPIED_RANGE_ELEMENT);
                foundProblems = true;
            }
        }

        if (foundProblems) {
            holder.registerProblem(assignedVariable, () -> CALLING_MODIFYING_METHOD_ON_COPIED_RANGE_ELEMENT);
        }
    }

    private boolean isReceiverPointer(GoCallExpr o) {
        GoSignatureOwner goSignatureOwner = GoPsiImplUtil.resolveCall(o);
        if (goSignatureOwner == null) return false;

        if (!(goSignatureOwner instanceof GoMethodDeclaration)) return false;
        GoMethodDeclaration methodDeclaration = (GoMethodDeclaration) goSignatureOwner;
        GoReceiver methodDeclarationReceiver = methodDeclaration.getReceiver();
        if (methodDeclarationReceiver == null) return false;
        GoType type = methodDeclarationReceiver.getType();
        if (type == null) return false;

        return type instanceof GoPointerType;
    }

    private void evaluateReferenceIssues(@NotNull GoCallExpr o, PsiElement assignedVariable, @NotNull GoProblemsHolder holder) {
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
                    if (areEqualReferences(assignedVariable, referenceElement)) {
                        holder.registerProblem(referenceElement, () -> PASSING_REFERENCE_OF_COPIED_RANGE_ELEMENT);
                        didFindProblems = true;
                    }

                    // Check with parent reference; but register problem as future
                    PsiElement parentResolvedElement = getParentPsiElement(referenceElement);
                    if (parentResolvedElement != null && areEqualReferences(assignedVariable, parentResolvedElement)) {
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

    private boolean areEqualReferences(PsiElement assignedVariable, PsiElement referenceElement) {
        return assignedVariable.getContainingFile().getManager().areElementsEquivalent(referenceElement, assignedVariable);
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