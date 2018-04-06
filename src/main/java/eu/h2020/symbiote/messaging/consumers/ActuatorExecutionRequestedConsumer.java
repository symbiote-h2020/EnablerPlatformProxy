package eu.h2020.symbiote.messaging.consumers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.enabler.messaging.model.ActuatorExecutionTaskInfo;
import eu.h2020.symbiote.enabler.messaging.model.ServiceExecutionTaskInfo;
import eu.h2020.symbiote.enabler.messaging.model.ServiceExecutionTaskResponse;
import eu.h2020.symbiote.manager.AcquisitionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/**
 * Consumer of the actuator execution requested.
 *
 * Created by Szymon Mueller on 27/02/2018.
 */
public class ActuatorExecutionRequestedConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ActuatorExecutionRequestedConsumer.class);
    private final AcquisitionManager acquisitionManager;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     *
     */
    public ActuatorExecutionRequestedConsumer(Channel channel, AcquisitionManager acquisitionManager, RabbitTemplate rabbitTemplate) {
        super(channel);
        this.acquisitionManager = acquisitionManager;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        String msg = new String(body);
        log.debug( "Consume actuation msg: " + msg );

        //Try to parse and execute the message
        try {
            ObjectMapper mapper = new ObjectMapper();
            ActuatorExecutionTaskInfo actuatorTaskInfo = mapper.readValue(msg, ActuatorExecutionTaskInfo.class);

            ServiceExecutionTaskResponse serviceExecutionTaskResponse = acquisitionManager.executeActuatorTask(actuatorTaskInfo);
            if( serviceExecutionTaskResponse == null ) {
                //Create internal server error task response
                serviceExecutionTaskResponse = new ServiceExecutionTaskResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Null task response returned");
            }

            log.debug( "Sending response to the sender");
            rabbitTemplate.convertAndSend(properties.getReplyTo(), serviceExecutionTaskResponse,
                    m -> {
                        m.getMessageProperties().setCorrelationId(properties.getCorrelationId());
                        return m;
                    });

            log.debug("-> Message sent back");
            this.getChannel().basicAck(envelope.getDeliveryTag(), false);

        } catch( JsonParseException | JsonMappingException e ) {
            log.error("Error occurred when parsing Resource object JSON: " + msg, e);
        } catch( IOException e ) {
            log.error("I/O Exception occurred when parsing Resource object" , e);
        }
    }
}
