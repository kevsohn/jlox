package lox;

public class Interpreter implements Expr.Visitor<Object> {

    public void interpret(Expr expr) {
        try {
            Object val = evaluate(expr);
            System.out.println(stringify(val));
        }catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
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

    private String stringify(Object obj) {
        if (obj == null) return "nil";
        else if (obj instanceof Double) {
            String text = obj.toString();
            if (text.endsWith(".0"))
                return text.substring(0, text.length()-2);
        }
        return obj.toString();
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator,"Operand must be a number.");
    }

    private void checkNumberOperand(Object left, Token operator, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator,"Operands must be numbers.");
    }

    public Object visitBinary(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        return switch (expr.op.type) {
            case PLUS -> {
                if (left instanceof Double && right instanceof Double)
                    yield (Double)left + (Double)right;
                else if (left instanceof String && right instanceof String)
                    yield (String)left + (String)right;
                else
                    throw new RuntimeError(expr.op,"Operands must be two numbers or two strings.");
            }
            case MINUS -> {
                checkNumberOperand(left, expr.op, right);
                yield (Double)left - (Double)right;
            }
            case STAR -> {
                checkNumberOperand(left, expr.op, right);
                yield (Double)left * (Double)right;
            }
            case SLASH -> {
                checkNumberOperand(left, expr.op, right);
                yield (Double)left / (Double)right;
            }
            case MOD -> {
                checkNumberOperand(left, expr.op, right);
                yield (Double)left % (Double)right;
            }
            case HAT -> {
                checkNumberOperand(left, expr.op, right);
                yield Math.pow((Double)left, (Double)right);
            }
            case GREATER -> {
                checkNumberOperand(left, expr.op, right);
                yield (Double)left > (Double)right;
            }
            case GREATER_EQ -> {
                checkNumberOperand(left, expr.op, right);
                yield (Double)left >= (Double)right;
            }
            case LESS -> {
                checkNumberOperand(left, expr.op, right);
                yield (Double)left < (Double)right;
            }
            case LESS_EQ -> {
                checkNumberOperand(left, expr.op, right);
                yield (Double)left <= (Double)right;
            }
            case EQ_EQ -> isEqual(left,right);
            case BANG_EQ -> !isEqual(left,right);
            default -> null;
        };
    }

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

    public Object visitLiteral(Expr.Literal expr) {
        return expr.val;
    }

    public Object visitGroup(Expr.Group expr) {
        return evaluate(expr.expr);
    }

}