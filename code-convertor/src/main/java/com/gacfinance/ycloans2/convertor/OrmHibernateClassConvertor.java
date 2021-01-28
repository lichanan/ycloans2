package com.gacfinance.ycloans2.convertor;

import com.gacfinance.ycloans2.convertor.grammar.single.Java8BaseVisitor;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.CompilationUnitContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.ImportDeclarationContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.TypeNameContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.NormalClassDeclarationContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.ClassBodyDeclarationContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.TypeParametersContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.TypeParameterListContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.TypeParameterContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.TypeBoundContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.WildcardBoundsContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.WildcardContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.SuperclassContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.SuperinterfacesContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.ClassTypeContext;
import com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.ClassOrInterfaceTypeContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrmHibernateClassConvertor extends Java8BaseVisitor<Void> {

    private boolean transformGetters;
    private boolean useValues;
    private Writer writer;
    private Pattern GETTER_PATTERN = Pattern.compile("(get|is)([A-Z]\\w*)");
    private ScopeTree scopeTree;
    private ClassTypeContext superClass;
    private static final List<String> RESERVED_KEYWORDS = Arrays.asList(
            "assembly", "abstracts", "alias", "assert", "assign", "break", "case", "catch", "class",
            "continue", "dynamic", "else", "exists", "extends", "finally", "for", "function", "given", "if", "import",
            "in", "interface", "is", "module", "nonempty", "object", "of", "out", "outer", "package", "return",
            "satisfies", "super", "switch", "then", "this", "throw", "try", "value", "void", "while"
    );


    public OrmHibernateClassConvertor(Writer out, boolean transformGetters, boolean useValues, ScopeTree scopeTree) {
        writer = out;
        this.transformGetters = transformGetters;
        this.useValues = useValues;
        this.scopeTree = scopeTree;
    }

    private void write(String str) {
        try {
            writer.write(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasModifier(List<? extends ParserRuleContext> modifiers, String modifier) {
        for (ParserRuleContext m : modifiers) {
            if (m.getText().equals(modifier)) {
                return true;
            }
        }

        return false;
    }

    private void addImport(Map<String, List<String>> importsByPackage, String pack, String type) {
        List<String> imports;

        pack = escapePackageIdentifiers(pack);

        if (importsByPackage.containsKey(pack)) {
            imports = importsByPackage.get(pack);
        } else {
            imports = new ArrayList<>();
            importsByPackage.put(pack, imports);
        }

        // import on demand wins over single imports
        if (type.equals("...") && !imports.isEmpty()) {
            imports.clear();
        }

        if (imports.size() == 1 && imports.get(0).equals("...")) {
            return; // No need to add a single import if there's already a wildcard
        }

        if(!imports.contains(type))
            imports.add(type);
    }

    private String escapePackageIdentifiers(String pack) {
        StringBuilder builder = new StringBuilder();

        for (String id : pack.split("\\.")) {
            if (builder.length() > 0) {
                builder.append(".");
            }
            builder.append(escapeIdentifier(id, false));
        }

        return builder.toString();
    }



    private void addStaticImport(Map<String, List<String>> staticImports, String pack, String type) {
        List<String> imports;

        if(staticImports.containsKey(pack)) {
            imports = staticImports.get(pack);
        } else {
            imports = new ArrayList<>();
            staticImports.put(pack, imports);
        }

        // import on demand wins over single imports
        if (type.equals("...") && !imports.isEmpty()) {
            imports.clear();
        }

        if (imports.size() == 1 && imports.get(0).equals("...")) {
            return; // No need to add a single import if there's already a wildcard
        }

        if(!imports.contains(type))
            imports.add(type);
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitContext ctx) {
        Map<String, List<String>> importsByPackage = new LinkedHashMap<>();
        Map<String, List<String>> staticImports = new LinkedHashMap<>();

        for (ImportDeclarationContext decl : ctx.importDeclaration()) {
            if (decl.singleTypeImportDeclaration() != null) {
                com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.TypeNameContext typeName = decl.singleTypeImportDeclaration().typeName();
                addImport(importsByPackage, typeName.packageOrTypeName().getText(), typeName.Identifier().getText());
            }
            if (decl.typeImportOnDemandDeclaration() != null) {
                String pkgName = decl.typeImportOnDemandDeclaration().packageOrTypeName().getText();
                addImport(importsByPackage, pkgName, "...");
            }

            if(decl.singleStaticImportDeclaration() != null) {
                TypeNameContext typeName = decl.singleStaticImportDeclaration().typeName();
                addImport(importsByPackage, typeName.packageOrTypeName().getText(), typeName.Identifier().getText());
                addStaticImport(staticImports, typeName.getText(), decl.singleStaticImportDeclaration().Identifier().getText());
            }
            if(decl.staticImportOnDemandDeclaration() != null) {
                TypeNameContext typeName = decl.staticImportOnDemandDeclaration().typeName();
                addImport(importsByPackage, typeName.packageOrTypeName().getText(), typeName.Identifier().getText());
                addStaticImport(staticImports, typeName.getText(), "...");
            }
        }

        for (Map.Entry<String, List<String>> entry : importsByPackage.entrySet()) {
            String key = entry.getKey();
            write("import ");
            write(key);
            write(" {\n");

            for (int i = 0; i < entry.getValue().size(); i++) {
                if (i > 0) {
                    write(",\n");
                }

                String value = entry.getValue().get(i);

                write(value);
                if(staticImports.containsKey(key + "." + value)) {
                    write("{\n");
                    List<String> imports = staticImports.get(key + "." + value);
                    for(int j = 0; j < imports.size(); j++) {
                        if(j > 0)
                            write(", \n");

                        write(imports.get(j));
                    }
                    write("\n}\n");
                }
            }
            write("\n}\n");
        }

        return super.visitCompilationUnit(ctx);
    }

    @Override
    public Void visitNormalClassDeclaration(NormalClassDeclarationContext ctx) {
        if (hasModifier(ctx.classModifier(), "public")) {
            write("public ");
        }
        if (hasModifier(ctx.classModifier(), "static")) {
            write("static ");
        }
        if (hasModifier(ctx.classModifier(), "abstract")) {
            write("abstract ");
        }
        write("class ");

        String identifier = ctx.Identifier().getText();
        identifier = Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1);

        write(identifier); // TODO uppercase first letter
        if (ctx.typeParameters() != null) {
            visitTypeParameters(ctx.typeParameters());
        }
        boolean hasExplicitConstructor = false;
        for (ClassBodyDeclarationContext decl : ctx.classBody().classBodyDeclaration()) {
            if (decl.constructorDeclaration() != null) {
                hasExplicitConstructor = true;
                break;
            }
        }
        if (!hasExplicitConstructor) {
            write("()");
        }
        if (ctx.superclass() != null) {
            visitSuperclass(ctx.superclass());

            if (!hasExplicitConstructor) {
                write("()");
            }
        }
        if (ctx.superinterfaces() != null) {
            visitSuperinterfaces(ctx.superinterfaces());
        }
        visitClassBody(ctx.classBody());

        return null;
    }


    @Override
    public Void visitTypeParameters(TypeParametersContext ctx) {
        write("<");
        visitTypeParameterList(ctx.typeParameterList());
        write(">");
        return null;
    }

    @Override
    public Void visitTypeParameterList(TypeParameterListContext ctx) {
        boolean isFirst = true;

        for (TypeParameterContext param : ctx.typeParameter()) {
            if (!isFirst) {
                write(", ");
            }
            visitTypeParameter(param);
            isFirst = false;
        }
        return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameterContext ctx) {
        write(ctx.Identifier().getText());
        if (ctx.typeBound() != null) {
            visitTypeBound(ctx.typeBound());
        }
        return null;
    }

    @Override
    public Void visitWildcard(WildcardContext ctx) {
        com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.WildcardBoundsContext bounds = ctx.wildcardBounds();
        if (bounds != null) {
            if (bounds.getChild(0).getText().equals("extends")) {
                write("out ");
            } else {
                write("in ");
            }
            visitReferenceType((com.gacfinance.ycloans2.convertor.grammar.single.Java8Parser.ReferenceTypeContext) bounds.getChild(1));
        } else {
            write("out Object");
        }
        return null;
    }

    @Override
    public Void visitTypeBound(TypeBoundContext ctx) {
        return super.visitTypeBound(ctx);
    }

    @Override
    public Void visitSuperclass(SuperclassContext ctx) {
        write(" extends ");
        superClass = ctx.classType();
        super.visitSuperclass(ctx);
        return null;
    }

    @Override
    public Void visitSuperinterfaces(SuperinterfacesContext ctx) {
        write(" implements ");
        return super.visitSuperinterfaces(ctx);
    }

    @Override
    public Void visitClassType(ClassTypeContext ctx) {
        if (ctx.getChild(0) instanceof ClassOrInterfaceTypeContext) {
            visitClassOrInterfaceType((ClassOrInterfaceTypeContext) ctx.getChild(0));
            write(".");
        }
        write(ctx.Identifier().getText());
        if (ctx.typeArguments() != null) {
            visitTypeArguments(ctx.typeArguments());
        }
        return null;
    }

    @Override
    public Void visitInterfaceTypeList(Java8Parser.InterfaceTypeListContext ctx) {
        boolean isFirst = true;
        for (Java8Parser.InterfaceTypeContext type : ctx.interfaceType()) {
            if (!isFirst) {
                write(" , ");
            }
            visitInterfaceType(type);
            isFirst = false;
        }
        return null;
    }

    @Override
    public Void visitClassBody(Java8Parser.ClassBodyContext ctx) {
        write(" {\n\n");
        Void result = super.visitClassBody(ctx);
        write("}\n");

        return result;
    }

    @Override
    public Void visitMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        if (hasModifier(ctx.methodModifier(), "public")) {
            write("public ");
        }
        if (hasModifier(ctx.methodModifier(), "static")) {
            write("static ");
        }
        if (hasModifier(ctx.methodModifier(), "@Override")) {
            if(!hasModifier(ctx.methodModifier(), "public"))
                write("private ");
            else
                write("public ");
        }
        if (hasModifier(ctx.methodModifier(), "abstract")) {
            write("abstract ");
        }

        visitMethodHeader(ctx.methodHeader());
        visitMethodBody(ctx.methodBody());
        write("\n");

        return null;
    }

    @Override
    public Void visitMethodHeader(Java8Parser.MethodHeaderContext ctx) {
        if (ctx.result().getText().equals("void")) {
            write("void ");
        } else {
            visitUnannType(ctx.result().unannType());
            write(" ");
        }
        visitMethodDeclarator(ctx.methodDeclarator());
        return null;
    }

    @Override
    public Void visitMethodDeclarator(Java8Parser.MethodDeclaratorContext ctx) {
        String methodName = ctx.Identifier().getText();
        Matcher matcher = GETTER_PATTERN.matcher(methodName);
        if (transformGetters && matcher.matches() && ctx.formalParameterList() == null) {
            String property = matcher.group(2);
            // TODO we should use NamingBase.getJavaBeanName() instead
            if (property.length() > 1) {
                property = Character.toLowerCase(property.charAt(0)) + property.substring(1);
            } else {
                property = property.toLowerCase();
            }
            write(escapeIdentifier(property, true));
        } else if ("toString".equals(methodName) && ctx.formalParameterList() == null) {
            write("string");
        } else if ("hashCode".equals(methodName) && ctx.formalParameterList() == null) {
            write("hash");
        } else {
            write(escapeIdentifier(methodName, true));

            write("(");
            if (ctx.formalParameterList() != null) {
                visitFormalParameterList(ctx.formalParameterList());
            }
            write(")");
        }
        return null;
    }

    @Override
    public Void visitMethodBody(Java8Parser.MethodBodyContext ctx) {
        if (ctx.block() == null) {
            write(";\n");
        } else {
            write(" ");
            return super.visitMethodBody(ctx);
        }
        return null;
    }

    @Override
    public Void visitFormalParameterList(Java8Parser.FormalParameterListContext ctx) {
        if (ctx.formalParameters() != null) {
            for (Java8Parser.FormalParameterContext param : ctx.formalParameters().formalParameter()) {
                visitFormalParameter(param);
                write(", ");
            }
        }

        if (ctx.lastFormalParameter() != null) {
            Java8Parser.LastFormalParameterContext lastFormalParameter = ctx.lastFormalParameter();

            if (lastFormalParameter.formalParameter() != null) {
                visitFormalParameter(lastFormalParameter.formalParameter());
            } else {
                visitLastFormalParameter(lastFormalParameter);
            }
        }

        return null;
    }

    @Override
    public Void visitLastFormalParameter(Java8Parser.LastFormalParameterContext ctx) {
        ScopeTree.Node n = scopeTree.getNode(ctx.variableDeclaratorId(), scopeTree.root);

        if (n.variable && !hasModifier(ctx.variableModifier(), "final")) {
            write("variable ");
        }

        visitUnannType(ctx.unannType());
        write("* ");
        write(escapeIdentifier(ctx.variableDeclaratorId().getText(), true));

        return null;
    }

    @Override
    public Void visitFormalParameter(Java8Parser.FormalParameterContext param) {
        ScopeTree.Node n = scopeTree.getNode(param.variableDeclaratorId(), scopeTree.root);

        if (n.variable && !hasModifier(param.variableModifier(), "final")) {
            write("variable ");
        }
        visitUnannType(param.unannType());
        write(" ");
        write(escapeIdentifier(param.variableDeclaratorId().getText(), true));
        return null;
    }


    @Override
    public Void visitUnannPrimitiveType(Java8Parser.UnannPrimitiveTypeContext ctx) {
        String type = ctx.getText();

//        switch (type) {
//            case "int":
//            case "long":
//            case "short":
//                write("Integer");
//                break;
//            case "float":
//            case "double":
//                write("Float");
//                break;
//            case "boolean":
//                write("Boolean");
//                break;
//            case "byte":
//                write("Byte");
//                break;
//            case "char":
//                write("Character");
//                break;
//            default:
//                write(type);
//        }

        return null;
    }


    @Override
    public Void visitUnannClassType_lfno_unannClassOrInterfaceType(Java8Parser.UnannClassType_lfno_unannClassOrInterfaceTypeContext ctx) {
        write(ctx.Identifier().getText());
        if (ctx.typeArguments() != null) {
            visitTypeArguments(ctx.typeArguments());
        }
        return null;
    }

    @Override
    public Void visitUnannClassType_lf_unannClassOrInterfaceType(Java8Parser.UnannClassType_lf_unannClassOrInterfaceTypeContext ctx) {
        write(".");
        write(ctx.Identifier().getText());
        if (ctx.typeArguments() != null) {
            visitTypeArguments(ctx.typeArguments());
        }
        return null;
    }

    @Override
    public Void visitNormalInterfaceDeclaration(Java8Parser.NormalInterfaceDeclarationContext ctx) {
        if (hasModifier(ctx.interfaceModifier(), "public")) {
            write("public ");
        }
        if (hasModifier(ctx.interfaceModifier(), "static")) { //TODO: or if it is any nested interface!!!
            write("static ");
        }

        write("interface ");

        String identifier = ctx.Identifier().getText();
        identifier = Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1);

        write(identifier); // TODO uppercase first letter
        if (ctx.typeParameters() != null) {
            visitTypeParameters(ctx.typeParameters());
        }
        if (ctx.extendsInterfaces() != null) {
            visitExtendsInterfaces(ctx.extendsInterfaces());
        }
        visitInterfaceBody(ctx.interfaceBody());

        return null;
    }

    @Override
    public Void visitInterfaceMethodDeclaration(Java8Parser.InterfaceMethodDeclarationContext ctx) {
        write("public ");

        if (hasModifier(ctx.interfaceMethodModifier(), "default")) {
            write("default ");
        } else {
//            write("formal ");
        }

        visitMethodHeader(ctx.methodHeader());
        visitMethodBody(ctx.methodBody());
        return null;
    }


    @Override
    public Void visitInterfaceBody(Java8Parser.InterfaceBodyContext ctx) {
        write(" {\n");
        for (Java8Parser.InterfaceMemberDeclarationContext decl : ctx.interfaceMemberDeclaration()) {
            visitInterfaceMemberDeclaration(decl);
        }
        write("}\n");
        return null;
    }

    @Override
    public Void visitExtendsInterfaces(Java8Parser.ExtendsInterfacesContext ctx) {
        write(" extends ");
        return super.visitExtendsInterfaces(ctx);
    }

    @Override
    public Void visitConstructorDeclaration(Java8Parser.ConstructorDeclarationContext ctx) {
        write("public ");
        visitConstructorDeclarator(ctx.constructorDeclarator());

        Java8Parser.ExplicitConstructorInvocationContext child =
                ctx.constructorBody().explicitConstructorInvocation();

        if(child != null) {
            for(ParseTree c : child.children) {
                if(c.getText().equals("super")) {
                    write(" extends " + superClass.getText());
                    write("(");
                    if (child.argumentList() != null) {
                        visitArgumentList(child.argumentList());
                    }
                    write(")");
                    break;
                }
            }
        }

        visitConstructorBody(ctx.constructorBody());
        return null;
    }

    @Override
    public Void visitExplicitConstructorInvocation(
            Java8Parser.ExplicitConstructorInvocationContext ctx) {
        for(ParseTree c : ctx.children) {
            if(c.getText().equals("super")) {
                return null;
            }
        }

        return super.visitExplicitConstructorInvocation(ctx);
    }

    @Override
    public Void visitConstructorDeclarator(Java8Parser.ConstructorDeclaratorContext ctx) {
        write("new ");
        // TODO? name constructor
        write("(");
        if (ctx.formalParameterList() != null) {
            visitFormalParameterList(ctx.formalParameterList());
        }
        write(")");
        return null;
    }

    @Override
    public Void visitConstructorBody(Java8Parser.ConstructorBodyContext ctx) {
        write(" {\n");
        super.visitConstructorBody(ctx);
        write("}\n\n");
        return null;
    }

    @Override
    public Void visitMethodInvocation(Java8Parser.MethodInvocationContext ctx) {
        String name = null;

        if (ctx.methodName() != null) {
            write(escapeIdentifier(ctx.methodName().getText(), true));
        } else if (ctx.typeName() != null) {
            String text = ctx.typeName().getText();
            if (text.equals("System.out") && ctx.Identifier().getText().equals("println")) {
                name = "System.out.println";
            } else {
                write(text);
                write(".");
            }
        } else if (ctx.expressionName() != null) {
            name = ctx.expressionName().getText() + "." + ctx.Identifier().getText();
        } else if (ctx.primary() != null) {
            visitPrimary(ctx.primary());
            write(".");
        } else if (ctx.typeName() != null) {
            visitTypeName(ctx.typeName());
            write(".super.");
        } else {
            write("super.");
        }

        if (name != null) {
/*            switch (name) {
                case "System.out.println":
                    name = "print";
                    break;
            }*/

            write(escapeIdentifier(name, true));
        } else {
            if (ctx.Identifier() != null) {
                Matcher matcher = GETTER_PATTERN.matcher(ctx.Identifier().getText());
                if (transformGetters && matcher.matches() && ctx.argumentList() == null) {
                    String property = matcher.group(2);
                    if (property.length() > 1) {
                        property = Character.toLowerCase(property.charAt(0)) + property.substring(1);
                    } else {
                        property = property.toLowerCase();
                    }
                    write(escapeIdentifier(property, true));
                    return null;
                } else {
                    write(escapeIdentifier(ctx.Identifier().getText(), true));
                }
            }
            if (ctx.typeArguments() != null) {
                visitTypeArguments(ctx.typeArguments());
            }
        }

        write("(");
        if (ctx.argumentList() != null) {
            visitArgumentList(ctx.argumentList());
        }
        write(")");

        return null;
    }

    @Override
    public Void visitMethodInvocation_lfno_primary(Java8Parser.MethodInvocation_lfno_primaryContext ctx) {
        String prefix = "";
        String methodName = "";

        if (ctx.Identifier() != null) {
            methodName = ctx.Identifier().getText();
        }

        if (ctx.methodName() != null) {
            methodName = ctx.methodName().getText();
        } else if (ctx.typeName() != null) {
            prefix = escapeIdentifier(ctx.typeName().getText(), false) + ".";
            if (ctx.getChild(2).getText().equals("super")) {
                prefix += "super.";
            }
        } else if (ctx.expressionName() != null) {
            prefix = ctx.expressionName().getText();
        } else {
            prefix = "super.";
        }

        if ((prefix + methodName).equals("System.out.println")) {
            prefix = "";
            methodName = "System.out.println";
        }

        write(prefix);
        Matcher matcher = GETTER_PATTERN.matcher(methodName);
        if (transformGetters && matcher.matches() && ctx.argumentList() == null) {
            String property = matcher.group(2);
            if (property.length() > 1) {
                property = Character.toLowerCase(property.charAt(0)) + property.substring(1);
            } else {
                property = property.toLowerCase();
            }
            write(escapeIdentifier(property, true));
        } else if ("toString".equals(methodName) && ctx.argumentList() == null) {
            write("toString");
        } else if ("hashCode".equals(methodName) && ctx.argumentList() == null) {
            write("hashCode");
        } else {
            write(escapeIdentifier(methodName, true));
            write("(");
            if (ctx.argumentList() != null) {
                visitArgumentList(ctx.argumentList());
            }
            write(")");
        }

        return null;
    }

    @Override
    public Void visitMethodInvocation_lf_primary(Java8Parser.MethodInvocation_lf_primaryContext ctx) {
        String methodName = "";

        write(".");
        if (ctx.typeArguments() != null) {
            visitTypeArguments(ctx.typeArguments());
        }

        if (ctx.Identifier() != null) {
            methodName = ctx.Identifier().getText();
        }

        Matcher matcher = GETTER_PATTERN.matcher(methodName);
        if (transformGetters && matcher.matches() && ctx.argumentList() == null) {
            String property = matcher.group(2);
            if (property.length() > 1) {
                property = Character.toLowerCase(property.charAt(0)) + property.substring(1);
            } else {
                property = property.toLowerCase();
            }
            write(escapeIdentifier(property, true));
        } else if ("toString".equals(methodName) && ctx.argumentList() == null) {
            write("toString");
        } else if ("hashCode".equals(methodName) && ctx.argumentList() == null) {
            write("hashCode");
        } else {
            write(escapeIdentifier(ctx.Identifier().getText(), true));
            write("(");
            if (ctx.argumentList() != null) {
                visitArgumentList(ctx.argumentList());
            }
            write(")");
        }
        return null;
    }


    @Override
    public Void visitLiteral(Java8Parser.LiteralContext ctx) {
        if (ctx.FloatingPointLiteral() != null) {
            double d = Double.parseDouble(ctx.FloatingPointLiteral().getText());
            write(String.valueOf(d));
        } else {
            write(ctx.getText());
        }

        return super.visitLiteral(ctx);
    }

    @Override
    public Void visitReturnStatement(Java8Parser.ReturnStatementContext ctx) {
        write("return ");
        if (ctx.expression() != null) {
            visitExpression(ctx.expression());
        }
        write(";\n");
        return null;
    }

    @Override
    public Void visitThrowStatement(Java8Parser.ThrowStatementContext ctx) {
        write("throw ");
        if (ctx.expression() != null) {
            visitExpression(ctx.expression());
        }
        write(";\n");
        return null;
    }

    @Override
    public Void visitExpressionStatement(Java8Parser.ExpressionStatementContext ctx) {
        super.visitExpressionStatement(ctx);
        write(";\n");
        return null;
    }

    @Override
    public Void visitAssignment(Java8Parser.AssignmentContext ctx) {
        Java8Parser.ArrayAccessContext array = ctx.leftHandSide().arrayAccess();

        // Bypass array assignment to replace it with a.set(b, c)
        if (array != null) {
            if (array.expressionName() != null) {
                visitExpressionName(array.expressionName());
            } else {
                visitPrimaryNoNewArray_lfno_arrayAccess(array.primaryNoNewArray_lfno_arrayAccess());
            }
//            write(".set(");
//            visitExpression(array.expression().get(0));
//            write(", ");
//            visitExpression(ctx.expression());
//            write(")");
        } else {
            super.visitAssignment(ctx);
        }
        return null;
    }

    @Override
    public Void visitArrayAccess_lfno_primary(Java8Parser.ArrayAccess_lfno_primaryContext ctx) {
        // Bypass array access to replace it with a.get(b)
        if (ctx.expressionName() != null) {
            visitExpressionName(ctx.expressionName());
        } else {
            visitPrimaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary(
                    ctx.primaryNoNewArray_lfno_primary_lfno_arrayAccess_lfno_primary());
        }
        write(".get(");
        visitExpression(ctx.expression().get(0));
        write(")");
        return null;
    }

    @Override
    public Void visitAssignmentOperator(Java8Parser.AssignmentOperatorContext ctx) {
        write(" ");
        write(ctx.getText());
        write(" ");
        return null;
    }

    @Override
    public Void visitUnannArrayType(Java8Parser.UnannArrayTypeContext ctx) {
        if (ctx.unannPrimitiveType() != null) {
            String type = ctx.unannPrimitiveType().getText();
//            switch (type) {
//                case "int":
//                    ceylonType = "IntArray";
//                    break;
//                case "short":
//                    ceylonType = "ShortArray";
//                    break;
//                case "boolean":
//                    ceylonType = "BooleanArray";
//                    break;
//                case "byte":
//                    ceylonType = "ByteArray";
//                    break;
//                case "long":
//                    ceylonType = "LongArray";
//                    break;
//                case "float":
//                    ceylonType = "FloatArray";
//                    break;
//                case "double":
//                    ceylonType = "DoubleArray";
//                    break;
//                case "char":
//                    ceylonType = "CharArray";
//                    break;
//                default:
//                    ceylonType = "ObjectArray<" + type + ">";
//                    break;
//            }
            write(type);
        } else if (ctx.unannTypeVariable() != null) {
            write( ctx.unannTypeVariable().Identifier().getText() );
        } else {
//            write("ObjectArray<");
            visitUnannClassOrInterfaceType(ctx.unannClassOrInterfaceType());
//            write(">");
        }
        return null;
    }


    @Override
    public Void visitArrayCreationExpression(Java8Parser.ArrayCreationExpressionContext ctx) {
        if (ctx.primitiveType() != null) {
            String ceylonType;
            String type = ctx.primitiveType().getText();

  /*          switch (type) {
                case "int":
                    ceylonType = "IntArray";
                    break;
                case "short":
                    ceylonType = "ShortArray";
                    break;
                case "boolean":
                    ceylonType = "BooleanArray";
                    break;
                case "byte":
                    ceylonType = "ByteArray";
                    break;
                case "long":
                    ceylonType = "LongArray";
                    break;
                case "float":
                    ceylonType = "FloatArray";
                    break;
                case "double":
                    ceylonType = "DoubleArray";
                    break;
                case "char":
                    ceylonType = "CharArray";
                    break;
                default:
                    ceylonType = "ObjectArray<" + type + ">";
                    break;
            }*/
            write(type);
            if (ctx.arrayInitializer() != null) {
//                write(".with");
            }
        } else {
//            write("ObjectArray<");
            visitClassOrInterfaceType(ctx.classOrInterfaceType());
//            write(">");
//            if (ctx.arrayInitializer() != null) {
//                write(".with");
//            }
        }
        write("(");

        if(ctx.arrayInitializer() != null) {
            visitArrayInitializer(ctx.arrayInitializer());
        } else if (ctx.dimExprs() != null) {
            visitDimExprs(ctx.dimExprs());
        } else {
            visitDims(ctx.dims());
        }
        write(")");

        return null;
    }

    @Override
    public Void visitArrayInitializer(Java8Parser.ArrayInitializerContext ctx) {
        write("{");
        if(ctx.variableInitializerList() != null)
            visitVariableInitializerList(ctx.variableInitializerList());
        write("}");

        return null;
    }


    @Override
    public Void visitVariableInitializerList(Java8Parser.VariableInitializerListContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if(ctx.getChild(i) instanceof Java8Parser.VariableInitializerContext)
                visitVariableInitializer((Java8Parser.VariableInitializerContext) ctx.getChild(i));
            else
                write(", ");
        }

        return null;
    }

    @Override
    public Void visitLocalVariableDeclarationStatement(Java8Parser.LocalVariableDeclarationStatementContext ctx) {
        for (Java8Parser.VariableDeclaratorContext var : ctx.localVariableDeclaration().variableDeclaratorList().variableDeclarator()) {
            boolean shouldUseAssert = var.variableInitializer() != null && isCastOutsideOfInstanceof(ctx.localVariableDeclaration(), var);

            ScopeTree.Node n = scopeTree.getNode(var.variableDeclaratorId(), scopeTree.root);

            if (!shouldUseAssert && useValues && var.variableInitializer() != null && !n.optional) {
                write("value");
            } else {
                if (shouldUseAssert) {
                    write("assert(is ");
                } else if (n.variable && !hasModifier(ctx.localVariableDeclaration().variableModifier(), "final")) {
                    write("variable ");
                }
                // TODO int a[] should be converted to IntArray, but unfortunately at this point we can't know that it's an array
                // we should do some sort of lookahead :(
                visitUnannType(ctx.localVariableDeclaration().unannType());

                if(n.optional)
                    write("?");
            }
            write(" ");

            String identifier = var.variableDeclaratorId().Identifier().getText();

            write(escapeIdentifier(identifier, true));

            if (var.variableInitializer() != null) {
                write(" = ");
                visitVariableInitializer(var.variableInitializer());
            }
            if (shouldUseAssert) {
                write(")");
            }
            write(";\n");
        }

        return null;
    }


    @Override
    public Void visitLocalVariableDeclaration(Java8Parser.LocalVariableDeclarationContext ctx) {
        for (Java8Parser.VariableDeclaratorContext var : ctx.variableDeclaratorList().variableDeclarator()) {
            Java8Parser.VariableDeclaratorIdContext context = var.variableDeclaratorId();

            ScopeTree.Node n = scopeTree.getNode(context, scopeTree.root);

            if (useValues && var.variableInitializer() != null && !n.optional) {
                write("value");
            } else {
                if (n.variable && !hasModifier(ctx.variableModifier(), "final")) {
                    write("variable ");
                }
                visitUnannType(ctx.unannType());

                if(n.optional) {
                    write("?");
                }
            }
            write(" ");

            write(escapeIdentifier(context.Identifier().getText(), true));


            if (var.variableInitializer() != null) {
                write(" = ");
                visitVariableInitializer(var.variableInitializer());
            }
            write(";\n");
        }

        return null;
    }

    @Override
    public Void visitClassInstanceCreationExpression_lfno_primary(Java8Parser.ClassInstanceCreationExpression_lfno_primaryContext ctx) {
        boolean isObjectSatisfying = false;
        if (ctx.classBody() != null) {
            if (ctx.argumentList() == null) {
                isObjectSatisfying = true;
                write("object extends ");
            } else {
                write("object extends ");
            }
        }

        for (int i = 0; i < ctx.Identifier().size(); i++) {
            if (i > 0) {
                write(".");
            }
            write(ctx.Identifier().get(i).getText());
        }
        // TODO other identifiers
        if (ctx.typeArgumentsOrDiamond() != null) {
            visitTypeArgumentsOrDiamond(ctx.typeArgumentsOrDiamond());
        }
        if (!isObjectSatisfying) {
            write("(");
        }
        if (ctx.argumentList() != null) {
            visitArgumentList(ctx.argumentList());
        }
        if (!isObjectSatisfying) {
            write(")");
        }

        if (ctx.classBody() != null) {
            visitClassBody(ctx.classBody());
        }
        return null;
    }


    @Override
    public Void visitConditionalExpression(Java8Parser.ConditionalExpressionContext ctx) {
        if (isTernaryOperator(ctx)) {
            // ternary operator
            write("if (");
            visitConditionalOrExpression(ctx.conditionalOrExpression());
            write(") then ");
            visitExpression(ctx.expression());
            write(" else ");
            visitConditionalExpression(ctx.conditionalExpression());
        } else {
            super.visitConditionalExpression(ctx);
        }

        return null;
    }

    @Override
    public Void visitExpressionName(Java8Parser.ExpressionNameContext ctx) {
        if (ctx.ambiguousName() != null) {
            visitAmbiguousName(ctx.ambiguousName());
            write(".");
        }
        boolean shouldBeLc = ctx.getParent() instanceof Java8Parser.ArrayAccessContext
                || ctx.getParent() instanceof Java8Parser.ArrayAccess_lfno_primaryContext
                || ctx.getParent() instanceof Java8Parser.PostfixExpressionContext;

        write(escapeIdentifier(ctx.Identifier().getText(), shouldBeLc));

        return null;
    }


    @Override
    public Void visitAmbiguousName(Java8Parser.AmbiguousNameContext ctx) {
        if (ctx.ambiguousName() != null) {
            visitAmbiguousName(ctx.ambiguousName());
            write(".");
        }
        write(ctx.Identifier().getText());
        return null;
    }

    @Override
    public Void visitArgumentList(Java8Parser.ArgumentListContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (!(ctx.getChild(i) instanceof Java8Parser.ExpressionContext)) {
                continue;
            }
            if (i > 0) {
                write(", ");
            }
            visitExpression((Java8Parser.ExpressionContext) ctx.getChild(i));
        }
        return null;
    }


    @Override
    public Void visitIfThenStatement(Java8Parser.IfThenStatementContext ctx) {
        write("if (");
        visitExpression(ctx.expression());
        write(") ");

        if (!isBlock(ctx.statement())) {
            write("{\n");
        }
        visitStatement(ctx.statement());
        if (!isBlock(ctx.statement())) {
            write("}\n");
        }
        return null;
    }

    @Override
    public Void visitIfThenElseStatement(Java8Parser.IfThenElseStatementContext ctx) {
        write("if (");
        visitExpression(ctx.expression());
        write(") ");

        if (!isBlock(ctx.statementNoShortIf())) {
            write("{\n");
        }
        visitStatementNoShortIf(ctx.statementNoShortIf());
        if (!isBlock(ctx.statementNoShortIf())) {
            write("}\n");
        }

        write("else ");
        if (!isBlock(ctx.statement()) && !isIf(ctx.statement())) {
            write("{\n");
        }
        visitStatement(ctx.statement());
        if (!isBlock(ctx.statement()) && !isIf(ctx.statement())) {
            write("}\n");
        }
        return null;
    }


    @Override
    public Void visitBasicForStatement(Java8Parser.BasicForStatementContext ctx) {
        // TODO can we detect for(i = 0; i < n; i++) and transform it to for (i in 0..n) ??
        if (ctx.forInit() != null) {
            visitForInit(ctx.forInit());
        }
        write("while(");
        if (ctx.expression() != null) {
            visitExpression(ctx.expression());
        } else {
            write("true");
        }
        write(") ");
        if (!isBlock(ctx.statement())) {
            write("{\n");
        }
        visitStatement(ctx.statement());
        if (ctx.forUpdate() != null) {
            visitForUpdate(ctx.forUpdate());
        }
        write("}\n");
        return null;
    }

    @Override
    public Void visitEnhancedForStatement(Java8Parser.EnhancedForStatementContext ctx) {
        write("for (");
        if (!useValues) {
            visitUnannType(ctx.unannType());
            write(" ");
        }
        write(escapeIdentifier(ctx.variableDeclaratorId().getText(), true));
        write(" in ");
        visitExpression(ctx.expression());
        write(") ");

        if (!isBlock(ctx.statement())) {
            write("{\n");
        }
        visitStatement(ctx.statement());
        if (!isBlock(ctx.statement())) {
            write("}\n");
        }
        return null;
    }

    @Override
    public Void visitSwitchStatement(Java8Parser.SwitchStatementContext ctx) {
        write("switch (");
        visitExpression(ctx.expression());
        write(")\n");
        return visitSwitchBlock(ctx.switchBlock());
    }

    @Override
    public Void visitSwitchBlock(Java8Parser.SwitchBlockContext ctx) {
        boolean hasElse = false;
        for (Java8Parser.SwitchBlockStatementGroupContext group : ctx.switchBlockStatementGroup()) {
            // TODO transform `case a: case b:` to `case (a|b)`
            Java8Parser.SwitchLabelContext firstLabel = group.switchLabels().switchLabel(0);
            if (firstLabel.getChild(0).getText().equals("case")) {
                write("case (");
                boolean first = true;
                for (Java8Parser.SwitchLabelContext label : group.switchLabels().switchLabel()) {
                    if (first) {
                        first = false;
                    }
                    else {
                        write(" | ");
                    }
                    if (label.constantExpression() != null) {
                        visitConstantExpression(label.constantExpression());
                    } else {
                        visitEnumConstantName(label.enumConstantName());
                    }
                }
                write(") {\n");
            } else {
                hasElse = true;
                write("else {\n");
            }
            visitBlockStatements(group.blockStatements());
            write("}\n");
        }

        if (!hasElse) {
            write("else {}\n");
        }
        return null;
    }

    @Override
    public Void visitBlock(Java8Parser.BlockContext ctx) {
        write("{\n");
        super.visitBlock(ctx);
        if (!isBlockInDoWhile(ctx)) {
            write("}\n");
        }
        return null;
    }

    @Override
    public Void visitDoStatement(Java8Parser.DoStatementContext ctx) {
        write("while (true) ");
        if (!isBlock(ctx.statement())) {
            write("{\n");
        }
        visitStatement(ctx.statement());
        write("if (");
        visitExpression(ctx.expression());
        write(") {break;}\n");
        write("}\n");
        return null;
    }

    @Override
    public Void visitPrimaryNoNewArray(Java8Parser.PrimaryNoNewArrayContext ctx) {
        if(ctx.getChild(2) != null && ctx.getChild(2).getText().equals("class"))
            write("`" + ctx.getChild(0).getText() + "`");
        else
            switch (ctx.getChild(0).getText()) {
                case "this":
                    write("this");
                    break;
                case "(":
                    write("(");
                    visitExpression(ctx.expression());
                    write(")");
                    break;
                default:
                    super.visitPrimaryNoNewArray(ctx);
            }
        return null;
    }

    @Override
    public Void visitPrimaryNoNewArray_lfno_primary(Java8Parser.PrimaryNoNewArray_lfno_primaryContext ctx) {
        if(ctx.getChild(2) != null && ctx.getChild(2).getText().equals("class"))
            write("`" + ctx.getChild(0).getText() + "`");
        else
            switch (ctx.getChild(0).getText()) {
                case "this":
                    write("this");
                    break;
                case "(":
                    write("(");
                    visitExpression(ctx.expression());
                    write(")");
                    break;
                default:
                    super.visitPrimaryNoNewArray_lfno_primary(ctx);
            }
        return null;
    }

    @Override
    public Void visitConditionalOrExpression(Java8Parser.ConditionalOrExpressionContext ctx) {
        if (ctx.conditionalOrExpression() != null) {
            visitConditionalOrExpression(ctx.conditionalOrExpression());
            write(" || ");
        }
        return visitConditionalAndExpression(ctx.conditionalAndExpression());
    }

    @Override
    public Void visitConditionalAndExpression(Java8Parser.ConditionalAndExpressionContext ctx) {
        if (ctx.conditionalAndExpression() != null) {
            visitConditionalAndExpression(ctx.conditionalAndExpression());

            if (isInIfCondition(ctx)) {
                write(", ");
            } else {
                write(" && ");
            }
        }
        return visitInclusiveOrExpression(ctx.inclusiveOrExpression());
    }

    @Override
    public Void visitInclusiveOrExpression(Java8Parser.InclusiveOrExpressionContext ctx) {
        if (ctx.inclusiveOrExpression() != null) {
            visitInclusiveOrExpression(ctx.inclusiveOrExpression());
            write(" | ");
        }
        return visitExclusiveOrExpression(ctx.exclusiveOrExpression());
    }

    @Override
    public Void visitExclusiveOrExpression(Java8Parser.ExclusiveOrExpressionContext ctx) {
        if (ctx.exclusiveOrExpression() != null) {
            visitExclusiveOrExpression(ctx.exclusiveOrExpression());
            write(" ^ ");
        }
        return visitAndExpression(ctx.andExpression());
    }

    @Override
    public Void visitAndExpression(Java8Parser.AndExpressionContext ctx) {
        if (ctx.andExpression() != null) {
            visitAndExpression(ctx.andExpression());
            write(" & ");
        }
        return visitEqualityExpression(ctx.equalityExpression());
    }

    @Override
    public Void visitEqualityExpression(Java8Parser.EqualityExpressionContext ctx) {
        if (ctx.equalityExpression() != null) {
            String operator = ctx.getChild(1).getText();
            if (ctx.relationalExpression().getText().equals("null")) {
                if (operator.equals("==")) {
                    write("!");
                }
                if (ctx.equalityExpression().getText().matches("\\w+") && isInIfCondition(ctx)) {
                    write("exists ");
                    visitEqualityExpression(ctx.equalityExpression());
                }
                else {
                    visitEqualityExpression(ctx.equalityExpression());
                    write(" exists");
                }
                return null;
            } else {
                visitEqualityExpression(ctx.equalityExpression());
                write(" " + operator + " ");
            }
        }

        return visitRelationalExpression(ctx.relationalExpression());
    }

    @Override
    public Void visitRelationalExpression(Java8Parser.RelationalExpressionContext ctx) {
        if (ctx.relationalExpression() != null) {
            String operator = ctx.getChild(1).getText();
            if (operator.equals("instanceof")) {
                if (ctx.relationalExpression().getText().matches("\\w+") && isInIfCondition(ctx)
                        && !isExpression(ctx)) {
                    write("instanceof ");
                    visitReferenceType(ctx.referenceType());
                    write(" ");
                    visitRelationalExpression(ctx.relationalExpression());
                }
                else {
                    visitRelationalExpression(ctx.relationalExpression());
                    write(" instanceof ");
                    visitReferenceType(ctx.referenceType());
                }
                return null;
            }

            visitRelationalExpression(ctx.relationalExpression());
            write(" ");
            write(operator.replace("instanceof", "instanceof"));
            write(" ");
        }

        if (ctx.shiftExpression() != null) {
            visitShiftExpression(ctx.shiftExpression());
        } else {
            visitReferenceType(ctx.referenceType());
        }
        return null;
    }

    @Override
    public Void visitShiftExpression(Java8Parser.ShiftExpressionContext ctx) {
        if (ctx.shiftExpression() != null) {
            visitShiftExpression(ctx.shiftExpression());
            write(" << ");
        }
        return visitAdditiveExpression(ctx.additiveExpression());
    }

    @Override
    public Void visitAdditiveExpression(Java8Parser.AdditiveExpressionContext ctx) {
        if (ctx.additiveExpression() != null) {
            visitAdditiveExpression(ctx.additiveExpression());
            write(" " + ctx.getChild(1).getText() + " ");
        }
        return visitMultiplicativeExpression(ctx.multiplicativeExpression());
    }

    @Override
    public Void visitMultiplicativeExpression(Java8Parser.MultiplicativeExpressionContext ctx) {
        if (ctx.multiplicativeExpression() != null) {
            visitMultiplicativeExpression(ctx.multiplicativeExpression());
            write(" " + ctx.getChild(1).getText() + " ");
        }
        return visitUnaryExpression(ctx.unaryExpression());
    }

    @Override
    public Void visitUnaryExpression(Java8Parser.UnaryExpressionContext ctx) {
        String op = ctx.getChild(0).getText();
        if (op.equals("+") || op.equals("-")) {
            write(op);
            return visitUnaryExpression(ctx.unaryExpression());
        } else {
            return super.visitUnaryExpression(ctx);
        }
    }

    @Override
    public Void visitPreIncrementExpression(Java8Parser.PreIncrementExpressionContext ctx) {
        write("++");
        return super.visitPreIncrementExpression(ctx);
    }

    @Override
    public Void visitPreDecrementExpression(Java8Parser.PreDecrementExpressionContext ctx) {
        write("--");
        return super.visitPreDecrementExpression(ctx);
    }

    @Override
    public Void visitPostIncrementExpression(Java8Parser.PostIncrementExpressionContext ctx) {
        super.visitPostIncrementExpression(ctx);
        write("++");
        return null;
    }

    @Override
    public Void visitPostIncrementExpression_lf_postfixExpression(Java8Parser.PostIncrementExpression_lf_postfixExpressionContext ctx) {
        super.visitPostIncrementExpression_lf_postfixExpression(ctx);
        write("++");
        return null;
    }

    @Override
    public Void visitPostDecrementExpression(Java8Parser.PostDecrementExpressionContext ctx) {
        super.visitPostDecrementExpression(ctx);
        write("--");
        return null;
    }

    @Override
    public Void visitPostDecrementExpression_lf_postfixExpression(
            Java8Parser.PostDecrementExpression_lf_postfixExpressionContext ctx) {
        super.visitPostDecrementExpression_lf_postfixExpression(ctx);
        write("--");
        return null;
    }

    @Override
    public Void visitUnaryExpressionNotPlusMinus(Java8Parser.UnaryExpressionNotPlusMinusContext ctx) {
        if (ctx.getChild(0).getText().equals("!")) {
            write("!");
        }
        return super.visitUnaryExpressionNotPlusMinus(ctx);
    }

    @Override
    public Void visitCastExpression(Java8Parser.CastExpressionContext ctx) {
        if (ctx.unaryExpression() != null) {
            visitUnaryExpression(ctx.unaryExpression());
        }
        if (ctx.unaryExpressionNotPlusMinus() != null) {
            visitUnaryExpressionNotPlusMinus(ctx.unaryExpressionNotPlusMinus());
        }
        if (ctx.lambdaExpression() != null) {
            visitLambdaExpression(ctx.lambdaExpression());
        }
        return null;
    }


    @Override
    public Void visitLambdaExpression(Java8Parser.LambdaExpressionContext ctx) {
        visitLambdaParameters(ctx.lambdaParameters());
        if (ctx.lambdaBody().block() == null) {
            write(" => ");
        }
        visitLambdaBody(ctx.lambdaBody());
        return null;
    }

    @Override
    public Void visitLambdaParameters(Java8Parser.LambdaParametersContext ctx) {
        write("(");
        if (ctx.Identifier() != null) {
            write(ctx.Identifier().getText());
        }
        super.visitLambdaParameters(ctx);
        write(")");
        return null;
    }

    @Override
    public Void visitInferredFormalParameterList(Java8Parser.InferredFormalParameterListContext ctx) {
        boolean isFirst = true;
        for (TerminalNode i : ctx.Identifier()) {
            if (!isFirst) {
                write(", ");
            }

            write(i.getText());
            isFirst = false;
        }

        return null;
    }


    @Override
    public Void visitEnumDeclaration(Java8Parser.EnumDeclarationContext ctx) {
        if (hasModifier(ctx.classModifier(), "public")) {
            write("public ");
        }
        if (hasModifier(ctx.classModifier(), "static")) {
            write("static ");
        }
        write("class ");
        write(ctx.Identifier().getText());
        if (ctx.superinterfaces() != null) {
            visitSuperinterfaces(ctx.superinterfaces());
        }
        return visitEnumBody(ctx.enumBody());
    }

    @Override
    public Void visitEnumBody(Java8Parser.EnumBodyContext ctx) {
        write(" {\n");
//        write("shared actual String string;\n");
        if (ctx.enumBodyDeclarations() != null) {
            for (ClassBodyDeclarationContext classBody : ctx
                    .enumBodyDeclarations().classBodyDeclaration()) {
                if (classBody.constructorDeclaration() != null) {
                    // Special case, we need to add an extra "String string"
                    // parameter
                    visitEnumConstructorDeclaration(
                            classBody.constructorDeclaration());
                } else {
                    visitClassBodyDeclaration(classBody);
                }
            }
        }
        if (ctx.enumConstantList() != null) {
            visitEnumConstantList(ctx.enumConstantList());
        }
        write("}\n");
        return null;
    }


    private void visitEnumConstructorDeclaration(
            Java8Parser.ConstructorDeclarationContext ctx) {
        write("abstract new \\i");
        write(ctx.constructorDeclarator().simpleTypeName().getText());
        write("(String string, ");
        if (ctx.constructorDeclarator().formalParameterList() != null) {
            visitFormalParameterList(
                    ctx.constructorDeclarator().formalParameterList());
        }
        write(")");

        write(" {\n");
        write("this.string = string;\n");
        super.visitConstructorBody(ctx.constructorBody());
        write("}\n\n");
    }


    @Override
    public Void visitEnumConstant(Java8Parser.EnumConstantContext ctx) {
        write("public new \\i");
        write(ctx.Identifier().getText());
        if (ctx.argumentList() == null) {
            write(" { string = \"");
            write(ctx.Identifier().getText());
            write("\"; }\n");
        } else {
            write(" extends \\i");
            write(((Java8Parser.EnumDeclarationContext) ctx.getParent().getParent()
                    .getParent()).Identifier().getText());
            write("(\"");
            write(ctx.Identifier().getText());
            write("\", ");
            visitArgumentList(ctx.argumentList());
            write(") { }\n");
        }
        return null;
    }

    @Override
    public Void visitFieldDeclaration(Java8Parser.FieldDeclarationContext ctx) {
        for (Java8Parser.VariableDeclaratorContext var : ctx.variableDeclaratorList()
                .variableDeclarator()) {
            Java8Parser.VariableDeclaratorIdContext context = var.variableDeclaratorId();

            ScopeTree.Node n = scopeTree.getNode(context, scopeTree.root);

            if (hasModifier(ctx.fieldModifier(), "public")) {
                write("public ");
            }
            if (hasModifier(ctx.fieldModifier(), "static")) {
                write("static ");
            }
            if (useValues && var.variableInitializer() != null
                    && !hasModifier(ctx.fieldModifier(), "public")
                    && !hasModifier(ctx.fieldModifier(), "protected")
                    && !n.optional) {
                write("value");
            } else {
                if (n.variable && !hasModifier(ctx.fieldModifier(), "final")) {
                    write("variable ");
                }
                visitUnannType(ctx.unannType());

                if(n.optional)
                    write("?");
            }
            write(" ");

            write(escapeIdentifier(var.variableDeclaratorId().getText(), true));

            if (var.variableInitializer() != null) {
                write(" = ");
                visitVariableInitializer(var.variableInitializer());
            }
            write(";\n");
        }

        return null;
    }


    @Override
    public Void visitFieldAccess(Java8Parser.FieldAccessContext ctx) {
        visitPrimary(ctx.primary());
        write(".");
        write(escapeIdentifier(ctx.Identifier().getText(), true));
        return null;
    }

    @Override
    public Void visitFieldAccess_lf_primary(Java8Parser.FieldAccess_lf_primaryContext ctx) {
        write(".");
        write(escapeIdentifier(ctx.Identifier().getText(), true));
        return null;
    }


    @Override
    public Void visitTryStatement(Java8Parser.TryStatementContext ctx) {
        if (ctx.tryWithResourcesStatement() == null) {
            write("try ");
        }
        return super.visitTryStatement(ctx);
    }

    @Override
    public Void visitCatchClause(Java8Parser.CatchClauseContext ctx) {
        write("catch (");
        visitCatchFormalParameter(ctx.catchFormalParameter());
        write(") ");
        return visitBlock(ctx.block());
    }

    @Override
    public Void visitTryWithResourcesStatement(
            Java8Parser.TryWithResourcesStatementContext ctx) {
        write("try ");
        return super.visitTryWithResourcesStatement(ctx);
    }

    @Override
    public Void visitResourceSpecification(Java8Parser.ResourceSpecificationContext ctx) {
        write("(");
        visitResourceList(ctx.resourceList());
        write(") ");
        return null;
    }

    @Override
    public Void visitUnannClassType(Java8Parser.UnannClassTypeContext ctx) {
        if (ctx.unannClassOrInterfaceType() != null) {
            visitUnannClassOrInterfaceType(ctx.unannClassOrInterfaceType());
            write(".");
            write(ctx.Identifier().getText());
        } else {
            write(ctx.Identifier().getText());
        }
        if (ctx.typeArguments() != null) {
            visitTypeArguments(ctx.typeArguments());
        }
        return null;
    }

    @Override
    public Void visitResourceList(Java8Parser.ResourceListContext ctx) {
        int i = 0;
        for (Java8Parser.ResourceContext resource : ctx.resource()) {
            if (i > 0) {
                write("; ");
            }
            visitResource(resource);
            i++;
        }
        return null;
    }

    @Override
    public Void visitResource(Java8Parser.ResourceContext ctx) {
        visitUnannType(ctx.unannType());
        write(" ");
        visitVariableDeclaratorId(ctx.variableDeclaratorId());
        write(" = ");
        return visitExpression(ctx.expression());
    }

    @Override
    public Void visitCatchType(Java8Parser.CatchTypeContext ctx) {
        visitUnannClassType(ctx.unannClassType());

        for (ClassTypeContext ct : ctx.classType()) {
            write(" | ");
            visitClassType(ct);
        }
        write(" ");
        return null;
    }


    @Override
    public Void visitVariableDeclaratorId(Java8Parser.VariableDeclaratorIdContext ctx) {
        write(escapeIdentifier(ctx.Identifier().getText(), true));
        return null;
    }

    @Override
    public Void visitFinally_(Java8Parser.Finally_Context ctx) {
        write("finally ");
        return super.visitFinally_(ctx);
    }

    @Override
    public Void visitWhileStatement(Java8Parser.WhileStatementContext ctx) {
        write("while (");
        visitExpression(ctx.expression());
        write(") ");
        if (ctx.statement().statementWithoutTrailingSubstatement() != null
                && ctx.statement().statementWithoutTrailingSubstatement()
                .block() != null) {
            visitStatement(ctx.statement());
        } else {
            write("{\n");
            visitStatement(ctx.statement());
            write("}\n");
        }
        return null;
    }


    @Override
    public Void visitWhileStatementNoShortIf(
            Java8Parser.WhileStatementNoShortIfContext ctx) {
        write("while (");
        visitExpression(ctx.expression());
        write(") ");
        if (ctx.statementNoShortIf()
                .statementWithoutTrailingSubstatement() != null
                && ctx.statementNoShortIf()
                .statementWithoutTrailingSubstatement()
                .block() != null) {
            visitStatementNoShortIf(ctx.statementNoShortIf());
        } else {
            write("{\n");
            visitStatementNoShortIf(ctx.statementNoShortIf());
            write("}\n");
        }
        return null;
    }


    @Override
    public Void visitBreakStatement(Java8Parser.BreakStatementContext ctx) {
        if (parent(ctx, 6) instanceof Java8Parser.SwitchBlockContext) {
            return null;
        }
        write("break;\n");
        return null;
    }


    @Override
    public Void visitContinueStatement(Java8Parser.ContinueStatementContext ctx) {
        write("continue;\n");
        return null;
    }


    @Override
    public Void visitStatementExpressionList(
            Java8Parser.StatementExpressionListContext ctx) {
        super.visitStatementExpressionList(ctx);
        write(";\n");
        return null;
    }

    @Override
    public Void visitAssertStatement(Java8Parser.AssertStatementContext ctx) {
        if (ctx.expression().size() > 1) {
            if (!ctx.expression(1).getText().matches("\"[^\"]*\"")) {
                write("// ");
            }
            visitExpression(ctx.expression(1));
            write("\n");
        }
        write("assert(");
        visitExpression(ctx.expression(0));
        write(");\n");

        return null;
    }


    private boolean isBlock(Java8Parser.StatementContext ctx) {
        return ctx.statementWithoutTrailingSubstatement() != null
                && ctx.statementWithoutTrailingSubstatement().block() != null;
    }

    private boolean isIf(Java8Parser.StatementContext ctx) {
        return ctx.ifThenStatement() != null
                || ctx.ifThenElseStatement() != null;
    }

    private boolean isBlock(Java8Parser.StatementNoShortIfContext ctx) {
        return ctx.statementWithoutTrailingSubstatement() != null
                && ctx.statementWithoutTrailingSubstatement().block() != null;
    }

    private boolean isBlockInDoWhile(Java8Parser.BlockContext block) {
        return block
                .getParent() instanceof Java8Parser.StatementWithoutTrailingSubstatementContext
                && block.getParent().getParent() instanceof Java8Parser.StatementContext
                && (block.getParent().getParent()
                .getParent() instanceof Java8Parser.DoStatementContext
                || block.getParent().getParent()
                .getParent() instanceof Java8Parser.BasicForStatementContext);
    }

    private boolean isExpression(Java8Parser.RelationalExpressionContext ctx) {
        for (int i = 1; i <= 8; i++) {
            if (parent(ctx, i).getChildCount() > 1) {
                return true;
            }
        }

        return false;
    }

    private boolean isInIfCondition(Java8Parser.ConditionalAndExpressionContext ctx) {
        while (ctx.getParent() instanceof Java8Parser.ConditionalAndExpressionContext) {
            ctx = (Java8Parser.ConditionalAndExpressionContext) ctx.getParent();
        }
        return parent(ctx, 1) instanceof Java8Parser.ConditionalOrExpressionContext
                && parent(ctx, 2) instanceof Java8Parser.ConditionalExpressionContext
                && isInIfCondition(
                (Java8Parser.ConditionalExpressionContext) parent(ctx, 2));

    }

    private boolean isInIfCondition(Java8Parser.ConditionalExpressionContext ctx) {
        if (parent(ctx, 1) instanceof Java8Parser.AssignmentExpressionContext
                && parent(ctx, 2) instanceof Java8Parser.ExpressionContext) {

            ParserRuleContext candidate = parent(ctx, 3);
            return candidate instanceof Java8Parser.IfThenElseStatementContext
                    || candidate instanceof Java8Parser.IfThenStatementContext
                    || candidate instanceof Java8Parser.IfThenElseStatementNoShortIfContext;
        }
        return false;
    }


    private boolean isInIfCondition(Java8Parser.RelationalExpressionContext ctx) {
        if (ctx.getParent() instanceof Java8Parser.EqualityExpressionContext
                && parent(ctx, 2) instanceof Java8Parser.AndExpressionContext
                && parent(ctx, 3) instanceof Java8Parser.ExclusiveOrExpressionContext
                && parent(ctx, 4) instanceof Java8Parser.InclusiveOrExpressionContext
                && parent(ctx, 5) instanceof Java8Parser.ConditionalAndExpressionContext
                && parent(ctx, 6) instanceof Java8Parser.ConditionalOrExpressionContext
                && parent(ctx, 7) instanceof Java8Parser.ConditionalExpressionContext) {

            Java8Parser.ConditionalExpressionContext condExpr = (Java8Parser.ConditionalExpressionContext) parent(
                    ctx, 7);

            if (isTernaryOperator(condExpr)) {
                return true;
            } else {
                return isInIfCondition(condExpr);
            }
        }
        return false;
    }

    private boolean isInIfCondition(Java8Parser.EqualityExpressionContext ctx) {
        if (ctx.getParent() instanceof Java8Parser.AndExpressionContext
                && parent(ctx, 2) instanceof Java8Parser.ExclusiveOrExpressionContext
                && parent(ctx, 3) instanceof Java8Parser.InclusiveOrExpressionContext
                && parent(ctx, 4) instanceof Java8Parser.ConditionalAndExpressionContext
                && parent(ctx, 5) instanceof Java8Parser.ConditionalOrExpressionContext
                && parent(ctx, 6) instanceof Java8Parser.ConditionalExpressionContext) {

            Java8Parser.ConditionalExpressionContext condExpr = (Java8Parser.ConditionalExpressionContext) parent(
                    ctx, 6);

            if (isTernaryOperator(condExpr)) {
                return true;
            } else {
                return isInIfCondition(condExpr);
            }
        }
        return false;
    }

    private boolean isTernaryOperator(Java8Parser.ConditionalExpressionContext ctx) {
        return ctx.getChildCount() > 1;
    }


    private ParserRuleContext parent(ParserRuleContext ctx, int level) {
        for (int i = 0; i < level; i++) {
            if (ctx != null) {
                ctx = ctx.getParent();
            }
        }

        return ctx;
    }

    private boolean isCastOutsideOfInstanceof(
            Java8Parser.LocalVariableDeclarationContext ctx,
            Java8Parser.VariableDeclaratorContext var) {
        // checks if this involves a cast
        if (getInnerChild(var.variableInitializer(), Java8Parser.ExpressionContext.class,
                Java8Parser.AssignmentExpressionContext.class,
                Java8Parser.ConditionalExpressionContext.class,
                Java8Parser.ConditionalOrExpressionContext.class,
                Java8Parser.ConditionalAndExpressionContext.class,
                Java8Parser.InclusiveOrExpressionContext.class,
                Java8Parser.ExclusiveOrExpressionContext.class, Java8Parser.AndExpressionContext.class,
                Java8Parser.EqualityExpressionContext.class,
                Java8Parser.RelationalExpressionContext.class, Java8Parser.ShiftExpressionContext.class,
                Java8Parser.AdditiveExpressionContext.class,
                Java8Parser.MultiplicativeExpressionContext.class,
                Java8Parser.UnaryExpressionContext.class,
                Java8Parser.UnaryExpressionNotPlusMinusContext.class,
                Java8Parser.CastExpressionContext.class) == null) {
            return false;
        }
        // checks if the variable declaration is located inside an if
        if (hasParents(ctx, Java8Parser.LocalVariableDeclarationStatementContext.class,
                Java8Parser.BlockStatementContext.class, Java8Parser.BlockStatementsContext.class,
                Java8Parser.BlockContext.class,
                Java8Parser.StatementWithoutTrailingSubstatementContext.class)) {

            Java8Parser.StatementWithoutTrailingSubstatementContext st = (Java8Parser.StatementWithoutTrailingSubstatementContext) parent(
                    ctx, 5);

            if (hasParents(st, Java8Parser.StatementNoShortIfContext.class,
                    Java8Parser.IfThenElseStatementContext.class)) {
                // checks if the condition involves an instanceof
                return !isInstanceofCondition(
                        ((Java8Parser.IfThenElseStatementContext) st.getParent()
                                .getParent()).expression(),
                        var.variableDeclaratorId().getText());
            } else if (hasParents(st, Java8Parser.StatementContext.class,
                    Java8Parser.IfThenStatementContext.class)) {
                // checks if the condition involves an instanceof
                return !isInstanceofCondition(
                        ((Java8Parser.IfThenStatementContext) st.getParent().getParent())
                                .expression(),
                        ""/* TODO extract casted identifier */);
            }
        }
        return true;
    }

    private boolean isInstanceofCondition(Java8Parser.ExpressionContext expr,
                                          String identifier) {
        Java8Parser.RelationalExpressionContext child = (Java8Parser.RelationalExpressionContext) getInnerChild(
                expr, Java8Parser.AssignmentExpressionContext.class,
                Java8Parser.ConditionalExpressionContext.class,
                Java8Parser.ConditionalOrExpressionContext.class,
                Java8Parser.ConditionalAndExpressionContext.class,
                Java8Parser.InclusiveOrExpressionContext.class,
                Java8Parser.ExclusiveOrExpressionContext.class, Java8Parser.AndExpressionContext.class,
                Java8Parser.EqualityExpressionContext.class,
                Java8Parser.RelationalExpressionContext.class);

        if (child == null) {
            return false;
        }

        if (child.getChildCount() > 1
                && child.getChild(1).getText().equals("instanceof")) {
            return true; // TODO compare identifiers
        }

        return false;
    }

    private boolean hasParents(ParserRuleContext ctx, Class<?>... parents) {
        if (parents != null) {
            ParserRuleContext parent = ctx;

            for (Class<?> p : parents) {
                parent = parent.getParent();
                if (!p.isAssignableFrom(parent.getClass())) {
                    return false;
                }
            }
        }

        return true;
    }

    private ParseTree getInnerChild(ParserRuleContext ctx,
                                    Class<?>... children) {
        if (children != null) {
            ParseTree rule = ctx;

            for (Class<?> p : children) {
                boolean foundChild = false;
                for (int i = 0; i < rule.getChildCount(); i++) {
                    ParseTree child = rule.getChild(i);

                    if (p.isAssignableFrom(child.getClass())) {
                        rule = child;
                        foundChild = true;
                        break;
                    }
                }

                if (!foundChild) {
                    return null;
                }
            }

            return rule;
        }

        return null;
    }

    private String escapeIdentifier(String identifier,
                                    boolean shouldBeLowercase) {

        if (RESERVED_KEYWORDS.contains(identifier)) {
            return "\\i" + identifier;
        } else if(is_CONSTANT_CASE(identifier.toCharArray())) {
            return constant_case_toCamelCase(identifier.toCharArray());
        } else if (shouldBeLowercase && identifier.charAt(0) != '_'
                && !Character.isLowerCase(identifier.charAt(0))) {
            return "\\i" + identifier;
        }

        return identifier;
    }

    private static String constant_case_toCamelCase(char[] newName) {
        int j = 0;
        boolean capitaliseNext = false;
        for(int i=0;i<newName.length;i++){
            char codepoint = newName[i];
            if(codepoint == '_'){
                // skip underscore
                capitaliseNext = true;
            }else if(capitaliseNext){
                newName[j++] = codepoint;
                capitaliseNext = false;
            }else{
                newName[j++] = Character.toLowerCase(codepoint);
            }
        }
        return new String(newName, 0, j);
    }

    private static boolean is_CONSTANT_CASE(char[] newName) {
        // reject empty, "U" and "_"
        if(newName.length <= 1)
            return false;
        for(int i=0;i<newName.length;i++){
            int codepoint = newName[i];
            if(Character.isLowerCase(codepoint) && codepoint != '_')
                return false;
        }
        return true;
    }




















}
