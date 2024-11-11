package lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static lox.TokenType.*;

// Precedence order lowest to highest
// program -> declaration* EOF
// declaration -> fnDecl | varDecl | arrDecl | statement
// fnDecl -> "fn" function
// function -> IDENTIFIER "(" params? ")" block
// params -> IDENTIFIER ("," IDENTIFIER)*
// varDecl -> "var" IDENTIFIER ("=" expression)? ";"
// arrDecl -> "arr" IDENTIFIER "[" expression "]" ("=" "{" arguments? "}" )? ";"
// arguments -> expression ("," expression)*
// statement -> exprStmt | ifStmt | printStmt | returnStmt | whileStmt | forStmt | block
// ifStmt -> "if" expression "then" ("else" statement)?
// printStmt -> "print" expression ";"
// returnStmt -> "return" expression? ";"
// breakStmt -> "break" ";"
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
// call -> array ("(" arguments? ")" | "." IDENTIFIER)*
// arrayCall -> primary "[" expression "]"
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
            else if (match(ARR)) return arrDeclaration();
            else if (match(FN)) return function("function");
            return statement();
        }catch (ParseError error) {
            sync();
            return null;
        }
    }

    // "kind" param used to accommodate functions and methods.
    // wrap the parsed params into a Function object to be bound to an identifier
    // on fn declaration to a LoxFunction runtime object.
    // calls are independent to functions.
    private Stmt function(String kind) {
        Token name = consume(IDENTIFIER, "Expect "+kind+" name.");
        consume(L_PAREN, "Expect '(' after "+kind+" name.");
        List<Token> params = new ArrayList<>();
        if (!check(R_PAREN)) {
            do {
                if (params.size() >= 255)
                    error(peek(),"Cannot exceed more than 255 parameters.");
                params.add(consume(IDENTIFIER, "Parameters must be identifiers."));
            }while (match(COMMA));
        }
        consume(R_PAREN, "Expect ')' after parameters.");
        consume(L_BRACE, "Expect '{' before "+kind+" body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, params, body);
    }

    private Stmt arrDeclaration() {
        Token name = consume(IDENTIFIER, "Expect array name.");
        consume(L_BRACKET, "Expect '[' after array name.");
        // any expr allowed as long as it evals to a Double
        Expr size = expression();
        consume(R_BRACKET, "Expect ']' after array size.");

        List<Expr> initElements = null;
        if (match(EQ)) {
            // {{},{}} recursive arrDefition() calls
            //arrDefinition(init);
            consume(L_BRACE, "Expect '{' for array initialization.");
            if (!check(R_BRACE)) {
                initElements = new ArrayList<>();
                // type-check in the Interpreter
                do {
                    // allows any expression as long as it evals to
                    // the allowed types.
                    Expr val = expression();
                    initElements.add(val);
                }while (match(COMMA));
            }
            consume(R_BRACE, "Missing '}' after array initialization.");
        }
        consume(SEMICOLON, "Missing ';' after array declaration.");
        return new Stmt.Array(name, size, initElements);
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
        else if (match(BREAK)) return breakStmt();
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

    private Stmt breakStmt() {
        Token keyword = prev();
        consume(SEMICOLON, "Missing ';' after break.");
        return new Stmt.Break(keyword);
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
        Expr expr = null;
        if (!check(SEMICOLON))
            expr = expression();
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

    // arrays are the only Expr.Call that can be
    // assigned to w/o using a getter
    private Expr assignment() {
        // right associative
        Expr expr = or();
        if (match(EQ, PLUS_EQ, MINUS_EQ, PLUS_PLUS, MINUS_MINUS)) {
            Token op = prev();
            if (expr instanceof Expr.Variable)
                return assignVariable((Expr.Variable)expr, op);
            else if (expr instanceof Expr.Call)
                return assignCallable((Expr.Call)expr, op);
            //else if (expr instanceof Expr.Get)
            //    return new Expr.Set();
            throw error(op, "Invalid assignment target.");
        }
        return expr;
    }

    // due to the plus operator being overloaded w/ string concat,
    // added redundant plus operation in Interpreter for '+=' and '++'
    private Expr assignVariable(Expr.Variable var, Token operator) {
        Expr val;
        if (operator.type == EQ) {
            // recursion so "a = b = 1;" works in a right associative way
            // this now allows "a = b[0] = c = 1;"
            val = assignment();
            return new Expr.Assign(var.name, val);
        }
        else if (operator.type == PLUS_EQ || operator.type == MINUS_EQ)
            val = or();
        else
            val = new Expr.Literal(Double.valueOf(1.));
        return new Expr.Assign(var.name, new Expr.Binary(var, operator, val));
    }

    // mainly used for assigning to array but future proofed.
    // think "[]" and "()" followed by "=" as a sep grammar to
    // the brackets/parentheses by themselves, which is just a call.
    // In BNF notation:
    // assignCallable -> caller "="
    // caller -> callee ("(args?)" | "[ind]")
    // callee -> IDENTIFIER | caller
    private Expr assignCallable(Expr.Call caller, Token operator) {
        Expr val;
        if (operator.type == EQ) {
            // recursion so "a = b = 1;" works in a right associative way
            // this now allows "a = b[0] = c = 1;"
            val = assignment();
            return new Expr.AssignCaller(caller.callee, caller.arguments, caller.error, val);
        }
        else if (operator.type == PLUS_EQ || operator.type == MINUS_EQ)
            val = or();
        else
            val = new Expr.Literal(Double.valueOf(1.));
        return new Expr.AssignCaller(caller.callee, caller.arguments, caller.error,
                                        new Expr.Binary(caller.callee, operator, val));
    }
/*
    // pass all the params of Expr.Array instead of passing Expr.Array itself b/c
    // then evaluate(expr) in the Interpreter will return the LoxArray object
    // through evoking the visitor of Expr.Variable rather than
    // the value of the array call given by the visitor of Expr.Array
    private Expr assignArray(Expr.Array arr, Token operator) {
        Expr val;
        if (operator.type == EQ) {
            // recursion so "a = b = 1;" works in a right associative way
            // this now allows "a = b[0] = c = 1;"
            val = assignment();
            return new Expr.AssignArray(arr.callee, arr.index, arr.bracket, val);
        }
        else if (operator.type == PLUS_EQ || operator.type == MINUS_EQ)
            val = or();
        else
            val = new Expr.Literal(Double.valueOf(1.));
        return new Expr.AssignArray(arr.callee, arr.index, arr.bracket, new Expr.Binary(arr.callee, operator, val));
    }
*/
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
        // can have nested function, array, and field/method calls.
        // if not callable, caught at runtime.
        // allows exprs like "a[0]()" if a[0] holds something callable.
        while (true) {
            if (match(L_PAREN))
                expr = finishCall(expr);
            else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name.");
                expr = new Expr.Get(expr, name);
            }
            // allows multi-dim arrays even if not supported
            else if (match(L_BRACKET))
                expr = finishArrayCall(expr);
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
            }while (match(COMMA));
        }
        Token paren = consume(R_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(expr, args, paren);
    }

    private Expr finishArrayCall(Expr expr) {
        Expr index = expression();
        Token bracket = consume(R_BRACKET, "Expect ']' after array index.");
        return new Expr.Call(expr, Arrays.asList(index), bracket);
        //return new Expr.Array(expr, index, bracket);
    }

    private Expr primary() {
        if (match(IDENTIFIER)) {
            Token name = prev();
            return new Expr.Variable(name);
        }
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
                case CLASS, FOR, FN, IF, PRINT, RETURN, BREAK, VAR, ARR, WHILE:
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
