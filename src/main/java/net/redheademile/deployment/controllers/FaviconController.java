package net.redheademile.deployment.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("favicon.ico")
public class FaviconController {
    @GetMapping
    public ResponseEntity<Void> showFavicon() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
