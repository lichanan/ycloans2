/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Apache License, Version 2.0 which is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0 
 ********************************************************************************/
package com.gacfinance.ycloans2.convertor;

import com.gacfinance.ycloans2.convertor.grammar.single.Java8BaseVisitor;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

public class ScopeTree extends Java8BaseVisitor<Void> {
    public Node root, scopeNode;

    public ScopeTree() {
        root = new Node();
    }

    public static class Node {
        ParserRuleContext data;
        Node parent;
        List<Node> children;
        boolean scope, variable, optional;

        Node() {
            data = new ParserRuleContext();
            scope = false;
            variable = false;
            optional = false;
            children = new ArrayList<>();
        }

        void addNode(Node n) {
            children.add(n);
        }
    }

    public Node getNode(ParserRuleContext ctx, Node n) {
        if (ctx == n.data) {
            return n;
        } else {
            for (Node c : n.children) {
                Node found = getNode(ctx, c);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    @Override
    public Void visitCompilationUnit(Java8Parser.CompilationUnitContext ctx) {
        Node n = new Node();
        n.data = ctx;
        n.scope = true;
        root = n;

        scopeNode = n;

        return super.visitCompilationUnit(ctx);
    }

    @Override
    public Void visitNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {
        Node n = new Node();
        n.data = ctx;
        n.parent = scopeNode;
        n.scope = true;
        scopeNode.addNode(n);

        scopeNode = n;
        visitClassBody(ctx.classBody());
        scopeNode = n.parent;

        return null;
    }

    @Override
    public Void visitVariableDeclaratorId(Java8Parser.VariableDeclaratorIdContext ctx) {
        Node n = new Node();
        n.data = ctx;
        n.parent = scopeNode;

        ParserRuleContext parent = ctx.getParent();

        if (parent instanceof Java8Parser.VariableDeclaratorContext) {
            Java8Parser.VariableInitializerContext initial = ((Java8Parser.VariableDeclaratorContext) parent).variableInitializer();

            if (initial != null && initial.getText().equals("null"))
                n.optional = true;
        }

        scopeNode.addNode(n);

        return super.visitVariableDeclaratorId(ctx);
    }

    @Override
    public Void visitMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        Node n = new Node();
        n.parent = scopeNode;
        n.data = ctx;
        n.scope = true;
        scopeNode.addNode(n);

        scopeNode = n;

        visitMethodHeader(ctx.methodHeader());
        visitMethodBody(ctx.methodBody());

        scopeNode = n.parent;

        return null;
    }

    @Override
    public Void visitPostIncrementExpression(Java8Parser.PostIncrementExpressionContext ctx) {
        String var = ctx.postfixExpression().getText();
        checkVariable(scopeNode, var, false);

        return super.visitPostIncrementExpression(ctx);
    }

    @Override
    public Void visitPostfixExpression(Java8Parser.PostfixExpressionContext ctx) {
        if (ctx.postDecrementExpression_lf_postfixExpression(0) != null
                || ctx.postIncrementExpression_lf_postfixExpression(0) != null) {
            String var = ctx.expressionName().getText();
            checkVariable(scopeNode, var, false);
        }

        return super.visitPostfixExpression(ctx);
    }

    @Override
    public Void visitPostDecrementExpression(Java8Parser.PostDecrementExpressionContext ctx) {
        String var = ctx.postfixExpression().getText();
        checkVariable(scopeNode, var, false);

        return super.visitPostDecrementExpression(ctx);
    }

    @Override
    public Void visitPreIncrementExpression(Java8Parser.PreIncrementExpressionContext ctx) {
        String var = ctx.unaryExpression().getText();
        checkVariable(scopeNode, var, false);

        return super.visitPreIncrementExpression(ctx);
    }

    @Override
    public Void visitPreDecrementExpression(Java8Parser.PreDecrementExpressionContext ctx) {
        String var = ctx.unaryExpression().getText();
        checkVariable(scopeNode, var, false);

        return super.visitPreDecrementExpression(ctx);
    }

    @Override
    public Void visitAssignment(Java8Parser.AssignmentContext ctx) {
        String leftHandSide = ctx.leftHandSide().getText();

        String expression = ctx.expression().getText();

        checkVariable(scopeNode, leftHandSide, expression.equals("null"));

        return super.visitAssignment(ctx);
    }

    private void checkVariable(Node n, String var, boolean isNull) {
        boolean flag = false;

        for (Node c : n.children) {
            if (c.data instanceof Java8Parser.LocalVariableDeclarationContext) {
                Java8Parser.LocalVariableDeclarationContext context = (Java8Parser.LocalVariableDeclarationContext) c.data;

                for (Java8Parser.VariableDeclaratorContext x : context.variableDeclaratorList().variableDeclarator()) {
                    String id = x.variableDeclaratorId().getText();

                    if (var.equals(id)) {
                        c.variable = true;

                        if (isNull)
                            c.optional = true;

                        flag = true;
                        break;
                    }

                }
            }
            if (c.data instanceof Java8Parser.VariableDeclaratorIdContext) {
                Java8Parser.VariableDeclaratorIdContext context = (Java8Parser.VariableDeclaratorIdContext) c.data;

                if (var.equals(context.Identifier().getText())) {
                    c.variable = true;
                    flag = true;

                    if (isNull)
                        c.optional = true;
                }
            }
        }

        if (!flag && n.parent != null)
            checkVariable(n.parent, var, isNull);
    }
}