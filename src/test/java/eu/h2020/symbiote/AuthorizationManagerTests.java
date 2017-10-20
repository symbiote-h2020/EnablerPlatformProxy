package eu.h2020.symbiote;

import eu.h2020.symbiote.manager.AuthorizationManager;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.context.TestConfiguration;

import static org.junit.Assert.*;

/**
 * Created by Szymon Mueller on 20/10/2017.
 */
@RunWith(MockitoJUnitRunner.class)
@TestConfiguration()
public class AuthorizationManagerTests {

    private static final String COMP_NAME = "Comp_name";
    private static final String COMP_PASS = "Comp_pass";
    private static final String AAM_ADDRESS = "http://myaam.address/paam";
    private static final String RAP_ADDRESS = "http://myaam.address/rap/";
    private static final String CLIENT_ID = "Client_id";
    private static final String KEYSTORE_NAME = "Keystore.jks";
    private static final String KEYSTORE_PASS = "pass";
    private static final Boolean SECURITY_ENABLED = Boolean.FALSE;
    private AuthorizationManager manager;

    @Before
    public void setUp() {
        try {
            manager = new AuthorizationManager(COMP_NAME,COMP_PASS,AAM_ADDRESS,CLIENT_ID,KEYSTORE_NAME,KEYSTORE_PASS,SECURITY_ENABLED);
        } catch (SecurityHandlerException e) {
            e.printStackTrace();
            fail();
        } catch (InvalidArgumentsException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetPaamAddress() {
        String result = manager.getPaamAddress(RAP_ADDRESS);
        assertEquals("Generated paam adress must be the same", AAM_ADDRESS,result);
    }

}
