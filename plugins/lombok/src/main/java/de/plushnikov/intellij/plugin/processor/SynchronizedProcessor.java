package de.plushnikov.intellij.plugin.processor;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import de.plushnikov.intellij.plugin.LombokNames;
import de.plushnikov.intellij.plugin.problem.LombokProblem;
import de.plushnikov.intellij.plugin.problem.ProblemNewBuilder;
import de.plushnikov.intellij.plugin.quickfix.PsiQuickFixFactory;
import de.plushnikov.intellij.plugin.util.PsiAnnotationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Inspect and validate @Synchronized lombok annotation
 *
 * @author Plushnikov Michail
 */
public class SynchronizedProcessor extends AbstractProcessor {

  public SynchronizedProcessor() {
    super(PsiElement.class, LombokNames.SYNCHRONIZED);
  }

  @NotNull
  @Override
  public Collection<PsiAnnotation> collectProcessedAnnotations(@NotNull PsiClass psiClass) {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<LombokProblem> verifyAnnotation(@NotNull PsiAnnotation psiAnnotation) {
    final ProblemNewBuilder problemNewBuilder = new ProblemNewBuilder(2);

    PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiAnnotation, PsiMethod.class);
    if (null != psiMethod) {
      if (psiMethod.hasModifierProperty(PsiModifier.ABSTRACT)) {
        problemNewBuilder.addError("'@Synchronized' is legal only on concrete methods.",
          PsiQuickFixFactory.createModifierListFix(psiMethod, PsiModifier.ABSTRACT, false, false)
        );
      }

      final String lockFieldName = PsiAnnotationUtil.getStringAnnotationValue(psiAnnotation, "value");
      if (StringUtil.isNotEmpty(lockFieldName)) {
        final PsiClass containingClass = psiMethod.getContainingClass();

        if (null != containingClass) {
          final PsiField lockField = containingClass.findFieldByName(lockFieldName, true);
          if (null != lockField) {
            if (!lockField.hasModifierProperty(PsiModifier.FINAL)) {
              problemNewBuilder.addWarning(String.format("Synchronization on a non-final field %s.", lockFieldName),
                PsiQuickFixFactory.createModifierListFix(lockField, PsiModifier.FINAL, true, false));
            }
          } else {
            final PsiClassType javaLangObjectType = PsiType.getJavaLangObject(containingClass.getManager(), containingClass.getResolveScope());

            problemNewBuilder.addError(String.format("The field %s does not exist.", lockFieldName),
              PsiQuickFixFactory.createNewFieldFix(containingClass, lockFieldName, javaLangObjectType, "new Object()", PsiModifier.PRIVATE, PsiModifier.FINAL));
          }
        }
      }
    } else {
      problemNewBuilder.addError("'@Synchronized' is legal only on methods.");
    }

    return problemNewBuilder.getProblems();
  }
}
