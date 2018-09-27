package eu.h2020.symbiote;

import eu.h2020.symbiote.enabler.messaging.model.*;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.manager.AuthorizationManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static eu.h2020.symbiote.StaticTestConfigs.*;

/**
 * Created by Szymon Mueller on 11/05/2018.
 */
public class AcquisitionManagerTests {

    private final static String PAAM_ADDRESS = "https://www.example.com/myplatform/paam";
    private final static String PLATFORM_ID = "platform1";

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
        repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);
        rabbitTemplate = Mockito.mock(RabbitTemplate.class);

        acquisitionManager = new AcquisitionManager(repository,rabbitManager,restTemplate,authorizationManager);
    }

    @Test
    public void testGetObservations() {
        try {
            initBehaviour();

            List<Observation> fakeObservationList = getFakeObservationList(DateTime.now());
            Observation[] body = fakeObservationList.toArray(new Observation[fakeObservationList.size()]);
            HttpHeaders headers = new HttpHeaders();
            HttpStatus status =HttpStatus.OK;
            ResponseEntity<Observation[]> responseEntity = new ResponseEntity<>(body,headers,status);

            when(restTemplate.exchange(anyString(),any(),any(),(Class)any())).thenReturn(responseEntity);
            when(authorizationManager.verifyServiceResponse(any(),any(),any())).thenReturn(true);

            PlatformProxyResourceInfo info = getPlatformProxyResourceInfo();
            List<Observation> observationForResource = acquisitionManager.getObservationForResource(info);
            assertNotNull(observationForResource);
            assertEquals("Returned observations must be the same",fakeObservationList,observationForResource);

            when(authorizationManager.verifyServiceResponse(any(),any(),any())).thenReturn(false);
            observationForResource = acquisitionManager.getObservationForResource(info);
            assertNotNull(observationForResource);
            assertEquals("Returned observations must be empty for not verified response",0,observationForResource.size());

            //RestProblems
            when(restTemplate.exchange(anyString(),any(),any(),(Class)any())).thenThrow(new RestClientException("msg") );
            observationForResource = acquisitionManager.getObservationForResource(info);
            assertNotNull(observationForResource);
            assertEquals("Returned observations must be empty for rest client exceptions during observation downloading",0,observationForResource.size());


        } catch (SecurityHandlerException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testExecuteActuator() {
        String body = "Actuator response";
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status =HttpStatus.OK;
        ResponseEntity<?> responseEntity = new ResponseEntity<Object>(body,headers,status);

        when(restTemplate.exchange(anyString(),any(),any(),(Class)any())).thenReturn(responseEntity);
        when(authorizationManager.verifyServiceResponse(any(),any(),any())).thenReturn(true);


        List<ServiceParameter> parameters = Arrays.asList(parameter1,parameter2);
        ActuatorExecutionTaskInfo actuatorInfo = new ActuatorExecutionTaskInfo("actuatorTask1",getPlatformProxyResourceInfo(),ENABLER_LOGIC_1,CAPABLITY_1,parameters);
        ServiceExecutionTaskResponse serviceExecutionTaskResponse = acquisitionManager.executeActuatorTask(actuatorInfo);

        assertNotNull(serviceExecutionTaskResponse);
        assertNotNull(serviceExecutionTaskResponse.getOutput());
        assertEquals("Status of the response must be OK", status, serviceExecutionTaskResponse.getStatus());
        assertEquals("Body of the response must be the same", body, serviceExecutionTaskResponse.getOutput());

        //queryResponse = restTemplate.exchange(url, method, entity, Object.class);
    }

    @Test
    public void testExecuteService() {
        String body = "Actuator response";
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status =HttpStatus.OK;
        ResponseEntity<?> responseEntity = new ResponseEntity<Object>(body,headers,status);

        when(restTemplate.exchange(anyString(),any(),any(),(Class)any())).thenReturn(responseEntity);
        when(authorizationManager.verifyServiceResponse(any(),any(),any())).thenReturn(true);


        List<ServiceParameter> parameters = Arrays.asList(parameter1,parameter2);
        ServiceExecutionTaskInfo serviceInfo = new ServiceExecutionTaskInfo("actuatorTask1",getPlatformProxyResourceInfo(),ENABLER_LOGIC_1,parameters);
        ServiceExecutionTaskResponse serviceExecutionTaskResponse = acquisitionManager.executeServiceTask(serviceInfo);

        assertNotNull(serviceExecutionTaskResponse);
        assertNotNull(serviceExecutionTaskResponse.getOutput());
        assertEquals("Status of the response must be OK", status, serviceExecutionTaskResponse.getStatus());
        assertEquals("Body of the response must be the same", body, serviceExecutionTaskResponse.getOutput());

        //queryResponse = restTemplate.exchange(url, method, entity, Object.class);
    }


    private void initBehaviour() throws SecurityHandlerException {
//        when(authorizationManager.getPaamAddress(anyString())).thenReturn(PAAM_ADDRESS);
//        when(authorizationManager.getPaamAddress(eq(PAAM_ADDRESS))).thenReturn(PLATFORM_ID);
        when(authorizationManager.generateSecurityHeaders()).thenReturn(new HashMap<String,String>());
    }

}
