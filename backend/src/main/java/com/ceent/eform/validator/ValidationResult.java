package com.ceent.eform.validator;

import java.util.HashMap;
import java.util.Map;

public class ValidationResult {
    private boolean valid;
    private Map<String, String> errors;

    public ValidationResult() {
        this.errors = new HashMap<>();
    }

    // Getters and setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public Map<String, String> getErrors() { return errors; }
    public void setErrors(Map<String, String> errors) { this.errors = errors; }
}
