package tool;

import lox.Token;

// reference class for automatic AST generator
abstract class Expr {
    // receiver class for Visitor pattern
    abstract void accept(ExprVisitor visitor);

    //------expressions--------
    // binary : (expr, op, expr)
    // unary : (op, expr)
    // group : ("(",expr,")")
    // literal : String, Num, Bool, Nil
    static class Binary extends Expr {
        private final Expr left;
        private final Token op;
        private final Expr right;

        Binary(Expr left, Token op, Expr right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        void accept(ExprVisitor v) {
            v.visitBinary(this);
        }
    }

    static class Unary extends Expr {
        private final Token op;
        private final Expr right;

        Unary(Token op, Expr right) {
            this.op = op;
            this.right = right;
        }

        @Override
        void accept(ExprVisitor v) {
            v.visitUnary(this);
        }
    }
}//EOC
