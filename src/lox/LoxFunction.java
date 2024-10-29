package lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment env = new Environment(interpreter.globals);
        for (int i=0; i<arity(); i++)
            env.define(declaration.params.get(i).lexeme, args.get(i));
        interpreter.executeBlock(declaration.body, env);
        return null;
    }

    @Override
    public String toString() {
        return "<fn "+declaration.name.lexeme+">";
    }
}
