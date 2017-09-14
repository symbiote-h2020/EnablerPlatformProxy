package eu.h2020.symbiote.model;

import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * Describes a single acquisition task
 *
 * Created by Szymon Mueller on 22/05/2017.
 */
public class AcquisitionTaskDescription {

    @Id
    private String taskId;

    private List<PlatformProxyResourceInfo> resources;

    private Long interval;

    private DateTime startTime;

    private AcquisitionStatus status;

    public AcquisitionTaskDescription() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public List<PlatformProxyResourceInfo> getResources() {
        return resources;
    }

    public void setResources(List<PlatformProxyResourceInfo> resources) {
        this.resources = resources;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public AcquisitionStatus getStatus() {
        return status;
    }

    public void setStatus(AcquisitionStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AcquisitionTaskDescription that = (AcquisitionTaskDescription) o;

        return new EqualsBuilder()
                .append(taskId, that.taskId)
                .append(resources, that.resources)
                .append(interval, that.interval)
                .append(startTime, that.startTime)
                .append(status, that.status)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(taskId)
                .append(resources)
                .append(interval)
                .append(startTime)
                .append(status)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "AcquisitionTaskDescription{" +
                "taskId='" + taskId + '\'' +
                ", resources=" + resources +
                ", interval=" + interval +
                ", startTime=" + startTime +
                ", status=" + status +
                '}';
    }
}
