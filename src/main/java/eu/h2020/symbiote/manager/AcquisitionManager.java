package eu.h2020.symbiote.manager;

import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.AcquisitionStatus;
import eu.h2020.symbiote.model.AcquisitionTask;
import eu.h2020.symbiote.model.AcquisitionTaskDescription;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
    private final AuthorizationManager authorizationManager;

//    private Map<String, AcquisitionTask> tasks = new HashMap<>();
    private Map<String, Timer> tasks = new HashMap<>();

    @Autowired
    public AcquisitionManager(AcquisitionTaskDescriptionRepository acquisitionTaskDescriptionRepository, RabbitManager rabbitManager, RestTemplate restTemplate, AuthorizationManager authorizationManager) {
        this.acquisitionTaskDescriptionRepository = acquisitionTaskDescriptionRepository;
        this.rabbitManager = rabbitManager;
        this.restTemplate = restTemplate;
        this.authorizationManager = authorizationManager;
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
        AcquisitionTask task = new AcquisitionTask(description, restTemplate, this, authorizationManager);
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

        //TODO fix - accessUrl check if not null
        String paamAddress = authorizationManager.getPaamAddress(info.getAccessURL());
        try {
            String platformId = authorizationManager.getPlatformIdForAAMAddress(paamAddress);
//            Token platformToken = tokenManager.obtainValidPlatformToken(paamAddress);
//            httpHeaders.set("X-Auth-Token", platformToken.getToken());

            Map<String, String> headers = authorizationManager.generateSecurityHeaders(platformId);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpHeaders.add(entry.getKey(), entry.getValue());
            }

            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<Observation[]> queryResponse = null;
            try {
                queryResponse = restTemplate.exchange(
                        info.getAccessURL() + "/Observations", HttpMethod.GET, entity, Observation[].class);
            } catch( RestClientException e ) {
                log.debug("Could not access OData endpoint, trying with REST");
                Optional<String> restUrl = PlatformProxyUtil.generateRestSensorEndpoint(info.getAccessURL());
                if (restUrl.isPresent() ) {
                    queryResponse = restTemplate.exchange(
                            restUrl.get(), HttpMethod.GET, entity, Observation[].class);
                } else {
                    log.error("Could not generate proper REST endpoint, problem with parsing " + info.getAccessURL());
                }
            }

            if (authorizationManager.verifyServiceResponse(queryResponse.getHeaders(), "rap", platformId)) {
                log.info("Service response from RAP of platform " + platformId + " is valid!");
                return Arrays.asList(queryResponse.getBody());
            }
            else {
                log.info("Service response from RAP of platform " + platformId + " is NOT valid!");
                return new ArrayList<>();
            }
        } catch (ValidationException e) {
            log.error("Error obtaining token for platform " + e.getMessage(), e);
            return Arrays.asList();
        } catch (SecurityHandlerException e) {
            log.error("Security handler exception occurred: " + e.getMessage(), e);
            return Arrays.asList();
        } catch (Exception e) {
            log.error("Internal exception occurred: " + e.getMessage(), e);
            return Arrays.asList();
        }
    }

}
