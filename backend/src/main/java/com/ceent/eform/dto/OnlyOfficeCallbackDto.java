package com.ceent.eform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlyOfficeCallbackDto {
    private Integer status;
    private String url;
    private String key;
    private Map<String, Object> users;
    private Map<String, Object> actions;
    private String changesurl;
    private String history;
}
