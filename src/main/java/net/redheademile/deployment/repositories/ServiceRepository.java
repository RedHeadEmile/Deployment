package net.redheademile.deployment.repositories;

import net.redheademile.deployment.models.ServiceModel;
import net.redheademile.jdapper.JDapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ServiceRepository implements IServiceRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ServiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createService(ServiceModel service) {
        this.jdbcTemplate.update(
                "INSERT INTO service(name, deployedAt, compilationTime, deploymentTime, message, status) VALUES(?, ?, ?, ?, ?, ?)",
                service.getServiceName(), service.getDeployedAt() != null ? new java.sql.Timestamp(service.getDeployedAt().getTime()) : null, service.getCompilationTime(), service.getDeploymentTime(), service.getMessage(), service.getStatus() != null ? service.getStatus().name() : null
        );
    }

    @Override
    public ServiceModel readService(String serviceName) {
        List<ServiceModel> services = this.jdbcTemplate.query("SELECT * FROM service WHERE name = ?", JDapper.getMapper(ServiceModel.class), serviceName);
        if (services.isEmpty())
            return null;

        if (services.size() > 1)
            throw new IllegalStateException("More than one element found");

        return services.getFirst();
    }

    @Override
    public List<ServiceModel> readServices(List<String> servicesName) {
        Object[] params = new Object[servicesName.size()];
        for (int i = 0; i < params.length; i++)
            params[i] = servicesName.get(i);

        return this.jdbcTemplate.query(
                "SELECT * FROM service WHERE name IN (?" + ", ?".repeat(servicesName.size() - 1) + ")",
                JDapper.getMapper(ServiceModel.class),
                params
        );
    }

    @Override
    public void updateService(ServiceModel service) {
        this.jdbcTemplate.update(
                "UPDATE service SET deployedAt = ?, compilationTime = ?, deploymentTime = ?, message = ?, status = ? WHERE name = ?",
                service.getDeployedAt() != null ? new java.sql.Timestamp(service.getDeployedAt().getTime()) : null, service.getCompilationTime(), service.getDeploymentTime(), service.getMessage(), service.getStatus() != null ? service.getStatus().name() : null, service.getServiceName()
        );
    }
}
