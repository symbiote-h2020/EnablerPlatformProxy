package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.AcquisitionTaskDescription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by Szymon Mueller on 22/05/2017.
 */
@Repository
public interface AcquisitionTaskDescriptionRepository extends MongoRepository<AcquisitionTaskDescription,String> {

}
