package com.xceptance.neodymium;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.JUnit4;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.xceptance.neodymium.module.EnhancedMethod;
import com.xceptance.neodymium.module.StatementBuilder;
import com.xceptance.neodymium.module.order.DefaultStatementRunOrder;
import com.xceptance.neodymium.module.statement.browser.multibrowser.Browser;
import com.xceptance.neodymium.util.Context;

/**
 * This class executes {@link JUnit4} test classes (aka JUnit Runner) and adds several features to test execution e.g.
 * multi {@link Browser browser} and
 * <a href="https://github.com/Xceptance/neodymium-library/wiki/Test-data-provider">test data</a>. Vanilla JUnit
 * parameterized tests are supported as well but only with parameter injection (as described here: <a href=
 * "https://github.com/junit-team/junit4/wiki/parameterized-tests#using-parameter-for-field-injection-instead-of-constructor">Using @Parameter
 * for Field injection instead of Constructor</a>). In order to run a {@link JUnit4} test with this runner the class or
 * its super-class has to be annotated with {@link RunWith}
 * <p>
 * <b>Example</b>
 * 
 * <pre>
 * &#64;RunWith(NeodymiumRunner.class)
 * public class MyTests
 * {
 *     &#64;Test
 *     public void testMethod()
 *     {
 *     }
 * }
 * </pre>
 * 
 * <b>Example</b>
 * 
 * <pre>
 * public class MyTests extends BaseTestClass
 * {
 *     &#64;Test
 *     public void testMethod()
 *     {
 *     }
 * }
 * 
 * &#64;RunWith(NeodymiumRunner.class)
 * public class BaseTestClass
 * {
 * }
 * </pre>
 * 
 * @author m.kaufmann
 */
public class NeodymiumRunner extends BlockJUnit4ClassRunner
{
    public NeodymiumRunner(Class<?> klass) throws InitializationError
    {
        super(klass);
    }

    public enum DescriptionMode
    {
     flat,
     tree,
    };

    private List<FrameworkMethod> computedTestMethods;

    private Map<FrameworkMethod, Description> childDescriptions;

    private Description globalTestDescription;

    private Object testClassInstance;

    @Override
    protected Statement methodBlock(FrameworkMethod method)
    {
        // This will build a default JUnit statement which runs excactly one of the test methods including before/after
        // methods. This call will also create the test class instance in which this method will be invoked.
        Statement methodStatement = super.methodBlock(method);
        // We need this particular test class instance for our own statements but we can not access it from here.
        // We can get the instance by having createTest overridden, see implementation of createTest in this class as
        // well as in BlockJUnit4ClassRunner.

        // At this point our createTest implementation was called and we have the testClassInstance

        if (method instanceof EnhancedMethod)
        {
            EnhancedMethod m = (EnhancedMethod) method;
            for (int i = m.getBuilder().size() - 1; i >= 0; i--)
            {
                StatementBuilder statementBuilder = m.getBuilder().get(i);
                Object data = m.getData().get(i);
                methodStatement = statementBuilder.createStatement(testClassInstance, methodStatement, data);
            }
        }
        else if (method instanceof FrameworkMethod)
        {
            // This could happen if there are plain test methods in the class with no data sets or test data defined.
            // Also the SuppressDataSets annotation can degrade a method to an FrameworkMethod even if there is some
            // test data or data sets.

            // It's fine, just make sure super.methodBlock is called with this method and return the resulting statement
        }

        return methodStatement;
    }

    @Override
    protected Object createTest() throws Exception
    {
        // Very important code which will be called from super's methodBlock function (see our methodBlock)
        // The super's call creates the instance of the class to test which will be a new one for each method.
        // Since we need this particular instance for our own statements we need to save it for us.
        // Keep in mind that this method will be called for every method that will be returned from computeTestMethods.
        // So there is not the one and only test class instance. Its one instance per method.
        this.testClassInstance = super.createTest();
        return testClassInstance;
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods()
    {
        // Normally JUnit works with all methods that are annotated with @Test, see super's implementation
        // But we override this function in order to do all the fancy stuff, like method multiplication and so on.
        // So we basically start with the list of test methods and add and rearrange new one's to this list and JUnit
        // will take this list and call us for each entry to create a statement which actually does all the stuff.
        // Each entry of this list causes a call to methodBlock().

        // Since this method is called at least two times and is somewhat expensive, we cache the result
        if (computedTestMethods != null)
            return computedTestMethods;

        // That list will contain all methods that need to be run for the class
        List<FrameworkMethod> testMethods = new LinkedList<>();

        // Statement run order defines the order of our own statements that will surround the default JUnit statement
        // from methodBlock
        List<Class<? extends StatementBuilder>> statementRunOrder = new DefaultStatementRunOrder().getRunOrder();

        // super.computeTestMethods will return all methods that are annotated with @Test
        for (FrameworkMethod testAnnotatedMethod : super.computeTestMethods())
        {
            // these lists contain all the builders and data that will be responsible for a partiuclar method
            List<StatementBuilder> builderList = new LinkedList<>();
            List<List<Object>> builderDataList = new LinkedList<>();

            // iterate over statements defined in the order
            for (Class<? extends StatementBuilder> statementClass : statementRunOrder)
            {
                // ask each statement builder if this method should be processed
                // results in a list of parameters for this statement for method multiplication
                // e.g. for @Browser({"A", "B"}) the data list will contain "A" and "B"

                StatementBuilder builder = StatementBuilder.instantiate(statementClass);

                List<Object> iterationData = null;
                try
                {
                    iterationData = builder.createIterationData(getTestClass(), testAnnotatedMethod);
                }
                catch (Throwable e)
                {
                    throw new RuntimeException(e);
                }

                // Avoid empty entries in the list since its a hassle to deal with
                if (iterationData != null && !iterationData.isEmpty())
                {
                    // we save both, the builder instance as well as the "data" to run with
                    builderList.add(builder);
                    builderDataList.add(iterationData);
                }
            }

            // This is the point where multiple test methods are computed for the current processed method.
            testMethods.addAll(buildCrossProduct(testAnnotatedMethod.getMethod(), builderList, builderDataList));
        }

        // this list is now final for class execution so make it unmodifiable
        computedTestMethods = Collections.unmodifiableList(testMethods);

        // compute test descriptions
        childDescriptions = new HashMap<>();
        globalTestDescription = createTestDescriptions(computedTestMethods);

        return computedTestMethods;
    }

    public Description createTestDescriptions(List<FrameworkMethod> methods)
    {
        switch (Context.get().configuration.junitViewMode())
        {
            case tree:
                return createHierarchicalTestDescription(methods);

            case flat:
            default:
                return createFlatTestDescription(methods);
        }
    }

    private Description createHierarchicalTestDescription(List<FrameworkMethod> methods)
    {
        Description hierarchicalDescription = Description.createSuiteDescription(getTestClass().getJavaClass());

        for (FrameworkMethod method : methods)
        {
            Description currentLevel = hierarchicalDescription;
            if (method instanceof EnhancedMethod)
            {
                EnhancedMethod enhancedMethod = (EnhancedMethod) method;
                List<StatementBuilder> statementBuilder = enhancedMethod.getBuilder();
                List<Object> builderData = enhancedMethod.getData();

                for (int i = 0; i < statementBuilder.size(); i++)
                {
                    StatementBuilder builder = statementBuilder.get(i);
                    String testName = builder.getTestName(builderData.get(i));

                    // check if hierarchical description has a child with that description
                    ArrayList<Description> currentLevelChildren = currentLevel.getChildren();
                    boolean found = false;
                    for (Description currentLevelChild : currentLevelChildren)
                    {
                        if (testName.equals(currentLevelChild.getDisplayName()))
                        {
                            found = true;
                            currentLevel = currentLevelChild;
                            break;
                        }
                    }

                    // create one if it's missing and set the new one as the current level, then dig deeper
                    if (!found)
                    {
                        Description newChild = Description.createSuiteDescription(testName);
                        currentLevel.addChild(newChild);
                        currentLevel = newChild;
                    }

                }
                // finally add the test method to lowest level
                currentLevel.addChild(describeChild(method));
            }
            else
            {
                // it's just a default JUnit method, just add it as child
                hierarchicalDescription.addChild(describeChild(method));
            }
        }

        return hierarchicalDescription;
    }

    private Description createFlatTestDescription(List<FrameworkMethod> methods)
    {
        Description flatDescription = Description.createSuiteDescription(getTestClass().getJavaClass());

        for (FrameworkMethod method : methods)
        {
            flatDescription.addChild(describeChild(method));
        }

        return flatDescription;
    }

    @Override
    protected Description describeChild(FrameworkMethod method)
    {
        return childDescriptions.computeIfAbsent(method, (m) -> {
            return Description.createTestDescription(getTestClass().getJavaClass(), m.getName());
        });
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier)
    {
        // clear the context before next child run
        Context.clearThreadContext();
        super.runChild(method, notifier);
    }

    private List<FrameworkMethod> buildCrossProduct(Method method, List<StatementBuilder> builderList, List<List<Object>> builderDataList)
    {
        List<FrameworkMethod> resultingMethods = new LinkedList<>();
        recursiveBuildCrossProduct(method, builderList, builderDataList, 0, resultingMethods, null);
        return resultingMethods;
    }

    private void recursiveBuildCrossProduct(Method method, List<StatementBuilder> builderList, List<List<Object>> builderDataList,
                                            int currentIndex, List<FrameworkMethod> resultingMethods, EnhancedMethod actualFrameworkMethod)
    {
        if (builderList.isEmpty())
        {
            // if there is no enclosing statement involved we handle it as single method call
            resultingMethods.add(new FrameworkMethod(method));
        }
        else
        {
            StatementBuilder builder = builderList.get(currentIndex);
            List<Object> builderData = builderDataList.get(currentIndex);

            for (Object data : builderData)
            {
                EnhancedMethod newMethod = new EnhancedMethod(method);
                if (actualFrameworkMethod != null)
                {
                    newMethod.getBuilder().addAll(actualFrameworkMethod.getBuilder());
                    newMethod.getData().addAll(actualFrameworkMethod.getData());
                }
                newMethod.getBuilder().add(builder);
                newMethod.getData().add(data);

                if (currentIndex < builderList.size() - 1)
                {
                    recursiveBuildCrossProduct(method, builderList, builderDataList, currentIndex + 1, resultingMethods, newMethod);
                }
                else
                {
                    resultingMethods.add(newMethod);
                }
            }
        }
    }

    @Override
    public Description getDescription()
    {
        return globalTestDescription;
    }

    @Override
    protected String testName(FrameworkMethod method)
    {
        return method.getName();
    }
}
