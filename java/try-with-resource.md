Before Java SE7, when we use try-catch, we use a finally block to ensure that codes will be surely executed. Like this:
```java
class TryCatchBeforeJDK7 {
    public void readFileToConsole(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = in.read(bytes)) != -1) {
                System.out.println(new String(bytes, 0, len));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
             if (null != in) {
                 try {
                     in.close();
                 } catch (IOException ignored) { }
             }
        }
    }	
}
```
We have to write the finally block to close the InputStream, and have to try-catch the close() method again. In JDK 7, the situation is much better:
```java
class TryCatchWhenJDK7 {
    public void readFileToConsole(File file) {
        try (InputStream in =new FileInputStream(file)){
            byte[] bytes = new byte[1024];
            int len;
            while ((len = in.read(bytes)) != -1) {
                System.out.println(new String(bytes, 0, len));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }	
}
```
We can read the source code of JDK 6 and 7 to see what's the difference in InputStream supports the try-wth-resource syntax:
```
public class FileInputStream extends InputStream
    \-- public abstract class InputStream implements Closeable 
        \-- public interface Closeable extends AutoCloseable
            \-- public interface AutoCloseable {
                    void close() throws Exception;
                }
```
so it's the AutoClosable interface that enables the try-with-resource block to close the resources whether there is any exception or not.