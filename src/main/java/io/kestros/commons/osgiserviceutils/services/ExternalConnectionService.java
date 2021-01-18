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

package io.kestros.commons.osgiserviceutils.services;

import java.util.Date;

/**
 * Interface for services which will make connections to external services.
 */
public interface ExternalConnectionService extends ManagedService {

  /**
   * Register a successful connection.
   */
  void connectionSuccessful();

  /**
   * Register a failed connection.
   *
   * @param reason Reason the connection failed.
   */
  void connectionFailed(String reason);

  /**
   * Date of the last success connection to the endpoint.
   *
   * @return Date of the last success connection to the endpoint.
   */
  Date getLastSuccessfulConnection();

  /**
   * Date of the last failed connection to the endpoint.
   *
   * @return Date of the last failed connection to the endpoint.
   */
  Date getLastFailedConnection();

  /**
   * Reason for the last failed connection.
   *
   * @return Reason for the last failed connection.
   */
  String getLastFailedConnectionReason();

}
