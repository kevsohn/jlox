package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Scanner {
    private final String text;
    private final List<Token> tokens;
    private int lexStart, cur, line;
    private static final HashMap<String,TokenType> hmap;

    static {
        hmap = new HashMap<>();
        hmap.put("and", TokenType.AND);
        hmap.put("or", TokenType.OR);
        hmap.put("true", TokenType.TRUE);
        hmap.put("false", TokenType.FALSE);
        hmap.put("if", TokenType.IF);
        hmap.put("else", TokenType.ELSE);
        hmap.put("elif", TokenType.ELIF);
        hmap.put("for", TokenType.FOR);
        hmap.put("while", TokenType.WHILE);
        hmap.put("var", TokenType.VAR);
        hmap.put("class", TokenType.CLASS);
        hmap.put("fn", TokenType.FN);
        hmap.put("this", TokenType.THIS);
        hmap.put("super", TokenType.SUPER);
        hmap.put("print", TokenType.PRINT);
        hmap.put("return", TokenType.RETURN);
        hmap.put("nil", TokenType.NIL);
    }

    public Scanner(String source) {
        this.text = source;
        this.tokens = new ArrayList<>();
        this.lexStart = 0;
        this.cur = 0;
        this.line = 1;
    }

    public List<Token> scanTokens() {
        while (!atEnd()) {
            // cur should be 1 more than lexStart for substring call except at init
            // also allows for lookahead
            lexStart = cur;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        // save char then advance pointer
        char c = text.charAt(cur++);
        // check for symbolic lexemes including Strings
        switch (c) {
            // single char
            case ';': addToken(TokenType.SEMICOLON); break;
            case '.':
                if (isNumber(peek())) {
                    advanceNumbers();
                    addToken(TokenType.NUMBER, Double.parseDouble(text.substring(lexStart, cur)));
                }
                else
                    addToken(TokenType.DOT);
                break;
            case ',': addToken(TokenType.COMMA); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '*': addToken(TokenType.STAR); break;
            case '%': addToken(TokenType.PERCENT); break;
            case '^': addToken(TokenType.HAT); break;

            // potential doubles
            case '+':
                if (peek() == '+') {
                    cur++;
                    addToken(TokenType.PLUS_PLUS);
                    break;
                }
                else if (peek() == '=') {
                    cur++;
                    addToken(TokenType.PLUS_EQUAL);
                    break;
                }
                addToken(TokenType.PLUS);
                break;
            case '-':
                if (peek() == '-') {
                    cur++;
                    addToken(TokenType.MINUS_MINUS);
                    break;
                }
                else if (peek() == '=') {
                    cur++;
                    addToken(TokenType.MINUS_EQUAL);
                    break;
                }
                addToken(TokenType.MINUS);
                break;
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
            case '!': addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG); break;
            case '/':
                // if comment, advance up to newline and ignore
                // else, division
                if (match('/')) {
                    while (peek() != '\n' && !atEnd())
                        cur++;
                }
                else addToken(TokenType.SLASH);
                break;

            // has literal
            case '"':
                // ends when '"' or atEnd()
                while (peek() != '"' && !atEnd()) {
                    if (peek() == '\n') line++;
                    cur++;
                }
                if (atEnd())
                    Lox.error(line, "Unterminated string.");
                // include end quote
                cur++;
                // remove quotes for literal arg
                addToken(TokenType.STRING, text.substring(lexStart+1, cur-1));
                break;

            // whitespace
            case ' ': break;
            case '\t': break;
            case '\r': break;
            case '\n': line++; break;

            default:
                if (isNumber(c)) {
                    advanceNumbers();
                    // cur now pointing at non-number
                    if (peek() == '.' && isNumber(peekNext())) {
                        cur++; // consume '.'
                        advanceNumbers();
                    }
                    addToken(TokenType.NUMBER, Double.parseDouble(text.substring(lexStart, cur)));
                }else if (isAlpha(c)) {
                    while (isAlpha(peek()) || isNumber(peek()))
                        cur++;
                    // "max munch": always take the longest lexeme (---a => -- -a NOT - --a)
                    String literal = text.substring(lexStart, cur);
                    TokenType type = hmap.get(literal);
                    // if not a reserved keyword
                    if (type == null)
                        type = TokenType.IDENTIFIER;
                    addToken(type);
                }else
                    Lox.error(line, "Unexpected character.");
                break;
        }
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isNumber(char c) {
        // java uses utf-16 so numbers in ascending order
        return c >= '0' && c <= '9';
    }

    // advance while current char is number
    private void advanceNumbers() {
        while (isNumber(peek()))
            cur++;
    }

    // for lexemes with no literal like ';'
    private void addToken(TokenType type) {
        addToken(type,null);
    }

    private void addToken(TokenType type, Object literal) {
        String lexeme = text.substring(lexStart, cur);
        tokens.add(new Token(type, lexeme, literal, line));
    }

    // check source text exhaustion
    private boolean atEnd() {
        return cur >= text.length();
    }

    // if matching, consumes the current char and advances
    // else, do nothing
    private boolean match(char expected) {
        if (atEnd() || text.charAt(cur) != expected)
            return false;
        cur++;
        return true;
    }

    // returns the current char
    // null char used since not number
    private char peek() {
        return atEnd() ? '\0' : text.charAt(cur);
    }

    private char peekNext() {
        return cur+1 >= text.length() ? '\0' : text.charAt(cur+1);
    }
}//EOC
