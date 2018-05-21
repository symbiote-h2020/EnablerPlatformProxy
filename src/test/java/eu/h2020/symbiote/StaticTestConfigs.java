package eu.h2020.symbiote;

import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyAcquisitionStartRequest;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyTaskInfo;
import eu.h2020.symbiote.enabler.messaging.model.ServiceParameter;
import eu.h2020.symbiote.model.cim.*;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Szymon Mueller on 17/05/2018.
 */
public class StaticTestConfigs {
    public static final String RESOURCE_ID = "resource1";
    public static final String RESOURCE_URL = "http://someurl";
    public static final String TASK_ID = "1";
    public static final String ENABLER_LOGIC_1 = "EnablerLogic1";
    public static final String CAPABLITY_1 = "Capability1";
    public static final String ACTUATOR_TASK_1 = "actuatorTask1";
    public static final String E_LOGIC = "eLogic";
    public static final String CAP_1 = "cap1";
    public static final String PARAM_1 = "param1";
    public static final String PARAM_1_VALUE = "param1Value";
    public static final String SERVICE_TASK_1 = "serviceTask1";
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

        booleanParam = new ServiceParameter(booleanParamName, booleanParamValue);
        doubleParam = new ServiceParameter(doubleParamName, doubleParamValue);

        arrayParamValue = Arrays.asList(new String("1"), new String("2"));
        arrayParam = new ServiceParameter(arrayParamName, arrayParamValue);
    }


    public static PlatformProxyTaskInfo getTestTaskInfo() {
        PlatformProxyTaskInfo info = new PlatformProxyTaskInfo();
        info.setQueryInterval_ms(-1l);
        info.setEnablerLogicName("EL");

        info.setResources(Arrays.asList(getPlatformProxyResourceInfo()));
        info.setTaskId(TASK_ID);

        return info;
    }

    public static PlatformProxyResourceInfo getPlatformProxyResourceInfo() {
        PlatformProxyResourceInfo resDesc = new PlatformProxyResourceInfo();
        resDesc.setAccessURL(RESOURCE_URL);
        resDesc.setResourceId(RESOURCE_ID);
        return resDesc;
    }



    public static PlatformProxyAcquisitionStartRequest getAcquisitionStartRequest() {
        PlatformProxyAcquisitionStartRequest request = new PlatformProxyAcquisitionStartRequest();
        request.setTaskId(TASK_ID);
        request.setEnablerLogicName(ENABLER_LOGIC_1);
        request.setQueryInterval_ms(Long.valueOf(10000l));
        List<PlatformProxyResourceInfo> resources = Arrays.asList(getPlatformProxyResourceInfo());
        request.setResources(resources);
        return request;
    }

    public static List<Observation> getFakeObservationList(DateTime time ) {
        ObservationValue obsValue1 = new ObservationValue("15",new Property("p1","p1iri",Arrays.asList("")),new UnitOfMeasurement("p","p","pIri",Arrays.asList("")));
        List<ObservationValue> observations = Arrays.asList(obsValue1);
        Observation obs1 = new Observation(RESOURCE_ID,new WGS84Location(15.0d,100.0d,15.0d,"loc1",Arrays.asList("loc1")),time.toString(),"10",observations);
        List<Observation> values = Arrays.asList(obs1);


        return values;
    }


    public static List<String> getAcquisitionStopRequest() {
        return Arrays.asList(TASK_ID);
    }

}
