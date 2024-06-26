package io.kestros.commons.osgiserviceutils.exceptions;

import static org.junit.Assert.*;

import org.junit.Test;

public class CachePurgeExceptionTest {

  @Test
  public void testCachePurgeException() {
    CachePurgeException exception = new CachePurgeException("Test Message");
    assertEquals("Test Message", exception.getMessage());
  }

  @Test
  public void testCachePurgeExceptionWithCause() {
    Exception cause = new Exception("Test Cause");
    CachePurgeException exception = new CachePurgeException("Test Message", cause);
    assertEquals("Test Message", exception.getMessage());
    assertEquals("Test Cause", exception.getCause().getMessage());
  }

}