package eu.h2020.symbiote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.enabler.messaging.model.*;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.manager.AuthorizationManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.messaging.consumers.ActuatorExecutionRequestedConsumer;
import eu.h2020.symbiote.messaging.consumers.SingleReadingRequestedConsumer;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import static eu.h2020.symbiote.StaticTestConfigs.*;

/**
 * Created by Szymon Mueller on 03/04/2018.
 */
@RunWith(MockitoJUnitRunner.class)
@TestConfiguration()
public class ServiceManagerTests {



    AcquisitionManager manager;

    RabbitManager rabbitManager;
    RestTemplate restTemplate;
    AuthorizationManager authorizationManager;
    AcquisitionManager acquisitionManager;
    RabbitTemplate rabbitTemplate;
    AcquisitionTaskDescriptionRepository repository;



    @Before
    public void setUp() {
        rabbitManager = Mockito.mock(RabbitManager.class);
        restTemplate = Mockito.mock(RestTemplate.class);
        authorizationManager = Mockito.mock(AuthorizationManager.class);
        repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);
        acquisitionManager = Mockito.mock(AcquisitionManager.class);
        rabbitTemplate = Mockito.mock(RabbitTemplate.class);

        manager = new AcquisitionManager(repository, rabbitManager, restTemplate, authorizationManager);
    }

    @Test
    public void testParameterCreationForEmptyAndNullList() {
        List<ServiceParameter> parameters = null;
        String resultForNullList = manager.createJsonForParameters(parameters);
        assertNull("Result for null list must be null", resultForNullList);

        parameters = new ArrayList<>();
        String resultForEmptyList = manager.createJsonForParameters(parameters);
        assertNull("Result for empty list must be null", resultForEmptyList);
    }

    @Test
    public void testParameterCreation() {
        List<ServiceParameter> parameters = new ArrayList<>();
        parameters.add(parameter1);
        parameters.add(parameter2);
        String result = manager.createJsonForParameters(parameters);
        System.out.println("Got json:");
        System.out.println(result);

        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map> s = mapper.readValue(result, new TypeReference<List<Map>>() {
            });

            assertNotNull("Deserialised object must not be null", s);
            assertEquals("Size of the deserialized list must be 2", 2, s.size());
            assertEquals("result must have 2 entry", 2, s.size());
            Map<String, Object> s0 = s.get(0);
            Map<String, Object> s1 = s.get(1);
            assertEquals("result1 must have 1 param", 1, s0.size());
            assertEquals("result2 must have 1 param", 1, s1.size());
            Object s0nameFromMap = s0.keySet().iterator().next();
            assertEquals("Name after deserialise must be the same", parameter1Name, s0nameFromMap);
            Object s0valueFromMap = s0.get(s0nameFromMap);
            assertEquals("Value after deserialise must be same", parameter1Value, s0valueFromMap);

            Object s1nameFromMap = s1.keySet().iterator().next();
            assertEquals("Name after deserialise must be the same", parameter2Name, s1nameFromMap);
            Object s1valueFromMap = s1.get(s1nameFromMap);
            assertEquals("Value after deserialise must be same", parameter2Value, s1valueFromMap);

//
//            if (s0.getName().equals(parameter1Name)) {
//                assertEquals(s0.getValue(), parameter1Value);
//                assertEquals(s1.getName(), parameter2Name);
//                assertEquals(s1.getValue(), parameter2Value);
//            } else {
//                assertEquals(s0.getName(), parameter2Name);
//                assertEquals(s0.getValue(), parameter2Value);
//                assertEquals(s1.getName(), parameter1Name);
//                assertEquals(s1.getValue(), parameter1Value);
//            }
        } catch (IOException e) {
            fail("Could not create objects from created Json");
            e.printStackTrace();
        }
    }

    @Test
    public void testBooleanParameterCreation() {
        assertParameterIsSerialisedCorrectly(booleanParam, booleanParamName, booleanParamValue);
    }

    @Test
    public void testDoubleParameterCreation() {
        assertParameterIsSerialisedCorrectly(doubleParam, doubleParamName, doubleParamValue);
    }

    @Test
    public void testArrayParameterCreation() {
        assertParameterIsSerialisedCorrectly(arrayParam, arrayParamName, arrayParamValue);
    }

    @Test
    public void testActuatorConsumer() {
        Channel channel = Mockito.mock(Channel.class);
        ActuatorExecutionRequestedConsumer consumer = new ActuatorExecutionRequestedConsumer(channel, acquisitionManager, rabbitTemplate);
//        SingleReadingRequestedConsumer consumer = new SingleReadingRequestedConsumer(channel
        Envelope envelope = Mockito.mock(Envelope.class);
        AMQP.BasicProperties properties = new AMQP.BasicProperties();


        ActuatorExecutionTaskInfo testTaskInfo = getActuatorTaskInfo();
        ObjectMapper mapper = new ObjectMapper();
        try {
            consumer.handleDelivery("consumerTag", envelope, properties, mapper.writeValueAsBytes(testTaskInfo));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Error should not happen in handle delivery");
        }

        ArgumentCaptor<ServiceExecutionTaskResponse> returnMsg = ArgumentCaptor.forClass(ServiceExecutionTaskResponse.class);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), returnMsg.capture(), (MessagePostProcessor) anyObject());
        assertNotNull(returnMsg);
        assertNotNull(returnMsg.getValue());

//        ServiceExecutionTaskResponse serviceExecutionTaskResponse = acquisitionManager.executeActuatorTask(actuatorTaskInfo);

        ArgumentCaptor<ActuatorExecutionTaskInfo> argumentResInfo = ArgumentCaptor.forClass(ActuatorExecutionTaskInfo.class);
        verify(acquisitionManager, times(1)).executeActuatorTask(argumentResInfo.capture());
        ActuatorExecutionTaskInfo resInfo = argumentResInfo.getValue();
        assertEquals("Capability name of the internal call must be equal", CAP_1, resInfo.getCapabilityName());
        assertEquals("Resource of the internal call must be equal", getResourceInfo(), resInfo.getResource());

        //Try to deserialize and check values
//        assertEquals("Task ids must be equal", TASK_ID, returnMsg.getValue().getTaskId());
    }


    private void assertParameterIsSerialisedCorrectly(ServiceParameter parameter, String paramName, Object paramValue) {
        List<ServiceParameter> parameters = new ArrayList<>();
        parameters.add(parameter);

        String result = manager.createJsonForParameters(parameters);
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map> s = mapper.readValue(result, new TypeReference<List<Map>>() {
            });
            assertNotNull(s);
            assertEquals("result must have 1 entry", 1, s.size());
            assertEquals("result must have 1 param", 1, s.get(0).size());
            Object nameFromMap = s.get(0).keySet().iterator().next();
            assertEquals("Name after deserialise must be the same", paramName, nameFromMap);
            Object valueFromMap = s.get(0).get(nameFromMap);
            assertEquals("Value after deserialise must be same", paramValue, valueFromMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ActuatorExecutionTaskInfo getActuatorTaskInfo() {
        ActuatorExecutionTaskInfo info = new ActuatorExecutionTaskInfo(ACTUATOR_TASK_1, getResourceInfo(), E_LOGIC, CAP_1, getParameters());
        return info;
    }

    private List<ServiceParameter> getParameters() {
        ServiceParameter param1 = new ServiceParameter(PARAM_1, PARAM_1_VALUE);
        return Arrays.asList(param1);
    }

    private ServiceExecutionTaskInfo getServiceTaskInfo() {
        ServiceExecutionTaskInfo info = new ServiceExecutionTaskInfo(SERVICE_TASK_1, getResourceInfo(), E_LOGIC, getParameters());
        return info;
    }

    private PlatformProxyResourceInfo getResourceInfo() {
        PlatformProxyResourceInfo resDesc = new PlatformProxyResourceInfo();
        resDesc.setAccessURL("http://www.example.com/res1");
        resDesc.setResourceId("res1");
        return resDesc;
    }


}
