package lox;

import java.util.List;

abstract class Stmt {
    interface Visitor<R> {
        R visitExpression(Stmt.Expression stmt);
        R visitPrint(Stmt.Print stmt);
    }

    abstract <R> R accept(Stmt.Visitor<R> v);

    static class Expression extends Stmt {
        final Expr expr;

        Expression(Expr expr) {
            this.expr = expr;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitExpression(this);
        }
    }

    static class Print extends Stmt {
        final Expr expr;

        Print(Expr expr) {
            this.expr = expr;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitPrint(this);
        }
    }

}
