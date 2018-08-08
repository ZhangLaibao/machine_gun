```java
package java.util;

import java.util.function.Supplier;

/**
 * This class consists of static utility methods for operating on objects. These utilities include 
 * null-safe or null-tolerant methods for computing the hash code of an object, returning a string for 
 * an object, and comparing two objects.
 */
public final class Objects {

    /**
     * Returns true if the arguments are equal to each other and false otherwise. Consequently, if both arguments 
     * are null, true is returned and if exactly one argument is null, false is returned. Otherwise, equality is 
     * determined by using the Object.equals() method of the first argument.
     */
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

   /**
    * Returns true if the arguments are deeply equal to each other and false otherwise. Two null values are 
    * deeply equal. If both arguments are arrays, the algorithm in Arrays.deepEquals(Object[], Object[]) 
    * is used to determine equality. Otherwise, equality is determined by using the Object.equals() method 
    * of the first argument.
    */
    public static boolean deepEquals(Object a, Object b) {
        if (a == b)
            return true;
        else if (a == null || b == null)
            return false;
        else
            return Arrays.deepEquals0(a, b);
    }

    /** Returns the hash code of a non-null argument and 0 for a null argument */
    public static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }

   /**
    * Generates a hash code for a sequence of input values. The hash code is generated as if all the 
    * input values were placed into an array, and that array were hashed by calling Arrays.hashCode(Object[]).
    *
    * This method is useful for implementing Object.hashCode() on objects containing multiple fields. For
    * example, if an object that has three fields, x, y, and z, one could write:
    * @Override public int hashCode() {
    *     return Objects.hash(x, y, z);
    * }
    *
    * Warning: When a single object reference is supplied, the returned value does not equal the hash code of 
    * that object reference. This value can be computed by calling hashCode(Object).
    */
    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    /** Returns the result of calling toString for a non-null argument and "null" for a null argument */
    public static String toString(Object o) {
        return String.valueOf(o);
    }

    /**
     * Returns the result of calling toString() on the first argument if the first argument is not 
     * null and returns the second argument otherwise.
     */
    public static String toString(Object o, String nullDefault) {
        return (o != null) ? o.toString() : nullDefault;
    }

    /**
     * Returns 0 if the arguments are identical and  c.compare(a, b) otherwise. Consequently, if both arguments 
     * are null 0 is returned.
     *
     * Note that if one of the arguments is null, a NullPointerException may or may not be thrown depending on
     * what ordering policy the Comparator chooses to have for null values.
     */
    public static <T> int compare(T a, T b, Comparator<? super T> c) {
        return (a == b) ? 0 :  c.compare(a, b);
    }

    /**
     * Checks that the specified object reference is not null and throws a customized NullPointerException if it is. 
     * This method is designed primarily for doing parameter validation in methods and constructors with multiple 
     * parameters, as demonstrated below:
     * public Foo(Bar bar, Baz baz) {
     *     this.bar = Objects.requireNonNull(bar, "bar must not be null");
     *     this.baz = Objects.requireNonNull(baz, "baz must not be null");
     * }
     */
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        return obj;
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }
    
    /** Returns true if the provided reference is null otherwise returns false */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /** Returns true if the provided reference is non-null otherwise returns false */
    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    /**
     * Checks that the specified object reference is not null and throws a customized NullPointerException 
     * if it is. Unlike the method requireNonNull(Object, String), this method allows creation of the message 
     * to be deferred until after the null check is made. While this may confer a performance advantage in the 
     * non-null case, when deciding to call this method care should be taken that the costs of creating the 
     * message supplier are less than the cost of just creating the string message directly.
     */
    public static <T> T requireNonNull(T obj, Supplier<String> messageSupplier) {
        if (obj == null)
            throw new NullPointerException(messageSupplier.get());
        return obj;
    }
}

```