/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.kestros.commons.osgiserviceutils.exceptions;

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

  @Test
  public void testCacheRetrievalExceptionWithCause() {
    Throwable cause = new Throwable();
    CacheRetrievalException exception = new CacheRetrievalException("cache retrieval exception", cause);
    assertEquals("cache retrieval exception", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testCacheRetrievalExceptionWithCauseOnly() {
    Throwable cause = new Exception("cause message");
    CacheRetrievalException exception = new CacheRetrievalException("Wrapped Exception", cause);
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testCacheRetrievalExceptionWithEmptyMessage() {
    CacheRetrievalException exception = new CacheRetrievalException("");
    assertEquals("", exception.getMessage());
  }

  @Test
  public void testCacheRetrievalExceptionWithNullCause() {
    CacheRetrievalException exception = new CacheRetrievalException("message", null);
    assertEquals("message", exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  public void testCacheRetrievalExceptionIsException() {
    CacheRetrievalException exception = new CacheRetrievalException("message");
    assertTrue(exception instanceof Exception);
  }

}