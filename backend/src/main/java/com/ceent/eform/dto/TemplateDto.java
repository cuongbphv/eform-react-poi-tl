package com.ceent.eform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDto {
    private Long id;
    private String name;
    private String filename;
    private List<String> variables;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
