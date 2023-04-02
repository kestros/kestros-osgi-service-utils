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
}