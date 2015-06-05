package some;

import org.junit.*;
import com.igormaznitsa.jute.annotations.*;

public abstract class AbstractTest {
  
  @BeforeClass
  public static void beforeClass(){
    System.out.println("AbsBeforeClass");
  }
  
  @AfterClass
  public static void afterClass(){
    System.out.println("AbsAfterClass");
  }
  
  @Before
  public void absbefore(){
    System.out.println("AbsBeforeTest");
  }
  
  @After
  public void absafter(){
    System.out.println("AbsAfterTest");
  }

}