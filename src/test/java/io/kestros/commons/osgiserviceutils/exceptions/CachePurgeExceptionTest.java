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

  @Test
  public void testCachePurgeExceptionWithCauseOnly() {
    Exception cause = new Exception("Cause");
    CachePurgeException exception = new CachePurgeException("Wrapped Exception", cause);
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void testCachePurgeExceptionWithEmptyMessage() {
    CachePurgeException exception = new CachePurgeException("");
    assertEquals("", exception.getMessage());
  }

  @Test
  public void testCachePurgeExceptionIsException() {
    CachePurgeException exception = new CachePurgeException("Test");
    assertTrue(exception instanceof Exception);
  }

  @Test
  public void testCachePurgeExceptionWithNullCause() {
    CachePurgeException exception = new CachePurgeException("message", null);
    assertEquals("message", exception.getMessage());
    assertNull(exception.getCause());
  }

}