package lox;

abstract class Expr {
    interface Visitor<R> {
        R visitBinary(Expr.Binary expr);
        R visitUnary(Expr.Unary expr);
        R visitGroup(Expr.Group expr);
        R visitLiteral(Expr.Literal expr);
        R visitVariable(Expr.Variable expr);
    }

    abstract <R> R accept(Expr.Visitor<R> v);

    static class Binary extends Expr {
        final Expr left;
        final Token op;
        final Expr right;

        Binary(Expr left, Token op, Expr right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitBinary(this);
        }
    }

    static class Unary extends Expr {
        final Token op;
        final Expr right;

        Unary(Token op, Expr right) {
            this.op = op;
            this.right = right;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitUnary(this);
        }
    }

    static class Group extends Expr {
        final Expr expr;

        Group(Expr expr) {
            this.expr = expr;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitGroup(this);
        }
    }

    static class Literal extends Expr {
        final Object val;

        Literal(Object val) {
            this.val = val;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitLiteral(this);
        }
    }

    static class Variable extends Expr {
        final Token name;

        Variable(Token name) {
            this.name = name;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitVariable(this);
        }
    }

}
