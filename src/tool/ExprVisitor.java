package tool;

/*
The Visitor design pattern is used to emulate functional programming in OOP.

In OOP: each time you want to implement a new functionality, you have to
implement this new method for every class.

In FOP: you just have to make 1 function that handles all "types" a.k.a classes
using pattern matching. However, the opposite is true, where, if you add a new type,
you need to modify ALL functions to handle the new type.

The Visitor interface acts like the FOP function, where a class simply implements it
and writes a visit(Class receiver) function for each "receiving" class.
Then, each receiving class just needs to implement an "accept(Visitor v)" method
that just calls v.visit(this);
 */
interface ExprVisitor {
    void visitBinary(Expr.Binary receiver);
    void visitUnary(Expr.Unary receiver);
}
