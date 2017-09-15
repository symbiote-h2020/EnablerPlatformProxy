package eu.h2020.symbiote.security;

import eu.h2020.symbiote.security.commons.SecurityConstants;
import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.commons.enums.ValidationStatus;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.commons.jwt.JWTEngine;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private String coreToken = null;

    private Map<String,String> tokensMap = new HashMap<String,String>();

    @Autowired
    public TokenManager(RestTemplate restTemplate) {
        //Empty constructor
        this.restTemplate = restTemplate;
    }

    public String obtainCoreToken() {
        synchronized (TokenManager.class) {
            if( !checkIfCoreRefreshNeeded() ) {
                return coreToken;
            }
            coreToken = null;
            HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            // httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);

//        String input = "{\"username\":\""+ userName +  "\",\"password\":\" " + userPassword + "\"}";
            Credentials credentials = new Credentials(userName, userPassword);

//        HttpEntity<String> entity = new HttpEntity<>(input, httpHeaders);
//
//        ResponseEntity<String> response = restTemplate.exchange(coreInterfaceUrl+coreInterfaceLoginPath, HttpMethod.POST, entity, String.class);

            //TODO check if proper call for R3
            ResponseEntity<String> response = restTemplate.postForEntity(coreInterfaceUrl + SecurityConstants.AAM_GET_GUEST_TOKEN, credentials, String.class);

            if (response.getStatusCode().equals(HttpStatus.OK)) {
                log.debug("Login in the core successful");
                List<String> auth = response.getHeaders().get("X-Auth-Token");
                if (auth != null && auth.size() > 0) {
                    coreToken = auth.get(0);
                    log.debug("Found token: " + coreToken);
                } else {
                    log.debug(auth != null ? ("Token size: " + auth.size()) : "token is null");
                }
            } else {
                log.error("Couldnt get correct core token, error: " + response.getStatusCodeValue());
            }
            return coreToken;
        }
    }

    public String getPaamAddress( String accessUrl ) {
        String result = null;
        int relevantIndex = accessUrl.indexOf("/rap/");
        if( relevantIndex > 0 ) {
            result = accessUrl.substring(0,relevantIndex) + "/paam";
        }
        return result;
    }

    public Token obtainValidPlatformToken(String paamAddress ) throws ValidationException {
        Token token = null;
        String existingToken = tokensMap.get(paamAddress);
        if( existingToken != null ) {
            try {
                ValidationStatus validationStatus = JWTEngine.validateTokenString(existingToken);
                if( validationStatus == ValidationStatus.VALID ) {
                    log.debug("Token is ok");
                    token = new Token(existingToken);
                } else if (validationStatus == ValidationStatus.EXPIRED_TOKEN ) {
                    token = getNewPlatformToken(paamAddress);
                }
            } catch (ValidationException e) {
                log.warn("Token not valid: " + e.getMessage());
            }
        } else {
            token = getNewPlatformToken(paamAddress);
        }
        return token;
    }

    private Token getNewPlatformToken( String paamAddress ) throws ValidationException {
        Token token = null;
        String coreToken = obtainCoreToken();

        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        // httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-Auth-Token", coreToken);

//        String input = "{\"username\":\""+ userName +  "\",\"password\":\" " + userPassword + "\"}";
//        Credentials credentials = new Credentials(userName, userPassword);

        HttpEntity<String> entity = new HttpEntity<>("", httpHeaders);
//
        ResponseEntity<String> response = restTemplate.exchange(paamAddress + SecurityConstants.AAM_GET_FOREIGN_TOKEN, HttpMethod.POST, entity, String.class);

//        ResponseEntity<String> response = restTemplate.postForEntity(paamAddress + AAMConstants.AAM_REQUEST_FOREIGN_TOKEN, "" , String.class);

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            log.debug("Login in the platform successful");
            List<String> auth = response.getHeaders().get("X-Auth-Token");
            if (auth != null && auth.size() > 0) {
                String tokenString = auth.get(0);
                token = new Token(tokenString);
                log.debug("Found token: " + tokenString);
            } else {
                log.debug(auth != null ? ("Token size: " + auth.size()) : "token is null");
            }
        } else {
            log.error("Couldnt get correct platform token, error: " + response.getStatusCodeValue());
        }


        return token;
    }


    /**
     * Checks if the core token stored in the manager is needed to be refreshed.
     */
    private boolean checkIfCoreRefreshNeeded() {
        if( coreToken == null ) {
            return true;
        }
        try {
            ValidationStatus validationStatus = JWTEngine.validateTokenString(coreToken);
            if( validationStatus == ValidationStatus.VALID ) {
                log.debug("Current token is valid");
                return false;
            }
            if( validationStatus == ValidationStatus.EXPIRED_TOKEN ) {
                log.info("Core token expired, need to issue a new one");
                return true;
            } else {
                log.debug("Other problems with token : " + validationStatus );
                return true;
            }
        } catch (ValidationException e) {
            log.error(e);
            return true;
        }
    }

}
