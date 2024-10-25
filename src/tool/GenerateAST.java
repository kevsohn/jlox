package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Arrays;

public class GenerateAST {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: generateAST <output dir>");
            System.exit(1);
        }
        String outputDir = args[0];

        defineAST(outputDir, "Expr", Arrays.asList(
                "Assign : Token name, Expr value",
                "Binary : Expr left, Token op, Expr right",
                "Unary : Token op, Expr right",
                "Group : Expr expr",
                "Literal : Object val",
                "Variable : Token name"));

        defineAST(outputDir, "Stmt", Arrays.asList(
                "If : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Block : List<Stmt> statements",
                "Print : Expr expr",
                "Expression : Expr expr",
                "Var : Token name, Expr initializer"));
    }

    private static void defineAST(String dir,
                                  String baseClassName,
                                  List<String> subClasses) throws IOException {
        PrintWriter writer = new PrintWriter(dir+"/"+baseClassName+".java", "UTF-8");

        writer.println("package lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        /*writer.println("// expr -> unary | binary | group | literal");
        writer.println("// unary -> (\"-\" | \"!\") expr");
        writer.println("// binary -> expr operator expr");
        writer.println("// group -> \"(\" expr \")\"");
        writer.println("// operator -> \"+\", \"-\", \"*\", \"/\", " +
                "\"<\", \">\", \"!\", \"==\" | \"!=\" | \">=\" | \"<=\"  " +
                "(only arithmetic & logic for now)");
        writer.println("// literal -> NUMBER | STRING | \"nil\" | \"true\" | \"false\"");*/
        writer.println("abstract class "+ baseClassName +" {");
        // Visitor design pattern
        printVisitor(writer,baseClassName,subClasses);
        writer.println();
        writer.println("    abstract <R> R accept("+baseClassName+".Visitor<R> v);");
        writer.println();
        // Receivers
        for (String subClass: subClasses) {
            String subClassName = subClass.split(":")[0].trim();
            String fields = subClass.split(":")[1].trim();
            printReceiver(writer, baseClassName, subClassName, fields);
            writer.println();
        }
        writer.println("}");
        writer.close();
    }

    static void printVisitor(PrintWriter w, String baseClassName, List<String> subClasses) {
        w.println("    interface Visitor<R> {");
        for (String subClass: subClasses) {
            String subClassName = subClass.split(":")[0].trim();
            w.println("        R visit"+subClassName+baseClassName+"("+baseClassName+"."+subClassName+" "+baseClassName.toLowerCase()+");");
        }
        w.println("    }");
    }

    static void printReceiver(PrintWriter w, String baseClassName, String subClassName, String fields) {
        String[] fieldList = fields.split(", ");
        w.println("    static class "+subClassName+" extends "+baseClassName+" {");
        for (String field: fieldList)
            w.println("        final "+field+";");
        w.println();
        w.println("        "+subClassName+"("+fields+") {");
        for (String field: fieldList) {
            String varName = field.split(" ")[1].trim();
            w.println("            this."+varName+ " = "+varName+";");
        }
        w.println("        }");
        w.println();
        w.println("        @Override");
        w.println("        <R> R accept("+baseClassName+".Visitor<R> v) {");
        w.println("            return v.visit"+subClassName+baseClassName+"(this);");
        w.println("        }");
        w.println("    }");
    }
}//EOC
