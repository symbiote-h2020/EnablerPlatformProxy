package eu.h2020.symbiote;

import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.manager.AuthorizationManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Szymon Mueller on 11/05/2018.
 */
public class AcquisitionManagerTests {

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





}
