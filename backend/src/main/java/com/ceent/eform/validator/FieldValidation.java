package com.ceent.eform.validator;

public class FieldValidation {
    private String fieldName;
    private String type;
    private boolean required;
    private int minLength;
    private int maxLength;
    private String pattern;

    // Constructors, getters, setters
    public FieldValidation() {}

    public FieldValidation(String fieldName, String type, boolean required) {
        this.fieldName = fieldName;
        this.type = type;
        this.required = required;
    }

    // Getters and setters
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public int getMinLength() { return minLength; }
    public void setMinLength(int minLength) { this.minLength = minLength; }

    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
}
