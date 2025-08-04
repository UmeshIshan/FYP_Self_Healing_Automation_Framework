import com.fyp.qa.base.TestBase;
import com.fyp.qa.functions.TestFunction;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestCase1 extends TestBase {

    //This is a testing test case
    @BeforeMethod
    public void setup(){
        initialization();
    }

    @Test
    public void VerifyTextInWidgetTest(){
        TestFunction.navigateToWidgets();
    }

    @AfterMethod
    public void teardown(){

    }

}
