package com.ceent.eform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormDto {
    private Long id;
    private Long templateId;
    private String templateName;
    private String name;
    private Map<String, Object> formData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
