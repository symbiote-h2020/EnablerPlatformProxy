package eu.h2020.symbiote.messaging.consumers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.h2020.symbiote.enabler.messaging.model.CancelTaskRequest;
import eu.h2020.symbiote.manager.AcquisitionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;



import eu.h2020.symbiote.enabler.messaging.model.CancelTaskRequest;




/**
 * Consumer of the acquisition stop request.
 *
 * Created by Szymon Mueller on 03/04/2017.
 */
public class AcquisitionStopRequestedConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(AcquisitionStopRequestedConsumer.class);
    private final AcquisitionManager acquisitionManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     *
     */
    public AcquisitionStopRequestedConsumer(Channel channel, AcquisitionManager acquisitionManager) {
        super(channel);
        this.acquisitionManager = acquisitionManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        String msg = new String(body);
        log.debug( "Consume acquisition stop requested message: " + msg );

        //Try to parse the message
        try {
            ObjectMapper mapper = new ObjectMapper();
            //TODO read proper value and handle acq start request

            CancelTaskRequest cancelRequest = mapper.readValue(msg, new TypeReference<CancelTaskRequest>() {});

            acquisitionManager.stopAcquisition(cancelRequest);

            log.debug( "Sending response to the sender");

            byte[] responseBytes = mapper.writeValueAsBytes("Response");

            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(properties.getCorrelationId())
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
