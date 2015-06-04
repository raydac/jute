package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTestSec {

  @Test
  public void testSecB() throws Exception {
    System.out.println("TestSecB");
  }

  @Test
  public void testSecG() throws Exception {
    System.out.println("TestSecG");
  }

  @Test
  public void testSecC() throws Exception {
    System.out.println("TestSecC");
  }

  @Test
  public void testSecD() throws Exception {
    System.out.println("TestSecD");
  }

  @Test
  public void testSecF() throws Exception {
    System.out.println("TestSecF");
  }

  @Test
  public void testSecE() throws Exception {
    System.out.println("TestSecE");
  }

  @Test
  public void testSecA() throws Exception {
    System.out.println("TestSecA");
  }

}