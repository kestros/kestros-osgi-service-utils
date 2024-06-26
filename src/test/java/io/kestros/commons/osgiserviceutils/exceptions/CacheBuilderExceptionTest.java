package io.kestros.commons.osgiserviceutils.exceptions;

import static org.junit.Assert.*;

import org.junit.Test;

public class CacheBuilderExceptionTest {

  @Test
  public void testCacheBuilderException() {
    CacheBuilderException exception = new CacheBuilderException("Test Message");
    assertEquals("Test Message", exception.getMessage());
  }

  @Test
  public void testCacheBuilderExceptionWithCause() {
    Exception cause = new Exception("Test Cause");
    CacheBuilderException exception = new CacheBuilderException("Test Message", cause);
    assertEquals("Test Message", exception.getMessage());
    assertEquals("Test Cause", exception.getCause().getMessage());
  }

}