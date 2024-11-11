package lox;

import java.util.List;

class LoxArray implements LoxCallable {
    private final Stmt.Array declaration;
    private Object[] arr;
    private final int length;
    // could add Object Type later if wanted
    private enum Type {
        NONE, DOUBLE, STRING, BOOLEAN
    }
    private Type type = Type.NONE;

    LoxArray(Stmt.Array declaration, int length, List<Object> initElements) {
        this.declaration = declaration;
        this.length = length;
        // if initializer is empty, all elems == nil
        // then, first assignment value sets array type.
        if (initElements != null) {
            if (initElements.size() != length)
                throw new RuntimeError(declaration.name,"Expected "+length+" element(s)"+
                                        " but got "+initElements.size()+".");
            // throws error if type == NONE or if not all same type
            this.type = determineArrayType(initElements);
            // arr never null here
            this.arr = setArrayType();
        }
        else
            this.arr = new Object[length];
    }

    // array arg is just an index
    @Override
    public int arity() {
        return 1;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> index) {
        int ind = checkIndex(index.get(0));
        return arr[ind];
    }

    public void assign(List<Object> index, Object value) {
        int ind = checkIndex(index.get(0));
        // now valid assignment target
        Type type = getType(value);
        // Type.NONE cur represents any Object not literal
        if (type == Type.NONE)
            throw new RuntimeError(declaration.name, "Type unsupported for array assignment.");
        // only true if initializer was null in constructor
        if (this.type == Type.NONE) {
            this.type = type;
            arr = setArrayType();
        }
        if (type != this.type)
            throw new RuntimeError(declaration.name, "Array is of type "+this.type.toString()+".");
        //System.out.println(value.getClass());
        arr[ind] = value;
    }

    public Object get(Token name) {
        // can't return integer cuz Lox only works with Doubles!
        if (name.lexeme.equals("len"))
            return (double)length;
        throw new RuntimeError(name, "No property named "+name.lexeme+".");
    }

    private Object[] setArrayType() {
        if (type == Type.DOUBLE)
            return new Double[this.length];
        else if (type == Type.STRING)
            return new String[this.length];
        else if (type == Type.BOOLEAN)
            return new Boolean[this.length];
        return new Object[this.length];
        //return null;
    }

    private Type determineArrayType(List<Object> elements) {
        for (int i=0; i<length-1; i++) {
            Type curType = getType(elements.get(i));
            if (curType == Type.NONE)
                throw new RuntimeError(this.declaration.name, "Elements must be literals.");
            if (curType != getType(arr[i+1]))
                throw new RuntimeError(declaration.name, "Elements must be of the same type.");
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
