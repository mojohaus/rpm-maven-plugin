package org.codehaus.mojo.rpm.it.module.jar;

public final class TestClass
{
    public TestClass()
    {
        org.apache.log4j.Logger.getLogger(getClass()).info("some message");
    }
}