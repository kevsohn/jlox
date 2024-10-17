package lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        }catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
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

    @Override
    public Void visitPrint(Stmt.Print stmt) {
        System.out.println(stringify(evaluate(stmt.expr)));
        return null;
    }

    @Override
    public Void visitExpression(Stmt.Expression stmt) {
        evaluate(stmt.expr);
        return null;
    }

    @Override
    public Object visitBinary(Expr.Binary expr) {
        // left associative so order matters!
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        return switch (expr.op.type) {
            case PLUS -> {
                if (left instanceof Double && right instanceof Double)
                    yield (double)left + (double)right;
                else if (left instanceof String && right instanceof String)
                    yield (String)left + (String)right;
                else if (left instanceof String && right instanceof Double)
                    yield (String)left + (double)right;
                else if (left instanceof Double && right instanceof String)
                    yield (double)left + (String)right;
                else
                    throw new RuntimeError(expr.op,"Operands must be two numbers or two strings.");
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
    public Object visitUnary(Expr.Unary expr) {
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
    public Object visitLiteral(Expr.Literal expr) {
        return expr.val;
    }

    @Override
    public Object visitGroup(Expr.Group expr) {
        return evaluate(expr.expr);
    }

    private boolean isTruthy(Object obj) {
        if (obj == null) return false;
        else if (obj instanceof Boolean) return (Boolean)obj;
        return true;
    }

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
