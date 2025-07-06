package com.ceent.eform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlyOfficeConfigDto {
    private String documentServerUrl;
    private Map<String, Object> document;
    private String documentType;
    private Map<String, Object> editorConfig;
    private String width;
    private String height;
    private String token;
}
