package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTest {
  @JUteTest(jvm="java", printConsole=true)
  public void test1() throws Exception {
    System.out.println("TestOne");
  }

  @JUteTest(jvm = "_illegalJava")
  public void test2() throws Exception {
    System.out.println("TestTwo");
  }

}