package lox;

public class Break extends RuntimeException {
    public Break() {
        super(null, null, false, false);
    }
}
