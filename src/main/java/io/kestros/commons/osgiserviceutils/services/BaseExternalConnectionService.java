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
 * Baseline logic for external connection services. Manages timestamps for recent successful and
 * failure connections.
 */
public abstract class BaseExternalConnectionService implements ExternalConnectionService {

  private Date lastConnectionFailureDate = null;
  private String getLastConnectionFailureReason = null;
  private Date lastConnectionSuccessDate = null;

  @Override
  public void connectionSuccessful() {
    this.lastConnectionSuccessDate = new Date();
  }

  @Override
  public void connectionFailed(String reason) {
    this.getLastConnectionFailureReason = reason;
    this.lastConnectionFailureDate = new Date();
  }

  @Override
  public Date getLastSuccessfulConnection() {
    if (this.lastConnectionSuccessDate != null) {
      return new Date(this.lastConnectionSuccessDate.getTime());
    }
    return null;
  }

  @Override
  public Date getLastFailedConnection() {
    if (this.lastConnectionFailureDate != null) {
      return new Date(this.lastConnectionFailureDate.getTime());
    }
    return null;
  }

  @Override
  public String getLastFailedConnectionReason() {
    return this.getLastConnectionFailureReason;
  }

}