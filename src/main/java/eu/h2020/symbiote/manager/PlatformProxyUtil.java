package eu.h2020.symbiote.manager;

import org.apache.commons.lang.StringUtils;

import java.util.Optional;

/**
 * Created by Szymon Mueller on 26/01/2018.
 */
public class PlatformProxyUtil {

    /**
     * Returns Rest endpoint of a sensor for specified OData format.
     *
     * @param originalAddress Sensor url address in OData format
     * @return REST address of the sensor or <code>null</code> if
     */
    public static Optional<String> generateRestSensorEndpoint(String originalAddress ){
        String result = null;
        int sensorsIndex = StringUtils.lastIndexOf(originalAddress, "Sensors");
        if( sensorsIndex > 0 ) {
            String resourceId = StringUtils.substring(originalAddress, sensorsIndex + 9, originalAddress.length() - 2);
            result = originalAddress.substring(0, sensorsIndex + 6) + "/" + resourceId;
        }
        return Optional.of(result);
    }
}
