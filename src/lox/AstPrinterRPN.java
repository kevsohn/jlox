package lox;

public class AstPrinterRPN extends AstPrinter {
    // (1 + 2) * (4 - 3) -> 1 2 + 4 3 - *
    // really simple once already parsed with AST since
    // the operator precedence is baked into the input construction
    // just change the operator print to be last
    @Override
    public String format(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        for (Expr expr : exprs) {
            builder.append(expr.accept(this));
            builder.append(" ");
        }
        if (!name.equals("group"))
            builder.append(name);
        return builder.toString();
    }

    public static void main(String[] args) {
        /*System.out.println("(1 + 2) * (4 - 3)");
        Expr expr = new Expr.Binary(
                new Expr.Group(
                        new Expr.Binary(
                                new Expr.Literal(1),
                                new Token(TokenType.PLUS,"+",null,1),
                                new Expr.Literal(2))),
                new Token(TokenType.STAR,"*",null,1),
                new Expr.Group(
                        new Expr.Binary(
                                new Expr.Literal(4),
                                new Token(TokenType.MINUS,"-",null,1),
                                new Expr.Literal(3)))
        );*/
        // RPN cant do unary well?
        System.out.println("(1 - 2) == -1");
        Expr expr = new Expr.Binary(
                new Expr.Group(
                        new Expr.Binary(
                                new Expr.Literal(1),
                                new Token(TokenType.MINUS,"-",null,1),
                                new Expr.Literal(2))),
                new Token(TokenType.EQ_EQ,"==",null,1),
                new Expr.Unary(
                        new Token(TokenType.MINUS,"-",null,1),
                        new Expr.Literal(1))
        );
        System.out.println(new AstPrinterRPN().print(expr));
    }
}
