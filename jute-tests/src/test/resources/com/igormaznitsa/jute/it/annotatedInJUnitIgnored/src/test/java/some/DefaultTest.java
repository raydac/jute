package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

@Ignore
public class DefaultTest {
  
  @Test
  @Ignore
  @JUteTest(printConsole=true)
  public void testOne() throws Exception {
    System.out.println("TestOne");
  }

  @Test
  @Ignore
  @JUteTest(printConsole=true)
  public void testTwo() throws Exception {
    System.out.println("TestTwo");
  }

}