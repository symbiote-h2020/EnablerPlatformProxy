package eu.h2020.symbiote.model;

import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.manager.AuthorizationManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by Szymon Mueller on 22/05/2017.
 */
public class AcquisitionTask extends TimerTask {

    private static final Log log = LogFactory.getLog(AcquisitionTask.class);

//    private final TokenManager tokenManager;

    private final AcquisitionTaskDescription description;

    private final RestTemplate restTemplate;

    private final AcquisitionManager manager;

    private final AuthorizationManager authorizationManager;

    public AcquisitionTask(AcquisitionTaskDescription description, RestTemplate restTemplate, AcquisitionManager manager, AuthorizationManager authorizationManager) {
        this.description = description;
        this.restTemplate = restTemplate;
        this.manager = manager;
        this.authorizationManager = authorizationManager;
//        this.tokenManager = tokenManager;
    }

    @Override
    public void run() {
        try {
            log.debug("Executing acquisition for task " + description.getTaskId());
            //1. Obtain token
//        String token = this.tokenManager.obtainCoreToken();

            //2. Send request to RAP and dl data
            log.debug("Accessing resources for task " + description.getTaskId() + " ...");
            List<Observation> observations = new ArrayList<>();
            int i = 0;
            List<PlatformProxyResourceInfo> resources=description.getResources();
            for (i=0; i<resources.size(); i++) {
            	PlatformProxyResourceInfo info=resources.get(i);
                log.debug("[" + (i+1) + " of "+resources.size()+"] Trying " + info.getAccessURL() + " ...");

//            this.authorizationManager.requestHomeToken();
                List<Observation> resObs = manager.getObservationForResource(info);
                log.debug("[" + i + "] " + info.getAccessURL() + " got " + resObs.size() + " observations");
                if (resObs.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("res: ");
                    sb.append(resObs.get(0).getResourceId());
                    sb.append(" obsValues: ");
                    resObs.get(0).getObsValues().stream().forEach(obsVal -> {
                        sb.append("[ ");
                        sb.append(obsVal.getObsProperty().getName());
                        sb.append(" - ");
                        sb.append(obsVal.getValue());
                        sb.append(" ]");
                    });
                    log.debug("Printing first obs results: " + sb.toString());
                }
                observations.addAll(resObs);
            }

            //3. Create new data appeared event if there is at least one observation
            // gdb: Sending a "data appeared" message, even if there are zero observations in it.
            // This allows easier understanding and debugging of fundamental communication.
            // Experience from operation so far: If no message comes at all, we try to debug communication
            // If an empty message would come (maybe with a hint) then we would debug search.
//            if (observations.size() > 0) {
                log.info("Sending a \"data appeared\" message with "+observations.size()+" entries");
                EnablerLogicDataAppearedMessage message = new EnablerLogicDataAppearedMessage();
                message.setTaskId(description.getTaskId());
                message.setObservations(observations);
                message.setTimestamp("" + DateTime.now().getMillis());

                this.manager.dataAppeared(message);
//            } else {
//                log.info("Returned 0 observations: skipping informing enabler logic");
//            }
        } catch( Exception e ) {
            log.error("Unexcepted error occurred when executing scheduled task " + description.getTaskId(), e);
        }

    }


}
