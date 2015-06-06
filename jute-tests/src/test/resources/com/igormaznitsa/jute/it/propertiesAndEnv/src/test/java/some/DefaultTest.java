package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTest {
  @JUteTest(printConsole=true)
  public void testProperties() throws Exception {
    System.out.println(System.getenv("global.jute.env.property"));
    System.out.println(System.getProperty("global.jut.jvm.property"));
    System.out.println(System.getProperty("global.jute.opt.property"));
  }

}