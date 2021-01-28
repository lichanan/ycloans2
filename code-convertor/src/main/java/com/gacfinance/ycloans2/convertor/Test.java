package com.gacfinance.ycloans2.convertor;

import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.io.File;
import java.io.IOException;

public class Test {

    public static void main(String[] args){
        try {
            DetailAST detailAST = JavaParser.parseFile(new File(
//                    "D:\\intellij_project\\ycloans\\src\\ycloans\\com\\yuchengtech\\ycloans\\db\\dao\\LmLoanDAO.java",
                    "D:\\intellij_project\\ycloans\\src\\ycloans\\com\\yuchengtech\\ycloans\\db\\dao\\imp\\LmLoanDAOImp.java"),
                    JavaParser.Options.WITH_COMMENTS);

            DetailAST curNode = detailAST;
            while (curNode != null) {
                notifyVisit(curNode);
                DetailAST toVisit = curNode.getFirstChild();
                while (curNode != null && toVisit == null) {
                    toVisit = curNode.getNextSibling();
                    curNode = curNode.getParent();
                }
                curNode = toVisit;
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (CheckstyleException e) {
            e.printStackTrace();
        }
    }

    private static void notifyVisit(DetailAST detailAST) {
        if (detailAST.getType() == TokenTypes.PACKAGE_DEF) {
            String pkgName = FullIdent.createFullIdent(
                    detailAST.getLastChild().getPreviousSibling()).getText();

            System.out.println(pkgName);
        }
        else if (detailAST.getType() == TokenTypes.IMPORT) {
            final FullIdent imp = FullIdent.createFullIdentBelow(detailAST);
/*                if (isFromPackage(imp.getText(), "java.lang")) {
                log(ast, MSG_LANG, imp.getText());
            }
            // imports from unnamed package are not allowed,
            // so we are checking SAME rule only for named packages
            else if (pkgName != null && isFromPackage(imp.getText(), pkgName)) {
                log(ast, MSG_SAME, imp.getText());
            }
            // Check for a duplicate import
            imports.stream().filter(full -> imp.getText().equals(full.getText()))
                    .forEach(full -> log(ast, MSG_DUPLICATE, full.getLineNo(), imp.getText()));

            imports.add(imp);*/
            System.out.println(imp.getText());
        }
        else if (detailAST.getType() == TokenTypes.STATIC_IMPORT){
            // Check for a duplicate static import
            final FullIdent imp =
                    FullIdent.createFullIdent(
                            detailAST.getLastChild().getPreviousSibling());
//                staticImports.stream().filter(full -> imp.getText().equals(full.getText()))
//                        .forEach(full -> log(ast, MSG_DUPLICATE, full.getLineNo(), imp.getText()));
//
//                staticImports.add(imp);
            System.out.println(imp.getText());
        }else if (detailAST.getType() == TokenTypes.METHOD_DEF){
            final DetailAST modifiers =
                    detailAST.findFirstToken(TokenTypes.MODIFIERS);

            final DetailAST returnType =
                    detailAST.findFirstToken(TokenTypes.TYPE);

            final DetailAST method =
                    detailAST.findFirstToken(TokenTypes.IDENT);

            final DetailAST parameters =
                    detailAST.findFirstToken(TokenTypes.PARAMETERS);

            System.out.println(method.getText());

            if (modifiers.hasChildren()) {
                System.out.println(modifiers.getLastChild().getText());
            }

            if (returnType.getLastChild().hasChildren()){
                System.out.println(returnType.getLastChild().getFirstChild().getText());
                if (returnType.getLastChild().getFirstChild().getNextSibling().findFirstToken(TokenTypes.IDENT)!=null){
                    System.out.println(returnType.getLastChild().getFirstChild().getNextSibling().findFirstToken(TokenTypes.IDENT).getText());
                }
                System.out.println(returnType.getLastChild().getLastChild().getText());
            }else{
                System.out.println(returnType.getLastChild().getText());
            }

            if (parameters.hasChildren()) {
                DetailAST paramter = parameters.getFirstChild();
                DetailAST parameterName = paramter.findFirstToken(TokenTypes.IDENT);
                DetailAST parameterType =  paramter.findFirstToken(TokenTypes.TYPE).getFirstChild();

                System.out.println(parameterName.getText());
                System.out.println(parameterType.getText());

                while (paramter.getNextSibling()!=null){
                    paramter = paramter.getNextSibling();
                    parameterName = paramter.findFirstToken(TokenTypes.IDENT);
                    if (parameterName!=null) {
                        System.out.println(parameterName.getText());
                        parameterType = paramter.findFirstToken(TokenTypes.TYPE).getFirstChild();
                        System.out.println(parameterType.getText());
                    }
                }
            }
        }else if (detailAST.getType() == TokenTypes.BLOCK_COMMENT_BEGIN){
            final DetailAST commentContent = detailAST.getFirstChild();
            System.out.println(commentContent.getText());
        }else if (detailAST.getType() == TokenTypes.SLIST){
            final DetailAST codeContent = detailAST.getFirstChild();
            System.out.println(codeContent.getText());
        }else if (detailAST.getType() == TokenTypes.CLASS_DEF) {
//            final DetailAST codeContent = detailAST.findFirstToken();
//            System.out.println("classDef:" + detailAST.);
        }
    }

}
