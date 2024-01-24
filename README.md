# Deployment Service

This basic deployment service try to make simple automatic deployments.<br/>
Writen in Java 21 with Spring Boot, it uses Docker in order to build your project.

---

## Set Up

### Database schema

See the script `src/main/resources/deployment.sql` to create the database schema.

### Systemd service

You can deploy this program with systemd. You can find a service file example at `/src/main/resources/redheademile-deployment.service`.

---

## Configuration

You can configure as many services as you want to deploy. You just have to create an JSON file into the `configs` directory placed in the working directory of the program.

### Override files

After the build, you can override files. Place the files you want to use as replacement in the `override-files` directory.

### Configuration file schema

| Field                  | Type           | Description                                                  |
|------------------------|----------------|--------------------------------------------------------------|
| dockerImage            | string         | The docker image to use for compilation                      |
| buildCommand           | string         | The command to execute in order to compile                   |
| secret                 | boolean        | Indicate if this service will be shown on the status request |
| deploymentTokens       | string list    | List of tokens, see how to use them below                    |
| emailToNotify          | string list    | List of emails to notify                                     |
| overrideFiles          | dictionary     | List of files to override, see details below                 |
| deploymentStrategy     | string         | Either `local` or `remote`                                   |
| remoteConfig           | RemoteConfig   | Configuration required if using remote deployment strategy   |
| directory              | string         | Directory where the project must be deployed                 |
| preDeploymentCommands  | 2d string list | Commands to execute after compilation and before deployment  |
| postDeploymentCommands | 2d string list | Commands to execute after deployment                         |

#### Override files dictionary

This dictionary has destination file as key and the source file in the `override-file` directory as value.

#### RemoteConfig type

| Field    | Type    | Description                                   |
|----------|---------|-----------------------------------------------|
| user     | string  | User to authenticate on the remote SSH server |
| address  | string  | Address of the remote SSH server              |
| port     | integer | Port of the remote SSH server                 |
| password | string  | Password to log into the remote SSH server    |
| key      | string  | File path of the private SSH key to log in    |

---

## Usage

### Trigger a deployment

To deploy a service, you need to do a `POST` request on the service with a `token` in query parameters. Tokens are strings to identify a service. A token can be declared by only one service.

Example: `POST https://deployment.domain.com/?token=abcdefgh`.

### See deployments statuses

You can see all the non-secret services deployment status by doing a `GET` request on the service.

So: `GET https://deployment.domain.com/`.

---

## Notes on commands

You can use `!ALLOWFAIL` as first param in your command to permits the command execution to fail without considering the deployment failed.

---

## Example

```json
{
  "dockerImage": "openjdk21withangular",
  "buildCommand": "git clone --depth 1 -b main https://gitlab.com/RedHeadEmile/example.git . && sh build.sh",
  "secret": false,
  "deploymentTokens": [
    "abcdefghijklmnopqrstuvwxyz0123456789"
  ],
  "emailToNotify": [
    "emile58000@gmail.com"
  ],
  "overrideFiles": {
    "serverapp/application.properties": "myservice_application.properties",
    "clientapp/assets/settings.json": "myservice_settings.json"
  },
  "deploymentStrategy": "remote",
  "remoteConfig": {
    "user": "user",
    "address": "192.168.X.X",
    "port": 22,
    "key": "/home/user/.ssh/id_ed25519"
  },
  "directory": "/home/pi/services/eventmanager",
  "preDeploymentCommands": [
    ["!ALLOWFAIL", "docker", "compose", "stop"]
  ],
  "postDeploymentCommands": [
    ["docker", "compose", "up", "-d", "--build"]
  ]
}
```
