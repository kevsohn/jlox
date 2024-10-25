package lox;

import java.util.List;

abstract class Stmt {
    interface Visitor<R> {
        R visitIfStmt(Stmt.If stmt);
        R visitBlockStmt(Stmt.Block stmt);
        R visitPrintStmt(Stmt.Print stmt);
        R visitExpressionStmt(Stmt.Expression stmt);
        R visitVarStmt(Stmt.Var stmt);
    }

    abstract <R> R accept(Stmt.Visitor<R> v);

    static class If extends Stmt {
        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;

        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitIfStmt(this);
        }
    }

    static class Block extends Stmt {
        final List<Stmt> statements;

        Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitBlockStmt(this);
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
