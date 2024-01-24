package net.redheademile.deployment.sevices;

import net.redheademile.deployment.models.ServiceModel;

import java.util.List;

public interface IEmailService {
    void sendDeploymentSuccessEmailAsync(List<String> recipients, ServiceModel status);
    void sendDeploymentFailureEmailAsync(List<String> recipients, ServiceModel status);
}
