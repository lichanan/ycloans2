package com.gacfinance.ycloans2.convertor;

import com.gacfinance.ycloans2.convertor.grammar.Java8Lexer;
import com.gacfinance.ycloans2.convertor.grammar.Java8Parser;
import com.gacfinance.ycloans2.convertor.grammar.Java8ParserBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main {

    public static class ClassNameOutputListener extends Java8ParserBaseListener {
//        BufferedTokenStream tokens;
//        TokenStreamRewriter rewriter;
        /** Create TokenStreamRewriter attached to token stream
         *  sitting between the Cymbol lexer and parser.
         */
/*        public CommentShifter() {
//            this.tokens = tokens;
//            rewriter = new TokenStreamRewriter(tokens);
        }*/

//        public void enterClassModifierContext

        @Override
        public void enterClassModifier(Java8Parser.ClassModifierContext ctx) {
//            System.out.println(ctx.getText());
        }





        @Override
        public void enterClassOrInterfaceType(Java8Parser.ClassOrInterfaceTypeContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override
        public void enterLiteral(Java8Parser.LiteralContext ctx) {
//            System.out.println(ctx.getText());
//            System.out.println(ctx.getParent().getText());
        }

        @Override
        public void enterPrimitiveType(Java8Parser.PrimitiveTypeContext ctx) {
//            System.out.println(ctx.getText());
//            System.out.println(ctx.getParent().getText());
        }


        @Override
        public void enterClassType(Java8Parser.ClassTypeContext ctx){
//            System.out.println(ctx.Identifier().getText());
//            System.out.println(ctx.getText());
        }

        @Override
        public void enterTypeArgument(Java8Parser.TypeArgumentContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override
        public void enterPackageOrTypeName(Java8Parser.PackageOrTypeNameContext ctx) {
//            System.out.println(ctx.getText());
        }

        public void enterMethodName(Java8Parser.PackageOrTypeNameContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override public void exitImportDeclaration(Java8Parser.ImportDeclarationContext ctx) {
//            System.out.println(ctx.getText());
        }


        @Override public void enterImportDeclaration(Java8Parser.ImportDeclarationContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override public void enterBlock(Java8Parser.BlockContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override public void enterBlockStatements(Java8Parser.BlockStatementsContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override public void enterMethodDeclarator(Java8Parser.MethodDeclaratorContext ctx) {
//            System.out.println(ctx.getText());
        }

//        @Override public void enterExceptionType(Java8Parser.ExceptionTypeListContext ctx) {
//            System.out.println(ctx.getText());
//        }

        @Override public void enterExceptionTypeList(Java8Parser.ExceptionTypeListContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override public void enterFormalParameter(Java8Parser.FormalParameterContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override public void enterMethodInvocation(Java8Parser.MethodInvocationContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override public void enterReturnStatement(Java8Parser.ReturnStatementContext ctx) {
//            System.out.println(ctx.getText());
        }

        @Override
        public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {
            System.out.println(ctx.Identifier().getText());
        }
    }


    public static void main(String[] args) throws IOException {
        String fileName = "D:\\intellij_project\\ycloans\\src\\ycloans\\com\\yuchengtech\\ycloans\\db\\dao\\imp\\LmLoanDAOImp.java";

        InputStream is = new FileInputStream(fileName);
        CharStream stream = new ANTLRInputStream(is);
        Java8Lexer java8Lexer = new Java8Lexer(stream);
        TokenStream tokens = new CommonTokenStream(java8Lexer);

        // Feed the tokens into the parser.
        Java8Parser parser = new Java8Parser(tokens);
        ParseTree parseTree = parser.compilationUnit();

        ParseTreeWalker walker = new ParseTreeWalker();
        ClassNameOutputListener classNameOutputListener = new ClassNameOutputListener();
        walker.walk(classNameOutputListener,parseTree);
    }

}
