package lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(65);
        }
        else if (args.length == 1)
            runFile(args[0]);
        else
            runPrompt();
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        System.out.print("lexer: ");
        for (Token t: tokens)
            System.out.print(t.lexeme+" ");
        System.out.println();

        /*ParserRPN parser = new ParserRPN(tokens);
        List<Token> output = parser.parseTokens();
        System.out.print("RPN: ");
        for (Token t: output) {
            System.out.print(t.lexeme+" ");
        }
        System.out.println();*/

        Parser parser = new Parser(tokens);
        Expr expr = parser.parse();
        if (hadError) return;
        System.out.println("parser: "+new AstPrinter().print(expr));

        interpreter.interpret(expr);
    }

    static void runtimeError(RuntimeError e) {
        System.err.println(e.getMessage()+"\n[line "+e.token.line+"]");
        hadRuntimeError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF)
            report(token.line,"at end ",message);
        else
            report(token.line, "at '"+token.lexeme+"' ", message);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.out.println("[line "+line+"] Error "+where+": "+message);
        hadError = true;
    }
}
