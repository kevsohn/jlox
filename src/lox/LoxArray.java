package lox;

import java.util.ArrayList;
import java.util.List;

public class LoxArray implements LoxCallable {
    private final Stmt.Array declaration;
    private final Environment shadowing;
    private Object[] array;
    private int size;

    LoxArray(Stmt.Array declaration, Environment shadowing, List<Object> initElements) {
        this.declaration = declaration;
        this.shadowing = shadowing;
        this.size = ((Double)this.declaration.size.literal).intValue();

        array = new Object[size];
        if (initElements != null) {
            for (int i = 0; i < size; i++)
                array[i] = initElements.get(i);
        }
    }

    public int size() {
        return array.length;
    }

    @Override
    public int arity() {
        return size;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> indices) {
        return array[(int)indices.get(0)];
    }

    @Override
    public String toString() {
        return "<array "+declaration.name.lexeme+">";
    }

}
