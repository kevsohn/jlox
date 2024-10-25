package lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment env = new Environment();

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements)
                execute(stmt);
        }catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition)))
            execute(stmt.thenBranch);
        else if (stmt.elseBranch != null)
            execute(stmt.elseBranch);
        return null;
    }

    // block chain ha. ha.
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        Environment previous = env;
        try {
            env = new Environment(previous);
            for (Stmt statement : stmt.statements)
                execute(statement);
        }finally {
            env = previous;
        }
        return null;
    }

    // var stmts have initializing exprs
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object val = null;
        if (stmt.initializer != null) {
            val = evaluate(stmt.initializer);
        }
        env.declare(stmt.name.lexeme, val);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(stringify(evaluate(stmt.expr)));
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
        env.assign(expr.name, val);
        return val;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return env.get(expr.name);
    }

    // the type casting makes it runtime error rather than compile-time
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        // left associative so order matters!
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        return switch (expr.op.type) {
            case PLUS_EQ -> {
                checkNumberOperand(left, expr.op, right);
                yield (double) left + (double) right;
            }
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
            case MINUS -> {
                checkNumberOperand(left, expr.op, right);
                yield (double)left - (double)right;
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
