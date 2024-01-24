package net.redheademile.deployment.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token")
public class TokenGeneratorController {
    @GetMapping
    public ResponseEntity<String> indexDeploymentStatuses() {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder tokenBuilder = new StringBuilder();
        for (int i = 0; i < 64; i++)
            tokenBuilder.append(alphabet.charAt((int) (Math.random() * 62)));

        return ResponseEntity.ok(tokenBuilder.toString());
    }
}
