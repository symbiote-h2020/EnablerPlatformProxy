package eu.h2020.symbiote.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.commons.credentials.AuthorizationCredentials;
import eu.h2020.symbiote.security.commons.credentials.HomeCredentials;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.communication.payloads.AAM;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import eu.h2020.symbiote.security.handler.ISecurityHandler;
import eu.h2020.symbiote.security.helpers.MutualAuthenticationHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Component responsible for dealing with Symbiote Tokens and checking access right for requests.
 *
 * @author mateuszl
 * @author vasgl
 */
@Component
public class AuthorizationManager {

    private static Log log = LogFactory.getLog(AuthorizationManager.class);

    private String componentOwnerName;
    private String componentOwnerPassword;
    private String aamAddress;
    private String clientId;
    private String keystoreName;
    private String keystorePass;
    private Boolean securityEnabled;

    private IComponentSecurityHandler componentSecurityHandler;

    @Autowired
    public AuthorizationManager(@Value("${symbiote.enabler.core.username}") String componentOwnerName,
                                @Value("${symbiote.enabler.core.password}") String componentOwnerPassword,
                                @Value("${symbiote.enabler.core.interface.url}") String aamAddress,
                                @Value("${enablerPlatformProxy.environment.clientId}") String clientId,
                                @Value("${enablerPlatformProxy.environment.keystoreName}") String keystoreName,
                                @Value("${enablerPlatformProxy.environment.keystorePass}") String keystorePass,
                                @Value("${enablerPlatformProxy.security.enabled}") Boolean securityEnabled)
            throws SecurityHandlerException, InvalidArgumentsException {

        Assert.notNull(componentOwnerName,"componentOwnerName can not be null!");
        this.componentOwnerName = componentOwnerName;

        Assert.notNull(componentOwnerPassword,"componentOwnerPassword can not be null!");
        this.componentOwnerPassword = componentOwnerPassword;

        Assert.notNull(aamAddress,"aamAddress can not be null!");
        this.aamAddress = aamAddress;

        Assert.notNull(clientId,"clientId can not be null!");
        this.clientId = clientId;

        Assert.notNull(keystoreName,"keystoreName can not be null!");
        this.keystoreName = keystoreName;

        Assert.notNull(keystorePass,"keystorePass can not be null!");
        this.keystorePass = keystorePass;

        Assert.notNull(securityEnabled,"securityEnabled can not be null!");
        this.securityEnabled = securityEnabled;

        if (this.securityEnabled)
            enableSecurity();
    }

    /**
     * Returns HTTP headers containing security request for specified platform.
     *
     * @param platformId Id of the platform for which headers should be generated
     * @return Map containing security HTTP headers.
     * @throws SecurityHandlerException
     */
    public Map<String, String> generateSecurityHeaders(String platformId) throws SecurityHandlerException {

//        platformId = SecurityConstants.CORE_AAM_INSTANCE_ID;

        if (securityEnabled) {
            ISecurityHandler iSecurityHandler = componentSecurityHandler.getSecurityHandler();
            Set<AuthorizationCredentials> authorizationCredentialsSet = new HashSet<>();

            try {
                Map<String, AAM> availableAAMs = iSecurityHandler.getAvailableAAMs();


                // Todo: Change hardcoded values "username" and "password" // what is it used for?
                iSecurityHandler.getCertificate(availableAAMs.get(platformId), componentOwnerName, componentOwnerPassword, clientId);
                Token homeToken = iSecurityHandler.login(availableAAMs.get(platformId));

                HomeCredentials homeCredentials = iSecurityHandler.getAcquiredCredentials().get(platformId).homeCredentials;
                authorizationCredentialsSet.add(new AuthorizationCredentials(homeToken, homeCredentials.homeAAM, homeCredentials));

                return MutualAuthenticationHelper.getSecurityRequest(authorizationCredentialsSet, false)
                        .getSecurityRequestHeaderParams();
            } catch (NoSuchAlgorithmException | ValidationException | JsonProcessingException e) {
                log.error(e);
                throw new SecurityHandlerException("Failed to generate security request: " + e.getMessage());
            }
        } else {
            log.debug("Security is disabled. Returning empty Map");
            return new HashMap<>();
        }
    }

    public String getPlatformIdForAAMAddress( String paamAddress) throws Exception {
        Map<String, AAM> availableAAMs = componentSecurityHandler.getSecurityHandler().getAvailableAAMs();
        List<String> platformIds = availableAAMs.values().stream().filter(aam -> aam.getAamAddress().equals(availableAAMs)).map(aam -> aam.getAamInstanceId()).collect(Collectors.toList());
        if( platformIds.size() != 1 ) {
            throw new Exception("Could not find platform with specified aam address " + paamAddress );
        }
        return platformIds.get(0);
    }


    private void enableSecurity() throws SecurityHandlerException {
        securityEnabled = true;
        componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler(
                aamAddress,
                keystoreName,
                keystorePass,
                clientId,
                aamAddress,
                false,
                componentOwnerName,
                componentOwnerPassword);

    }

    /**
     * Setters and Getters
     */
    public IComponentSecurityHandler getComponentSecurityHandler() {
        return componentSecurityHandler;
    }

    public void setComponentSecurityHandler(IComponentSecurityHandler componentSecurityHandler) {
        this.componentSecurityHandler = componentSecurityHandler;
    }

    public String getPaamAddress( String accessUrl ) {
        String result = null;
        int relevantIndex = accessUrl.indexOf("/rap/");
        if( relevantIndex > 0 ) {
            result = accessUrl.substring(0,relevantIndex) + "/paam";
        }
        return result;
    }
}
