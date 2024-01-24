package net.redheademile.deployment.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceConfigurationRemoteConfigModel {
    private String user;
    private String address;
    private Integer port;
    private String password;
    private String key;
}
