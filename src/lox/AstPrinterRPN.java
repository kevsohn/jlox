package lox;

public class AstPrinterRPN implements Expr.Visitor<String> {
    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitUnary(Expr.Unary expr) {
        return RPN(expr.op.lexeme, expr.right);
    }

    @Override
    public String visitBinary(Expr.Binary expr) {
        return RPN(expr.op.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroup(Expr.Group expr) {
        return RPN("group", expr.expr);
    }

    @Override
    public String visitLiteral(Expr.Literal expr) {
        if (expr.val == null) return "nil";
        return expr.val.toString();
    }

    // (1 + 2) * (4 - 3) -> 1 2 + 4 3 - *
    // really simple once already parsed with AST since
    // the operator precedence is baked into the input construction
    // just change the operator print to be last
    private String RPN(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        for (Expr expr : exprs) {
            builder.append(expr.accept(this));
            //builder.append(" ");
        }
        if (!name.equals("group"))
            builder.append(name);
        return builder.toString();
    }

    public static void main(String[] args) {
        Expr expr = new Expr.Binary(
                new Expr.Group(
                        new Expr.Binary(
                                new Expr.Literal(1),
                                new Token(TokenType.PLUS,"+",null,1),
                                new Expr.Literal(2))
                ),
                new Token(TokenType.STAR,"*",null,1),
                new Expr.Group(
                        new Expr.Binary(
                                new Expr.Literal(4),
                                new Token(TokenType.MINUS,"-",null,1),
                                new Expr.Literal(3)))
        );
        System.out.println(new AstPrinterRPN().print(expr));
    }
}
