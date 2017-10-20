package eu.h2020.symbiote.messaging.consumers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyAcquisitionStartRequestResponse;
import eu.h2020.symbiote.manager.AcquisitionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Consumer of the acquisition start request.
 *
 * Created by Szymon Mueller on 03/04/2017.
 */
public class AcquisitionStartRequestedConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(AcquisitionStartRequestedConsumer.class);
    private final AcquisitionManager acquisitionManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     *
     */
    public AcquisitionStartRequestedConsumer(Channel channel, AcquisitionManager acquisitionManager) {
        super(channel);
        this.acquisitionManager = acquisitionManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        String msg = new String(body);
        log.debug( "Consume acquisition start requested message: " + msg );

        //Try to parse the message
        try {
            ObjectMapper mapper = new ObjectMapper();
            PlatformProxyAcquisitionStartRequest acquisitionStartRequest = mapper.readValue(msg, PlatformProxyAcquisitionStartRequest.class);

            acquisitionManager.startAcquisition(acquisitionStartRequest);

            PlatformProxyAcquisitionStartRequestResponse response = new PlatformProxyAcquisitionStartRequestResponse();
            response.setStatus("OK");
            response.setTaskId(acquisitionStartRequest.getTaskId());

            byte[] responseBytes = mapper.writeValueAsBytes(response);

            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(properties.getCorrelationId())
                    .contentType("application/json")
                    .build();
            this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, responseBytes);
            log.debug("-> Message sent back");

            this.getChannel().basicAck(envelope.getDeliveryTag(), false);

        } catch( JsonParseException | JsonMappingException e ) {
            log.error("Error occurred when parsing Resource object JSON: " + msg, e);
        } catch( IOException e ) {
            log.error("I/O Exception occurred when parsing Resource object" , e);
        }
    }
}
