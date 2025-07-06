package com.ceent.eform.service;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.ceent.eform.entity.Form;
import com.ceent.eform.entity.Template;
import com.ceent.eform.repository.FormRepository;
import com.ceent.eform.repository.TemplateRepository;
import com.deepoove.poi.config.ConfigureBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.itextpdf.text.pdf.BaseFont;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfProService {

    private final TemplateRepository templateRepository;
    private final FormRepository formRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.output.dir:outputs}")
    private String outputDir;

    /**
     * Tạo PDF với font Times New Roman và UTF-8 support
     */
    public byte[] generateProPdf(Long formId, Map<String, Object> data) throws Exception {
        // Lấy thông tin form và template
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found with id: " + formId));

        Template template = form.getTemplate();
        String templatePath = template.getFilePath();

        log.info("Generating PDF for form ID: {}, template path: {}", formId, templatePath);

        // Kiểm tra file template tồn tại
        if (!Files.exists(Paths.get(templatePath))) {
            log.error("Template file not found at path: {}", templatePath);
            throw new RuntimeException("Template file not found: " + templatePath);
        }

        // Sử dụng data từ request hoặc form
        Map<String, Object> formData = data != null ? data :
                objectMapper.readValue(form.getFormData(), new TypeReference<Map<String, Object>>() {});

        log.info("Form data: {}", formData);

        return generatePdfFromTemplateWithProperFont(templatePath, formData);
    }

    /**
     * Tạo PDF từ template với font configuration đúng
     */
    private byte[] generatePdfFromTemplateWithProperFont(String templatePath, Map<String, Object> data) throws Exception {
        // Tạo thư mục output nếu chưa tồn tại
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            log.info("Created output directory: {}", outputPath.toAbsolutePath());
        }

        log.info("Starting PDF generation with proper font handling");

        // Step 1: Render template với POI-TL
        Configure config = createUtf8Configure();
        XWPFTemplate template = null;

        try {
            template = XWPFTemplate.compile(templatePath, config).render(data);
            log.info("Template rendered successfully");
        } catch (Exception e) {
            log.error("Error rendering template: {}", e.getMessage());
            throw new RuntimeException("Failed to render template: " + e.getMessage(), e);
        }

        // Step 2: Tạo file Word tạm thời
        String tempWordFile = outputPath.resolve("temp_" + System.currentTimeMillis() + ".docx").toString();

        try (FileOutputStream wordOut = new FileOutputStream(tempWordFile)) {
            template.write(wordOut);
            log.info("Temporary Word file created: {}", tempWordFile);
        } catch (Exception e) {
            log.error("Error writing temporary Word file: {}", e.getMessage());
            throw new RuntimeException("Failed to create temporary Word file", e);
        } finally {
            if (template != null) {
                template.close();
            }
        }

        // Step 3: Cải thiện font trong Word document
        try {
            improveWordDocumentFont(tempWordFile);
            log.info("Word document font improved");
        } catch (Exception e) {
            log.warn("Error improving Word document font: {}", e.getMessage());
        }

        // Step 4: Chuyển đổi sang PDF với font configuration
        byte[] pdfBytes;
        try {
            pdfBytes = convertToPdfWithFontConfig(tempWordFile);
            log.info("PDF conversion completed, size: {} bytes", pdfBytes.length);
        } catch (Exception e) {
            log.error("Error converting to PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to convert to PDF", e);
        }

        // Step 5: Xóa file tạm thời
        try {
            Files.deleteIfExists(Paths.get(tempWordFile));
            log.info("Temporary file deleted: {}", tempWordFile);
        } catch (Exception e) {
            log.warn("Could not delete temporary file: {}", tempWordFile);
        }

        return pdfBytes;
    }

    /**
     * Cải thiện font trong Word document trước khi convert
     */
    private void improveWordDocumentFont(String wordFilePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(wordFilePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            boolean documentChanged = false;

            // Cải thiện font cho tất cả paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (improveFont(paragraph)) {
                    documentChanged = true;
                }
            }

            // Cải thiện font cho tables
            document.getTables().forEach(table -> {
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> {
                        cell.getParagraphs().forEach(this::improveFont);
                    });
                });
            });

            // Lưu lại document nếu có thay đổi
            if (documentChanged) {
                try (FileOutputStream fos = new FileOutputStream(wordFilePath)) {
                    document.write(fos);
                }
                log.info("Word document font updated");
            }
        }
    }

    /**
     * Cải thiện font cho paragraph
     */
    private boolean improveFont(XWPFParagraph paragraph) {
        boolean changed = false;

        try {
            // Thiết lập spacing
            paragraph.setSpacingBetween(1.15);
            paragraph.setSpacingAfter(120);
            paragraph.setSpacingBefore(0);

            // Cải thiện font cho từng run
            for (XWPFRun run : paragraph.getRuns()) {
                // Thiết lập font Times New Roman
                String currentFont = run.getFontFamily();
                if (currentFont == null || !currentFont.equals("Times New Roman")) {
                    run.setFontFamily("Times New Roman");
                    changed = true;
                }

                // Thiết lập font size
                int currentSize = run.getFontSize();
                if (currentSize == -1 || currentSize != 12) {
                    run.setFontSize(12);
                    changed = true;
                }
            }
        } catch (Exception e) {
            log.warn("Error improving font for paragraph: {}", e.getMessage());
        }

        return changed;
    }

    /**
     * Chuyển đổi Word sang PDF với font configuration
     */
    private byte[] convertToPdfWithFontConfig(String wordFilePath) throws Exception {
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();

        try (FileInputStream wordIn = new FileInputStream(wordFilePath);
             XWPFDocument document = new XWPFDocument(wordIn)) {

            // Tạo PDF options với font configuration
            PdfOptions options = createPdfOptionsWithFont();

            // Chuyển đổi
            PdfConverter.getInstance().convert(document, pdfOut, options);

            log.info("PDF converted successfully with font configuration");
        }

        return pdfOut.toByteArray();
    }

    /**
     * Tạo PdfOptions với font configuration
     */
    private PdfOptions createPdfOptionsWithFont() {
        PdfOptions options = PdfOptions.create();

        try {
            // Thiết lập UTF-8 encoding
            options.fontEncoding("UTF-8");

            // Kiểm tra font resources có tồn tại
            checkFontResources();

            options.fontProvider((familyName, encoding, size, style, color) -> {
                try {
                    return FontFactory.getFont("fonts/times.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style, color);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Font was not found" + e);
                }
            });

            log.info("PDF options created with font configuration");

        } catch (Exception e) {
            log.warn("Error configuring PDF options: {}", e.getMessage());
        }

        return options;
    }

    /**
     * Kiểm tra font resources
     */
    private void checkFontResources() {
        String[] fontFiles = {
                "fonts/times.ttf",
                "fonts/timesbd.ttf",
                "fonts/timesi.ttf",
                "fonts/timesbi.ttf",
                "fonts/arial.ttf"
        };

        for (String fontPath : fontFiles) {
            try {
                ClassPathResource fontResource = new ClassPathResource(fontPath);
                if (fontResource.exists()) {
                    log.info("Font resource found: {}", fontPath);
                } else {
                    log.warn("Font resource not found: {}", fontPath);
                }
            } catch (Exception e) {
                log.warn("Error checking font resource {}: {}", fontPath, e.getMessage());
            }
        }
    }

    /**
     * Tạo cấu hình POI-TL
     */
    private Configure createUtf8Configure() {
        ConfigureBuilder builder = Configure.builder();
        return builder
                .useSpringEL(false)
                .build();
    }

    /**
     * Clean data cho PDF - giữ nguyên UTF-8
     */
    public Map<String, Object> cleanDataForPdf(Map<String, Object> rawData) {
        Map<String, Object> cleanData = new HashMap<>();

        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                cleanData.put(key, "");
            } else {
                String stringValue = value.toString();
                // Chỉ xử lý line breaks thừa, giữ nguyên dấu tiếng Việt
                stringValue = stringValue.replace("\r\n", " ")
                        .replace("\r", " ")
                        .replaceAll("  +", " ")
                        .trim();
                cleanData.put(key, stringValue);
            }
        }

        return cleanData;
    }

    /**
     * Format số tiền
     */
    public String formatCurrency(Object amount) {
        if (amount == null) return "0";

        try {
            long value = Long.parseLong(amount.toString().replaceAll("[^\\d]", ""));
            return String.format("%,d", value).replace(",", ".") + " VND";
        } catch (NumberFormatException e) {
            return amount.toString();
        }
    }

    /**
     * Format ngày tháng
     */
    public String formatDate(Object date) {
        if (date == null) return "";
        return date.toString();
    }

    /**
     * Debug paths và fonts
     */
    public void debugTemplatePaths() {
        log.info("=== Debug Template Paths ===");
        log.info("Upload directory: {}", uploadDir);
        log.info("Output directory: {}", outputDir);

        // Debug upload directory
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (Files.exists(uploadPath)) {
                log.info("Upload directory exists");
                Files.list(uploadPath).forEach(file ->
                        log.info("File in upload dir: {}", file.toString())
                );
            } else {
                log.warn("Upload directory does not exist: {}", uploadPath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error checking upload directory: {}", e.getMessage());
        }

        // Debug font resources
        log.info("=== Debug Font Resources ===");
        checkFontResources();

        log.info("=== End Debug ===");
    }
}