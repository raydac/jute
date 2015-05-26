package some;

import org.junit.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTest {
  @JUteTest
  public void test1() {
    System.out.println("Test_Method1");
  }

  @JUteTest(skip=true)
  public void test2() {
    System.out.println("Ignored_Method2");
  }

  @JUteTest
  public void test3() {
    System.out.println("Test_Method3");
  }

}