package com.slalom.mvn_component_generator.dto;

import lombok.Data;

@Data
public class DialogValueDTO {

    private String fieldLabel;
    private String fieldName;
    private String fieldType;
    private boolean isFieldRequired;
}
