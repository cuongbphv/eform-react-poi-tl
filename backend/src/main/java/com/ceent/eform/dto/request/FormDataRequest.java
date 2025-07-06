package com.ceent.eform.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormDataRequest {
    private Long templateId;
    private String name;
    private Map<String, Object> data;
}
