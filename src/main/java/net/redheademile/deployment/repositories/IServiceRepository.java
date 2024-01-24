package net.redheademile.deployment.repositories;

import net.redheademile.deployment.models.ServiceModel;

import java.util.List;

public interface IServiceRepository {
    void createService(ServiceModel service);
    ServiceModel readService(String serviceName);
    List<ServiceModel> readServices(List<String> servicesName);
    void updateService(ServiceModel service);
}
