package lox;

import java.util.Map;
import java.util.HashMap;

public class Environment {
    final Environment shadowing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        this.shadowing = null;
    }

    Environment(Environment shadowing) {
        this.shadowing = shadowing;
    }

    // could make language throw error for assigning a val to a var inside a block
    // that has the same name as a var in the top level, but that makes stmts like
    // var a; if (true) { a=1; } else { a=2; }
    // not possible.
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        else if (shadowing != null) {
            shadowing.assign(name, value);
            return;
        }
        throw new RuntimeError(name, "Undeclared variable: "+name.lexeme);
    }

    // returns "nil" if var initializer is not set (aka null)
    // b/c stringify().
    // only ever declare new vars in the local scope
    void declare(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme))
            return values.get(name.lexeme);
        else if (shadowing != null)
            return shadowing.get(name);
        throw new RuntimeError(name,"Undefined variable: '"+name.lexeme+"'.");
    }

}
