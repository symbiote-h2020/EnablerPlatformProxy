package eu.h2020.symbiote.controllers;

import eu.h2020.symbiote.messaging.RabbitManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Class defining all REST endpoints.
 * <p>
 * 
 */
@RestController
public class EnablerPlatformProxyController {
    private static final String URI_PREFIX = "/EnablerPlatformProxy";

    public static Log log = LogFactory.getLog(EnablerPlatformProxyController.class);

    private final RabbitManager rabbitManager;

    /**
     * Class constructor which autowires RabbitManager bean.
     *
     * @param rabbitManager RabbitManager bean
     */
    @Autowired
    public EnablerPlatformProxyController(RabbitManager rabbitManager) {
        this.rabbitManager = rabbitManager;
    }

    /**
     * Endpoint for placeholder messages.
     * <p>
     * Currently not implemented.
     */
    @RequestMapping(method = RequestMethod.POST,
            value = URI_PREFIX + "/placeholder")
    public ResponseEntity<?> placeholder() {
        return new ResponseEntity<>("Placeholder: ", HttpStatus.NOT_IMPLEMENTED);
    }


}
