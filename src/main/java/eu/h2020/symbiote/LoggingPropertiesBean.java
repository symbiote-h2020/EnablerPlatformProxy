package eu.h2020.symbiote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LoggingPropertiesBean implements InitializingBean {
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private Environment env;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log.debug("************Active properties**********************");
		logProperty("platform.id");
		logProperty("symbIoTe.core.interface.url");
		logProperty("symbIoTe.core.cloud.interface.url");
		logProperty("symbIoTe.interworking.interface.url");
		logProperty("symbIoTe.localaam.url");
		logProperty("rabbit.host");
		logProperty("rabbit.username");
		log.debug("************Special properties*********************");
		logProperty("symbiote.enabler.core.username");
		logProperty("enablerPlatformProxy.environment.clientId");
		log.debug("***************************************************");
	}

	void logProperty(String key) {
		log.debug("{}={}", key, env.getProperty(key));
	}

}
