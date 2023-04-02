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