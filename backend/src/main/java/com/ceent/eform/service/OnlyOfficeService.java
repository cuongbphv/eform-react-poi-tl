package com.ceent.eform.service;

import com.ceent.eform.dto.OnlyOfficeConfigDto;
import com.ceent.eform.entity.Template;
import com.ceent.eform.repository.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlyOfficeService {

    private final TemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    @Value("${onlyoffice.docs.url:http://localhost:80}")
    private String onlyOfficeUrl;

    @Value("${onlyoffice.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.server.url:http://localhost:8080}")
    private String serverUrl;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Tạo OnlyOffice config cho template editing
     */
    public OnlyOfficeConfigDto createEditorConfig(Long templateId, String userId, String userName) throws Exception {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        String documentKey = generateDocumentKey(templateId);
        String fileUrl = serverUrl + "/api/v1/onlyoffice/files/" + templateId;
        String callbackUrl = serverUrl + "/api/v1/onlyoffice/callback/" + templateId;

        log.info("Creating OnlyOffice config for template {}", templateId);
        log.info("File URL: {}", fileUrl);
        log.info("Callback URL: {}", callbackUrl);
        log.info("Document Key: {}", documentKey);

        // Create complete configuration object theo OnlyOffice spec
        Map<String, Object> config = new HashMap<>();

        // Document configuration
        Map<String, Object> document = createDocumentConfig(template, documentKey, fileUrl);
        config.put("document", document);
        config.put("documentType", "word");
        config.put("editorConfig", createEditorConfig(userId, userName, callbackUrl));
        config.put("width", "100%");
        config.put("height", "100%");

        // Generate JWT token if secret is configured
        String token = null;
        if (jwtSecret != null && !jwtSecret.trim().isEmpty()) {
            try {
                token = generateOnlyOfficeJwtToken(config);
                log.info("JWT token generated successfully for template {}", templateId);
                log.debug("JWT payload size: {} characters", objectMapper.writeValueAsString(config).length());
            } catch (Exception e) {
                log.error("Failed to generate JWT token for template {}: {}", templateId, e.getMessage());
                throw new RuntimeException("JWT token generation failed", e);
            }
        } else {
            log.warn("JWT secret not configured - OnlyOffice will run without security");
        }

        // Build DTO response
        OnlyOfficeConfigDto configDto = OnlyOfficeConfigDto.builder()
                .documentServerUrl(onlyOfficeUrl + "/web-apps/apps/api/documents/api.js")
                .document(document)
                .documentType("word")
                .editorConfig((Map<String, Object>) config.get("editorConfig"))
                .width("100%")
                .height("100%")
                .token(token)
                .build();

        log.info("OnlyOffice config created successfully for template {}", templateId);
        return configDto;
    }

    /**
     * Tạo document config theo OnlyOffice specification
     */
    private Map<String, Object> createDocumentConfig(Template template, String documentKey, String fileUrl) {
        Map<String, Object> document = new HashMap<>();
        document.put("fileType", "docx");
        document.put("key", documentKey);
        document.put("title", template.getName() + ".docx");
        document.put("url", fileUrl);

        // Document info
        Map<String, Object> info = new HashMap<>();
        info.put("author", "E-Form System");
        info.put("created", System.currentTimeMillis());
        document.put("info", info);

        // Permissions - đầy đủ quyền cho editing
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("edit", true);
        permissions.put("download", true);
        permissions.put("print", true);
        permissions.put("review", true);
        permissions.put("comment", true);
        permissions.put("fillForms", true);
        permissions.put("modifyFilter", true);
        permissions.put("modifyContentControl", true);
        permissions.put("copy", true);
        document.put("permissions", permissions);

        return document;
    }

    /**
     * Tạo editor config theo OnlyOffice specification
     */
    private Map<String, Object> createEditorConfig(String userId, String userName, String callbackUrl) {
        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("mode", "edit");
        editorConfig.put("lang", "vi");
        editorConfig.put("callbackUrl", callbackUrl);

        // User info
        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("name", userName);
        user.put("group", "");
        editorConfig.put("user", user);

        // Customization settings
        Map<String, Object> customization = new HashMap<>();
        customization.put("autosave", true);
        customization.put("forcesave", true);
        customization.put("compactToolbar", false);
        customization.put("toolbarNoTabs", false);
        customization.put("chat", false);
        customization.put("comments", true);
        customization.put("help", false);
        customization.put("hideRightMenu", false);
        customization.put("plugins", true);
        customization.put("zoom", 100);
        customization.put("about", true);
        customization.put("feedback", false);

        // Go back configuration
        Map<String, Object> goback = new HashMap<>();
        goback.put("url", "#");
        goback.put("text", "Đóng Editor");
        goback.put("requestClose", true);
        customization.put("goback", goback);

        editorConfig.put("customization", customization);

        return editorConfig;
    }

    /**
     * Generate document key theo OnlyOffice requirements
     */
    private String generateDocumentKey(Long templateId) {
        // OnlyOffice requires unique key for each edit session
        return "template_" + templateId + "_" + System.currentTimeMillis();
    }

    /**
     * Generate JWT token theo OnlyOffice specification - FIXED
     */
    private String generateOnlyOfficeJwtToken(Map<String, Object> payload) throws Exception {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret is required but not configured");
        }

        try {
            // OnlyOffice JWT Header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            // Encode header và payload với Base64URL (no padding)
            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes("UTF-8"));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes("UTF-8"));

            // Create signature input
            String signatureInput = encodedHeader + "." + encodedPayload;

            // Generate HMAC-SHA256 signature
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(signatureInput.getBytes("UTF-8"));

            // Encode signature với Base64URL (no padding)
            String encodedSignature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signatureBytes);

            // Combine tất cả parts
            String jwtToken = signatureInput + "." + encodedSignature;

            log.debug("JWT Token generated:");
            log.debug("Header: {}", headerJson);
            log.debug("Payload length: {} characters", payloadJson.length());
            log.debug("Token length: {} characters", jwtToken.length());

            return jwtToken;

        } catch (Exception e) {
            log.error("Error generating OnlyOffice JWT token", e);
            throw new RuntimeException("Failed to generate JWT token: " + e.getMessage(), e);
        }
    }

    /**
     * Verify JWT token từ OnlyOffice callbacks
     */
    public boolean verifyJwtToken(String token, Map<String, Object> payload) {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            log.warn("JWT secret not configured, skipping token verification");
            return true;
        }

        try {
            String expectedToken = generateOnlyOfficeJwtToken(payload);
            boolean isValid = token.equals(expectedToken);

            if (!isValid) {
                log.warn("JWT token verification failed");
                log.debug("Expected: {}", expectedToken);
                log.debug("Received: {}", token);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying JWT token", e);
            return false;
        }
    }

    /**
     * Handle callback từ OnlyOffice khi save document
     */
    public Map<String, Object> handleCallback(Long templateId, Map<String, Object> callbackData) throws Exception {
        log.info("Received OnlyOffice callback for template {}: {}", templateId, callbackData);

        try {
            Integer status = (Integer) callbackData.get("status");
            String downloadUrl = (String) callbackData.get("url");

            Map<String, Object> response = new HashMap<>();

            // Status meanings:
            // 0 = not found
            // 1 = editing
            // 2 = ready for saving
            // 3 = save error
            // 4 = closed with no changes
            // 6 = editing (force save)
            // 7 = save error (force save)

            if (status != null && (status == 2 || status == 6)) {
                if (downloadUrl != null && !downloadUrl.isEmpty()) {
                    try {
                        downloadAndSaveDocument(templateId, downloadUrl);
                        log.info("Template {} updated successfully from OnlyOffice", templateId);
                        response.put("error", 0);
                    } catch (Exception e) {
                        log.error("Error saving document for template {}: {}", templateId, e.getMessage());
                        response.put("error", 1);
                        response.put("message", "Failed to save document: " + e.getMessage());
                    }
                } else {
                    log.warn("No download URL provided in callback for template {}", templateId);
                    response.put("error", 1);
                    response.put("message", "No download URL provided");
                }
            } else {
                // Acknowledge other statuses
                response.put("error", 0);
                log.info("Callback acknowledged for template {} with status {}", templateId, status);
            }

            return response;

        } catch (Exception e) {
            log.error("Error processing callback for template {}: {}", templateId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", 1);
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Download document từ OnlyOffice và save
     */
    private void downloadAndSaveDocument(Long templateId, String downloadUrl) throws Exception {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        log.info("Downloading updated document from: {}", downloadUrl);

        // Download file từ OnlyOffice
        try (InputStream inputStream = new URL(downloadUrl).openStream()) {
            Path filePath = Paths.get(template.getFilePath());

            // Create backup của original file
            Path backupPath = Paths.get(template.getFilePath().replace(".docx", "_backup_" + System.currentTimeMillis() + ".docx"));
            if (Files.exists(filePath)) {
                Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Backup created: {}", backupPath);
            }

            // Save new version
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Template file updated successfully: {}", template.getFilePath());
        }
    }

    /**
     * Serve file cho OnlyOffice
     */
    public Resource loadFileAsResource(Long templateId) throws Exception {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        Path filePath = Paths.get(template.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            log.info("Serving template file: {}", template.getFilePath());
            return resource;
        } else {
            throw new RuntimeException("Could not read template file: " + template.getFilePath());
        }
    }

    /**
     * Get file info cho OnlyOffice
     */
    public Map<String, Object> getFileInfo(Long templateId) throws Exception {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        Path filePath = Paths.get(template.getFilePath());

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("id", templateId);
        fileInfo.put("name", template.getName() + ".docx");
        fileInfo.put("size", Files.size(filePath));
        fileInfo.put("lastModified", Files.getLastModifiedTime(filePath).toMillis());
        fileInfo.put("downloadUrl", serverUrl + "/api/v1/onlyoffice/files/" + templateId);

        return fileInfo;
    }
}