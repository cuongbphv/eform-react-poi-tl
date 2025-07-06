package com.ceent.eform.controller;

import com.ceent.eform.dto.*;
import com.ceent.eform.dto.request.FormDataRequest;
import com.ceent.eform.dto.request.GeneratePdfRequest;
import com.ceent.eform.service.EFormService;
import com.ceent.eform.service.PdfProService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class EFormController {

    private final EFormService eFormService;
    private final PdfProService pdfProService;

    @PostMapping("/templates/upload")
    public ResponseEntity<TemplateDto> uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String templateName) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (!file.getOriginalFilename().toLowerCase().endsWith(".docx")) {
                return ResponseEntity.badRequest().build();
            }

            TemplateDto template = eFormService.uploadTemplate(file, templateName);
            log.info("Template uploaded successfully: {}", template);

            return ResponseEntity.ok(template);
        } catch (Exception e) {
            log.error("Error uploading template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<List<TemplateDto>> getAllTemplates() {
        try {
            List<TemplateDto> templates = eFormService.getAllTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            log.error("Error getting templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<TemplateDto> getTemplate(@PathVariable Long id) {
        try {
            TemplateDto template = eFormService.getTemplate(id);
            return ResponseEntity.ok(template);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/forms")
    public ResponseEntity<FormDto> saveForm(@RequestBody FormDataRequest request) {
        try {
            // Clean data trước khi lưu
            Map<String, Object> cleanData = pdfProService.cleanDataForPdf(request.getData());
            request.setData(cleanData);

            FormDto form = eFormService.saveForm(request);
            log.info("Form saved successfully: {}", form);

            return ResponseEntity.ok(form);
        } catch (RuntimeException e) {
            log.error("Error saving form: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error saving form", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/forms")
    public ResponseEntity<List<FormDto>> getAllForms() {
        try {
            List<FormDto> forms = eFormService.getAllForms();
            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            log.error("Error getting forms", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/forms/{id}")
    public ResponseEntity<FormDto> getForm(@PathVariable Long id) {
        try {
            FormDto form = eFormService.getForm(id);
            return ResponseEntity.ok(form);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting form", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate PDF với format cải thiện
     */
    @PostMapping("/forms/{id}/generate-pdf-pro")
    public ResponseEntity<byte[]> generateProPdfFromForm(@PathVariable Long id) {
        try {
            log.info("Generating PDF for form ID: {}", id);

            // Debug template paths
            pdfProService.debugTemplatePaths();

            byte[] pdfBytes = pdfProService.generateProPdf(id, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "pro_form_" + id + ".pdf");
            headers.setContentLength(pdfBytes.length);

            log.info("PDF generated successfully for form ID: {}, size: {} bytes", id, pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (RuntimeException e) {
            log.error("Runtime error generating PDF for form {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(("Error: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Error generating PDF from form {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Internal error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Generate PDF chuẩn (fallback)
     */
    @PostMapping("/forms/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(@RequestBody GeneratePdfRequest request) {
        try {
            byte[] pdfBytes = eFormService.generatePdf(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "generated_form.pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/forms/{id}/generate-pdf")
    public ResponseEntity<byte[]> generatePdfFromForm(@PathVariable Long id) {
        try {
            GeneratePdfRequest request = new GeneratePdfRequest();
            request.setFormId(id);

            byte[] pdfBytes = eFormService.generatePdf(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "form_" + id + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating PDF from form", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Debug endpoint để kiểm tra file system
     */
    @GetMapping("/debug/files")
    public ResponseEntity<Map<String, Object>> debugFiles() {
        Map<String, Object> debug = new HashMap<>();

        try {
            // Kiểm tra thư mục uploads
            boolean uploadsExists = Files.exists(Paths.get("uploads"));
            debug.put("uploads_directory_exists", uploadsExists);

            if (uploadsExists) {
                List<String> files = Files.list(Paths.get("uploads"))
                        .map(path -> path.toString())
                        .toList();
                debug.put("files_in_uploads", files);
            }

            // Kiểm tra thư mục hiện tại
            debug.put("current_directory", System.getProperty("user.dir"));

            // Kiểm tra templates từ database
            List<TemplateDto> templates = eFormService.getAllTemplates();
            debug.put("templates_in_database", templates.size());

            for (TemplateDto template : templates) {
                // Kiểm tra file path của từng template
                debug.put("template_" + template.getId() + "_exists", "checking...");
            }

            pdfProService.debugTemplatePaths();

        } catch (Exception e) {
            debug.put("error", e.getMessage());
            log.error("Debug error", e);
        }

        return ResponseEntity.ok(debug);
    }

    /**
     * Endpoint để test format data
     */
    @PostMapping("/forms/test-format")
    public ResponseEntity<Map<String, Object>> testDataFormat(@RequestBody Map<String, Object> rawData) {
        try {
            Map<String, Object> cleanData = pdfProService.cleanDataForPdf(rawData);
            return ResponseEntity.ok(cleanData);
        } catch (Exception e) {
            log.error("Error testing data format", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint để format currency
     */
    @GetMapping("/utils/format-currency/{amount}")
    public ResponseEntity<String> formatCurrency(@PathVariable String amount) {
        try {
            String formatted = pdfProService.formatCurrency(amount);
            return ResponseEntity.ok(formatted);
        } catch (Exception e) {
            log.error("Error formatting currency", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}