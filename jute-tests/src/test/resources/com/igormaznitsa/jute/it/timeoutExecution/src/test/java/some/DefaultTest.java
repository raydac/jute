package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTest {

  @JUteTest(timeout = 2000L)
  public void testB() throws Exception {
    System.out.println("TestB");
    Thread.sleep(5000L);
  }

  @JUteTest(timeout = 5000L)
  public void testC() throws Exception {
    System.out.println("TestC");
    Thread.sleep(2000L);
  }

  @JUteTest(timeout = 2000L)
  public void testA() throws Exception {
    System.out.println("TestA");
  }

}
