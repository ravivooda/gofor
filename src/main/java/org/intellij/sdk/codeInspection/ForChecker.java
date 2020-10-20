package org.intellij.sdk.codeInspection;

import com.goide.inspections.core.GoInspectionBase;
import com.goide.inspections.core.GoProblemsHolder;
import com.goide.psi.*;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ForChecker extends GoInspectionBase {
    private static final Logger LOG = Logger.getInstance(ForChecker.class);

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

                ArrayList<PsiElement> usages = getUsages(forStatement, assignedVariable);

                LOG.warn("\n\n\n");

                LOG.warn("visitCallExpr: " + o + ", text: " + o.getText() + "\nargument list: " + o.getArgumentList().getText() + "\nexpression: " + o.getExpression().getText());

                PsiReference psiReference = o.getExpression().getReference();
                PsiElement resolved = psiReference != null ? psiReference.resolve() : null;
                if (!(resolved instanceof GoFunctionOrMethodDeclaration)) return;

                LOG.warn("visitCallExpr: successfully resolved to " + resolved);

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
                            for (PsiElement usage : usages) {
                                LOG.warn("visitCallExpr: comparing with usages: " + usages.stream().map(PsiElement::getText).collect(Collectors.joining()));
                                LOG.warn("USAGE reference: " + assignedVariable.getReference());
                                LOG.warn("REFERENCE reference: " + referenceElement.getReference());
                                holder.registerProblem(referenceElement, () -> "Passing reference of copied range element");
                                holder.registerProblem(assignedVariable, () -> "Passing reference of copied range element");
                                return;
                                /*if (forStatement.getManager().areElementsEquivalent(referenceElement, usage)) {
                                    LOG.warn("visitCallExpr: RAVI MATCHED: usage: " + usage.getText() + ", referenceElement: " + referenceElement.getText());
                                } else {
                                    LOG.warn("visitCallExpr: did not match usage: " +
                                            assignedVariable.getText() +
                                            ", referenceElement: " + referenceElement.getText());
                                }*/
                            }
                        }
                    }
                }
            }
        };
    }

    @Nullable
    private PsiElement getDeclarationElement(PsiElement element) {
        if (element == null) return null;

        if (element.getReference() != null) {
            element = element.getReference().resolve();
        }

        return element;
    }

    private @NotNull ArrayList<PsiElement> getReferenceElements(@NotNull GoExpression expr) {
        ArrayList<PsiElement> references = new ArrayList<>();
        if (expr instanceof GoUnaryExpr) {
            LOG.warn("getSingleReferences unary: " + expr.getText());
        } else if (expr instanceof GoReferenceExpression) {
            PsiElement identifier = ((GoReferenceExpression) expr).getIdentifier();
            LOG.warn("getSingleReferences reference identifier: " + identifier.getText());
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

    private @NotNull ArrayList<PsiElement> getUsages(@Nullable PsiElement o, @NotNull PsiElement element) {
        ArrayList<PsiElement> usageElements = new ArrayList<>();

        if (o == null) {
            return usageElements;
        }

        ReferencesSearch.search(element, o.getUseScope()).forEach(psiReference -> {
            ProgressManager.checkCanceled();
            usageElements.add(psiReference.getElement());
        });

        return usageElements;
    }

    private void logChildren(@NotNull PsiElement element) {
        for (int i = 0; i < element.getChildren().length; i++) {
            LOG.warn("Ravi: " + element.toString() + ": Children at index " + i + " is " + element.getChildren()[i].getText());
        }
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


/*@Override
            public void visitForStatement(@NotNull GoForStatement o) {
                super.visitForStatement(o);
                if (o.getBlock() == null) {
                    LOG.warn("Ravi: Empty block");
                    return;
                }
                LOG.warn("Ravi: Got inside the visitForStatement: " + o.getFor().getText());
                PsiElement assignedVariable = getFaultyForLoop(o);
                if (assignedVariable == null) return;

                if (assignedVariable.getReference() != null) {
                    assignedVariable = assignedVariable.getReference().resolve();
                }

                logChildren(o);

                assert assignedVariable != null;
                Collection<PsiReference> blockUsages = ReferencesSearch.search(assignedVariable, o.getBlock().getUseScope()).findAll();

                if (blockUsages.size() == 0) {
                    LOG.warn("Ravi: Did not find any usages of the assignedVariable: " + assignedVariable.getText());
                    return;
                }

                LOG.warn("Ravi: registered a problem for " + assignedVariable.getText() + " for all usages " + blockUsages.stream().map(PsiReference::getCanonicalText).collect(Collectors.joining()));
                *//*for (PsiReference usage : blockUsages) {
                    holder.registerProblem(usage.getElement(), () -> "Ravi Problem");
                }
                holder.registerProblem(assignedVariable, () -> "Ravi Problem");*//*

            }*/