package eu.h2020.symbiote;

import eu.h2020.symbiote.messaging.RabbitManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RabbitManagerTests {
    @Test
    public void testPlaceholder_success() {
        RabbitManager rabbitManager = spy(new RabbitManager());
        // doReturn(null).when(rabbitManager).sendRpcMessage(any(), any(), any());

    }

}
