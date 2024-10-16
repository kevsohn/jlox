package lox;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.HashMap;
import static lox.TokenType.*;

// Reverse Polish Notation cant handle operators with
// more than 1 assigned role, which is why the "-" op
// being both binary and unary doesn't work for RPN,
// since it changes operation depending on context.
// Context-free
public class ParserRPN {
    private final List<Token> tokens;
    private final Stack<Token> ops = new Stack<>();
    private final List<Token> output = new ArrayList<>();
    private int cur = 0;
    private static final HashMap<TokenType, Integer> hmap;

    // operator precedence from lowest to highest
    // equality: ==, !=
    // comparison: >, <, >=, <=
    // term: +, -
    // factor: *, /
    // group: (, )
    static {
        hmap = new HashMap<>();
        hmap.put(EQ_EQ, 0);
        hmap.put(BANG_EQ, 0);
        hmap.put(GREATER_EQ, 1);
        hmap.put(LESS_EQ, 1);
        hmap.put(GREATER, 1);
        hmap.put(LESS, 1);
        hmap.put(PLUS, 2);
        hmap.put(MINUS, 2);
        hmap.put(STAR, 3);
        hmap.put(SLASH, 3);
        hmap.put(R_PAREN, 4);
        //hmap.put(TokenType.BANG, 4);
        //hmap.put(TokenType.MINUS, 4); // RPN can't handle unary?
    }

    public ParserRPN(List<Token> tokens) {
        this.tokens = tokens;
    }

    // shunting yard algo
    // stack ops until an op with lower precedence is read,
    // from which offload all ops into output stack
    public List<Token> parseTokens() {
        while (!atEOF()) {
            Token t = tokens.get(cur++);
            // highest precedence to lowest
            if (t.type == NUMBER)
                output.add(t);
            else if (isOperator(t)) {
                if (t.type == L_PAREN)
                    ops.push(new Token(R_PAREN,")",null,t.line));
                else if (t.type == R_PAREN) {
                    while (!ops.empty() && ops.peek().type != R_PAREN)
                        output.add(ops.pop());
                    if (ops.empty()) {
                        Lox.error(t,"Unmatched parenthesis.");
                        // to avoid popping empty
                        break;
                    }
                    // while loop ends when ops is at ")"
                    ops.pop();
                }
                // right paren always has highest precedence
                else if (ops.empty() || hmap.get(t.type) > hmap.get(ops.peek().type))
                    ops.push(t);
                else {
                    // go until cur becomes highest precedence except parenthesis
                    while (!ops.empty() && ops.peek().type != R_PAREN &&
                            hmap.get(t.type) <= hmap.get(ops.peek().type)) {
                        output.add(ops.pop());
                    }
                    ops.push(t);
                }
            }
            else {
                Lox.error(t, "Token unsupported by parser.");
            }
        }

        while (!ops.empty())
            output.add(ops.pop());
        return output;
    }

    private Boolean isOperator(Token token) {
        return switch (token.type) {
            case L_PAREN, R_PAREN, PLUS, MINUS, STAR, SLASH,
                 GREATER, LESS, GREATER_EQ, LESS_EQ, EQ_EQ, BANG_EQ
                    -> true;
            default -> false;
        };
    }

    private Boolean atEOF() {
        return tokens.get(cur).type == EOF;
    }

    // !!! no need to peek for shunting algo b/c it's LR(1) !!!
    // don't need to make new Token cuz fields are final
    // .get() passes by reference
    /*
    private Token peek() {
        if (atEOF()) return null;
        return tokens.get(cur);
    }
    */
}
