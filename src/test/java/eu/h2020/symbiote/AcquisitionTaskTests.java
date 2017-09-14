package eu.h2020.symbiote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyTaskInfo;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.messaging.consumers.SingleReadingRequestedConsumer;
import eu.h2020.symbiote.model.AcquisitionTask;
import eu.h2020.symbiote.model.AcquisitionTaskDescription;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import eu.h2020.symbiote.security.TokenManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AcquisitionTaskTests {

    public static final String RESOURCE_ID = "resource1";
    public static final String RESOURCE_URL = "http://someurl";
    public static final String TASK_ID = "1";
    RabbitManager rabbitManager;
    RestTemplate restTemplate;
    TokenManager tokenManager;
    AcquisitionManager acquisitionManager;
    AcquisitionTaskDescriptionRepository repository;

    @Before
    public void setUp() {
        rabbitManager = Mockito.mock(RabbitManager.class);
        restTemplate = Mockito.mock(RestTemplate.class);
        tokenManager = Mockito.mock(TokenManager.class);
        acquisitionManager = Mockito.mock(AcquisitionManager.class);
        repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("username");
        factory.setPassword("password");
    }


//    @Test
//    public void testAcquisitionTaskAttemptsToReadData() {
//        AcquisitionTaskDescription description = new AcquisitionTaskDescription();
//        description.set
//        new AcquisitionTask(description,restTemplate,acquisitionManager,tokenManager);
//
//    }

    @Test
    public void testSingleAcquisitionConsumer() {
        Channel channel = Mockito.mock(Channel.class);
        SingleReadingRequestedConsumer consumer = new SingleReadingRequestedConsumer(channel, acquisitionManager);
        Envelope envelope = Mockito.mock(Envelope.class);
        AMQP.BasicProperties properties = new AMQP.BasicProperties();


        PlatformProxyTaskInfo testTaskInfo = getTestTaskInfo();
        ObjectMapper mapper = new ObjectMapper();
        try {
            consumer.handleDelivery("consumerTag", envelope, properties, mapper.writeValueAsBytes(testTaskInfo));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Error should not happen in handle delivery");
        }
        try {
            ArgumentCaptor<byte[]> argumentByteArray = ArgumentCaptor.forClass(byte[].class);
            verify(channel,times(1)).basicPublish(anyString(),anyString(),any(),argumentByteArray.capture());
            assertNotNull(argumentByteArray);

            ArgumentCaptor<PlatformProxyResourceInfo> argumentResInfo = ArgumentCaptor.forClass(PlatformProxyResourceInfo.class);
            verify(acquisitionManager,times(1)).getObservationForResource(argumentResInfo.capture());
            PlatformProxyResourceInfo resInfo = argumentResInfo.getValue();
            assertEquals("Resource id of the internal call must be equal", RESOURCE_ID, resInfo.getResourceId() );
            assertEquals("Resource url of the internal call must be equal", RESOURCE_URL, resInfo.getAccessURL() );

            //Try to deserialize and check values
            EnablerLogicDataAppearedMessage response = mapper.readValue(argumentByteArray.getValue(), EnablerLogicDataAppearedMessage.class);
            assertEquals("Task ids must be equal", TASK_ID, response.getTaskId());

        } catch (IOException e) {
            e.printStackTrace();
            fail("Mock channel should not have any errors");
        }
    }

    private PlatformProxyTaskInfo getTestTaskInfo() {
        PlatformProxyTaskInfo info = new PlatformProxyTaskInfo();
        info.setQueryInterval_ms(-1l);
        info.setEnablerLogicName("EL");
        PlatformProxyResourceInfo resDesc = new PlatformProxyResourceInfo();
        resDesc.setAccessURL(RESOURCE_URL);
        resDesc.setResourceId(RESOURCE_ID);
        info.setResources(Arrays.asList(resDesc));
        info.setTaskId(TASK_ID);

        return info;
    }

}