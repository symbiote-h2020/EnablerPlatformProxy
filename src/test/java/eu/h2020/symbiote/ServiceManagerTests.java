package eu.h2020.symbiote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.enabler.messaging.model.ServiceParameter;
import eu.h2020.symbiote.manager.AcquisitionManager;
import eu.h2020.symbiote.manager.AuthorizationManager;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.repository.AcquisitionTaskDescriptionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by Szymon Mueller on 03/04/2018.
 */
@RunWith(MockitoJUnitRunner.class)
@TestConfiguration()
public class ServiceManagerTests {

    AcquisitionManager manager;

    RabbitManager rabbitManager;
    RestTemplate restTemplate;
    AuthorizationManager authorizationManager;
    AcquisitionTaskDescriptionRepository repository;

    static final ServiceParameter parameter1;
    static final String parameter1Name = "param1";
    static final String parameter1Value = "val1";
    static final ServiceParameter parameter2;
    static final String parameter2Name = "param2";
    static final String parameter2Value = "val2";

    static final ServiceParameter booleanParam;
    static final String booleanParamName = "booleanParam";
    static final Boolean booleanParamValue = Boolean.TRUE;

    static final ServiceParameter doubleParam;
    static final String doubleParamName = "floatParam";
    static final Double doubleParamValue = new Double(5.0d);

    static final ServiceParameter arrayParam;
    static final String arrayParamName = "floatParam";
    static final List<Object> arrayParamValue;

    static {
        parameter1 = new ServiceParameter(parameter1Name, parameter1Value);
        parameter2 = new ServiceParameter(parameter2Name, parameter2Value);

        booleanParam = new ServiceParameter(booleanParamName,booleanParamValue);
        doubleParam = new ServiceParameter(doubleParamName,doubleParamValue);

        arrayParamValue = Arrays.asList(new String("1"),new String("2"));
        arrayParam = new ServiceParameter(arrayParamName,arrayParamValue);
    }

    @Before
    public void setUp() {
        rabbitManager = Mockito.mock(RabbitManager.class);
        restTemplate = Mockito.mock(RestTemplate.class);
        authorizationManager = Mockito.mock(AuthorizationManager.class);
        repository = Mockito.mock(AcquisitionTaskDescriptionRepository.class);

        manager = new AcquisitionManager(repository,rabbitManager, restTemplate, authorizationManager);
    }

    @Test
    public void testParameterCreationForEmptyAndNullList() {
        List<ServiceParameter> parameters = null;
        String resultForNullList = manager.createJsonForParameters(parameters);
        assertNull("Result for null list must be null", resultForNullList);

        parameters = new ArrayList<>();
        String resultForEmptyList = manager.createJsonForParameters(parameters);
        assertNull("Result for empty list must be null", resultForEmptyList);
    }

    @Test
    public void testParameterCreation() {
        List<ServiceParameter> parameters = new ArrayList<>();
        parameters.add(parameter1);
        parameters.add(parameter2);
        String result = manager.createJsonForParameters(parameters);
        System.out.println("Got json:");
        System.out.println(result);

        ObjectMapper mapper = new ObjectMapper();
        try {
            List<ServiceParameter> s = mapper.readValue(result, new TypeReference<List<ServiceParameter>>() {
            });

            assertNotNull("Deserialised object must not be null", s);
            assertEquals("Size of the deserialized list must be 2", 2, s.size());
            ServiceParameter s0 = s.get(0);
            assertNotNull(s0);
            assertNotNull(s0.getName());
            assertNotNull(s0.getValue());
            ServiceParameter s1 = s.get(1);
            assertNotNull(s1);
            assertNotNull(s1.getName());
            assertNotNull(s1.getValue());
            if (s0.getName().equals(parameter1Name)) {
                assertEquals(s0.getValue(), parameter1Value);
                assertEquals(s1.getName(), parameter2Name);
                assertEquals(s1.getValue(), parameter2Value);
            } else {
                assertEquals(s0.getName(), parameter2Name);
                assertEquals(s0.getValue(), parameter2Value);
                assertEquals(s1.getName(), parameter1Name);
                assertEquals(s1.getValue(), parameter1Value);
            }
        } catch (IOException e) {
            fail("Could not create objects from created Json");
            e.printStackTrace();
        }
    }

    @Test
    public void testBooleanParameterCreation() {
        assertParameterIsSerialisedCorrectly(booleanParam,booleanParamName,booleanParamValue);
    }

    @Test
    public void testDoubleParameterCreation() {
        assertParameterIsSerialisedCorrectly(doubleParam,doubleParamName,doubleParamValue);
    }

    @Test
    public void testArrayParameterCreation() {
        assertParameterIsSerialisedCorrectly(arrayParam,arrayParamName,arrayParamValue);
    }

    private void assertParameterIsSerialisedCorrectly(ServiceParameter parameter, String paramName, Object paramValue ) {
        List<ServiceParameter> parameters = new ArrayList<>();
        parameters.add(parameter);

        String result = manager.createJsonForParameters(parameters);
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map> s = mapper.readValue(result, new TypeReference<List<Map>>() {});
            assertNotNull(s);
            assertEquals("result must have 1 entry",1,s.size());
            assertEquals("result must have 1 param",1,s.get(0).size());
            Object nameFromMap = s.get(0).keySet().iterator().next();
            assertEquals("Name after deserialise must be the same",paramName,nameFromMap);
            Object valueFromMap = s.get(0).get(nameFromMap);
            assertEquals("Value after deserialise must be same",paramValue,valueFromMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
