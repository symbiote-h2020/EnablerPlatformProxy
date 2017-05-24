package eu.h2020.symbiote;

import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.AcquisitionTaskDescription;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class EnablerPlatformProxyTests {

    @Test
    public void testCreationOfDescriptionFromRequest() {
        RabbitManager rabbitManager = Mockito.mock(RabbitManager.class);
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        AcquisitionTaskDescriptionRepository repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);

        AcquisitionManager manager = new AcquisitionManager(repository,rabbitManager,restTemplate);
        PlatformProxyAcquisitionStartRequest request = createAcquisitionStartRequest();
        AcquisitionTaskDescription desc = manager.createDescriptionFromRequest(request);

        assertNotNull(desc);
        assertEquals(desc.getInterval(),request.getInterval());
        assertEquals(desc.getTaskId(),request.getTaskId());
        assertNotNull(desc.getStartTime());
        assertEquals(desc.getResources(),request.getResources());
        assertNotNull(desc.getStatus());
    }

    @Test
    public void testAcquisitionManagerStartAcquisition() {
        RabbitManager rabbitManager = Mockito.mock(RabbitManager.class);
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        AcquisitionTaskDescriptionRepository repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);

        AcquisitionManager manager = new AcquisitionManager(repository,rabbitManager,restTemplate);



        manager.startAcquisition(createAcquisitionStartRequest());
        verify(repository).save((AcquisitionTaskDescription)any());
    }

    @Test
    public void testAcquisitionManagerStopAcquisition() {
        RabbitManager rabbitManager = Mockito.mock(RabbitManager.class);
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        AcquisitionTaskDescriptionRepository repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);

        AcquisitionManager manager = new AcquisitionManager(repository,rabbitManager,restTemplate);


        manager.startAcquisition(createAcquisitionStartRequest());

        List<String> tasksToStop = Arrays.asList("task1");

        manager.stopAcquisition(tasksToStop);
        verify(repository).delete("task1");
    }


    private PlatformProxyAcquisitionStartRequest createAcquisitionStartRequest() {
        PlatformProxyAcquisitionStartRequest request = new PlatformProxyAcquisitionStartRequest();
        request.setInterval(10);
        request.setTaskId("task1");
        PlatformProxyResourceInfo res1 = new PlatformProxyResourceInfo();
        res1.setAccessURL("URL1");
        res1.setResourceId("res1");

        request.setResources(Arrays.asList(res1));
        return request;
    }

}