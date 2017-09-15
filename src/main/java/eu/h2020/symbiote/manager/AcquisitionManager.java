package eu.h2020.symbiote.manager;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.AcquisitionStatus;
import eu.h2020.symbiote.model.AcquisitionTask;
import eu.h2020.symbiote.model.AcquisitionTaskDescription;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import eu.h2020.symbiote.security.TokenManager;
import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Manager of the acquisition tasks. Is responsible for starting (scheduling) and stopping (cancelling) of the acquisition tasks.
 * <p>
 * Created by Szymon Mueller on 19/05/2017.
 */
@Component
public class AcquisitionManager {

    private static final Log log = LogFactory.getLog(AcquisitionManager.class);

    private final AcquisitionTaskDescriptionRepository acquisitionTaskDescriptionRepository;
    private final RabbitManager rabbitManager;
    private final RestTemplate restTemplate;
    private final TokenManager tokenManager;

//    private Map<String, AcquisitionTask> tasks = new HashMap<>();
    private Map<String, Timer> tasks = new HashMap<>();

    @Autowired
    public AcquisitionManager(AcquisitionTaskDescriptionRepository acquisitionTaskDescriptionRepository, RabbitManager rabbitManager, RestTemplate restTemplate, TokenManager tokenManager) {
        this.acquisitionTaskDescriptionRepository = acquisitionTaskDescriptionRepository;
        this.rabbitManager = rabbitManager;
        this.restTemplate = restTemplate;
        this.tokenManager = tokenManager;
    }

    public void init() {
        log.info("Initializing acquisition tasks from database...");
        List<AcquisitionTaskDescription> all = acquisitionTaskDescriptionRepository.findAll();
        log.info("Found " + all.size() + " tasks. Initializing...");
        all.forEach(desc -> startAcquisitionTimerTask(desc));

    }

    public AcquisitionTaskDescription createDescriptionFromRequest(PlatformProxyAcquisitionStartRequest request) {
        AcquisitionTaskDescription description = new AcquisitionTaskDescription();
        description.setTaskId(request.getTaskId());
        description.setInterval(request.getQueryInterval_ms());
        description.setResources(request.getResources());
        description.setStartTime(DateTime.now());
        description.setStatus(AcquisitionStatus.STARTED);
        return description;
    }


    public void startAcquisition(PlatformProxyAcquisitionStartRequest startRequest) {
        log.debug("Starting acquisition of task with id " + startRequest.getTaskId());
        //1. Save in database
        AcquisitionTaskDescription description = createDescriptionFromRequest(startRequest);
        //Check if it is already there
        AcquisitionTaskDescription existingOne = acquisitionTaskDescriptionRepository.findOne(startRequest.getTaskId());
        if (existingOne != null) {
            //Do stuff to stop existing task...
            log.debug("Task with id " + startRequest.getTaskId() + " already exists. Stopping and deleting existing one...");
            Timer timer = tasks.get(existingOne.getTaskId());
            if( timer != null ) {
                timer.cancel();
                tasks.remove(existingOne.getTaskId());
            }
            acquisitionTaskDescriptionRepository.delete(startRequest.getTaskId());
        }
        acquisitionTaskDescriptionRepository.save(description);

        //2. Start
        startAcquisitionTimerTask(description);
    }

    private void startAcquisitionTimerTask(AcquisitionTaskDescription description) {
        AcquisitionTask task = new AcquisitionTask(description, restTemplate, this, tokenManager);
        Timer taskTimer = new Timer("Acquisition task " + description.getTaskId(),true);

        long period = description.getInterval().longValue();

        log.debug("Starting acquisition timer task for task " + description.getTaskId() + " period: " + period );
        taskTimer.schedule(task,10000,period);
        tasks.put(description.getTaskId(),taskTimer);
    }

    public void stopAcquisition(List<String> stopRequest) {
        if( stopRequest != null ) {
            for( String taskId: stopRequest ) {
                if( !tasks.containsKey(taskId) ) {
                    log.warn("Tried to stop task with id " + taskId + " but no task with this id could be found");
                } else {
                    log.debug("Cancelling timer for task " + taskId );
                    Timer timer = tasks.get(taskId);
                    timer.cancel();
                }
                acquisitionTaskDescriptionRepository.delete(taskId);
            }
        }
    }

    public void dataAppeared(EnablerLogicDataAppearedMessage message) {
        rabbitManager.sendDataAppearedMessage(message);
    }

    public List<Observation> getObservationForResource(PlatformProxyResourceInfo info) {
        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
//        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        String paamAddress = tokenManager.getPaamAddress(info.getAccessURL());
        try {
            Token platformToken = tokenManager.obtainValidPlatformToken(paamAddress);
            httpHeaders.set("X-Auth-Token", platformToken.getToken());
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<Observation[]> queryResponse = restTemplate.exchange(
                    info.getAccessURL()+"/Observations", HttpMethod.GET, entity, Observation[].class);

            return Arrays.asList(queryResponse.getBody());
        } catch (ValidationException e) {
            log.error("Error obtaining token for platform " + e.getMessage(), e);
            return Arrays.asList();
        }

    }

}
