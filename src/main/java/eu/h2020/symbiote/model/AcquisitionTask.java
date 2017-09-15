package eu.h2020.symbiote.model;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.security.TokenManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by Szymon Mueller on 22/05/2017.
 */
public class AcquisitionTask extends TimerTask {

    private static final Log log = LogFactory.getLog(AcquisitionTask.class);

    private final TokenManager tokenManager;

    private final AcquisitionTaskDescription description;

    private final RestTemplate restTemplate;

    private final AcquisitionManager manager;

    public AcquisitionTask(AcquisitionTaskDescription description, RestTemplate restTemplate, AcquisitionManager manager, TokenManager tokenManager) {
        this.description = description;
        this.restTemplate = restTemplate;
        this.manager = manager;
        this.tokenManager = tokenManager;
    }

    @Override
    public void run() {
        log.debug("Executing acquisition for task " + description.getTaskId());
        //1. Obtain token
        String token = this.tokenManager.obtainCoreToken();

        //2. Send request to RAP and dl data
        log.debug("Accessing resources for task " + description.getTaskId() + " ...");
        List<Observation> observations = new ArrayList<>();
        int i = 0;
        for (PlatformProxyResourceInfo info : description.getResources()) {
            log.debug("[" + ++i + "] Trying " + info.getAccessURL() + " ...");
            List<Observation> resObs = manager.getObservationForResource(info);
            log.debug("[" + ++i + "] " + info.getAccessURL() + " got " + resObs.size() + " observations");
            observations.addAll(resObs);
        }

        //3. Create new data appeared event if there is at least one observation
        if( observations.size() > 0 ) {
            EnablerLogicDataAppearedMessage message = new EnablerLogicDataAppearedMessage();
            message.setTaskId(description.getTaskId());
            message.setObservations(observations);
            message.setTimestamp("" + DateTime.now().getMillis());

            this.manager.dataAppeared(message);
        } else {
            log.info("Returned 0 observations: skipping informing enabler logic");
        }

    }


}
