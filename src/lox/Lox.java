package lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    // prob want to access this at some point in other class
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(1);
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
        System.out.print("tokens: ");
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
        Expr expr = parser.parseTokens();
        AstPrinter printer = new AstPrinterRPN();
        System.out.println(printer.print(expr));
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.out.println("[line "+line+"] Error"+where+": "+message);
        hadError = true;
    }
}
