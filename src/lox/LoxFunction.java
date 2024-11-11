package lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Environment closure;
    private final Stmt.Function declaration;

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    // returns an Object, which means it can return function references!
    // because all fns are currently bound to a name, the fn ref can simply
    // be saved to a var identifier then invoked with a "()" call
    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment env = new Environment(closure);
        for (int i=0; i<arity(); i++)
            env.define(declaration.params.get(i).lexeme, args.get(i));
        try {
            interpreter.executeBlock(declaration.body, env);
        }catch (Return returnValue) {
            //System.out.println(returnValue.getClass());
            return returnValue.value;
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn "+declaration.name.lexeme+">";
    }
}
