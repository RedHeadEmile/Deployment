package net.redheademile.deployment.controllers;

import net.redheademile.deployment.models.ServiceModel;
import net.redheademile.deployment.sevices.IDeploymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class DeploymentController {
    private final IDeploymentService deploymentService;

    @Autowired
    public DeploymentController(IDeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceModel>> indexDeploymentStatuses() {
        return ResponseEntity.ok(this.deploymentService.getLastDeployments());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<Void> storeDeploymentRequest(@RequestParam("token") String token) {
        this.deploymentService.deployAsync(token);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
