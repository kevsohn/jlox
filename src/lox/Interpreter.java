package lox;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static lox.TokenType.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment env = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    // for native function decl
    Interpreter() {
        // clock() for benchmarking
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements)
                execute(stmt);
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // just binds the identifier to the parsed Function object.
    // offloads function calls to the LoxFunction object.
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // b/c the env state is temporary w/o saved states,
        // pass a copy of the cur env to be stored in case
        // nested fns need to ref vars declared in top level.
        LoxFunction function = new LoxFunction(stmt, env);
        env.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitArrayStmt(Stmt.Array stmt) {
        Object length = evaluate(stmt.length);
        if (!(length instanceof Double))
            throw new RuntimeError(stmt.name, "Array size must be an integer.");
        int len = ((Double)length).intValue();
        if (len <= 0)
            throw new RuntimeError(stmt.name, "Array size must be positive.");

        List<Object> initElems = null;
        if (stmt.initializer != null) {
            initElems = new ArrayList<>();
            for (Expr expr : stmt.initializer) {
                initElems.add(evaluate(expr));
            }
        }
        LoxArray array = new LoxArray(stmt, len, initElems);
        env.define(stmt.name.lexeme, array);
        return null;
    }

    // var stmts have initializing exprs
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object val = null;
        if (stmt.initializer != null) {
            val = evaluate(stmt.initializer);
        }
        env.define(stmt.name.lexeme, val);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition)))
            execute(stmt.thenBranch);
        else if (stmt.elseBranch != null)
            execute(stmt.elseBranch);
        return null;
    }

    // wraps the interpreted value into a
    // catchable Return obj by the LoxFunction obj.
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object val = null;
        if (stmt.expr != null)
            val = evaluate(stmt.expr);
        throw new Return(val);
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new Break();
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        try {
            while (isTruthy(evaluate(stmt.condition)))
                execute(stmt.body);
        }catch (Break b) {}
        return null;
    }

    // block chain ha. ha.
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(env));
        return null;
    }

    // executes the list of stmts in the given environment
    // returns the interpreter's env back to its top level
    public void executeBlock(List<Stmt> stmts, Environment env) {
        Environment prev = this.env;
        try {
            this.env = env;
            for (Stmt statement : stmts)
                execute(statement);
        }finally {
            this.env = prev;
        }
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        if (stmt.expr != null)
            System.out.println(stringify(evaluate(stmt.expr)));
        else
            System.out.println();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expr);
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object val = evaluate(expr.value);
        Integer depth = locals.get(expr);
        if (depth != null)
            env.assignAt(expr.name, val, depth);
        else
            globals.assign(expr.name, val);
        return val;
    }

    // assignment is dealt by the LoxArray object, so
    // no need to call env for reassignment, unlike vars.
    @Override
    public Object visitAssignCallerExpr(Expr.AssignCaller expr) {
        Object callee = evaluate(expr.callee);
        if (!(callee instanceof LoxArray array))
            throw new RuntimeError(expr.error, "Object cannot be assigned to.");
        // index checks happen inside LoxArray
        // currently a list, but it should only have 1 elem
        // future-proof in case functions/classes can be assigned to
        List<Object> index = new ArrayList<>();
        for (Expr arg : expr.arguments)
            index.add(evaluate(arg));
        Object val = evaluate(expr.value);
        array.assign(index, val);
        return val;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        // if Expr.Variable, get associated val
        // if Expr.Array, get LoxArray obj ref
        // if Expr.Function, get LoxFunction obj ref
        Object callee = evaluate(expr.callee);
        if (!(callee instanceof LoxCallable object))
            throw new RuntimeError(expr.error, "Object not callable.");

        List<Object> args = new ArrayList<>();
        // if LoxArray, args is a list with just 1 entry for the index
        for (Expr argument : expr.arguments)
            args.add(evaluate(argument));
        if (args.size() != object.arity())
            throw new RuntimeError(expr.error, "Expected " + object.arity() +
                                    " arguments but got " + args.size() + ".");
        return object.call(this, args);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr, expr.name);
    }

    private Object lookupVariable(Expr expr, Token name) {
        Integer depth = locals.get(expr);
        if (depth == null) {
            // either global or undeclared; error handled by .get() in Environment
            return globals.get(name);
        }
        return env.getAt(name, depth);
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.op.type == OR) {
            if (isTruthy(left))
                return left;
        }
        else {
            if (!isTruthy(left))
                return left;
        }
        return evaluate(expr.right);
    }

    // the type casting makes it runtime error rather than compile-time
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        // left associative so order matters!
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        return switch (expr.op.type) {
            case PLUS -> {
                if (left instanceof Double && right instanceof Double)
                    yield (double)left + (double)right;
                else if (left instanceof String && right instanceof String)
                    yield (String)left + (String)right;
                // use stringify to avoid cases like (1 + "1" = 1.01)
                else if (left instanceof String && right instanceof Double)
                    yield (String)left + stringify((double)right);
                else if (left instanceof Double && right instanceof String)
                    yield stringify((double)left) + (String)right;
                else
                    throw new RuntimeError(expr.op,"Operands must be numbers and/or strings.");
            }
            case MINUS, MINUS_MINUS -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left - (double)right;
            }
            case PLUS_EQ, PLUS_PLUS -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left + (double)right;
            }
            case STAR -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left * (double)right;
            }
            case SLASH -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left / (double)right;
            }
            case MOD -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left % (double)right;
            }
            case HAT -> {
                checkNumberOperand(left, expr.op, right);
                yield Math.pow((double)left, (double)right);
            }
            case GREATER -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left > (double)right;
            }
            case GREATER_EQ -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left >= (double)right;
            }
            case LESS -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left < (double)right;
            }
            case LESS_EQ -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left <= (double)right;
            }
            case EQ_EQ -> isEqual(left,right);
            case BANG_EQ -> !isEqual(left,right);
            default -> null;
        };
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        return switch (expr.op.type) {
            case MINUS -> {
                checkNumberOperand(expr.op, right);
                yield -(double)right;
            }
            case BANG -> !isTruthy(right);
            default -> null;
        };
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object caller = evaluate(expr.caller);
        if (caller instanceof LoxArray)
            return ((LoxArray)caller).get(expr.property);
        throw new RuntimeError(expr.property,"Only arrays have properties.");
    }

    @Override
    public Object visitGroupExpr(Expr.Group expr) {
        return evaluate(expr.expr);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.val;
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // Expr obj is unique
    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    private String stringify(Object obj) {
        if (obj == null) return "nil";
        else if (obj instanceof Double) {
            String num = obj.toString();
            if (num.endsWith(".0"))
                return num.substring(0, num.length()-2);
        }
        return obj.toString();
    }

    // anything other than "nil" and "false" returns true
    // for niche cases like "if (1)"
    private boolean isTruthy(Object obj) {
        if (obj == null) return false;
        else if (obj instanceof Boolean) return (Boolean)obj;
        return true;
    }

    // equality op can compare different Objects
    // follows Java's .equals rule
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        else if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator,"Operand must be a number.");
    }

    private void checkNumberOperand(Object left, Token operator, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator,"Operands must be numbers.");
    }
}
