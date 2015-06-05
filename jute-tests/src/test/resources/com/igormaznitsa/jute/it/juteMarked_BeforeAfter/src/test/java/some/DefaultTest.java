package some;

import org.junit.*;
import static org.junit.Assert.*;
import com.igormaznitsa.jute.annotations.*;

public class DefaultTest extends AbstractTest{
  
  @BeforeClass
  public static void beforeClass(){
    System.out.println("BeforeClass");
  }
  
  @AfterClass
  public static void afterClass(){
    System.out.println("AfterClass");
  }
  
  @Before
  public void before(){
    System.out.println("BeforeTest");
  }
  
  @After
  public void after(){
    System.out.println("AfterTest");
  }
  
  @JUteTest(skip=true)
  public void testSkept() {
    System.out.println("TESTIGNORED");
  }

  @JUteTest(printConsole = true)
  public void test() {
    System.out.println("TEST");
  }

  @JUteTest(printConsole=true)
  public void testFailed() {
    System.out.println("FAILEDTEST");
    fail("FAIL_TEXT");
  }

}