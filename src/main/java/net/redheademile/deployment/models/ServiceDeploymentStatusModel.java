package net.redheademile.deployment.models;

public enum ServiceDeploymentStatusModel {
    NEVER_DEPLOYED,
    COMPILATION,
    POST_COMPILATION,
    DEPLOYMENT_DONE,
    DEPLOYMENT_ABORTED;

    public boolean isBlockingForNewDeployment() {
        return switch (this) {
            case COMPILATION, POST_COMPILATION -> true;
            default -> false;
        };
    }
}
