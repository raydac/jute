package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

@JUteTest
public class DefaultTestSec {

  public void testSecB() throws Exception {
    System.out.println("TestSecB");
  }

  public void testSecG() throws Exception {
    System.out.println("TestSecG");
  }

  public void testSecC() throws Exception {
    System.out.println("TestSecC");
  }

  public void testSecD() throws Exception {
    System.out.println("TestSecD");
  }

  public void testSecF() throws Exception {
    System.out.println("TestSecF");
  }

  public void testSecE() throws Exception {
    System.out.println("TestSecE");
  }

  public void testSecA() throws Exception {
    System.out.println("TestSecA");
  }

}