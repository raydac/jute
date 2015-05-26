package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

@JUteTest(jvmOpts={"-Dsuper.global1=aaa","-Dsuper.global2=bbb"})
public class DefaultTest {
  @Test
  public void test1() throws Exception {
    assertEquals("hello",System.getProperty("global.param1"));
    assertEquals("hello2",System.getProperty("global.param2"));
    assertEquals("aaa",System.getProperty("super.global1"));
    assertEquals("bbb",System.getProperty("super.global2"));
    assertNull(System.getProperty("local.param1"));
    assertNull(System.getProperty("local.param2"));
  }

  @Test
  @JUteTest(jvmOpts={"-Dlocal.param1=local_hello","-Dlocal.param2=local_hello2"})
  public void test2() throws Exception {
    assertEquals("hello", System.getProperty("global.param1"));
    assertEquals("hello2", System.getProperty("global.param2"));
    assertEquals("local_hello",System.getProperty("local.param1"));
    assertEquals("local_hello2",System.getProperty("local.param2"));
    assertEquals("aaa", System.getProperty("super.global1"));
    assertEquals("bbb", System.getProperty("super.global2"));
  }

}