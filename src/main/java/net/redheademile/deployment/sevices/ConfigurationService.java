package net.redheademile.deployment.sevices;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import jakarta.websocket.RemoteEndpoint;
import net.redheademile.deployment.exceptions.InvalidConfigurationException;
import net.redheademile.deployment.models.ServiceConfigurationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.*;

@Service
public class ConfigurationService implements IConfigurationService {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationService.class);

    public static final String DEPLOYMENT_STRATEGY_LOCAL = "local";
    public static final String DEPLOYMENT_STRATEGY_REMOTE = "remote";
    private static final Set<String> KNOWN_DEPLOYMENT_STRATEGIES = Set.of(DEPLOYMENT_STRATEGY_LOCAL, DEPLOYMENT_STRATEGY_REMOTE);

    private Map<String, ServiceConfigurationModel> configurationByToken = Collections.emptyMap();
    private List<ServiceConfigurationModel> visibleConfigurations = new ArrayList<>();

    @Override
    public void reloadConfigurationAndEnsureValidity() throws InvalidConfigurationException {
        File configDirectory;
        try {
            configDirectory = ResourceUtils.getFile("configs");
            if (!configDirectory.exists() && !configDirectory.mkdirs())
                throw new InvalidConfigurationException("Unable to create config file");

            LOGGER.info("Reload configurations from config directory...");
        } catch (FileNotFoundException ignore) {
            throw new InvalidConfigurationException("No configuration directory found");
        }

        if (!configDirectory.exists() || !configDirectory.isDirectory())
            throw new InvalidConfigurationException("No configurations directory found");

        File[] configurationFiles = configDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (configurationFiles == null)
            throw new InvalidConfigurationException("Unable to list files of the directory");

        int configurationAmount = 0;
        Map<String, ServiceConfigurationModel> configurationByToken = new HashMap<>();
        List<ServiceConfigurationModel> visibleConfigurations = new ArrayList<>();

        for (File configurationFile : configurationFiles) {
            if (!configurationFile.isFile())
                continue;

            if (configurationFile.getName().length() <= 5)
                throw new InvalidConfigurationException("Invalid configuration name: \"" + configurationFile.getName() + "\"");

            try (Reader fileReader = new FileReader(configurationFile)) {
                ServiceConfigurationModel configuration = GSON.fromJson(fileReader, ServiceConfigurationModel.class);
                configuration.setServiceName(configurationFile.getName().substring(0, configurationFile.getName().length() - 5));

                if (configuration.getDockerImage() == null || configuration.getDockerImage().isBlank()
                        || configuration.getBuildCommand() == null || configuration.getBuildCommand().isBlank()
                        || configuration.getSecret() == null
                        || configuration.getDeploymentTokens() == null
                        || configuration.getEmailToNotify() == null
                        || configuration.getOverrideFiles() == null
                        || !KNOWN_DEPLOYMENT_STRATEGIES.contains(configuration.getDeploymentStrategy())
                        || configuration.getDirectory() == null || configuration.getDirectory().isBlank()
                        || configuration.getPostDeploymentCommands() == null)
                    throw new InvalidConfigurationException("Invalid configuration: " + configurationFile.getName());

                if (configuration.getDeploymentStrategy().equals(DEPLOYMENT_STRATEGY_REMOTE) && (
                        configuration.getRemoteConfig() == null
                        || configuration.getRemoteConfig().getUser() == null || configuration.getRemoteConfig().getUser().isBlank()
                        || configuration.getRemoteConfig().getAddress() == null || configuration.getRemoteConfig().getAddress().isBlank()
                        || configuration.getRemoteConfig().getPort() == null || configuration.getRemoteConfig().getPort() == 0
                    )
                )
                    throw new InvalidConfigurationException("Invalid configuration: " + configurationFile.getName());

                if (!configuration.getSecret())
                    visibleConfigurations.add(configuration);

                ++configurationAmount;
                LOGGER.info("Configuration loaded: " + configurationFile.getName());

                for (String deploymentToken : configuration.getDeploymentTokens()) {
                    if (configurationByToken.containsKey(deploymentToken))
                        throw new InvalidConfigurationException("Conflict between deployment tokens");

                    configurationByToken.put(deploymentToken, configuration);
                }
            }
            catch (IOException | JsonParseException ignore) {
                throw new InvalidConfigurationException("Failed to load " + configurationFile.getName());
            }
        }

        LOGGER.info(configurationAmount + " configuration(s) loaded");
        this.configurationByToken = configurationByToken;
        this.visibleConfigurations = visibleConfigurations;
    }

    @Override
    public ServiceConfigurationModel getConfiguration(String deploymentToken) {
        return this.configurationByToken.get(deploymentToken);
    }

    @Override
    public List<ServiceConfigurationModel> getVisibleConfigurations() {
        return this.visibleConfigurations;
    }
}
