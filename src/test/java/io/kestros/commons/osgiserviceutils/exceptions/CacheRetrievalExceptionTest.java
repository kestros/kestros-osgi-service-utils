package io.kestros.commons.osgiserviceutils.services.exceptions;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class CacheRetrievalExceptionTest {

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testCacheRetrievalException() {
    assertEquals("cache retrieval exception", new CacheRetrievalException("cache retrieval exception").getMessage());
  }
}