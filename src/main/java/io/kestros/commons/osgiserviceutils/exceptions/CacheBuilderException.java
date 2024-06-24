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

import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import javax.annotation.Nonnull;

/**
 * Exception thrown when a {@link CacheService}
 * fails to cache a value.
 */
public class CacheBuilderException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Exception thrown when a {@link CacheService}
   * fails to cache a value.
   *
   * @param message Cause message.
   */
  public CacheBuilderException(@Nonnull final String message) {
    super(message);
  }

  /**
   * Exception thrown when a {@link CacheService}
   * fails to cache a value.
   *
   * @param message Cause message.
   * @param cause Cause of the exception.
   */
  public CacheBuilderException(@Nonnull final String message, @Nonnull final Throwable cause) {
    super(message, cause);
  }

}
