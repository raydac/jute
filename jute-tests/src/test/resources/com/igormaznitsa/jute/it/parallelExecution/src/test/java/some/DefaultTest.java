package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTest {

  @JUteTest(order = 0)
  public void testB() throws Exception {
    System.out.println("TestB");
  }

  @JUteTest(order = 1)
  public void testC() throws Exception {
    System.out.println("TestC");
  }

  @JUteTest(order = 1)
  public void testD() throws Exception {
    System.out.println("TestD");
  }

  @JUteTest(order = 0)
  public void testA() throws Exception {
    System.out.println("TestA");
  }

  @JUteTest(order = 1)
  public void testE() throws Exception {
    System.out.println("TestE");
  }

}
