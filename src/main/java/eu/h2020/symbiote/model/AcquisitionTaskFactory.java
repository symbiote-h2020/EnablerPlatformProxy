package eu.h2020.symbiote.model;

import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyAcquisitionStartRequest;
import eu.h2020.symbiote.manager.AcquisitionManager;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Factory class for creating acquisition task descriptions from requests and acquisition tasks from descriptions.
 *
 * Created by Szymon Mueller on 22/05/2017.
 */
@Component
public class AcquisitionTaskFactory {

    private final AcquisitionManager acquisitionManager;
    private final RestTemplate restTemplate;

    @Autowired
    public AcquisitionTaskFactory(AcquisitionManager acquisitionManager, RestTemplate restTemplate) {
        this.acquisitionManager = acquisitionManager;
        this.restTemplate = restTemplate;
    }



}
