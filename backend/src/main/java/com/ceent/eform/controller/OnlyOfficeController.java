package com.ceent.eform.controller;

import com.ceent.eform.dto.OnlyOfficeConfigDto;
import com.ceent.eform.service.OnlyOfficeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/onlyoffice")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OnlyOfficeController {

    private final OnlyOfficeService onlyOfficeService;

    @Value("${onlyoffice.jwt.secret:}")
    private String jwtSecret;

    @Value("${onlyoffice.docs.url:http://localhost:80}")
    private String onlyOfficeUrl;

    /**
     * Get OnlyOffice editor config cho template - ENHANCED
     */
    @GetMapping("/config/{templateId}")
    public ResponseEntity<?> getEditorConfig(
            @PathVariable Long templateId,
            @RequestParam(defaultValue = "user1") String userId,
            @RequestParam(defaultValue = "Editor") String userName) {
        try {
            log.info("Getting OnlyOffice config for template {}, user: {}", templateId, userId);

            // Validate inputs
            if (templateId == null || templateId <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid template ID"));
            }

            OnlyOfficeConfigDto config = onlyOfficeService.createEditorConfig(templateId, userId, userName);

            log.info("OnlyOffice config created successfully for template {}", templateId);
            return ResponseEntity.ok(config);

        } catch (RuntimeException e) {
            log.error("Template not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error creating OnlyOffice config for template {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to create editor config",
                            "message", e.getMessage(),
                            "templateId", templateId
                    ));
        }
    }

    /**
     * Serve file cho OnlyOffice Document Server
     */
    @GetMapping("/files/{templateId}")
    public ResponseEntity<?> getFile(@PathVariable Long templateId) {
        try {
            log.info("Serving file for template {}", templateId);

            Resource resource = onlyOfficeService.loadFileAsResource(templateId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "template.docx");
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "*");

            log.info("File served successfully for template {}", templateId);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (RuntimeException e) {
            log.error("Template not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Template not found", "templateId", templateId));
        } catch (Exception e) {
            log.error("Error serving file for template {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to serve file",
                            "message", e.getMessage(),
                            "templateId", templateId
                    ));
        }
    }

    /**
     * Handle callback từ OnlyOffice khi document được save - ENHANCED
     */
    @PostMapping("/callback/{templateId}")
    public ResponseEntity<Map<String, Object>> handleCallback(
            @PathVariable Long templateId,
            @RequestBody Map<String, Object> callbackData,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        try {
            log.info("OnlyOffice callback received for template {}", templateId);
            log.debug("Callback data: {}", callbackData);
            log.debug("Authorization header: {}", authorization);

            // Verify JWT token if present
            if (jwtSecret != null && !jwtSecret.trim().isEmpty() && authorization != null) {
                String token = authorization.replace("Bearer ", "");
                boolean isValid = onlyOfficeService.verifyJwtToken(token, callbackData);

                if (!isValid) {
                    log.warn("Invalid JWT token in callback for template {}", templateId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", 1, "message", "Invalid JWT token"));
                }
            }

            Map<String, Object> response = onlyOfficeService.handleCallback(templateId, callbackData);

            log.info("Callback processed successfully for template {}", templateId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error handling OnlyOffice callback for template {}", templateId, e);
            return ResponseEntity.ok(Map.of(
                    "error", 1,
                    "message", e.getMessage(),
                    "templateId", templateId
            ));
        }
    }

    /**
     * Get file info
     */
    @GetMapping("/info/{templateId}")
    public ResponseEntity<?> getFileInfo(@PathVariable Long templateId) {
        try {
            log.info("Getting file info for template {}", templateId);

            Map<String, Object> fileInfo = onlyOfficeService.getFileInfo(templateId);
            return ResponseEntity.ok(fileInfo);

        } catch (RuntimeException e) {
            log.error("Template not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Template not found", "templateId", templateId));
        } catch (Exception e) {
            log.error("Error getting file info for template {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to get file info",
                            "message", e.getMessage(),
                            "templateId", templateId
                    ));
        }
    }

    /**
     * Test OnlyOffice connection và JWT configuration
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "ok");
            status.put("message", "OnlyOffice integration is ready");
            status.put("timestamp", System.currentTimeMillis());

            // Test OnlyOffice server connection
            try {
                // You could add actual health check here
                status.put("onlyoffice_server", onlyOfficeUrl);
                status.put("onlyoffice_available", "unknown"); // Would need actual check
            } catch (Exception e) {
                status.put("onlyoffice_available", false);
                status.put("onlyoffice_error", e.getMessage());
            }

            // JWT configuration status
            boolean jwtConfigured = jwtSecret != null && !jwtSecret.trim().isEmpty();
            status.put("jwt_configured", jwtConfigured);
            status.put("jwt_length", jwtConfigured ? jwtSecret.length() : 0);

            if (!jwtConfigured) {
                status.put("warning", "JWT secret not configured - OnlyOffice will run without security");
            }

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Error testing OnlyOffice connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    /**
     * Debug endpoint để check JWT token generation
     */
    @GetMapping("/debug/jwt/{templateId}")
    public ResponseEntity<Map<String, Object>> debugJwt(@PathVariable Long templateId) {
        try {
            Map<String, Object> debug = new HashMap<>();

            // JWT Secret info
            boolean secretConfigured = jwtSecret != null && !jwtSecret.trim().isEmpty();
            debug.put("jwt_secret_configured", secretConfigured);
            debug.put("jwt_secret_length", secretConfigured ? jwtSecret.length() : 0);

            if (secretConfigured) {
                debug.put("jwt_secret_first_chars", jwtSecret.substring(0, Math.min(8, jwtSecret.length())) + "...");
            }

            // Try to create config
            try {
                OnlyOfficeConfigDto config = onlyOfficeService.createEditorConfig(templateId, "debug_user", "Debug User");
                debug.put("config_creation", "success");
                debug.put("token_generated", config.getToken() != null);

                if (config.getToken() != null) {
                    debug.put("token_length", config.getToken().length());
                    debug.put("token_parts", config.getToken().split("\\.").length);
                }

                debug.put("document_key", config.getDocument().get("key"));
                debug.put("file_url", config.getDocument().get("url"));

            } catch (Exception e) {
                debug.put("config_creation", "failed");
                debug.put("error", e.getMessage());
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * CORS preflight handler
     */
    @RequestMapping(method = RequestMethod.OPTIONS, value = "/**")
    public ResponseEntity<?> handleOptions() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
        headers.add("Access-Control-Max-Age", "3600");

        return ResponseEntity.ok().headers(headers).build();
    }
}