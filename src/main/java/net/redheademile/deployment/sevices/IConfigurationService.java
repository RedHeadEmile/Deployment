package net.redheademile.deployment.sevices;

import net.redheademile.deployment.exceptions.InvalidConfigurationException;
import net.redheademile.deployment.models.ServiceConfigurationModel;

import java.util.List;

public interface IConfigurationService {
    void reloadConfigurationAndEnsureValidity() throws InvalidConfigurationException;
    ServiceConfigurationModel getConfiguration(String deploymentToken);
    List<ServiceConfigurationModel> getVisibleConfigurations();
}
