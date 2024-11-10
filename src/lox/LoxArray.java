package lox;

import java.util.List;

class LoxArray implements LoxCallable {
    private final Stmt.Array declaration;
    private final Object[] arr;
    private final int length;
    private enum Type {
        NONE, DOUBLE, STRING, BOOLEAN
    }
    private Type type = Type.NONE;

    LoxArray(Stmt.Array declaration, int length, List<Object> initElements) {
        this.declaration = declaration;
        this.length = length;
        arr = new Object[length];
        // if initializer is empty, all elems == nil
        // then, first assignment value sets array type.
        if (initElements != null) {
            for (int i=0; i<this.length; i++) {
                Object value = initElements.get(i);
                Type type = getType(value);
                // checks if Double, String, or Boolean and NOT null
                isValidType(type);
                arr[i] = value;
            }
            if (initElements.size() != length)
                throw new RuntimeError(this.declaration.name, "Expected "+this.length+" element(s)" +
                        " but got "+initElements.size()+".");
            // fails if any 1 value is of diff type
            // can't be Type.NONE here
            this.type = checkArrayType();
        }
    }

    public int length() {
        return length;
    }

    @Override
    public int arity() {
        return length;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> indices) {
        int ind = checkIndex(indices.get(0));
        return arr[ind];
    }

    public void assign(Interpreter interpreter, Object index, Object value) {
        int ind = checkIndex(index);
        // now valid assignment target
        Type type = getType(value);
        // since Type.NONE covers more than just "nil"
        if (type == Type.NONE)
            throw new RuntimeError(declaration.name, "Invalid assignment value for array.");
        // only true if initializer was null in constructor
        if (this.type == Type.NONE) {
            this.type = type;
            // replaces all nulls w/ default type val
            initializeArrayType(type);
        }
        if (type != this.type)
            throw new RuntimeError(declaration.name, "Array is of type "+this.type.toString()+".");
        arr[ind] = value;
        //checkType(); // dont need?
    }

    private void initializeArrayType(Type type) {
        Object init;
        if (type == Type.STRING)
            init = "";
        else if (type == Type.BOOLEAN)
            init = false;
        // if Type.NONE, default to double
        else
            init = 0.;
        for (int i=0; i<this.length; i++) {
            arr[i] = init;
        }
    }

    private Type checkArrayType() {
        Type type = getType(arr[0]);
        for (int i=1; i<length; i++) {
            if (type != getType(arr[i]))
                throw new RuntimeError(declaration.name, "Elements must all be of the same type.");
        }
        return type;
    }

    private Type getType(Object value) {
        if (value instanceof Double)
            return Type.DOUBLE;
        else if (value instanceof String)
            return Type.STRING;
        else if (value instanceof Boolean)
            return Type.BOOLEAN;
        return Type.NONE;
    }

    private void isValidType(Type type) {
        if (type == Type.NONE)
            throw new RuntimeError(declaration.name, "Array elements must be literals.");
    }

    private int checkIndex(Object index) {
        if (!(index instanceof Double))
            throw new RuntimeError(declaration.name, "Index must be an integer.");
        int ind = ((Double)index).intValue();
        if (ind < 0)
            throw new RuntimeError(declaration.name, "Index must be non-negative.");
        if (ind >= length)
            throw new RuntimeError(declaration.name,"Index out of bounds: array has length "+length+".");
        return ind;
    }

    @Override
    public String toString() {
        return "<array "+declaration.name.lexeme+">";
    }
}
