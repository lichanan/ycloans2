package com.gacfinance.ycloans2.convertor;

import com.gacfinance.ycloans2.convertor.grammar.single.Java8Lexer;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;

public class OrmMain {

    public static void main(String[] args) throws Exception{
        String fileName = "D:\\intellij_project\\ycloans2\\code-convertor\\src\\main\\resources\\code\\LmLoanDAOImp.java";
        String targetFileName = "d:\\test\\LmLoanDAOImp.java";
//        String fileName = "D:\\intellij_project\\ceylon.tool.converter.java2ceylon\\testFiles\\TestAbstract.java";
//        String targetFileName = "d:\\test\\TestAbstract.ceylon";

        InputStream is = new FileInputStream(fileName);
        CharStream stream = new ANTLRInputStream(is);
        Java8Lexer java8Lexer = new Java8Lexer(stream);
        TokenStream tokens = new CommonTokenStream(java8Lexer);

        // Feed the tokens into the parser.
        Java8Parser parser = new Java8Parser(tokens);
        ParseTree parseTree = parser.compilationUnit();

        FileWriter fw = new FileWriter(targetFileName);
        ScopeTree scopeTree = new ScopeTree();

//        scopeTree.visit(parseTree);
        parseTree.accept(scopeTree);

//        JavaToCeylonConverter javaToCeylonConverter = new JavaToCeylonConverter(fw,false,false,scopeTree);
        OrmHibernateClassConvertor ormHibernateClassConvertor = new OrmHibernateClassConvertor(fw,false,false,scopeTree);
        parseTree.accept(ormHibernateClassConvertor);
//        javaToCeylonConverter.visit(parseTree);

//        javaToCeylonConverter.visitCompilationUnit(parser.compilationUnit());

        fw.flush();
        fw.close();

    }

}
