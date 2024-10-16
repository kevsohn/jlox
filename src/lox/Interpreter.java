package lox;

public class Interpreter implements Expr.Visitor<Object> {

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

    public Object visitBinary(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        return switch (expr.op.type) {
            case PLUS -> (left instanceof Double && right instanceof Double)
                    ? (Double)left + (Double)right : (String)left + (String)right;
            case MINUS -> (Double)left - (Double)right;
            case STAR -> (Double)left * (Double)right;
            case SLASH -> (Double)left / (Double)right;
            case MOD -> (Double)left % (Double)right;
            case HAT-> Math.pow((Double)left, (Double)right);
            case GREATER -> (Double)left > (Double)right;
            case GREATER_EQ -> (Double)left >= (Double)right;
            case LESS -> (Double)left < (Double)right;
            case LESS_EQ -> (Double)left <= (Double)right;
            case EQ_EQ -> isEqual(left,right);
            case BANG_EQ -> !isEqual(left,right);
            default -> null;
        };
    }

    public Object visitUnary(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        return switch (expr.op.type) {
            case MINUS -> -(double) right;
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
