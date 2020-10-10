package org.intellij.sdk.codeInspection;

import com.goide.inspections.core.GoInspectionBase;
import com.goide.inspections.core.GoProblemsHolder;
import com.goide.psi.GoForStatement;
import com.goide.psi.GoVisitor;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ForChecker extends GoInspectionBase {
    private static final Logger LOG = Logger.getInstance(ForChecker.class);

    @Override
    protected @NotNull GoVisitor buildGoVisitor(
            @NotNull GoProblemsHolder holder,
            @NotNull LocalInspectionToolSession session) {
        LOG.warn("Ravi: Got inside the buildGoVisitor");
        return new GoVisitor() {
            @Override
            public void visitForStatement(@NotNull GoForStatement o) {
                LOG.warn("Ravi: Got inside the visitForStatement: " + o.getFor().getText());
                PsiElement assignedVariable = getFaultyForLoop(o);
                if (assignedVariable == null) return;

                logChildren(o);

                ArrayList<PsiElement> blockUsages = getUsages(o.getBlock(), assignedVariable);
                if (blockUsages.size() == 0) {
                    LOG.warn("Ravi: Did not find any usages of the assignedVariable: " + assignedVariable.getText());
                    return;
                }

                LOG.warn("Ravi: registered a problem for " + assignedVariable.getText() + " for all usages " + blockUsages.stream().map(PsiElement::getText).collect(Collectors.joining()));
                for (PsiElement usage : blockUsages) {
                    holder.registerProblem(usage, () -> "Ravi Problem");
                }
                holder.registerProblem(assignedVariable, () -> "Ravi Problem");

            }
        };
    }

    private @NotNull ArrayList<PsiElement> getUsages(@Nullable PsiElement o, @NotNull PsiElement element) {
        ArrayList<PsiElement> usageElements = new ArrayList<>();

        if (o == null) {
            return usageElements;
        }

        if (PsiManager.getInstance(o.getProject()).areElementsEquivalent(o, element)) {
            LOG.warn("Ravi: areElementsEquivalent");
            ReferencesSearch.search(element, o.getUseScope()).forEach(psiReference -> {
                ProgressManager.checkCanceled();

            });
            usageElements.add(o);
        }

        for (PsiElement child : o.getChildren()) {
            usageElements.addAll(getUsages(child, element));
        }

        logChildren(o);

        return usageElements;
    }

    private void logChildren(@NotNull PsiElement element) {
        for (int i = 0; i < element.getChildren().length; i++) {
            LOG.warn("Ravi: " + element.toString() + ": Children at index " + i + " is " + element.getChildren()[i].getText());
        }
    }

    @Nullable
    private PsiElement getFaultyForLoop(@NotNull GoForStatement o) {
        if (o.getRangeClause() == null) {
//            LOG.warn("Ravi: did not find any range element");
            return null;
        }
        PsiElement assignedVariable;
        if (o.getRangeClause().getVarAssign() != null) {
//            LOG.warn("Ravi: " + o.getRangeClause().getVarAssign());
            if (o.getRangeClause().getVarDefinitionList().size() <= 1) {
//                LOG.warn("Ravi: only using single assignment like a great person in new assignment");
                return null;
            }
//            LOG.warn("Ravi: " + o.getRangeClause().getVarDefinitionList().get(0).getName());
//            LOG.warn("Ravi: " + o.getRangeClause().getVarDefinitionList().get(1).getName());
            assignedVariable = o.getRangeClause().getVarDefinitionList().get(1);
        } else {
            if (o.getRangeClause().getLeftExpressionsList().size() <= 1) {
//                LOG.warn("Ravi: only using single assignment like a great person for old assignment");
                return null;
            }
//            LOG.warn("Ravi: " + o.getRangeClause().getLeftExpressionsList().get(0).getText());
//            LOG.warn("Ravi: " + o.getRangeClause().getLeftExpressionsList().get(1).getText());
//            LOG.warn("Ravi: " + o.getRangeClause().getRightExpressionsList().get(0).getText());
            assignedVariable = o.getRangeClause().getLeftExpressionsList().get(1);
        }
        return assignedVariable;
    }
}
