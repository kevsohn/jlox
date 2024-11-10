package lox;

import java.util.List;

abstract class Stmt {
    interface Visitor<R> {
        R visitIfStmt(Stmt.If stmt);
        R visitPrintStmt(Stmt.Print stmt);
        R visitReturnStmt(Stmt.Return stmt);
        R visitBreakStmt(Stmt.Break stmt);
        R visitWhileStmt(Stmt.While stmt);
        R visitBlockStmt(Stmt.Block stmt);
        R visitExpressionStmt(Stmt.Expression stmt);
        R visitVarStmt(Stmt.Var stmt);
        R visitArrayStmt(Stmt.Array stmt);
        R visitFunctionStmt(Stmt.Function stmt);
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

    static class Return extends Stmt {
        final Token keyword;
        final Expr expr;

        Return(Token keyword, Expr expr) {
            this.keyword = keyword;
            this.expr = expr;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitReturnStmt(this);
        }
    }

    static class Break extends Stmt {
        final Token keyword;

        Break(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitBreakStmt(this);
        }
    }

    static class While extends Stmt {
        final Expr condition;
        final Stmt body;

        While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitWhileStmt(this);
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

    static class Array extends Stmt {
        final Token name;
        final Expr length;
        final List<Expr> initializer;

        Array(Token name, Expr length, List<Expr> initializer) {
            this.name = name;
            this.length = length;
            this.initializer = initializer;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitArrayStmt(this);
        }
    }

    static class Function extends Stmt {
        final Token name;
        final List<Token> params;
        final List<Stmt> body;

        Function(Token name, List<Token> params, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        <R> R accept(Stmt.Visitor<R> v) {
            return v.visitFunctionStmt(this);
        }
    }

}
