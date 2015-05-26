package some;

import org.junit.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTest2 {
  @Test
  public void test1() {
    System.out.println("Test2_Method1");
  }

  @Test
  @Ignore
  public void test2() {
    System.out.println("Ignored2_Method2");
  }

  @Test
  public void test3() {
    System.out.println("Test2_Method3");
  }

}