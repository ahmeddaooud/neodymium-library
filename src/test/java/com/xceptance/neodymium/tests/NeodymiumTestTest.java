package com.xceptance.neodymium.tests;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class NeodymiumTestTest extends NeodymiumTest
{
    @Rule
    public TestName name = new TestName();

    @Test
    public void testCheckFailedOneFromOne() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final String errorMessage = "This is RuntimeException 1";
        Result result = createResult(1, 0, new HashMap<String, Throwable>()
        {
            {
                this.put(name.getMethodName() + "1", new RuntimeException(errorMessage));
            }
        });
        checkFail(result, 1, 0, 1, errorMessage);
    }

    @Test
    public void testCheckFailedOneFromTwo() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final String errorMessage = "This is RuntimeException 1";
        Result result = createResult(2, 0, new HashMap<String, Throwable>()
        {
            {
                this.put(name.getMethodName() + "1", new RuntimeException("This is RuntimeException 1"));
            }
        });
        checkFail(result, 2, 0, 1, errorMessage);
    }

    @Test
    public void testCheckFailedTwoFromTwoNumber() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Result result = createResult(2, 0, new HashMap<String, Throwable>()
        {
            {
                this.put(name.getMethodName() + "1", new RuntimeException("This is RuntimeException 1"));
                this.put(name.getMethodName() + "2", new RuntimeException("This is RuntimeException 1"));
            }
        });

        // no assertion for failure message, as one is only possible for single fail
        checkFail(result, 2, 0, 2);
    }

    @Test
    public void testCheckFailedTwoFromTwoOneFailureMessage() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Result result = createResult(2, 0, new HashMap<String, Throwable>()
        {
            {
                this.put(name.getMethodName() + "1", new RuntimeException("This is RuntimeException 1"));
                this.put(name.getMethodName() + "2", new RuntimeException("This is RuntimeException 1"));
            }
        });

        // no assertion for failure message, as one is only possible for single fail
        checkFail(result, 2, 0, 2, "This is RuntimeException 1");
    }

    @Test
    public void testCheckFailedTwoFromTwoTwoFailureMessages() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Result result = createResult(2, 0, new HashMap<String, Throwable>()
        {
            {
                this.put(name.getMethodName() + "1", new RuntimeException("This is RuntimeException 1"));
                this.put(name.getMethodName() + "2", new RuntimeException("This is RuntimeException 2"));
            }
        });
        HashMap<String, String> expectedFailureMessages = new HashMap<String, String>()
        {
            {
                this.put(name.getMethodName() + "1", "This is RuntimeException 1");
                this.put(name.getMethodName() + "2", "This is RuntimeException 2");
            }
        };
        // no assertion for failure message, as one is only possible for single fail
        checkFail(result, 2, 0, 2, expectedFailureMessages);
    }

    @Test
    public void testCheckOneFailedOneIgnoredFromTwo() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final String errorMessage = "This is RuntimeException 1";
        Result result = createResult(2, 1, new HashMap<String, Throwable>()
        {
            {
                this.put(name.getMethodName() + "1", new RuntimeException(errorMessage));
            }
        });
        checkFail(result, 2, 1, 1, errorMessage);
    }

    // ------ checkPass-----------
    @Test
    public void testCheckPassedOneFromOne() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Result result = createResult(1, 0, null);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCheckPassedTwoFromTwo() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Result result = createResult(2, 0, null);
        checkPass(result, 2, 0);
    }

    @Test
    public void testCheckOnePassedOneIgnoredFromTwo() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Result result = createResult(2, 1, null);
        checkPass(result, 2, 1);
    }

    private Result createResult(int runCount, int ignoreCount, Map<String, Throwable> failureCauses)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Result result = new Result();
        Field countField = Result.class.getDeclaredField("count");
        Field ignoreCountField = Result.class.getDeclaredField("ignoreCount");
        Field failuresField = Result.class.getDeclaredField("failures");

        countField.setAccessible(true);
        ignoreCountField.setAccessible(true);
        failuresField.setAccessible(true);

        countField.set(result, new AtomicInteger(runCount));
        ignoreCountField.set(result, new AtomicInteger(ignoreCount));

        if (failureCauses != null)
        {
            CopyOnWriteArrayList<Failure> failures = new CopyOnWriteArrayList<>();
            for (String testMethodName : failureCauses.keySet())
            {
                Failure failure = new Failure(Description.createTestDescription(getClass(), testMethodName), failureCauses.get(testMethodName));
                failures.add(failure);
            }
            failuresField.set(result, failures);
        }
        countField.setAccessible(false);
        ignoreCountField.setAccessible(false);
        failuresField.setAccessible(false);
        return result;
    }
}
