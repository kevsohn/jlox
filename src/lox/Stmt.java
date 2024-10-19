package lox;

abstract class Stmt {
    interface Visitor<R> {
        R visitExpressionStmt(Stmt.Expression stmt);
        R visitPrintStmt(Stmt.Print stmt);
        R visitVarStmt(Stmt.Var stmt);
    }

    abstract <R> R accept(Stmt.Visitor<R> v);

    static class Expression extends Stmt {
        final Expr expr;

        Expression(Expr expr) {
            this.expr = expr;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitExpressionStmt(this);
        }
    }

    static class Print extends Stmt {
        final Expr expr;

        Print(Expr expr) {
            this.expr = expr;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitPrintStmt(this);
        }
    }

    static class Var extends Stmt {
        final Token name;
        final Expr initializer;

        Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitVarStmt(this);
        }
    }

}
