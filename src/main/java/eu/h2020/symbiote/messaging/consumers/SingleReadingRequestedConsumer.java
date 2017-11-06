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
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.model.cim.Observation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumer of the single reading request.
 *
 * Created by Szymon Mueller on 12/09/2017.
 */
public class SingleReadingRequestedConsumer extends DefaultConsumer {

    private static Log log = LogFactory.getLog(AcquisitionStopRequestedConsumer.class);
    private final AcquisitionManager acquisitionManager;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     *
     */
    public SingleReadingRequestedConsumer(Channel channel, AcquisitionManager acquisitionManager) {
        super(channel);
        this.acquisitionManager = acquisitionManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        String msg = new String(body);
        log.debug( "Consume single read requested message: " + msg );

        //Try to parse the message
        try {
            ObjectMapper mapper = new ObjectMapper();
            PlatformProxyTaskInfo singleReadInfo = mapper.readValue(msg, PlatformProxyTaskInfo.class);
            EnablerLogicDataAppearedMessage message = new EnablerLogicDataAppearedMessage();
            message.setTaskId(singleReadInfo.getTaskId());

            if( singleReadInfo != null ) {
                log.debug( "Executing reads for " + singleReadInfo.getResources()!=null? singleReadInfo.getResources().size() + " resources ":"resources are empty");
                List<Observation> allObservations = new ArrayList<>();
                singleReadInfo.getResources().forEach(res -> allObservations.addAll(acquisitionManager.getObservationForResource(res)));

                message.setObservations(allObservations);
                message.setTimestamp("" + DateTime.now().getMillis());
            }

            log.debug( "Sending response to the sender");

            byte[] responseBytes = mapper.writeValueAsBytes(message);

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
