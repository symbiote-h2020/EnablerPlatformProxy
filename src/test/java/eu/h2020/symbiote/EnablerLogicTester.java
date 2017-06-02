package eu.h2020.symbiote;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.springframework.amqp.rabbit.connection.RabbitUtils.closeChannel;

/**
 * Created by Szymon Mueller on 02/06/2017.
 */
public class EnablerLogicTester {

    public static void main(String[] args) {
        EnablerLogicTester tester = new EnablerLogicTester();
        tester.sendStartAcquisition();
    }

    public void sendStartAcquisition( ) {
        sendMessage("symbIoTe.enablerLogic","symbIoTe.enablerLogic.acquireMeasurements","test");
    }

    public void sendMessage( String exchange, String routingKey, String message ) {
        AMQP.BasicProperties props;
        Channel channel = null;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        try {
            Connection connection = factory.newConnection();
            channel = connection.createChannel();
            props = new AMQP.BasicProperties()
                    .builder()
                    .contentType("application/json")
                    .build();

            channel.basicPublish(exchange, routingKey, props, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } finally {
            closeChannel(channel);
        }
    }

}
