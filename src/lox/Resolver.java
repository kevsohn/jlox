package lox;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;

// traverse the AST and emulate env calls that would be done by the Interpreter,
// creating a stack of env calls.
// counts and reports back to the Interpreter the current stack depth that the vars
// are defined in for future lookup during runtime.
// basically saves the env state at runtime and limits where the assigned value for
// the var should be taken from:
// i.e. Expr.Var objects inside the function body will no longer check for the var
// cascading from the innermost env to global but instead will just grab it from
// the saved depth from the innermost env, avoiding issues from var shadowing.
// only handles local scope variables; globals are dealt by the Interpreter.
// assumes Interpreter and Resolver are synced.
class Resolver implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String,Boolean>> scopes = new Stack<>();
    private boolean curFunction = false;
    private boolean curLoop = false;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme))
            Lox.error(name, "Variable previously declared in this scope.");
        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    private void beginScope() {
        scopes.push(new HashMap<String,Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null)
            resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        if (stmt.expr != null)
            resolve(stmt.expr);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (!curFunction)
            Lox.error(stmt.keyword, "Cannot return from top-level.");
        if (stmt.expr != null)
            resolve(stmt.expr);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (!curLoop)
            Lox.error(stmt.keyword, "Must be enclosed by a loop.");
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolveWhile(stmt, true);
        curLoop = false;
        return null;
    }

    private void resolveWhile(Stmt.While stmt, boolean curLoop) {
        boolean enclosing = this.curLoop;
        this.curLoop = curLoop;
        resolve(stmt.condition);
        resolve(stmt.body);
        this.curLoop = enclosing;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expr);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null)
            resolve(stmt.initializer);
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitArrayStmt(Stmt.Array stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            for (Expr expr : stmt.initializer)
                resolve(expr);
        }
        define(stmt.name);
        resolve(stmt.length);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, true);
        return null;
    }

    private void resolveFunction(Stmt.Function stmt, boolean curFunction) {
        boolean enclosing = this.curFunction;
        this.curFunction = curFunction;
        beginScope();
        for (Token param : stmt.params) {
            declare(param);
            define(param);
        }
        resolve(stmt.body);
        endScope();
        this.curFunction = enclosing;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Object visitAssignCallerExpr(Expr.AssignCaller expr) {
        resolve(expr.value);
        resolve(expr.callee);
        for (Expr args : expr.arguments)
            resolve(args);
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr arg : expr.arguments)
            resolve(arg);
        return null;
    }
/*
    @Override
    public Object visitArrayExpr(Expr.Array expr) {
        resolve(expr.callee);
        resolve(expr.index);
        return null;
    }
*/
    @Override
    public Object visitGetExpr(Expr.Get expr) {
        resolve(expr.caller);
        return null;
    }

    @Override
    public Object visitGroupExpr(Expr.Group expr) {
        resolve(expr.expr);
        return null;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    // variable equivalent to a function/method call
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        // case: var a = a;
        // declared 'a' (false) but not defined yet since resolve(initializer) will
        // get a Expr.Variable obj from how the AST was parsed and will
        // run into this error cond
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE)
            Lox.error(expr.name, "Can't read local var in its own initializer.");
        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i=scopes.size()-1; i>=0; i--) {
            // cur assumes global if scopes dont contain var
            if (scopes.get(i).containsKey(name.lexeme)) {
                // dist from most recent scope to scope where var is defined
                interpreter.resolve(expr, scopes.size()-1 - i);
                return;
            }
        }
    }

}
