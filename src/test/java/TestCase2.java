import com.fyp.qa.base.TestBase;
import com.fyp.qa.functions.TestFunction;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class TestCase2 extends TestBase {
    SoftAssert softAssert = new SoftAssert();

    //This is a testing test case
    @BeforeMethod
    public void setup(){
        System.out.println(">>> BEFOREMETHOD START");
        initialization();
        System.out.println(">>> BEFOREMETHOD END");
    }

    @Test
    public void VerifyTextInWidgetTest(){

        softAssert.assertEquals(driver.getTitle(),"Swag Labs", "Title should contain DEMOQA");
        System.out.println(">>> TEST START");
        TestFunction.userNameInput("standard_user");
        TestFunction.passwordInput("secret_sauce");
        TestFunction.clickLoginBtn();
        System.out.println(">>> TEST END");
        softAssert.assertAll();
    }

    @AfterMethod
    public void teardown(){
        //driver.quit();
    }
}
