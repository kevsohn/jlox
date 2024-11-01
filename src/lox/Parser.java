package lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static lox.TokenType.*;

/* How to implement arrays?
Could have a keyword "list" in addition to the "var" keyword with
 declaration -> varDecl | listDecl | statement
 listDecl -> "list" IDENTIFIER ("=" "[" (NUMBER | STRING) ("," (NUMBER | STRING))* "]")? ";"
or could have the initializer be a primary in the expression tree with
 listDecl -> "list" IDENTIFIER ("=" expression)? ";"
 primary -> "[" (NUMBER | STRING) ("," (NUMBER | STRING))* "]"
but does mean I have to modify the assignment syntax since you shouldn't be able to assign
a single value to the list identifier.
Also, it means I may need 2 separate environments to keep track of duplicate list and var names
unless I forbid this by throwing an error if the hmap contains the name. would have to change
variable declaration to disallow redeclaring a var with the same name.
In terms of implementation, just add to List<Object> until one hits "]" and throw runtime error
if not all just NUMBER or STRING.

Otherwise, could treat "[]" like a function call on an IDENTIFIER
 */

// Precedence order lowest to highest
// program -> declaration* EOF
// declaration -> fnDecl | varDecl | statement
// fnDecl -> "fn" function
// function -> IDENTIFIER "(" params? ")" block
// params -> IDENTIFIER ("," IDENTIFIER)*
// varDecl -> "var" IDENTIFIER ("=" expression)? ";"
// statement -> exprStmt | ifStmt | printStmt | returnStmt | whileStmt | forStmt | block
// ifStmt -> "if" expression "then" ("else" statement)?
// printStmt -> "print" expression ";"
// returnStmt -> "return" expression? ";"
// whileStmt -> "while" "(" expression ")" statement
// forStmt-> "for" "(" (varDecl | exprStmt)? ";" expression? ";" expression? ")" statement
// block -> "{" statement* "}"
// exprStmt -> expression ";"
// expression -> assignment
// assignment -> IDENTIFIER ("=" assignment | ("+=" | "-=" ) logic_or) | logic_or
// logic_or -> logic_and ("or" logic_and)*
// logic_and -> equality ("and" equality)*
// equality -> comparison (( '==' | '!=' ) comparison)*
// comparison -> term (( '>' | '>=' | '<=' | '<' ) term)*
// term -> factor (( '+' | '-' ) factor)*
// factor -> exponent (( '*' | '/' | '%' ) exponent)*
// exponent -> unary (( '*' | '/' | '%' ) unary)*
// unary -> (( '!' | '-' ) unary) | primary
// call -> primary ( "(" arguments? ")" )*
// arguments -> expression ("," expression)*
// primary -> IDENTIFIER | '(' expr ')' | NUMBER | STRING | 'true' | 'false' | 'nil'
public class Parser {
    private static class ParseError extends RuntimeException { }

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
            if (match(VAR)) return varDeclaration();
            else if (match(FN)) return function("function");
            return statement();
        }catch (ParseError error) {
            sync();
            return null;
        }
    }

    private Stmt function(String kind) {
        Token name = consume(IDENTIFIER, "Expect "+kind+" name.");
        consume(L_PAREN, "Expect '(' after "+kind+" name.");
        List<Token> params = new ArrayList<>();
        if (!check(R_PAREN)) {
            do {
                if (params.size() >= 255)
                    error(peek(),"Cannot exceed more than 255 parameters.");
                params.add(consume(IDENTIFIER, "Parameters must be identifiers."));
            } while (match(COMMA));
        }
        consume(R_PAREN, "Expect ')' after parameters.");
        consume(L_BRACE, "Expect '{' before "+kind+" body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, params, body);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr init = null;
        if (match(EQ)) {
            init = expression();
        }
        consume(SEMICOLON, "Missing ';' after var declaration.");
        return new Stmt.Var(name, init);
    }

    private Stmt statement() {
        // does order matter?
        if (match(IF)) return ifStmt();
        else if (match(PRINT)) return printStmt();
        else if (match(RETURN)) return returnStmt();
        else if (match(WHILE)) return whileStmt();
        else if (match(FOR)) return forStmt();
        else if (match(L_BRACE)) return new Stmt.Block(block());
        return exprStmt();
    }

    // if stmts need a delimiter to discern the condition from the following stmt.
    // design decision: "if" expression blockStmt ("else" blockStmt)?
    // this follows Go, where you need curly braces to define scope of 'if'.
    // Java does "if" "(" expression ")" statement ("else" statement)?
    // which is actually why "else if" is not a separate token but actually just
    // a chain of ifs, unlike Python's elif.
    // In the above, the "else" is tied to the nearest "if" to avoid ambiguity.
    private Stmt ifStmt() {
        Expr cond = expression();
        // without delim, "if a == 1 and a +=2; print a;" fails cuz
        // the condition becomes (a==1 and a) instead of failing with
        // (a==1 and _).
        consume(COLON, "Expect ':' after if condition.");
        if (check(VAR))
            throw error(peek(), "Global var declaration not allowed inside 'if'.");
        // otherwise, just call declaration() instead of statement()
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            if (check(VAR))
                throw error(peek(), "Global var declaration not allowed inside 'else'.");
            elseBranch = statement();
        }
        return new Stmt.If(cond, thenBranch, elseBranch);
    }

    private Stmt returnStmt() {
        Token keyword = prev();
        Expr expr = null;
        if (!check(SEMICOLON))
            expr = expression();
        consume(SEMICOLON, "Missing ';' after return.");
        return new Stmt.Return(keyword, expr);
    }

    private Stmt whileStmt() {
        consume(L_PAREN, "Expect '(' after 'while'.");
        Expr cond = expression();
        consume(R_PAREN, "Expect ')' after while condition.");
        Stmt body = statement();
        return new Stmt.While(cond, body);
    }

    private Stmt forStmt() {
        consume(L_PAREN, "Expect '(' after 'for'.");

        // no direct semicolon consume cuz expecting statements
        Stmt init;
        if (match(SEMICOLON)) init = null;
        else if (match(VAR)) init = varDeclaration();
        else init = exprStmt();

        Expr cond = null;
        if (!check(SEMICOLON)) cond = expression();
        consume(SEMICOLON, "Expect ';' after for condition.");

        Expr incr = null;
        if (!check(SEMICOLON)) incr = expression();
        consume(R_PAREN, "Expect ')' after for increment.");

        Stmt body = statement();
        if (incr != null)
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(incr)));

        if (cond == null)
            cond = new Expr.Literal(true);
        body = new Stmt.While(cond, body);

        if (init != null)
            body = new Stmt.Block(Arrays.asList(init, body));
        return body;
    }

    private List<Stmt> block() {
        List<Stmt> stmts = new ArrayList<>();
        // the !atEnd() check is super important, otherwise possible to
        // get an infinite loop if an error occurs inside a block due to
        // sync() schenanigans reading past the R_BRACE
        while (!check(R_BRACE) && !atEnd()) {
            stmts.add(declaration());
        }
        consume(R_BRACE, "Expect '}' after block.");
        return stmts;
    }

    private Stmt printStmt() {
        Expr expr = expression();
        consume(SEMICOLON, "Missing ';' after print.");
        return new Stmt.Print(expr);
    }

    private Stmt exprStmt() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        // right associative
        Expr expr = or();
        if (match(EQ, PLUS_EQ, MINUS_EQ)) {
            Token op = prev();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                Expr val;
                if (op.type == EQ) {
                    // recursion so "a = b = 1;" works in a right associative way
                    val = assignment();
                    return new Expr.Assign(name, val);
                }
                else {
                    val = or();
                    // the issue with this method is you can get situations like
                    // var a = 1; print a; => 1
                    // var b = "jank"; print a += b; => "1jank"
                    // due to the plus operator being overloaded for string concat.
                    // this is peak dynamic typing but i dont like b/c
                    // print a -= 1; => error since a is now a string.
                    /*
                    if (op.type == PLUS_EQ)
                        op = new Token(PLUS, "+", null, op.line);
                    else
                        op = new Token(MINUS, "-", null, op.line);
                    */
                    // sol: add redundant plus operation in Interpreter
                    if (op.type == MINUS_EQ)
                        op = new Token(MINUS, "-", null, op.line);
                    return new Expr.Assign(name, new Expr.Binary(expr, op, val));
                }
            }
            throw error(op, "Invalid assignment target");
        }
        return expr;
    }

    private Expr or() {
        Expr left = and();
        // use while + no recursion for left associative
        while (match(OR)) {
            Token op = prev();
            Expr right = and();
            left = new Expr.Logical(left, op, right);
        }
        return left;
    }

    private Expr and() {
        Expr left = equality();
        // use while + no recursion for left associative
        while (match(AND)) {
            Token op = prev();
            Expr right = equality();
            left = new Expr.Logical(left, op, right);
        }
        return left;
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
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            // for calls upon calls f()+
            if (match(L_PAREN))
                expr = finishCall(expr);
            else
                break;
        }
        return expr;
    }

    private Expr finishCall(Expr expr) {
        List<Expr> args = new ArrayList<>();
        if (!check(R_PAREN)) {
            do {
                // added to be consistent with clox bytecode
                if (args.size() >= 255)
                    // doesn't throw error and enter panic mode cuz parsed correctly
                    error(peek(), "Cannot exceed more than 255 arguments.");
                args.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(R_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(expr, paren, args);
    }

    private Expr primary() {
        if (match(IDENTIFIER)) return new Expr.Variable(prev());
        // nil, true, and false have "null" in its literal field
        else if (match(NUMBER, STRING, NIL)) return new Expr.Literal(prev().literal);
        else if (match(TRUE)) return new Expr.Literal(true);
        else if (match(FALSE)) return new Expr.Literal(false);
        else if (match(L_PAREN)) {
            Expr expr = expression();
            consume(R_PAREN, "Expect ')' to close expression.");
            return new Expr.Group(expr);
        }
        else if (match(R_PAREN))
            throw error(peek(), "Missing '(' somewhere.");
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
