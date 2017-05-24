package eu.h2020.symbiote;

import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Szymon Mueller on 23/05/2017.
 */

@RunWith(SpringRunner.class)
@ContextConfiguration
@SpringBootTest
@DirtiesContext
public class SpringTest {

    @Autowired
    protected RabbitManager rabbitManager;

    @Autowired
    protected AcquisitionManager acquisitionManager;


//    @Autowired
//    protected InternalSecurityHandler securityHandler;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void springLoadTest() {

    }

    @Configuration
    @ComponentScan(basePackages = {"eu.h2020.symbiote"})
    static class ContextConfiguration {
    }

}