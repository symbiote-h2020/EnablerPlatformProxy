package eu.h2020.symbiote.messaging.consumers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyTaskInfo;
import eu.h2020.symbiote.enabler.messaging.model.ServiceExecutionTaskInfo;
import eu.h2020.symbiote.enabler.messaging.model.ServiceExecutionTaskResponse;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.model.cim.Observation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumer of the service execution requested.
 *
 * Created by Szymon Mueller on 27/02/2018.
 */
public class ServiceExecutionRequestedConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(ServiceExecutionRequestedConsumer.class);
    private final AcquisitionManager acquisitionManager;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     *
     */
    public ServiceExecutionRequestedConsumer(Channel channel, AcquisitionManager acquisitionManager, RabbitTemplate rabbitTemplate) {
        super(channel);
        this.acquisitionManager = acquisitionManager;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        String msg = new String(body);
        log.debug( "Consume service execution task: " + msg );

        //Try to parse and execute the message
        try {
            ObjectMapper mapper = new ObjectMapper();
            ServiceExecutionTaskInfo serviceTaskInfo = mapper.readValue(msg, ServiceExecutionTaskInfo.class);

            ServiceExecutionTaskResponse serviceExecutionTaskResponse = acquisitionManager.executeServiceTask(serviceTaskInfo);
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
