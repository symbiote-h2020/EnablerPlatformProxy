package eu.h2020.symbiote.security;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.model.LoginInfo;
import eu.h2020.symbiote.security.constants.AAMConstants;
import eu.h2020.symbiote.security.payloads.Credentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Szymon Mueller on 25/05/2017.
 */
@Component
public class TokenManager {

    private Log log = LogFactory.getLog(TokenManager.class);

    @Value("${symbiote.enabler.core.username}")
    private String userName;

    @Value("${symbiote.enabler.core.password}")
    private String userPassword;

    @Value("${symbiote.enabler.core.interface.url}")
    private String coreInterfaceUrl;

    private final RestTemplate restTemplate;

    private final String coreInterfaceLoginPath = "/login";

    @Autowired
    public TokenManager(RestTemplate restTemplate) {
        //Empty constructor
        this.restTemplate = restTemplate;
    }

    public String obtainCoreToken() {
        String token = null;
        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        // httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

//        String input = "{\"username\":\""+ userName +  "\",\"password\":\" " + userPassword + "\"}";
        Credentials credentials = new Credentials(userName,userPassword);

//        HttpEntity<String> entity = new HttpEntity<>(input, httpHeaders);
//
//        ResponseEntity<String> response = restTemplate.exchange(coreInterfaceUrl+coreInterfaceLoginPath, HttpMethod.POST, entity, String.class);

        ResponseEntity<String> response = restTemplate.postForEntity(coreInterfaceUrl + AAMConstants.AAM_LOGIN, credentials, String.class);

        if( response.getStatusCode().equals(HttpStatus.OK) ){
            log.debug("Login in the core successful");
            List<String> auth = response.getHeaders().get("X-Auth-Token");
            if( auth!=null&&auth.size()>0) {
                token = auth.get(0);
                log.debug("Found token: " + token);
            } else {
                log.debug(auth!=null?("Token size: " +auth.size()):"token is null");
            }
        } else {
            log.error("Couldnt get correct core token, error: " + response.getStatusCodeValue() );
        }

        return token;
    }

}
