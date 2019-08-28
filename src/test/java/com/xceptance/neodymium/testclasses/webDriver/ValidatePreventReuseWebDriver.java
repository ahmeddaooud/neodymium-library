package com.xceptance.neodymium.testclasses.webDriver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.NeodymiumRunner;
import com.xceptance.neodymium.module.statement.browser.multibrowser.Browser;
import com.xceptance.neodymium.module.statement.browser.multibrowser.WebDriverCache;
import com.xceptance.neodymium.tests.NeodymiumTest;
import com.xceptance.neodymium.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.WebDriverUtils;

@RunWith(NeodymiumRunner.class)
public class ValidatePreventReuseWebDriver
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static WebDriver webDriver3;

    private static File tempConfigFile;

    @BeforeClass
    public static void beforeClass()
    {
        // set up a temporary neodymium.properties
        final String fileLocation = "config/temp-ValidateReuseWebDriver-neodymium.properties";
        tempConfigFile = new File("./" + fileLocation);
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        NeodymiumTest.writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);

        Assert.assertNull(webDriver1);
        Assert.assertNull(Neodymium.getDriver());
    }

    @Before
    public void before()
    {
        if (webDriver1 == null)
        {
            webDriver1 = Neodymium.getDriver();
        }
        else if (webDriver2 == null)
        {
            webDriver2 = Neodymium.getDriver();
        }
        else if (webDriver3 == null)
        {
            webDriver3 = Neodymium.getDriver();
        }
        else
        {
            Assert.assertNotNull(Neodymium.getDriver());
        }
        Assert.assertNotNull(webDriver1);
    }

    @Test
    @Browser("Chrome_headless")
    public void test1()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        Assert.assertNotNull(webDriver1);
        Assert.assertNotEquals(webDriver1, webDriver2);

        Assert.assertEquals(0, WebDriverCache.instance.getAllWebdriver().size());
    }

    @Test
    @Browser("Chrome_headless")
    public void test2()
    {
        Assert.assertEquals(webDriver2, Neodymium.getDriver());
        Assert.assertNotNull(webDriver1);
        Assert.assertNotNull(webDriver2);
        Assert.assertNotEquals(webDriver1, webDriver2);

        Assert.assertEquals(0, WebDriverCache.instance.getAllWebdriver().size());
    }

    @Test
    @Browser("Chrome_headless")
    public void test3()
    {
        Assert.assertEquals(webDriver3, Neodymium.getDriver());
        Assert.assertNotNull(webDriver1);
        Assert.assertNotNull(webDriver2);
        Assert.assertNotNull(webDriver3);
        Assert.assertNotEquals(webDriver1, webDriver2);
        Assert.assertEquals(webDriver2, webDriver3);

        Assert.assertEquals(0, WebDriverCache.instance.getAllWebdriver().size());
    }

    @After
    public void after()
    {
        if (webDriver2 == null)
        {
            WebDriverUtils.preventReuseAndTearDown();
        }
    }

    @AfterClass
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);
        Assert.assertEquals(1, WebDriverCache.instance.getAllWebdriver().size());

        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
