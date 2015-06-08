package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTest {

  @JUteTest
  public void testJUteA() throws Exception {
    System.out.println("JUteTestA");
  }

  @JUteTest(skip=true)
  public void testJUteB() throws Exception {
    System.out.println("JUteTestB");
  }

  @JUteTest
  public void testJUteC() throws Exception {
    System.out.println("JUteTestC");
  }

}