package lox;

public class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        // these args to super suppresses error msgs and stack traces
        // since this exception is not used for error handling.
        super(null,null,false,false);
        this.value = value;
    }
}
