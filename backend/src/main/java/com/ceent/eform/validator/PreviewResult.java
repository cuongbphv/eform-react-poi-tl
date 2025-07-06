package com.ceent.eform.validator;

public class PreviewResult {
    private boolean success;
    private byte[] previewData;
    private String message;

    public PreviewResult(boolean success, byte[] previewData, String message) {
        this.success = success;
        this.previewData = previewData;
        this.message = message;
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public byte[] getPreviewData() { return previewData; }
    public void setPreviewData(byte[] previewData) { this.previewData = previewData; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
