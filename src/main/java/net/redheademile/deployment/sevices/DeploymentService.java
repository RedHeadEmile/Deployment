package net.redheademile.deployment.sevices;

import com.jcraft.jsch.*;
import net.redheademile.deployment.exceptions.ForbiddenException;
import net.redheademile.deployment.exceptions.UnauthorizedException;
import net.redheademile.deployment.models.ServiceConfigurationModel;
import net.redheademile.deployment.models.ServiceDeploymentStatusModel;
import net.redheademile.deployment.models.ServiceModel;
import net.redheademile.deployment.repositories.IServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

@Service
public class DeploymentService implements IDeploymentService {
    private final static Object LOCK = new Object();
    private final static Logger LOGGER = LoggerFactory.getLogger(DeploymentService.class);

    private final IConfigurationService configurationService;
    private final IEmailService emailService;

    private final IServiceRepository repository;

    private final Set<String> servicesToDeploy;

    @Autowired
    public DeploymentService(IConfigurationService configurationService, IEmailService emailService, IServiceRepository repository) {
        this.configurationService = configurationService;
        this.emailService = emailService;
        this.repository = repository;
        this.servicesToDeploy = new HashSet<>();
    }

    @Override
    public List<ServiceModel> getLastDeployments() {
        List<ServiceConfigurationModel> configurations = configurationService.getVisibleConfigurations();
        List<String> servicesName = new ArrayList<>();

        for (ServiceConfigurationModel configuration : configurations)
            servicesName.add(configuration.getServiceName());

        List<ServiceModel> service = this.repository.readServices(servicesName);
        mainLoop : for (ServiceConfigurationModel configuration : configurations) {
            for (ServiceModel serviceDeployment : service)
                if (serviceDeployment.getServiceName().equals(configuration.getServiceName())) {
                    serviceDeployment.setNextDeploymentInComing(servicesToDeploy.contains(configuration.getServiceName()));
                    continue mainLoop;
                }

            service.add(new ServiceModel() {{
                setServiceName(configuration.getServiceName());
                setStatus(ServiceDeploymentStatusModel.NEVER_DEPLOYED);
                setNextDeploymentInComing(servicesToDeploy.contains(configuration.getServiceName()));
            }});
        }

        return service;
    }

    //#region Local Deployment
    private String executeCommandsLocally(List<List<String>> commands, File directory) throws IOException, InterruptedException {
        StringBuilder warningMessage = new StringBuilder();

        for (List<String> command : commands) {
            boolean allowFail = command.getFirst().equals("!ALLOWFAIL");
            command = allowFail ? command.subList(1, command.size()) : command;

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(directory);
            Process process = pb.start();
            int result = process.waitFor();
            if (result != 0) {
                if (allowFail)
                    warningMessage.append("[FAIL] ").append(String.join(" ", command)).append(" ; ");
                else {
                    if (process.getErrorStream() != null)
                        throw new IOException(new String(process.getErrorStream().readAllBytes()));
                    else
                        throw new IOException("Result code unexpected: " + result);
                }
            }
        }

        return warningMessage.toString();
    }

    private String deployLocally(ServiceConfigurationModel config, File workingDirectory) throws IOException, InterruptedException {
        File localFile = new File(config.getDirectory());
        if (localFile.exists() && !localFile.isDirectory())
            throw new IOException("The target path is not a directory");

        StringBuilder warningMessage = new StringBuilder();
        warningMessage.append(executeCommandsLocally(config.getPreDeploymentCommands(), localFile.exists() ? localFile : null));

        if (localFile.exists())
            FileSystemUtils.deleteRecursively(localFile);

        String localDirectoryLocation = localFile.getAbsolutePath();
        String sourceDirectoryLocation = workingDirectory.getAbsolutePath();

        try (Stream<Path> sourcePathsStream = Files.walk(workingDirectory.getAbsoluteFile().toPath())) {
            Iterator<Path> sourcePathsIterator = sourcePathsStream.iterator();
            while (sourcePathsIterator.hasNext()) {
                Path source = sourcePathsIterator.next();
                Path destination = Paths.get(localDirectoryLocation, source.toString().substring(sourceDirectoryLocation.length()));
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        warningMessage.append(executeCommandsLocally(config.getPostDeploymentCommands(), localFile));
        return warningMessage.toString();
    }
    //#endregion

    //#region Remote Deployment
    private String executeCommandsRemotely(List<List<String>> commands, String directory, Session session) throws JSchException, IOException, InterruptedException {
        StringBuilder warningMessage = new StringBuilder();

        for (List<String> command : commands) {
            boolean allowFail = command.getFirst().equals("!ALLOWFAIL");
            String strCommand = String.join(" ", allowFail ? command.subList(1, command.size()) : command);

            ChannelExec exec = (ChannelExec) session.openChannel("exec");
            String prefix = "";
            if (directory != null)
                prefix = "cd \"" + directory + "\" && ";
            exec.setCommand(prefix + strCommand);
            InputStream errStream = exec.getErrStream();
            exec.connect();

            while (!exec.isClosed())
                Thread.sleep(100L);

            exec.disconnect();
            if (exec.getExitStatus() != 0) {
                if (allowFail)
                    warningMessage.append("[FAIL] ").append(strCommand).append(" ; ");
                else
                    throw new IOException(new String(errStream.readAllBytes()));
            }
        }

        return warningMessage.toString();
    }

    private void recursiveFolderDelete(ChannelSftp channelSftp, String path) throws SftpException {
        // Iterate objects in the list to get file/folder names.
        for (ChannelSftp.LsEntry item : channelSftp.ls(path)) {
            if (!item.getAttrs().isDir()) {
                channelSftp.rm(path + "/" + item.getFilename()); // Remove file.
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) { // If it is a subdir.
                try {
                    // removing sub directory.
                    channelSftp.rmdir(path + "/" + item.getFilename());
                } catch (Exception e) { // If subdir is not empty and error occurs.
                    // Do lsFolderRemove on this subdir to enter it and clear its contents.
                    recursiveFolderDelete(channelSftp, path + "/" + item.getFilename());
                }
            }
        }
        channelSftp.rmdir(path); // delete the parent directory after empty
    }

    private String deployRemotely(ServiceConfigurationModel config, File workingDirectory) throws JSchException, SftpException, IOException, InterruptedException {
        JSch jsch = new JSch();
        if (config.getRemoteConfig().getKey() != null && !config.getRemoteConfig().getKey().isBlank())
            jsch.addIdentity(config.getRemoteConfig().getKey());

        Session session = jsch.getSession(config.getRemoteConfig().getUser(), config.getRemoteConfig().getAddress(), config.getRemoteConfig().getPort());
        session.setUserInfo(new UserInfo() {
            @Override public String getPassphrase() { return null; }
            @Override public String getPassword() { return config.getRemoteConfig().getPassword(); }
            @Override public boolean promptPassword(String s) { return config.getRemoteConfig().getPassword() != null && !config.getRemoteConfig().getPassword().isBlank(); }
            @Override public boolean promptPassphrase(String s) { return false; }
            @Override public boolean promptYesNo(String s) { return true; }
            @Override public void showMessage(String s) { LOGGER.debug(s); }
        });
        session.connect();

        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        boolean fileExists;
        try {
            if (!sftp.stat(config.getDirectory()).isDir())
                throw new IOException("The target path is not a directory");

            fileExists = true;
        } catch (SftpException e) {
            if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE)
                throw e;

            fileExists = false;
        }

        StringBuilder warningMessage = new StringBuilder();
        warningMessage.append(executeCommandsRemotely(config.getPreDeploymentCommands(), fileExists ? config.getDirectory() : null, session));

        if (fileExists)
            recursiveFolderDelete(sftp, config.getDirectory());

        String sourceDirectoryLocation = workingDirectory.getAbsolutePath();

        try (Stream<Path> sourcePathsStream = Files.walk(workingDirectory.getAbsoluteFile().toPath())) {
            Iterator<Path> sourcePathsIterator = sourcePathsStream.iterator();
            while (sourcePathsIterator.hasNext()) {
                Path source = sourcePathsIterator.next();
                String dest = config.getDirectory() + source.toString().substring(sourceDirectoryLocation.length()).replace('\\', '/');

                if (source.toFile().isDirectory())
                    sftp.mkdir(dest);
                else
                    sftp.put(source.toString(), dest);
            }
        }

        warningMessage.append(executeCommandsRemotely(config.getPostDeploymentCommands(), config.getDirectory(), session));

        sftp.disconnect();
        session.disconnect();

        return warningMessage.toString();
    }
    //#endregion

    private void deploy(ServiceConfigurationModel config) {
        ServiceModel service;
        synchronized (LOCK) {
            service = repository.readService(config.getServiceName());
            if (service == null) {
                service = new ServiceModel() {{
                    setServiceName(config.getServiceName());
                    setStatus(ServiceDeploymentStatusModel.COMPILATION);
                }};
                repository.createService(service);
            } else {
                if (service.getStatus() != null && service.getStatus().isBlockingForNewDeployment()) {
                    servicesToDeploy.add(config.getServiceName());
                    return;
                }

                service.setCompilationTime(null);
                service.setDeploymentTime(null);

                service.setMessage(null);
                service.setStatus(ServiceDeploymentStatusModel.COMPILATION);
                repository.updateService(service);
            }

            servicesToDeploy.remove(config.getServiceName());
        }

        LOGGER.info("Starting deployment of " + config.getServiceName());

        File workingDirectory = null;
        try {
            workingDirectory = Files.createTempDirectory("deployment" + UUID.randomUUID()).toFile();
            String workingDirectoryPath = workingDirectory.getAbsolutePath();

            long compilationStart = System.currentTimeMillis();
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList("docker", "run", "--rm", "-v", workingDirectoryPath + ":/output", config.getDockerImage(), "sh", "-c", config.getBuildCommand()));
            Process process = pb.start();
            int result = process.waitFor();

            long compilationEnd = System.currentTimeMillis();

            if (result != 0) {
                if (process.getErrorStream() != null)
                    throw new IOException(new String(process.getErrorStream().readAllBytes()));
                else
                    throw new IOException("Result code unexpected: " + result);
            }

            service.setStatus(ServiceDeploymentStatusModel.POST_COMPILATION);
            repository.updateService(service);

            File overrideFilesDirectory = ResourceUtils.getFile("override-files");
            if (!overrideFilesDirectory.exists() && !overrideFilesDirectory.mkdirs())
                throw new IOException("Unable to create override files directory");

            for (Map.Entry<String, String> overrideFile : config.getOverrideFiles().entrySet()) {
                File hostFile = new File(overrideFilesDirectory, overrideFile.getValue());
                File destFile = new File(workingDirectory, overrideFile.getKey());

                if (!hostFile.exists())
                    throw new IOException("File not found: " + overrideFile.getValue());

                Files.copy(hostFile.getAbsoluteFile().toPath(), destFile.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            long deploymentStart = System.currentTimeMillis();
            String warningMessage = switch (config.getDeploymentStrategy()) {
                case ConfigurationService.DEPLOYMENT_STRATEGY_LOCAL -> deployLocally(config, workingDirectory);
                case ConfigurationService.DEPLOYMENT_STRATEGY_REMOTE -> deployRemotely(config, workingDirectory);
                default -> throw new IOException("Unknown deployment strategy: " + config.getDeploymentStrategy());
            };
            long deploymentEnd = System.currentTimeMillis();

            service.setCompilationTime(compilationEnd - compilationStart);
            service.setDeploymentTime(deploymentEnd - deploymentStart);

            service.setStatus(ServiceDeploymentStatusModel.DEPLOYMENT_DONE);

            service.setMessage(warningMessage);
            service.setDeployedAt(new Date());

            repository.updateService(service);

            LOGGER.info("Deployment succeeded for: " + config.getServiceName());

            emailService.sendDeploymentSuccessEmailAsync(config.getEmailToNotify(), service);

            if (servicesToDeploy.remove(config.getServiceName()))
                deploy(config);
        }
        catch (IOException | InterruptedException | JSchException | SftpException e) {
            service.setMessage(e.getMessage());
            service.setStatus(ServiceDeploymentStatusModel.DEPLOYMENT_ABORTED);
            repository.updateService(service);

            LOGGER.info("Deployment failed for: " + config.getServiceName());

            emailService.sendDeploymentFailureEmailAsync(config.getEmailToNotify(), service);
        } finally {
            if (workingDirectory != null)
                FileSystemUtils.deleteRecursively(workingDirectory);
        }
    }

    @Override
    @Async
    public void deployAsync(String deploymentToken) {
        if (deploymentToken == null || deploymentToken.isBlank())
            throw new UnauthorizedException();

        ServiceConfigurationModel config = this.configurationService.getConfiguration(deploymentToken);
        if (config == null)
            throw new ForbiddenException();

        deploy(config);
    }
}
