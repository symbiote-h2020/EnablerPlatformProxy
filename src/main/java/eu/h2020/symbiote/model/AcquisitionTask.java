package eu.h2020.symbiote.model;

import com.netflix.niws.client.http.RestClient;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.manager.AcquisitionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.http.*;
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

    private AcquisitionTaskDescription description;

    private RestTemplate restTemplate;

    private AcquisitionManager manager;


    public AcquisitionTask(AcquisitionTaskDescription description, RestTemplate restTemplate, AcquisitionManager manager ) {
        this.description = description;
        this.restTemplate = restTemplate;
        this.manager = manager;
    }



    @Override
    public void run() {
        log.debug( "Executing acquisition for task " + description.getTaskId());
        //1. Obtain token
        //TODO obtain real token
        String token = "";

        //2. Send request to RAP and dl data
        log.debug("Accessing resources for task " + description.getTaskId() + " ...");
        List<Observation> observations = new ArrayList<>();
        int i=0;
        for(PlatformProxyResourceInfo info: description.getResources() ) {
            log.debug("["+ ++i + "] Trying " +info.getAccessURL() + " ..." );
            List<Observation> resObs = getObservationForResource(info);
            log.debug("["+ ++i + "] " +info.getAccessURL() +  " got " +  resObs.size() + " observations");
            observations.addAll(resObs);
        }

        //3. Create new data appeared event
        EnablerLogicDataAppearedMessage message = new EnablerLogicDataAppearedMessage();
        message.setTaskId(description.getTaskId());
        //TODO fix which observations are needed
//        message.setObservations(observations);
        message.setTimestamp(""+DateTime.now().getMillis());

        this.manager.dataAppeared( message );

    }

    private List<Observation> getObservationForResource( PlatformProxyResourceInfo info ) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        // httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        ResponseEntity<Observation[]> queryResponse = restTemplate.exchange(
                info.getAccessURL(), HttpMethod.GET, entity, Observation[].class);

        return Arrays.asList(queryResponse.getBody());
    }
}
