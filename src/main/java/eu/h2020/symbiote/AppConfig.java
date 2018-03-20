package eu.h2020.symbiote;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by mael on 7/18/2017.
 */
@Configuration
@EnableMongoRepositories
class AppConfig extends AbstractMongoConfiguration {

    @Value("${symbiote.enabler.platformproxy.mongo.dbname}")
    private String databaseName;

    @Value("${symbiote.enabler.platformproxy.mongo.host}")
    private String mongoHost;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public Mongo mongo() throws Exception {
        return new MongoClient(mongoHost);
    }

    @Override
    protected Collection<String> getMappingBasePackages() { return Arrays.asList("com.oreilly.springdata.mongodb"); }

}