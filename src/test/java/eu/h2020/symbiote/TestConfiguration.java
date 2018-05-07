package eu.h2020.symbiote;

import eu.h2020.symbiote.manager.AuthorizationManager;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Profile("test")
@Configuration
public class TestConfiguration {

    @Bean
    @Primary
    public AuthorizationManager authorizationManager() {
        return Mockito.mock(AuthorizationManager.class);
    }

    @Bean
    @Primary
    public RestTemplate restTemplate() { return Mockito.mock(RestTemplate.class); }
}
