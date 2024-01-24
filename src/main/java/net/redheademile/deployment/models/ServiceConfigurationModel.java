package net.redheademile.deployment.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ServiceConfigurationModel {
    private String serviceName;
    private String dockerImage;
    private String buildCommand;
    private Boolean secret;
    private List<String> deploymentTokens = new ArrayList<>();
    private List<String> emailToNotify = new ArrayList<>();
    private Map<String, String> overrideFiles = new HashMap<>();
    private String deploymentStrategy;
    private ServiceConfigurationRemoteConfigModel remoteConfig;
    private String directory;
    private List<List<String>> preDeploymentCommands = new ArrayList<>();
    private List<List<String>> postDeploymentCommands = new ArrayList<>();
}
