package some;

import org.junit.*;
import static org.junit.Assert.*;

public class DefaultJUTest {

  @Test
  public void testJUA() throws Exception {
    System.out.println("JUTestA");
  }

  @Test
  @Ignore
  public void testJUB() throws Exception {
    System.out.println("JUTestB");
  }

  @Test
  public void testJUC() throws Exception {
    System.out.println("JUTestC");
  }

}