package lox;

/*
public class AstPrinter implements Expr.Visitor<String> {

    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitUnary(Expr.Unary expr) {
        return format(expr.op.lexeme, expr.right);
    }

    @Override
    public String visitBinary(Expr.Binary expr) {
        return format(expr.op.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroup(Expr.Group expr) {
        return format("group", expr.expr);
    }

    @Override
    public String visitLiteral(Expr.Literal expr) {
        if (expr.val == null) return "nil";
        return expr.val.toString();
    }

    // StringBuilder is faster than concat with "+" if in loop
    // the "+" operator generates a StringBuilder at each loop iteration, so slow
    public String format(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    public static void main(String[] args) {
        /*Expr expr = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS,"-",null,1),
                        new Expr.Literal(123.45)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Group(
                        new Expr.Binary(
                                new Expr.Literal(1),
                                new Token(TokenType.PLUS,"+",null,1),
                                new Expr.Binary(
                                        new Expr.Literal(1),
                                        new Token(TokenType.STAR,"/",null,1),
                                        new Expr.Literal(null)
                                )
                        )

                )
        );*/
       /* Expr expr = new Expr.Binary(
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
        System.out.println(new AstPrinter().print(expr));
    }
}*/