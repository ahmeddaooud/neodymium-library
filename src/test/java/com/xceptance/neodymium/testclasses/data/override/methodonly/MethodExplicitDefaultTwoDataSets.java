package com.xceptance.neodymium.testclasses.data.override.methodonly;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.NeodymiumRunner;
import com.xceptance.neodymium.module.statement.testdata.Testdata;

@RunWith(NeodymiumRunner.class)
public class MethodExplicitDefaultTwoDataSets
{
    @Test
    @Testdata(-1)
    public void test1() throws Exception
    {

    }
}
