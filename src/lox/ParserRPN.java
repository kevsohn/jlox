package lox;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.HashMap;

// Reverse Polish Notation cant handle unary operators
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
        hmap.put(TokenType.EQUAL_EQUAL, 0);
        hmap.put(TokenType.BANG_EQUAL, 0);
        hmap.put(TokenType.GREATER_EQUAL, 1);
        hmap.put(TokenType.LESS_EQUAL, 1);
        hmap.put(TokenType.GREATER, 1);
        hmap.put(TokenType.LESS, 1);
        hmap.put(TokenType.PLUS, 2);
        hmap.put(TokenType.MINUS, 2);
        hmap.put(TokenType.STAR, 3);
        hmap.put(TokenType.SLASH, 3);
        hmap.put(TokenType.RIGHT_PAREN, 4);
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
            if (t.type == TokenType.NUMBER)
                output.add(t);
            else if (isOperator(t)) {
                if (t.type == TokenType.LEFT_PAREN)
                    ops.push(new Token(TokenType.RIGHT_PAREN,")",null,t.line));
                else if (t.type == TokenType.RIGHT_PAREN) {
                    while (!ops.empty() && ops.peek().type != TokenType.RIGHT_PAREN)
                        output.add(ops.pop());
                    if (ops.empty()) {
                        Lox.error(t.line,"Unmatched parenthesis.");
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
                    while (!ops.empty() && ops.peek().type != TokenType.RIGHT_PAREN &&
                            hmap.get(t.type) <= hmap.get(ops.peek().type)) {
                        output.add(ops.pop());
                    }
                    ops.push(t);
                }
            }
            else {
                Lox.error(t.line, "Token unsupported by parser.");
            }
        }

        while (!ops.empty())
            output.add(ops.pop());
        return output;
    }

    private Boolean isOperator(Token token) {
        return switch (token.type) {
            case LEFT_PAREN, RIGHT_PAREN, PLUS, MINUS, STAR, SLASH,
                 GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, EQUAL_EQUAL, BANG_EQUAL
                    -> true;
            default -> false;
        };
    }

    private Boolean atEOF() {
        return tokens.get(cur).type == TokenType.EOF;
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
