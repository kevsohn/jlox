package lox;

abstract class Stmt {
    interface Visitor<R> {
        R visitExpression(Stmt.Expression stmt);
        R visitPrint(Stmt.Print stmt);
        R visitVar(Stmt.Var stmt);
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

    static class Var extends Stmt {
        final Token identifier;
        final Expr init;

        Var(Token identifier, Expr init) {
            this.identifier = identifier;
            this.init = init;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitVar(this);
        }
    }

}
