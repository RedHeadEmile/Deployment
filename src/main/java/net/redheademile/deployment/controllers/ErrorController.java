package net.redheademile.deployment.controllers;

import net.redheademile.deployment.exceptions.ForbiddenException;
import net.redheademile.deployment.exceptions.TooManyRequestException;
import net.redheademile.deployment.exceptions.UnauthorizedException;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class ErrorController extends AbstractErrorController {

    @Autowired
    public ErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleException(Exception ex) {
        if (ex instanceof ClientAbortException)
            return null;

        if (ex instanceof UnauthorizedException)
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

        if (ex instanceof ForbiddenException)
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);

        if (ex instanceof NoHandlerFoundException)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        if (ex instanceof TooManyRequestException)
            return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
