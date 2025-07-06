package com.ceent.eform.service;

import com.ceent.eform.dto.TemplateDto;
import com.ceent.eform.dto.request.GeneratePdfRequest;
import com.ceent.eform.validator.FieldValidation;
import com.ceent.eform.validator.PreviewResult;
import com.ceent.eform.validator.ValidationResult;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.data.*;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.deepoove.poi.policy.PictureRenderPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedEFormService {

    private final EFormService eFormService;

    /**
     * Tạo template với bảng động
     */
    public Map<String, Object> createTableData(List<Map<String, Object>> tableRows) {
        Map<String, Object> data = new HashMap<>();

        // Validate input
        if (tableRows == null || tableRows.isEmpty()) {
            data.put("table", Tables.create());
            return data;
        }

        // Create table rows
        List<RowRenderData> rows = new ArrayList<>();
        for (Map<String, Object> row : tableRows) {
            if (row != null && !row.isEmpty()) {
                // Convert map values to CellRenderData array
                List<CellRenderData> cells = new ArrayList<>();
                for (Object value : row.values()) {
                    // Convert each value to a cell
                    String cellText = value != null ? value.toString() : "";
                    cells.add(Cells.of(cellText).create());
                }

                // Create row with cells
                RowRenderData rowData = Rows.of(cells.toArray(new CellRenderData[0])).create();
                rows.add(rowData);
            }
        }

        // Create table with rows
        if (!rows.isEmpty()) {
            data.put("table", Tables.of(rows.toArray(new RowRenderData[0])).create());
        } else {
            data.put("table", Tables.create());
        }

        return data;
    }

    /**
     * Xử lý hình ảnh trong template
     */
    public Map<String, Object> processImages(Map<String, Object> formData) {
        Map<String, Object> processedData = new HashMap<>(formData);

        formData.forEach((key, value) -> {
            if (key.contains("image") || key.contains("picture")) {
                if (value instanceof String) {
                    String imagePath = (String) value;
                    try {
                        PictureRenderData picture = Pictures.ofLocal(imagePath)
                                .size(150, 150)
                                .create();
                        processedData.put(key, picture);
                    } catch (Exception e) {
                        log.error("Error processing image: " + imagePath, e);
                        processedData.put(key, Texts.of("").create());
                    }
                }
            }
        });

        return processedData;
    }

    /**
     * Tạo dữ liệu với điều kiện
     */
    public Map<String, Object> createConditionalData(Map<String, Object> baseData,
                                                     Map<String, Boolean> conditions) {
        Map<String, Object> data = new HashMap<>(baseData);

        conditions.forEach((condition, value) -> {
            data.put(condition, value);

            // Thêm nội dung có điều kiện
            if (value) {
                data.put(condition + "_content", baseData.get(condition + "_true"));
            } else {
                data.put(condition + "_content", baseData.get(condition + "_false"));
            }
        });

        return data;
    }

    /**
     * Validation nâng cao cho form data
     */
    public ValidationResult validateFormData(Map<String, Object> formData,
                                             List<FieldValidation> validations) {
        ValidationResult result = new ValidationResult();
        Map<String, String> errors = new HashMap<>();

        for (FieldValidation validation : validations) {
            String fieldName = validation.getFieldName();
            Object value = formData.get(fieldName);

            // Required validation
            if (validation.isRequired() && (value == null || value.toString().trim().isEmpty())) {
                errors.put(fieldName, "Trường " + fieldName + " là bắt buộc");
                continue;
            }

            if (value != null && !value.toString().trim().isEmpty()) {
                String stringValue = value.toString().trim();

                // Email validation
                if ("email".equals(validation.getType())) {
                    if (!isValidEmail(stringValue)) {
                        errors.put(fieldName, "Email không hợp lệ");
                    }
                }

                // Phone validation
                if ("phone".equals(validation.getType())) {
                    if (!isValidPhone(stringValue)) {
                        errors.put(fieldName, "Số điện thoại không hợp lệ");
                    }
                }

                // Date validation
                if ("date".equals(validation.getType())) {
                    if (!isValidDate(stringValue)) {
                        errors.put(fieldName, "Định dạng ngày không hợp lệ");
                    }
                }

                // Length validation
                if (validation.getMinLength() > 0 && stringValue.length() < validation.getMinLength()) {
                    errors.put(fieldName, "Độ dài tối thiểu " + validation.getMinLength() + " ký tự");
                }

                if (validation.getMaxLength() > 0 && stringValue.length() > validation.getMaxLength()) {
                    errors.put(fieldName, "Độ dài tối đa " + validation.getMaxLength() + " ký tự");
                }

                // Pattern validation
                if (validation.getPattern() != null && !stringValue.matches(validation.getPattern())) {
                    errors.put(fieldName, "Định dạng không hợp lệ");
                }
            }
        }

        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        return result;
    }

    /**
     * Async PDF generation với progress tracking
     */
    @Async
    public CompletableFuture<byte[]> generatePdfAsync(GeneratePdfRequest request,
                                                      String taskId) {
        try {
            // Update progress: 10%
            updateProgress(taskId, 10, "Đang tải template...");

            // Update progress: 30%
            updateProgress(taskId, 30, "Đang xử lý dữ liệu...");

            // Process data
            Map<String, Object> processedData = processImages(request.getData());

            // Update progress: 60%
            updateProgress(taskId, 60, "Đang tạo document...");

            // Generate PDF
            byte[] pdfBytes = eFormService.generatePdf(request);

            // Update progress: 100%
            updateProgress(taskId, 100, "Hoàn thành");

            return CompletableFuture.completedFuture(pdfBytes);
        } catch (Exception e) {
            updateProgress(taskId, -1, "Lỗi: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Batch PDF generation
     */
    @Async
    public CompletableFuture<List<byte[]>> generateBatchPdf(List<GeneratePdfRequest> requests) {
        List<byte[]> results = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            try {
                byte[] pdf = eFormService.generatePdf(requests.get(i));
                results.add(pdf);

                // Log progress
                log.info("Generated PDF {}/{}", i + 1, requests.size());
            } catch (Exception e) {
                log.error("Error generating PDF for request " + i, e);
                results.add(new byte[0]); // Empty PDF for failed generation
            }
        }

        return CompletableFuture.completedFuture(results);
    }

    /**
     * Template preview without saving
     */
    public PreviewResult previewTemplate(Long templateId, Map<String, Object> sampleData) {
        try {
            TemplateDto template = eFormService.getTemplate(templateId);

            // Create sample data for preview
            Map<String, Object> previewData = new HashMap<>();
            for (String variable : template.getVariables()) {
                if (sampleData.containsKey(variable)) {
                    previewData.put(variable, sampleData.get(variable));
                } else {
                    previewData.put(variable, "[" + variable + "]");
                }
            }

            // Generate preview (first page only)
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setFormId(null); // Use template directly
            request.setData(previewData);

            // This would need modification in the service to accept template ID directly
            byte[] previewPdf = generatePreviewPdf(templateId, previewData);

            return new PreviewResult(true, previewPdf, "Preview generated successfully");
        } catch (Exception e) {
            log.error("Error generating preview", e);
            return new PreviewResult(false, null, "Error: " + e.getMessage());
        }
    }

    /**
     * Configure POI-TL with advanced features
     */
    private Configure createAdvancedConfigure() {
        return Configure.builder()
                .useSpringEL() // Enable Spring Expression Language
                .addPlugin('=', new LoopRowTableRenderPolicy()) // Table loops
                .addPlugin('@', new PictureRenderPolicy()) // Image handling
                .build();
    }

    // Helper methods
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("^[0-9+\\-\\s()]{10,15}$");
    }

    private boolean isValidDate(String date) {
        try {
            // Support multiple date formats
            String[] patterns = {"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy"};
            for (String pattern : patterns) {
                try {
                    java.time.LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern(pattern));
                    return true;
                } catch (Exception ignored) {}
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateProgress(String taskId, int progress, String message) {
        // Implementation would depend on your progress tracking mechanism
        // Could use WebSocket, Redis, or database
        log.info("Task {}: {}% - {}", taskId, progress, message);
    }

    private byte[] generatePreviewPdf(Long templateId, Map<String, Object> data) throws Exception {
        // This would need to be implemented to work directly with template ID
        // For now, this is a placeholder
        return new byte[0];
    }

}
