package com.ceent.eform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlyOfficePermissionsDto {
    private Boolean edit;
    private Boolean download;
    private Boolean print;
    private Boolean review;
    private Boolean comment;
}
