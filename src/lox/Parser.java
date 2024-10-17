package lox;

import java.util.ArrayList;
import java.util.List;

import static lox.TokenType.*;

// Precedence order lowest to highest
// program -> declaration* EOF
// declaration -> varDecl | statement
// varDecl -> "var" IDENTIFIER ("=" expression)? ";"
// statement -> printStmt | exprStmt
// printStmt -> "print" expression ";"
// exprStmt -> expression ";"
// expression -> equality
// equality -> comparison (( '==' | '!=' ) comparison)*
// comparison -> term (( '>' | '>=' | '<=' | '<' ) term)*
// term -> factor (( '+' | '-' ) factor)*
// factor -> exponent (( '*' | '/' | '%' ) exponent)*
// exponent -> unary (( '*' | '/' | '%' ) unary)*
// unary -> (( '!' | '-' ) unary) | primary
// primary -> IDENTIFIER | '(' expr ')' | NUMBER | STRING | 'true' | 'false' | 'nil'
public class Parser {
    private static class ParseError extends RuntimeException {

    }

    private final List<Token> tokens;
    private int cur = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!atEnd())
            statements.add(declaration());
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR))
                return varDeclaration();
            return statement();
        }catch (ParseError error) {
            sync();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr init = null;
        if (match(EQ)) {
            init = expression();
        }
        consume(SEMICOLON, "Missing semicolon after var declaration.");
        return new Stmt.Var(name, init);
    }

    private Stmt statement() {
        if (match(PRINT))
            return printStatement();
        return exprStatement();
    }

    private Stmt printStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Missing semicolon after print.");
        return new Stmt.Print(expr);
    }

    private Stmt exprStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Missing semicolon after expression.");
        return new Stmt.Expression(expr);
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
        while (match(EQ_EQ, BANG_EQ)) {
            // save token before calling b/c "cur" is global var
            Token op = prev();
            Expr right = comparison();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr comparison() {
        Expr left = term();
        while (match(GREATER, GREATER_EQ, LESS, LESS_EQ)) {
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
        Expr left = exponent();
        while (match(STAR, SLASH, MOD)) {
            Token op = prev();
            Expr right = exponent();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr exponent() {
        Expr left = unary();
        while (match(HAT)) {
            Token op = prev();
            Expr right = unary();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr unary() {
        // recurse unary until it hits primary basecase
        if (match(BANG, MINUS)) {
            Token op = prev();
            Expr right = unary();
            return new Expr.Unary(op, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(IDENTIFIER)) return new Expr.Variable(prev());
        // nil, true, and false have "null" in its literal field
        else if (match(NUMBER, STRING, NIL)) return new Expr.Literal(prev().literal);
        else if (match(TRUE)) return new Expr.Literal(true);
        else if (match(FALSE)) return new Expr.Literal(false);
        else if (match(L_PAREN)) {
            Expr expr = expression();
            consume(R_PAREN, "Expect ')' after expression.");
            return new Expr.Group(expr);
        }
        throw error(peek(),"Expect expression.");
    }

    // method doesn't throw error itself for control over error report.
    // throwing is done by the terminal production and caught higher up
    // while unwinding to the correct nonterminal to aid in syncing parser
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    // discard tokens until after end of statement is found
    private void sync() {
        advance();
        while (!atEnd()) {
            if (prev().type == SEMICOLON) return;
            // statements usually start with these keywords
            switch (peek().type) {
                case CLASS, FOR, FN, IF, PRINT, RETURN, VAR, WHILE:
                    return;
            }
            advance();
        }
    }

    // since atEnd() is only sensitive to cur == EOF,
    // need to care about advancing past it
    private void advance() {
        if (!atEnd()) cur++;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            advance();
            return prev();
        }
        throw error(peek(), message);
    }

    // returns true if matches any of the given types and advances
    // could also just use if-statements in each func
    private Boolean match(TokenType... types) {
        for (TokenType type: types) {
            if (check(type)) {
                advance();
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
