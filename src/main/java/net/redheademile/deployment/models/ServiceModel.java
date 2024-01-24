package net.redheademile.deployment.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import net.redheademile.jdapper.JDapperColumnName;

import java.util.Date;

@Getter
@Setter
public class ServiceModel {
    @NotBlank
    @NotNull
    @JDapperColumnName("name")
    private String serviceName;

    private Date deployedAt;

    @JDapperColumnName("compilationTime")
    private Long compilationTime;

    @JDapperColumnName("deploymentTime")
    private Long deploymentTime;

    @JDapperColumnName("message")
    private String message;
    private ServiceDeploymentStatusModel status;

    @NotNull
    private Boolean nextDeploymentInComing;

    @JDapperColumnName("deployedAt")
    public void setLastDeploymentFromData(java.sql.Timestamp lastDeployment) {
        this.deployedAt = lastDeployment;
    }

    @JDapperColumnName("status")
    public void setStatusData(String deploymentStatus) {
        if (deploymentStatus != null)
            this.status = ServiceDeploymentStatusModel.valueOf(deploymentStatus);
        else
            this.status = null;
    }
}
