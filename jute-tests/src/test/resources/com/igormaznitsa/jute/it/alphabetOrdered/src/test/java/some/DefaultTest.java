package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

@JUteTest(order=0)
public class DefaultTest {

  @JUteTest
  public void testB() throws Exception {
    System.out.println("TestB");
  }

  @JUteTest
  public void testG() throws Exception {
    System.out.println("TestG");
  }

  @JUteTest
  public void testC() throws Exception {
    System.out.println("TestC");
  }

  @JUteTest
  public void testA2() throws Exception {
    System.out.println("TestA2");
  }

  @JUteTest
  public void testD() throws Exception {
    System.out.println("TestD");
  }

  @JUteTest
  public void testF() throws Exception {
    System.out.println("TestF");
  }

  @JUteTest
  public void testE() throws Exception {
    System.out.println("TestE");
  }

  @JUteTest
  public void testA1() throws Exception {
    System.out.println("TestA1");
  }

}