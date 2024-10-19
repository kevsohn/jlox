package lox;

import java.util.Map;
import java.util.HashMap;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    // return nil if var initializer is not set
    void define(String name, Object value) {
        //if (value == null)
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme))
            return values.get(name.lexeme);
        throw new RuntimeError(name,"Undefined variable: '"+name.lexeme+"'.");
    }

    void put(String name, Object value) {
        // intentionally override previous binding
        values.put(name, value);
    }

}
