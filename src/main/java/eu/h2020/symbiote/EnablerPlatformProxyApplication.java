package eu.h2020.symbiote;

import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


/**
 * Created by tipech on 06.03.2017.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class EnablerPlatformProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnablerPlatformProxyApplication.class, args);
    }

    private static Log log = LogFactory.getLog(EnablerPlatformProxyApplication.class);


    @Bean
    RestTemplate RestTemplate() {
        return new RestTemplate();
    }

//    @Bean
//    public AlwaysSampler defaultSampler() {
//        return new AlwaysSampler();
//    }

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