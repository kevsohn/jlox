package tool;

import lox.Token;

// reference class for automatic AST generator
abstract class Expr {
    // expression subset
    // binary : (expr, op, expr)
    // unary : (op, expr)
    // literal : String, Num, Bool, Nil
    // group : ("(",expr,")")

    static class Binary extends Expr {
        private final Expr left;
        private final Token op;
        private final Expr right;

        Binary(Expr left, Token op, Expr right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }
    }
}//EOC
