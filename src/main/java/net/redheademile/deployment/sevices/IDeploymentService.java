package net.redheademile.deployment.sevices;

import net.redheademile.deployment.models.ServiceModel;

import java.util.List;

public interface IDeploymentService {
    List<ServiceModel> getLastDeployments();
    void deployAsync(String deploymentToken);
}
