package eu.h2020.symbiote;

import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.manager.AuthorizationManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.AcquisitionTaskDescription;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EnablerPlatformProxyTests {

    RabbitManager rabbitManager;
    RestTemplate restTemplate;
    AuthorizationManager authorizationManager;
    AcquisitionTaskDescriptionRepository repository;

    @Before
    public void setUp() {
        rabbitManager = Mockito.mock(RabbitManager.class);
        restTemplate = Mockito.mock(RestTemplate.class);
        authorizationManager = Mockito.mock(AuthorizationManager .class);
        repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);
    }


    @Test
    public void testCreationOfDescriptionFromRequest() {

        AcquisitionTaskDescriptionRepository repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);

        AcquisitionManager manager = new AcquisitionManager(repository,rabbitManager,restTemplate,authorizationManager);
        PlatformProxyAcquisitionStartRequest request = createAcquisitionStartRequest();
        AcquisitionTaskDescription desc = manager.createDescriptionFromRequest(request);

        assertNotNull(desc);
        assertEquals(desc.getInterval(),request.getQueryInterval_ms());
        assertEquals(desc.getTaskId(),request.getTaskId());
        assertNotNull(desc.getStartTime());
        assertEquals(desc.getResources(),request.getResources());
        assertNotNull(desc.getStatus());
    }

    @Test
    public void testAcquisitionManagerStartAcquisitionSavesEntry() {


        AcquisitionManager manager = new AcquisitionManager(repository,rabbitManager,restTemplate,authorizationManager);



        manager.startAcquisition(createAcquisitionStartRequest());
        verify(repository).save((AcquisitionTaskDescription)any());
    }

    @Test
    public void testAcquisitionManagerStopAcquisitionRemovesEntry() {

        AcquisitionManager manager = new AcquisitionManager(repository,rabbitManager,restTemplate,authorizationManager);


        manager.startAcquisition(createAcquisitionStartRequest());

        List<String> tasksToStop = Arrays.asList("task1");

        manager.stopAcquisition(tasksToStop);
        verify(repository).delete("task1");
    }

    @Test
    public void testAcquisitionManagerAcquisitionExecutes() {

        AcquisitionManager manager = new AcquisitionManager(repository,rabbitManager,restTemplate,authorizationManager);



        manager.startAcquisition(createAcquisitionStartRequest());
        verify(repository).save((AcquisitionTaskDescription)any());
    }

//    @Test
//    public void testAcquireToken() {
//
//        String token = "sometoken";
//        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.put("X-Auth-Token",Arrays.asList(token));
//        ResponseEntity<String> response = new ResponseEntity<String>(headers, HttpStatus.OK);
//        when(restTemplate.postForEntity(anyString(),anyString(),Mockito.eq(String.class) )).thenReturn(response);
//
////        TokenManager tokenManager = new TokenManager(restTemplate);
////        String obtainedToken = tokenManager.obtainCoreToken();
//        authorizationManager = new AuthorizationManager()
//
//        assertNotNull(obtainedToken);
//        assertEquals(obtainedToken,token);
//    }

    private PlatformProxyAcquisitionStartRequest createAcquisitionStartRequest() {
        PlatformProxyAcquisitionStartRequest request = new PlatformProxyAcquisitionStartRequest();
        request.setQueryInterval_ms(Long.valueOf(10000l));
        request.setTaskId("task1");
        PlatformProxyResourceInfo res1 = new PlatformProxyResourceInfo();
        res1.setAccessURL("URL1");
        res1.setResourceId("res1");

        request.setResources(Arrays.asList(res1));
        return request;
    }

}