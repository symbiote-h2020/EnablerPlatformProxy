package eu.h2020.symbiote;

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
import eu.h2020.symbiote.model.AcquisitionStatus;
import eu.h2020.symbiote.model.AcquisitionTaskDescription;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import org.joda.time.DateTime;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static eu.h2020.symbiote.StaticTestConfigs.*;

@RunWith(MockitoJUnitRunner.class)
public class AcquisitionTaskTests {


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

        DateTime now = DateTime.now();
        when(acquisitionManager.getObservationForResource(any())).thenReturn(getFakeObservationList(now));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("username");
        factory.setPassword("password");
    }

    @Test
    public void testStartAcquisitionConsumer() {
        Channel channel = Mockito.mock(Channel.class);

        PlatformProxyAcquisitionStartRequest acquisitionStartRequest = getAcquisitionStartRequest();

        AcquisitionStartRequestedConsumer consumer = new AcquisitionStartRequestedConsumer(channel, acquisitionManager);
        Envelope envelope = Mockito.mock(Envelope.class);
        AMQP.BasicProperties properties = new AMQP.BasicProperties();


        ObjectMapper mapper = new ObjectMapper();
        try {
            consumer.handleDelivery("consumerTag", envelope, properties, mapper.writeValueAsBytes(acquisitionStartRequest));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Error should not happen in handle delivery");
        }
//        try {
//            ArgumentCaptor<byte[]> argumentByteArray = ArgumentCaptor.forClass(byte[].class);
//            verify(channel, times(1)).basicPublish(anyString(), anyString(), any(), argumentByteArray.capture());
//            assertNotNull(argumentByteArray);

            ArgumentCaptor<PlatformProxyAcquisitionStartRequest> argumentStartAcqInfo = ArgumentCaptor.forClass(PlatformProxyAcquisitionStartRequest .class);
            verify(acquisitionManager, times(1)).startAcquisition(argumentStartAcqInfo.capture());
            PlatformProxyAcquisitionStartRequest startReq = argumentStartAcqInfo.getValue();
            assertEquals("Task id of the internal call must be equal", TASK_ID, startReq.getTaskId());
            assertEquals("Logic name of the internal call must be equal", ENABLER_LOGIC_1, startReq.getEnablerLogicName());

            //Try to deserialize and check values - disabling since response has been removed
//            PlatformProxyAcquisitionStartRequestResponse response = mapper.readValue(argumentByteArray.getValue(), PlatformProxyAcquisitionStartRequestResponse.class);
//            assertEquals("Task ids must be equal", TASK_ID, response.getTaskId());

//        } catch (IOException e) {
//            e.printStackTrace();
//            fail("Mock channel should not have any errors");
//        }
    }

    @Test
    public void testStopAcquisitionConsumer() {
        Channel channel = Mockito.mock(Channel.class);
        AcquisitionStopRequestedConsumer consumer = new AcquisitionStopRequestedConsumer(channel, acquisitionManager);
        Envelope envelope = Mockito.mock(Envelope.class);
        AMQP.BasicProperties properties = new AMQP.BasicProperties();

        List<String> taskIds = getAcquisitionStopRequest();
        CancelTaskRequest acquisitionStopRequest = new CancelTaskRequest();
        acquisitionStopRequest.setTaskIdList(taskIds);
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

            ArgumentCaptor<CancelTaskRequest> argumentStopAcqInfo = ArgumentCaptor.forClass((Class)CancelTaskRequest.class);
            verify(acquisitionManager, times(1)).stopAcquisition(argumentStopAcqInfo.capture());
            CancelTaskRequest stopReq = argumentStopAcqInfo.getValue();
            assertEquals("Should only call stop for one task", 1, stopReq.getTaskIdList().size());
            assertEquals("Task id of the internal call must be equal", TASK_ID, stopReq.getTaskIdList().get(0));

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


    @Test
    public void testAcquisitionTaskDescriptionBean() {
        AcquisitionTaskDescription taskDescription1 = new AcquisitionTaskDescription();
        AcquisitionTaskDescription taskDescription2 = new AcquisitionTaskDescription();
        AcquisitionTaskDescription taskDescription3 = new AcquisitionTaskDescription();
        AcquisitionStatus status = AcquisitionStatus.STARTED;
        DateTime dateTime = DateTime.now();
        Long interval = Long.valueOf(1100l);
        String taskId = "12345678";
        PlatformProxyResourceInfo res1 = new PlatformProxyResourceInfo();
        res1.setResourceId("res1");
        res1.setAccessURL("http://www.example.com/res1");
        List<PlatformProxyResourceInfo> resources = Arrays.asList(res1);

        taskDescription1.setStatus(status);
        taskDescription1.setStartTime(dateTime);
        taskDescription1.setInterval(interval);
        taskDescription1.setTaskId(taskId);
        taskDescription1.setResources(resources);

        taskDescription2.setStatus(status);
        taskDescription2.setStartTime(dateTime);
        taskDescription2.setInterval(interval);
        taskDescription2.setTaskId(taskId);
        taskDescription2.setResources(resources);

        taskDescription3.setStatus(status);
        taskDescription3.setStartTime(dateTime);
        taskDescription3.setInterval(interval);
        taskDescription3.setTaskId("new id");
        taskDescription3.setResources(resources);

        assertTrue(taskDescription1.equals(taskDescription2));
        assertFalse(taskDescription1.equals(taskDescription3));
        assertEquals(taskDescription1.hashCode(),taskDescription2.hashCode());
    }

}