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

import org.apache.felix.hc.api.FormattingResultLog;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import static org.junit.Assert.*;

public class BaseExternalConnectionServiceTest {


    BaseExternalConnectionService connectionService;
    @Before
    public void setUp() throws Exception {
         connectionService = new BaseExternalConnectionService() {
             @Override
             public String getDisplayName() {
                 return "Connection service";
             }

             @Override
             public void activate(ComponentContext componentContext) {

             }

             @Override
             public void deactivate(ComponentContext componentContext) {

             }

             @Override
             public void runAdditionalHealthChecks(FormattingResultLog log) {

             }
         };
    }

    @Test
    public void connectionSuccessful() {
        connectionService.connectionSuccessful();
        assertNotNull(connectionService.getLastSuccessfulConnection());
        assertNull(connectionService.getLastFailedConnection());
    }

    @Test
    public void connectionFailed() {
        connectionService.connectionFailed("Failure.");
        assertNull(connectionService.getLastSuccessfulConnection());
        assertNotNull(connectionService.getLastFailedConnection());
    }

    @Test
    public void getLastSuccessfulConnection() {
    }

    @Test
    public void getLastFailedConnection() {
    }

    @Test
    public void getLastFailedConnectionReason() {
    }
}