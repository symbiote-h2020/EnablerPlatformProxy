package eu.h2020.symbiote.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.enabler.messaging.model.*;
import eu.h2020.symbiote.model.cim.Observation;
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
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
            if (timer != null) {
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
        Timer taskTimer = new Timer("Acquisition task " + description.getTaskId(), true);

        long period = description.getInterval().longValue();

        log.debug("Starting acquisition timer task for task " + description.getTaskId() + " period: " + period);
        taskTimer.schedule(task, 10000, period);
        tasks.put(description.getTaskId(), taskTimer);
    }

    public void stopAcquisition(List<String> stopRequest) {
        if (stopRequest != null) {
            for (String taskId : stopRequest) {
                if (!tasks.containsKey(taskId)) {
                    log.warn("Tried to stop task with id " + taskId + " but no task with this id could be found");
                } else {
                    log.debug("Cancelling timer for task " + taskId);
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

            Map<String, String> headers = authorizationManager.generateSecurityHeaders();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpHeaders.add(entry.getKey(), entry.getValue());
            }

            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<Observation[]> queryResponse = null;
            try {
                queryResponse = restTemplate.exchange(
                        info.getAccessURL() + "/Observations", HttpMethod.GET, entity, Observation[].class);
            } catch (RestClientException e) {
                log.debug("Could not access OData endpoint, trying with REST");
                Optional<String> restUrl = PlatformProxyUtil.generateRestSensorEndpoint(info.getAccessURL());
                if (restUrl.isPresent()) {
                    queryResponse = restTemplate.exchange(
                            restUrl.get(), HttpMethod.GET, entity, Observation[].class);
                } else {
                    log.error("Could not generate proper REST endpoint, problem with parsing " + info.getAccessURL());
                }
            }

            if (authorizationManager.verifyServiceResponse(queryResponse.getHeaders(), "rap", platformId)) {
                log.info("Service response from RAP of platform " + platformId + " is valid!");
                return Arrays.asList(queryResponse.getBody());
            } else {
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

    public ResponseEntity<?> executeService(String url, String json, HttpMethod method) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
//        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
//        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<?> queryResponse = null;
        //TODO fix - accessUrl check if not null
        String paamAddress = authorizationManager.getPaamAddress(url);
        try {
            String platformId = authorizationManager.getPlatformIdForAAMAddress(paamAddress);

            Map<String, String> headers = authorizationManager.generateSecurityHeaders();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpHeaders.add(entry.getKey(), entry.getValue());
            }

            HttpEntity<String> entity = new HttpEntity<>(json, httpHeaders);

            try {
                queryResponse = restTemplate.exchange(url, method, entity, Object.class);
                if (authorizationManager.verifyServiceResponse(queryResponse.getHeaders(), "rap", platformId)) {
                    log.info("Service response from RAP of platform " + platformId + " is valid!");
                    return queryResponse;
                } else {
                    log.info("Service response from RAP of platform " + platformId + " is NOT valid!");
                    queryResponse = new ResponseEntity<>("Service response from RAP of platform "
                            + platformId + " is NOT valid!", HttpStatus.FORBIDDEN);
                }
            } catch (HttpClientErrorException e) {
                log.error("Error contacting REST endpoint: " + e.getMessage());
                queryResponse = new ResponseEntity<>("Error contacting REST endpoint " + e.getMessage(), e.getStatusCode());
            }


        } catch (ValidationException e) {
            log.error("Error obtaining token for platform " + e.getMessage(), e);
            queryResponse = new ResponseEntity<>("Error obtaining token for platform " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (SecurityHandlerException e) {
            log.error("Security handler exception occurred: " + e.getMessage(), e);
            queryResponse = new ResponseEntity<>("Security handler exception occurred: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Internal exception occurred: " + e.getMessage(), e);
            queryResponse = new ResponseEntity<>("Internal exception occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return queryResponse;
    }


    public ServiceExecutionTaskResponse executeServiceTask(ServiceExecutionTaskInfo serviceTaskInfo) {
        ServiceExecutionTaskResponse response = null;
        if (serviceTaskInfo != null && serviceTaskInfo.getResource() != null) {
            log.debug("Executing service task " + serviceTaskInfo.getTaskId());
            String jsonForService = createJsonForService(serviceTaskInfo);
            log.debug("Created json for service: " + jsonForService);
            if (jsonForService == null || jsonForService.isEmpty()) {
                log.error("Got " + jsonForService == null ? "null" : "empty" + " service task info");
            } else {
                if (serviceTaskInfo.getResource().getAccessURL() != null) {
                    ResponseEntity<?> responseEntity = executeService(
                            serviceTaskInfo.getResource().getAccessURL(), jsonForService, HttpMethod.PUT);
                    if (responseEntity != null) {
                        //Check status
                        if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                            log.debug("Service executed successfully: " + responseEntity.getBody());
                        } else {
                            log.debug("Service returned code " + responseEntity.getStatusCode());
                            log.debug("Body of the return message: " + responseEntity.getBody());
                        }
                        response = new ServiceExecutionTaskResponse(responseEntity.getStatusCode(), responseEntity.getBody().toString());
                    } else {
                        response = new ServiceExecutionTaskResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error occurred: response from service is null");
                    }

                } else {
                    log.debug("Could not contact service for null access url");
                }
            }
        } else {
            log.debug("Could not execute service task - task is null");
        }
        return response;
    }

    public ServiceExecutionTaskResponse executeActuatorTask(ActuatorExecutionTaskInfo actuatorTaskInfo) {
        ServiceExecutionTaskResponse response = null;
        if (actuatorTaskInfo != null) {
            log.debug("Executing actuator task " + actuatorTaskInfo.getTaskId());
            String jsonForActuator = createJsonForActuator(actuatorTaskInfo);
            log.debug("Created json for actuator: " + jsonForActuator);
            if (jsonForActuator == null || jsonForActuator.isEmpty()) {
                log.error("Got " + jsonForActuator == null ? "null" : "empty" + " actuator task info");
                response = new ServiceExecutionTaskResponse(HttpStatus.BAD_REQUEST, "Got " + jsonForActuator == null ? "null" : "empty" + " actuator task info");
            } else {
                if (actuatorTaskInfo.getResource().getAccessURL() != null) {
                    ResponseEntity<?> responseEntity = executeService(
                            actuatorTaskInfo.getResource().getAccessURL(), jsonForActuator, HttpMethod.PUT);
                    if (responseEntity != null) {
                        //Check status
                        if (responseEntity.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
                            log.debug("Service executed successfully: " + responseEntity.getBody());
                        } else {
                            log.debug("Service returned code " + responseEntity.getStatusCode());
                            log.debug("Body of the return message: " + responseEntity.getBody());
                        }
                        response = new ServiceExecutionTaskResponse(responseEntity.getStatusCode(), responseEntity.getBody()!=null?responseEntity.getBody().toString():"");
                    } else {
                        response = new ServiceExecutionTaskResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error occurred: response from service is null");
                    }

                } else {
                    log.debug("Could not contact service for null access url");
                    response = new ServiceExecutionTaskResponse(HttpStatus.BAD_REQUEST, "Resource must contain not null access url");
                }
            }
        } else {
            log.debug("Could not execute actuator task - task is null");
            response = new ServiceExecutionTaskResponse(HttpStatus.BAD_REQUEST, "Could not execute actuator task - task is null");
        }
        return response;
    }

    public String createJsonForService(ServiceExecutionTaskInfo serviceTaskInfo) {
        StringBuilder json = new StringBuilder();

        if (serviceTaskInfo != null && serviceTaskInfo.getParameters() != null && serviceTaskInfo.getParameters().size() > 0) {
            json.append(createJsonForParameters(serviceTaskInfo.getParameters()));
        }
        return json.toString();
    }

    public String createJsonForActuator(ActuatorExecutionTaskInfo actuatorExecutionTaskInfo) {
        StringBuilder sb = new StringBuilder();
        if (actuatorExecutionTaskInfo != null && actuatorExecutionTaskInfo.getCapabilityName() != null) {
            sb.append("{ \"");
            sb.append(actuatorExecutionTaskInfo.getCapabilityName());
            sb.append("\":");
            if (actuatorExecutionTaskInfo.getParameters() != null && actuatorExecutionTaskInfo.getParameters().size() > 0) {
                sb.append(createJsonForParameters(actuatorExecutionTaskInfo.getParameters()));
            } else {
                sb.append("null");
            }
            sb.append("}");
        } else {
            log.error("Could not create actuation JSON for " + actuatorExecutionTaskInfo == null ? "null actuator task info" : "actuation task info without capability name");
        }
        return sb.toString();
    }

    public String createJsonForParameters(List<ServiceParameter> parameters) {
        String result = null;

        if (parameters != null && parameters.size() > 0) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("[");
//            Iterator<ServiceParameter> iterator = parameters.iterator();
//            while( iterator.hasNext() ) {
//                ServiceParameter next = iterator.next();
//                try {
//                    mapper.writewriteValueAsString(next.getValue());
//                } catch (JsonProcessingException e) {
//                    e.printStackTrace();
//                }
//                sb.append("{\"");
//                sb.append(next.getName());
//                sb.append("\":\"");
//                sb.append(next.getValue());
//                sb.append("\"}");
//                if( iterator.hasNext() ) {
//                    sb.append(",");
//                }
//            }
//            sb.append("]");
//            result = sb.toString();
            //Using object mapper
            ObjectMapper objectMapper = new ObjectMapper();
            List<HashMap<String, Object>> listOfMap = new ArrayList<>();
            parameters.stream().forEach(p -> {
                HashMap<String, Object> map = new HashMap<>();
                map.put(p.getName(), p.getValue());
                listOfMap.add(map);
            });
            try {
                result = objectMapper.writeValueAsString(listOfMap);
                log.debug("Parsed parameters map: " + result);
            } catch (JsonProcessingException e) {
                log.error("Error when parsing parameters json: " + e.getMessage(), e);
            }
        } else {
            log.debug("Could not create json for empty parameters");
        }

        return result;
    }

}
