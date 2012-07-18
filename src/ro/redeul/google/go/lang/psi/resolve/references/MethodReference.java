/*
* Copyright 2012 Midokura Europe SARL
*/
package ro.redeul.google.go.lang.psi.resolve.references;

import java.util.ArrayList;
import java.util.List;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.scope.util.PsiScopesUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import ro.redeul.google.go.lang.psi.GoPsiElement;
import ro.redeul.google.go.lang.psi.expressions.literals.GoLiteralIdentifier;
import ro.redeul.google.go.lang.psi.expressions.primary.GoSelectorExpression;
import ro.redeul.google.go.lang.psi.processors.GoResolveStates;
import ro.redeul.google.go.lang.psi.resolve.MethodResolver;
import ro.redeul.google.go.lang.psi.toplevel.GoMethodDeclaration;
import ro.redeul.google.go.lang.psi.types.GoType;
import ro.redeul.google.go.lang.psi.types.GoTypeName;
import ro.redeul.google.go.lang.psi.types.GoTypePointer;
import ro.redeul.google.go.util.LookupElementUtil;

/**
 * // TODO: mtoader ! Please explain yourself.
 */
public class MethodReference extends GoPsiReference<GoSelectorExpression> {

    GoTypeName baseTypeName;

    @Override
    public TextRange getRangeInElement() {
        GoLiteralIdentifier identifier = getElement().getIdentifier();
        if ( identifier == null )
            return null;

        return new TextRange(identifier.getStartOffsetInParent(),
                             identifier.getStartOffsetInParent() + identifier.getTextLength());
    }

    public MethodReference(@NotNull GoSelectorExpression element) {
        super(element);
    }

    @Override
    public PsiElement resolve() {
        baseTypeName = resolveBaseExpressionType();
        if (baseTypeName == null)
            return null;

        MethodResolver processor = new MethodResolver(this);

        PsiScopesUtil.treeWalkUp(
            processor,
            getElement().getContainingFile().getLastChild(),
            getElement().getContainingFile(),
            GoResolveStates.initial());

        return processor.getChildDeclaration();
    }

    @NotNull
    @Override
    public String getCanonicalText() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {

        if (!(element instanceof GoMethodDeclaration))
            return false;

        GoMethodDeclaration declaration = (GoMethodDeclaration)element;

        GoType receiverType = declaration.getMethodReceiver().getType();

        if (receiverType == null)
            return false;

        if (receiverType instanceof GoTypePointer) {
            receiverType = ((GoTypePointer)receiverType).getTargetType();
        }

        if ( !(receiverType instanceof GoTypeName))
            return false;

        GoTypeName methodTypeName = (GoTypeName)receiverType;

        if (baseTypeName.getName().equals(methodTypeName.getName()))
            return true;

        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @NotNull
    @Override
    public Object[] getVariants() {
        baseTypeName = resolveBaseExpressionType();
        if (baseTypeName == null)
            return LookupElementBuilder.EMPTY_ARRAY;

        final List<LookupElementBuilder> variants = new ArrayList<LookupElementBuilder>();

        MethodResolver processor = new MethodResolver(this) {
            @Override
            protected boolean addDeclaration(PsiElement declaration, PsiElement child) {
                String name = PsiUtilCore.getName(declaration);

                variants.add(LookupElementUtil.createLookupElement(
                    (GoPsiElement)declaration, name,
                    (GoPsiElement)child));
                return true;
            }
        };

        PsiScopesUtil.treeWalkUp(
            processor,
            getElement().getContainingFile().getLastChild(),
            getElement().getContainingFile(),
            GoResolveStates.initial());

        return variants.toArray(new LookupElementBuilder[variants.size()]);
    }

    private GoTypeName resolveBaseExpressionType() {
        GoType []types = getElement().getBaseExpression().getType();

        if (types.length != 1)
            return null;

        GoType type = types[0];
        if ( !(type instanceof GoTypeName) )
            return null;

        return (GoTypeName)type;
    }

    public boolean isSoft() {
        return false;
    }
}