package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTest {
  @Test
  public void test1() throws Exception {
    final StringBuilder bldr = new StringBuilder();
    while(true){
      final int val = System.in.read();
      if(val<0 || val == '$') break;
      bldr.append((char)val);
    }
    assertEquals("TerminalString", bldr.toString());
  }

  @Test
  @JUteTest(in="LocalStr$")
  public void test2() throws Exception {
    final StringBuilder bldr = new StringBuilder();
    while(true){
      final int val = System.in.read();
      if(val<0 || val == '$') break;
      bldr.append((char)val);
    }
    assertEquals("LocalStr", bldr.toString());
  }

}