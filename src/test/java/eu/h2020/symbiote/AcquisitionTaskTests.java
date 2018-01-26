package eu.h2020.symbiote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.enabler.messaging.model.*;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.manager.AuthorizationManager;
import eu.h2020.symbiote.manager.PlatformProxyUtil;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.messaging.consumers.AcquisitionStartRequestedConsumer;
import eu.h2020.symbiote.messaging.consumers.AcquisitionStopRequestedConsumer;
import eu.h2020.symbiote.messaging.consumers.SingleReadingRequestedConsumer;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AcquisitionTaskTests {

    public static final String RESOURCE_ID = "resource1";
    public static final String RESOURCE_URL = "http://someurl";
    public static final String TASK_ID = "1";
    public static final String ENABLER_LOGIC_1 = "EnablerLogic1";
    RabbitManager rabbitManager;
    RestTemplate restTemplate;
    AuthorizationManager authorizationManager;
    AcquisitionManager acquisitionManager;
    AcquisitionTaskDescriptionRepository repository;
    RabbitTemplate rabbitTemplate;

    @Before
    public void setUp() {
        rabbitManager = Mockito.mock(RabbitManager.class);
        restTemplate = Mockito.mock(RestTemplate.class);
        authorizationManager = Mockito.mock(AuthorizationManager.class);
        acquisitionManager = Mockito.mock(AcquisitionManager.class);
        repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);
        rabbitTemplate = Mockito.mock(RabbitTemplate.class);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("username");
        factory.setPassword("password");
    }

    @Test
    public void testStartAcquisitionConsumer() {
        Channel channel = Mockito.mock(Channel.class);
        AcquisitionStartRequestedConsumer consumer = new AcquisitionStartRequestedConsumer(channel, acquisitionManager);
        Envelope envelope = Mockito.mock(Envelope.class);
        AMQP.BasicProperties properties = new AMQP.BasicProperties();

        PlatformProxyAcquisitionStartRequest acquisitionStartRequest = getAcquisitionStartRequest();
        ObjectMapper mapper = new ObjectMapper();
        try {
            consumer.handleDelivery("consumerTag", envelope, properties, mapper.writeValueAsBytes(acquisitionStartRequest));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Error should not happen in handle delivery");
        }
        try {
            ArgumentCaptor<byte[]> argumentByteArray = ArgumentCaptor.forClass(byte[].class);
            verify(channel, times(1)).basicPublish(anyString(), anyString(), any(), argumentByteArray.capture());
            assertNotNull(argumentByteArray);

            ArgumentCaptor<PlatformProxyAcquisitionStartRequest> argumentStartAcqInfo = ArgumentCaptor.forClass(PlatformProxyAcquisitionStartRequest .class);
            verify(acquisitionManager, times(1)).startAcquisition(argumentStartAcqInfo.capture());
            PlatformProxyAcquisitionStartRequest startReq = argumentStartAcqInfo.getValue();
            assertEquals("Task id of the internal call must be equal", TASK_ID, startReq.getTaskId());
            assertEquals("Logic name of the internal call must be equal", ENABLER_LOGIC_1, startReq.getEnablerLogicName());

            //Try to deserialize and check values
            PlatformProxyAcquisitionStartRequestResponse response = mapper.readValue(argumentByteArray.getValue(), PlatformProxyAcquisitionStartRequestResponse.class);
            assertEquals("Task ids must be equal", TASK_ID, response.getTaskId());

        } catch (IOException e) {
            e.printStackTrace();
            fail("Mock channel should not have any errors");
        }
    }

    @Test
    public void testStopAcquisitionConsumer() {
        Channel channel = Mockito.mock(Channel.class);
        AcquisitionStopRequestedConsumer consumer = new AcquisitionStopRequestedConsumer(channel, acquisitionManager);
        Envelope envelope = Mockito.mock(Envelope.class);
        AMQP.BasicProperties properties = new AMQP.BasicProperties();

        List<String> acquisitionStopRequest = getAcquisitionStopRequest();
        ObjectMapper mapper = new ObjectMapper();
        try {
            consumer.handleDelivery("consumerTag", envelope, properties, mapper.writeValueAsBytes(acquisitionStopRequest));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Error should not happen in handle delivery");
        }
        try {
            ArgumentCaptor<byte[]> argumentByteArray = ArgumentCaptor.forClass(byte[].class);
            verify(channel, times(1)).basicPublish(anyString(), anyString(), any(), argumentByteArray.capture());
            assertNotNull(argumentByteArray);

            ArgumentCaptor<List<String>> argumentStopAcqInfo = ArgumentCaptor.forClass((Class)List.class);
            verify(acquisitionManager, times(1)).stopAcquisition(argumentStopAcqInfo.capture());
            List<String> stopReq = argumentStopAcqInfo.getValue();
            assertEquals("Should only call stop for one task", 1, stopReq.size());
            assertEquals("Task id of the internal call must be equal", TASK_ID, stopReq.get(0));

            //Try to deserialize and check values
            String response = mapper.readValue(argumentByteArray.getValue(), String.class);
            assertEquals("Task ids must be equal", "Response", response);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Mock channel should not have any errors");
        }
    }

    @Test
    public void testSingleAcquisitionConsumer() {
        Channel channel = Mockito.mock(Channel.class);
        SingleReadingRequestedConsumer consumer = new SingleReadingRequestedConsumer(channel, acquisitionManager, rabbitTemplate);
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

            ArgumentCaptor<EnablerLogicDataAppearedMessage> returnMsg = ArgumentCaptor.forClass(EnablerLogicDataAppearedMessage.class);
            verify(rabbitTemplate, times(1)).convertAndSend(anyString(),returnMsg.capture(),(MessagePostProcessor)anyObject());
            assertNotNull(returnMsg);
            assertNotNull(returnMsg.getValue());

            ArgumentCaptor<PlatformProxyResourceInfo> argumentResInfo = ArgumentCaptor.forClass(PlatformProxyResourceInfo.class);
            verify(acquisitionManager, times(1)).getObservationForResource(argumentResInfo.capture());
            PlatformProxyResourceInfo resInfo = argumentResInfo.getValue();
            assertEquals("Resource id of the internal call must be equal", RESOURCE_ID, resInfo.getResourceId());
            assertEquals("Resource url of the internal call must be equal", RESOURCE_URL, resInfo.getAccessURL());

            //Try to deserialize and check values
            assertEquals("Task ids must be equal", TASK_ID, returnMsg.getValue().getTaskId());
    }

    @Test
    public void testGenerateRestEndpointAddress() {
        String resourceId = "12345";
        String address1 = "https://www.example.com/rap/Sensors('"+resourceId+"')";
        String address2 = "https://www.example.com/rap/Sensor/"+resourceId;

        Optional<String> result = PlatformProxyUtil.generateRestSensorEndpoint(address1);
        assertTrue(result.isPresent());
        assertEquals("Addresses must be equal",address2, result.get());

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

    private PlatformProxyAcquisitionStartRequest getAcquisitionStartRequest() {
        PlatformProxyAcquisitionStartRequest request = new PlatformProxyAcquisitionStartRequest();
        request.setTaskId(TASK_ID);
        request.setEnablerLogicName(ENABLER_LOGIC_1);
        request.setQueryInterval_ms(Long.valueOf(10000l));
        PlatformProxyResourceInfo resDesc = new PlatformProxyResourceInfo();
        resDesc.setAccessURL(RESOURCE_URL);
        resDesc.setResourceId(RESOURCE_ID);
        List<PlatformProxyResourceInfo> resources = Arrays.asList(resDesc);
        request.setResources(resources);
        return request;
    }

    private List<String> getAcquisitionStopRequest() {
        return Arrays.asList(TASK_ID);
    }

}