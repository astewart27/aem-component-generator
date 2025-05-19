package com.slalom.mvn_component_generator.dto;

import lombok.Data;

@Data
public class FormDataDTO {

    private String applicationId;
    private String applicationTitle;
    private String componentName;
    private String componentTitle;
    private String componentPath;
    private String componentGroup;
    private String componentDescription;
    private boolean includeSlingModelFile;
    private boolean includeCssFile;
    private boolean includeJsFile;
    private String javascriptFlavor;
}
