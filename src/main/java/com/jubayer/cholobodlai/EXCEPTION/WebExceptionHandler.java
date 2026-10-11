package com.jubayer.cholobodlai.EXCEPTION;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(annotations = Controller.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class WebExceptionHandler {
    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String rule(BusinessRuleException ex, Model m) { m.addAttribute("message", ex.getMessage()); return "error"; }
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missing(ResourceNotFoundException ex, Model m) { m.addAttribute("message", ex.getMessage()); return "error"; }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String denied(AccessDeniedException ex, Model m) {
        m.addAttribute("message", "You do not have permission to access this page."); return "error";
    }
}
