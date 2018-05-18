package eu.h2020.symbiote;

import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.messaging.RabbitManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


/**
 * Created by tipech on 06.03.2017.
 */
@ComponentScan
@EnableRabbit
@SpringBootApplication
public class EnablerPlatformProxyApplication {

    public static void main(String[] args) {
    	WaitForPort.waitForServices(WaitForPort.findProperty("SPRING_BOOT_WAIT_FOR_SERVICES"));
        SpringApplication.run(EnablerPlatformProxyApplication.class, args);
    }

    private static Log log = LogFactory.getLog(EnablerPlatformProxyApplication.class);

    @Value("${rabbit.host}")
    private String rabbitHost;

    @Value("${rabbit.username}")
    private String rabbitUsername;

    @Value("${rabbit.password}")
    private String rabbitPassword;

    @Bean
    RestTemplate RestTemplate() {
        return new RestTemplate();
    }

//    @Bean
//    public AlwaysSampler defaultSampler() {
//        return new AlwaysSampler();
//    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        /**
         * It is necessary to register the GeoJsonModule, otherwise the GeoJsonPoint cannot
         * be deserialized by Jackson2JsonMessageConverter.
         */
        // ObjectMapper mapper = new ObjectMapper();
        // mapper.registerModule(new GeoJsonModule());
        // converter.setJsonObjectMapper(mapper);
        return converter;
    }


    @Bean
    public ConnectionFactory connectionFactory() throws Exception {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHost);
        // connectionFactory.setPublisherConfirms(true);
        // connectionFactory.setPublisherReturns(true);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public AsyncRabbitTemplate asyncRabbitTemplate(RabbitTemplate rabbitTemplate) {

        /**
         * The following AsyncRabbitTemplate constructor uses "Direct replyTo" for replies.
         */
        AsyncRabbitTemplate asyncRabbitTemplate = new AsyncRabbitTemplate(rabbitTemplate);
        asyncRabbitTemplate.setReceiveTimeout(10000);

        return asyncRabbitTemplate;
    }

    @Component
    public static class CLR implements CommandLineRunner {

        private final RabbitManager rabbitManager;
        private final AcquisitionManager acquisitionManager;

        @Autowired
        public CLR(RabbitManager rabbitManager, AcquisitionManager acquisitionManager) {

            this.rabbitManager = rabbitManager;
            this.acquisitionManager = acquisitionManager;
        }

        @Override
        public void run(String... args) throws Exception {
//
            //message retrieval - start rabbit exchange and consumers
            this.rabbitManager.init();
            this.acquisitionManager.init();
            log.info("CLR run() and Rabbit Manager init()");
        }
    }
}