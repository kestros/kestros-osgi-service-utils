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

package io.kestros.commons.osgiserviceutils.healthchecks;

import io.kestros.commons.osgiserviceutils.services.ManagedService;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.Result;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import static org.junit.Assert.*;

public class BaseManagedServiceHealthCheckTest {

    private BaseManagedServiceHealthCheck healthCheckService;

    private ManagedService service;

    @Before
    public void setUp() throws Exception {
        service = new ManagedService() {
            @Override
            public String getDisplayName() {
                return "Managed Service";
            }

            @Override
            public void activate(ComponentContext componentContext) {

            }

            @Override
            public void deactivate(ComponentContext componentContext) {

            }

            @Override
            public void runAdditionalHealthChecks(FormattingResultLog log) {
                log.info(getDisplayName());
            }
        };

        healthCheckService = new BaseManagedServiceHealthCheck() {
            @Override
            public ManagedService getManagedService() {
                return service;
            }

            @Override
            public String getServiceName() {
                return "Sample Service Health Check";
            }
        };
    }

    @Test
    public void execute() {
        assertEquals(Result.Status.OK, healthCheckService.execute().getStatus());
    }

    @Test
    public void executeWhenManagedServiceIsNull() {
        healthCheckService = new BaseManagedServiceHealthCheck() {
            @Override
            public ManagedService getManagedService() {
                return null;
            }

            @Override
            public String getServiceName() {
                return "Unavailable Service";
            }
        };

        Result result = healthCheckService.execute();
        assertNotNull(result);
        assertEquals(Result.Status.CRITICAL, result.getStatus());
    }

    @Test
    public void executeWithHealthCheckFailures() {
        service = new ManagedService() {
            @Override
            public String getDisplayName() {
                return "Failing Service";
            }

            @Override
            public void activate(ComponentContext componentContext) {

            }

            @Override
            public void deactivate(ComponentContext componentContext) {

            }

            @Override
            public void runAdditionalHealthChecks(FormattingResultLog log) {
                log.warn("Service health check failed");
            }
        };

        healthCheckService = new BaseManagedServiceHealthCheck() {
            @Override
            public ManagedService getManagedService() {
                return service;
            }

            @Override
            public String getServiceName() {
                return "Failing Service Health Check";
            }
        };

        Result result = healthCheckService.execute();
        assertNotNull(result);
        // Status should reflect the warning from health check
        assertNotEquals(Result.Status.CRITICAL, result.getStatus());
    }

    @Test
    public void getServiceNameReturnsCorrectValue() {
        assertEquals("Sample Service Health Check", healthCheckService.getServiceName());
    }

    @Test
    public void getManagedServiceReturnsCorrectValue() {
        assertNotNull(healthCheckService.getManagedService());
        assertEquals("Managed Service", healthCheckService.getManagedService().getDisplayName());
    }
}