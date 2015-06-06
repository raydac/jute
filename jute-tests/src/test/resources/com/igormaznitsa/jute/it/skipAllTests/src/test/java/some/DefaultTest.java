package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;


@JUteTest
public class DefaultTest {

  @Test
  @JUteTest
  public void testB() throws Exception {
    System.out.println("TestB");
  }

  @Test
  @JUteTest
  public void testG() throws Exception {
    System.out.println("TestG");
  }

  @Test
  @JUteTest
  public void testC() throws Exception {
    System.out.println("TestC");
  }

  @Test
  @JUteTest
  public void testA2() throws Exception {
    System.out.println("TestA2");
  }

  @Test
  @JUteTest
  public void testD() throws Exception {
    System.out.println("TestD");
  }

  @Test
  @JUteTest
  public void testF() throws Exception {
    System.out.println("TestF");
  }

  @Test
  @JUteTest
  public void testE() throws Exception {
    System.out.println("TestE");
  }

  @Test
  @JUteTest
  public void testA1() throws Exception {
    System.out.println("TestA1");
  }

}