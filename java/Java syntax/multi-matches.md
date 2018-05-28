When we try-catch a code block that might raise more than one type of exception, before JDK7, we must write the code like this:
```java
public class MultiCatchersBeforeJDK7 {

    public static void main(String[] args) {
        try {
            if (new Random().nextBoolean())
                throw new IOException();
            else
                throw new ArithmeticException();

        } catch (IOException e) {
            System.out.println("catched an IOException");
        } catch (ArithmeticException e) {
            System.out.println("catched an ArithmeticException");
        }
    }
    
}
```
if there are more and more exceptions, the catch block will increase correspondingly, in JDK7, the situation is much better:
```java
public class MultiCatchersInJDK7 {

     public static void main(String[] args) {
        try {
            if (new Random().nextBoolean())
                throw new IOException();
            else
                throw new ArithmeticException();
    
         } catch (IOException | ArithmeticException e) {
            System.out.println("catched an IOException or ArithmeticException");
        }
    }
    
}
```
it's called multi-catch of try catch block, actually a candy, but there is still some limitations. For example:
```java
public class MultiCatchersLimitations {

     public static void main(String[] args) {
        try {
            if (new Random().nextBoolean())
                throw new IOException();
            else
                throw new Exception();
    
         } catch (IOException | Exception e) {
            System.out.println("catched an IOException or Exception");
        }
    }
    
}
```
the java compiler will complain that "Types in multi-catch must be disjoint: 'java.io.IOException' is a subclass of 'java.lang.Exception'"
it mean that in the catch block, the exception of subclass will be catched by the catch block who catch the exception of superclass, so whichever 
syntax you apply, the exception must be catched by the catch block clearly catches the type of that exception.
