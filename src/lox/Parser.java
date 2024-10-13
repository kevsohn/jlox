package lox;

import java.util.List;
import java.util.TreeMap;

import static lox.TokenType.*;

// Precedence order lowest to highest
// expr -> equality
// equality -> comparison (( '==' | '!=' ) comparison)*
// comparison -> term (( '>' | '>=' | '<=' | '<' ) term)*
// term -> factor (( '+' | '-' ) factor)*
// factor -> unary (( '*' | '/' ) unary)*
// unary -> (( '!' | '-' ) unary) | primary
// primary -> '(' expr ')' | NUMBER | STRING | 'true' | 'false' | 'nil'
public class Parser {
    private final List<Token> tokens;
    private int cur = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Expr parseTokens() {
        return expression();
    }

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        // left associative recursion
        Expr left = comparison();
        // match only advances cur if matches so op must be the prev token.
        // don't see why it needs to be a loop since it can at most match once
        // since we call comparison() for the right, which is higher precedence.
        // There is the "group" literal which calls "expression" but can't
        // see a case where an equality is the next token after finishing all
        // the higher precedence calls.
        // for now, 1 == 1 == 1 is valid syntax although it realistically
        // reduces to true == 1.
        // is it b/c Lox typechecks at runtime or it's just not the parser's job.
        // do all 1st-pass compilers do this?
        while (match(EQUAL_EQUAL, BANG_EQUAL)) {
            // save token before calling b/c "cur" is global var
            Token op = prev();
            Expr right = comparison();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr comparison() {
        Expr left = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token op = prev();
            Expr right = term();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr term() {
        Expr left = factor();
        while (match(PLUS, MINUS)) {
            Token op = prev();
            Expr right = factor();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr factor() {
        Expr left = unary();
        while (match(STAR, SLASH)) {
            Token op = prev();
            Expr right = unary();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr unary() {
        Expr expr;
        while (match(BANG, MINUS)) {
            Token op = prev();
            Expr right = unary();
            right = new Expr.Unary(op, right);
        }
        return right;
    }

    private Expr primary() {
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            cur++; // consume right paren
            return expr;
        }
        cur++;
        return new Expr.Literal(prev().literal);
    }

    // returns true if matches any of the given types and advances
    // could also just use if-statements in each func
    private Boolean match(TokenType... types) {
        for (TokenType type: types) {
            if (check(type)) {
                cur++;
                return true;
            }
        }
        return false;
    }

    private Boolean check(TokenType type) {
        if (atEnd()) return false;
        return peek().type == type;
    }

    private Token peek() {
        return tokens.get(cur);
    }

    private Token prev() {
        return tokens.get(cur-1);
    }

    private Boolean atEnd() {
        return tokens.get(cur).type == EOF;
    }
}
