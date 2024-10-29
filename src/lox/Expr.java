package lox;

import java.util.List;

abstract class Expr {
    interface Visitor<R> {
        R visitAssignExpr(Expr.Assign expr);
        R visitLogicalExpr(Expr.Logical expr);
        R visitBinaryExpr(Expr.Binary expr);
        R visitUnaryExpr(Expr.Unary expr);
        R visitCallExpr(Expr.Call expr);
        R visitGroupExpr(Expr.Group expr);
        R visitLiteralExpr(Expr.Literal expr);
        R visitVariableExpr(Expr.Variable expr);
    }

    abstract <R> R accept(Expr.Visitor<R> v);

    static class Assign extends Expr {
        final Token name;
        final Expr value;

        Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitAssignExpr(this);
        }
    }

    static class Logical extends Expr {
        final Expr left;
        final Token op;
        final Expr right;

        Logical(Expr left, Token op, Expr right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitLogicalExpr(this);
        }
    }

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
            return v.visitBinaryExpr(this);
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
            return v.visitUnaryExpr(this);
        }
    }

    static class Call extends Expr {
        final Expr callee;
        final Token paren;
        final List<Expr> arguments;

        Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitCallExpr(this);
        }
    }

    static class Group extends Expr {
        final Expr expr;

        Group(Expr expr) {
            this.expr = expr;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitGroupExpr(this);
        }
    }

    static class Literal extends Expr {
        final Object val;

        Literal(Object val) {
            this.val = val;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitLiteralExpr(this);
        }
    }

    static class Variable extends Expr {
        final Token name;

        Variable(Token name) {
            this.name = name;
        }

        @Override
        <R> R accept(Expr.Visitor<R> v) {
            return v.visitVariableExpr(this);
        }
    }

}
