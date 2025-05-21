package com.slalom.mvn_component_generator.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.slalom.mvn_component_generator.dto.DialogValueDTO;
import com.slalom.mvn_component_generator.dto.FormDataDTO;
import com.slalom.mvn_component_generator.service.ComponentGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ComponentGeneratorServiceImpl implements ComponentGeneratorService {

    Logger logger = LoggerFactory.getLogger(ComponentGeneratorServiceImpl.class);

    /**
     * @param data the component form data
     * @return byte[] of generated component file structure
     */
    @Override
    public byte[] createZipWithFolderStructure(FormDataDTO data) {
        logger.info("In createZipWithFolderStructure");
        try {
            Gson gson = new Gson();
            List<DialogValueDTO> dialogValues = gson.fromJson(
                    data.getDialogValues(),
                    new TypeToken<List<DialogValueDTO>>() {}.getType()
            );

            logger.info("Dialog Values: {}", dialogValues);

            // Extract appId from form data
            String appId = extractAppId(data);
            if (appId == null || appId.isEmpty()) {
                appId = "myapp";
            }

            logger.info("App Id: {}", appId);

            // Extract componentName from form data
            String componentName = extractComponentName(data);
            if (componentName == null || componentName.isEmpty()) {
                componentName = "mycomponent";
            }

            logger.info("Component Name: {}", componentName);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zipOutputStream = new ZipOutputStream(baos)) {

                logger.info("In ByteArray/ZipOutput");

                // Create the required folder structure
                String uiAppsPath = "root/ui.apps/src/main/content/jcr_root/apps/" + appId + "/components/";

                // Add component-specific folder if componentName is provided
                if (componentName != null && !componentName.isEmpty()) {
                    uiAppsPath += componentName + "/";
                }

                logger.info("Ui App Path: {}", uiAppsPath);

                createDirectoryEntry(zipOutputStream, uiAppsPath);

                logger.info("Created ui.apps directory entry");

                // Create component files in ui.apps directory
                createComponentFiles(zipOutputStream, uiAppsPath, data, dialogValues);

                logger.info("Created component files");

                // 2. core structure for models
                String corePath = "root/core/src/main/java/core/models/";
                createDirectoryEntry(zipOutputStream, corePath);

                logger.info("Created core directory entry");

                // Create Sling model if requested
                if (data.isIncludeSlingModelFile()) {
                    createSlingModelFile(zipOutputStream, corePath, data);
                } else {
                    // Create a placeholder file in the core directory
                    createPlaceholderFile(zipOutputStream, corePath + ".gitkeep", "");
                }

                // 3. ui.frontend.general structure
                String uiFrontendPath = "root/ui.frontend.general/src/main/webpack/components/";
                if (componentName != null && !componentName.isEmpty()) {
                    uiFrontendPath += componentName + "/";
                }
                createDirectoryEntry(zipOutputStream, uiFrontendPath);

                // Create CSS and JS files if requested
                if (data.isIncludeCssFile()) {
                    createCssFile(zipOutputStream, uiFrontendPath, data);
                }

                if (data.isIncludeJsFile()) {
                    createJsFile(zipOutputStream, uiFrontendPath, data);
                }

                if (!data.isIncludeCssFile() && !data.isIncludeJsFile()) {
                    // Create a placeholder file in the ui.frontend directory
                    createPlaceholderFile(zipOutputStream, uiFrontendPath + ".gitkeep", "");
                }

                zipOutputStream.finish();
                return baos.toByteArray();
            }

        } catch (Exception e) {
            logger.error("Error creating zip file", e);
            return new byte[0]; // Return empty byte array on error
        }
    }

    /**
     * Creates a directory entry in the zip file
     *
     * @param zipOutputStream the zip output stream
     * @param directoryPath the directory path
     * @throws IOException if an I/O error occurs
     */
    private void createDirectoryEntry(ZipOutputStream zipOutputStream, String directoryPath) throws IOException {
        logger.info("In createDirectoryEntry");
        if (!directoryPath.endsWith("/")) {
            directoryPath += "/";
        }
        ZipEntry zipEntry = new ZipEntry(directoryPath);
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.closeEntry();
        logger.info("Leaving createDirectoryEntry");
    }

    /**
     * Creates a file in the zip with the given content
     *
     * @param zipOutputStream the zip output stream
     * @param filePath the file path
     * @param content the file content
     * @throws IOException if an I/O error occurs
     */
    private void createPlaceholderFile(ZipOutputStream zipOutputStream, String filePath, String content) throws IOException {
        ZipEntry zipEntry = new ZipEntry(filePath);
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    /**
     * Creates component files in the ui.apps directory
     *
     * @param zipOutputStream the zip output stream
     * @param basePath the base path for the component
     * @param data the form data
     * @param dialogValues the dialog values
     * @throws IOException if an I/O error occurs
     */
    private void createComponentFiles(ZipOutputStream zipOutputStream, String basePath,
                                      FormDataDTO data, List<DialogValueDTO> dialogValues) throws IOException {
        logger.info("In createComponentFiles");
        // Create .content.xml file with component metadata
        String contentXml = createContentXml(data);
        createPlaceholderFile(zipOutputStream, basePath + ".content.xml", contentXml);

        // Create _cq_dialog/.content.xml file with dialog definition
        if (dialogValues != null && !dialogValues.isEmpty()) {
            String dialogPath = basePath + "_cq_dialog/";
            createDirectoryEntry(zipOutputStream, dialogPath);

            String dialogXml = createDialogXml(data, dialogValues);
            createPlaceholderFile(zipOutputStream, dialogPath + ".content.xml", dialogXml);
        }

        // Create HTL template file
        String htmlTemplate = createHtmlTemplate(data, dialogValues);
        createPlaceholderFile(zipOutputStream, basePath + data.getComponentName() + ".html", htmlTemplate);
        logger.info("Leaving createComponentFiles");
    }

    /**
     * Creates a Sling Model file in the core directory
     *
     * @param zipOutputStream the zip output stream
     * @param basePath the base path for the model
     * @param data the form data
     * @throws IOException if an I/O error occurs
     */
    private void createSlingModelFile(ZipOutputStream zipOutputStream, String basePath, FormDataDTO data) throws IOException {
        String modelName = capitalize(data.getComponentName()) + "Model";
        String packageName = "core.models";

        StringBuilder modelContent = new StringBuilder();
        modelContent.append("package ").append(packageName).append(";\n\n");
        modelContent.append("import org.apache.sling.api.resource.Resource;\n");
        modelContent.append("import org.apache.sling.models.annotations.Model;\n");
        modelContent.append("import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;\n");
        modelContent.append("import javax.annotation.PostConstruct;\n\n");

        modelContent.append("@Model(adaptables = Resource.class)\n");
        modelContent.append("public class ").append(modelName).append(" {\n\n");

        // Add dialog fields as properties
        Gson gson = new Gson();
        List<DialogValueDTO> dialogValues = gson.fromJson(
                data.getDialogValues(),
                new TypeToken<List<DialogValueDTO>>() {}.getType()
        );

        if (dialogValues != null && !dialogValues.isEmpty()) {
            for (DialogValueDTO field : dialogValues) {
                String fieldType = mapFieldTypeToJavaType(field.getFieldType());
                modelContent.append("    @ValueMapValue\n");
                modelContent.append("    private ").append(fieldType).append(" ").append(field.getFieldName()).append(";\n\n");

                // Add getter
                modelContent.append("    public ").append(fieldType).append(" get")
                        .append(capitalize(field.getFieldName())).append("() {\n");
                modelContent.append("        return ").append(field.getFieldName()).append(";\n");
                modelContent.append("    }\n\n");
            }
        }

        modelContent.append("    @PostConstruct\n");
        modelContent.append("    protected void init() {\n");
        modelContent.append("        // initialization logic here\n");
        modelContent.append("    }\n");
        modelContent.append("}\n");

        createPlaceholderFile(zipOutputStream, basePath + modelName + ".java", modelContent.toString());
    }

    /**
     * Creates a CSS file in the ui.frontend directory
     *
     * @param zipOutputStream the zip output stream
     * @param basePath the base path for the CSS file
     * @param data the form data
     * @throws IOException if an I/O error occurs
     */
    private void createCssFile(ZipOutputStream zipOutputStream, String basePath, FormDataDTO data) throws IOException {
        StringBuilder cssContent = new StringBuilder();
        cssContent.append("/* CSS for ").append(data.getComponentTitle()).append(" */\n\n");
        cssContent.append(".").append(data.getComponentName()).append(" {\n");
        cssContent.append("    display: block;\n");
        cssContent.append("    margin: 0 auto;\n");
        cssContent.append("    padding: 1rem;\n");
        cssContent.append("}\n");

        createPlaceholderFile(zipOutputStream, basePath + data.getComponentName() + ".css", cssContent.toString());
    }

    /**
     * Creates a JavaScript file in the ui.frontend directory
     *
     * @param zipOutputStream the zip output stream
     * @param basePath the base path for the JS file
     * @param data the form data
     * @throws IOException if an I/O error occurs
     */
    private void createJsFile(ZipOutputStream zipOutputStream, String basePath, FormDataDTO data) throws IOException {
        String jsContent;

        if ("vanilla".equals(data.getJavascriptFlavor())) {
            jsContent = createVanillaJsFile(data);
        } else {
            jsContent = createReactJsFile(data);
        }

        createPlaceholderFile(zipOutputStream, basePath + data.getComponentName() + ".js", jsContent);
    }

    /**
     * Creates a vanilla JavaScript file content
     *
     * @param data the form data
     * @return the JS file content
     */
    private String createVanillaJsFile(FormDataDTO data) {
        StringBuilder jsContent = new StringBuilder();
        jsContent.append("/* Vanilla JavaScript for ").append(data.getComponentTitle()).append(" */\n\n");
        jsContent.append("(function() {\n");
        jsContent.append("    'use strict';\n\n");
        jsContent.append("    document.addEventListener('DOMContentLoaded', function() {\n");
        jsContent.append("        // Select all instances of the component\n");
        jsContent.append("        var components = document.querySelectorAll('.")
                .append(data.getComponentName()).append("');\n\n");
        jsContent.append("        // Initialize each instance\n");
        jsContent.append("        components.forEach(function(component) {\n");
        jsContent.append("            // Component initialization logic\n");
        jsContent.append("            console.log('Component initialized:', component);\n");
        jsContent.append("        });\n");
        jsContent.append("    });\n");
        jsContent.append("})();\n");

        return jsContent.toString();
    }

    /**
     * Creates an ES6 JavaScript file content
     *
     * @param data the form data
     * @return the JS file content
     */
    private String createReactJsFile(FormDataDTO data) {
        StringBuilder jsContent = new StringBuilder();
        jsContent.append("/* ES6 JavaScript for ").append(data.getComponentTitle()).append(" */\n\n");
        jsContent.append("class ").append(capitalize(data.getComponentName())).append(" {\n");
        jsContent.append("    constructor(element) {\n");
        jsContent.append("        this.element = element;\n");
        jsContent.append("        this.init();\n");
        jsContent.append("    }\n\n");
        jsContent.append("    init() {\n");
        jsContent.append("        // Component initialization logic\n");
        jsContent.append("        console.log('Component initialized:', this.element);\n");
        jsContent.append("    }\n");
        jsContent.append("}\n\n");
        jsContent.append("// Initialize all instances of the component\n");
        jsContent.append("document.addEventListener('DOMContentLoaded', () => {\n");
        jsContent.append("    const components = document.querySelectorAll('.")
                .append(data.getComponentName()).append("');\n");
        jsContent.append("    components.forEach(component => new ")
                .append(capitalize(data.getComponentName())).append("(component));\n");
        jsContent.append("});\n");

        return jsContent.toString();
    }

    /**
     * Creates the HTML template file for the component
     *
     * @param data the form data
     * @param dialogValues the dialog values
     * @return the HTML content
     */
    private String createHtmlTemplate(FormDataDTO data, List<DialogValueDTO> dialogValues) {
        StringBuilder html = new StringBuilder();

        // Add HTL template with header comment
        html.append("<!--/*\n");
        html.append("    ").append(data.getComponentTitle()).append("\n");
        if (data.getComponentDescription() != null && !data.getComponentDescription().isEmpty()) {
            html.append("    ").append(data.getComponentDescription()).append("\n");
        }
        html.append("*/-->\n\n");

        // Add data-sly-use if Sling Model is included
        if (data.isIncludeSlingModelFile()) {
            html.append("<div class=\"").append(data.getComponentName()).append("\"\n");
            html.append("     data-sly-use.model=\"");
            html.append("core.models.").append(capitalize(data.getComponentName())).append("Model\">\n");
        } else {
            html.append("<div class=\"").append(data.getComponentName()).append("\">\n");
        }

        // Add sample content based on dialog fields
        if (dialogValues != null && !dialogValues.isEmpty()) {
            for (DialogValueDTO field : dialogValues) {
                if (data.isIncludeSlingModelFile()) {
                    // Use Sling Model property
                    html.append("    <div class=\"").append(data.getComponentName()).append("__")
                            .append(field.getFieldName()).append("\">\n");
                    html.append("        <span>${model.").append(field.getFieldName()).append("}</span>\n");
                    html.append("    </div>\n");
                } else {
                    // Use direct property access
                    html.append("    <div class=\"").append(data.getComponentName()).append("__")
                            .append(field.getFieldName()).append("\">\n");
                    html.append("        <span data-sly-test=\"${properties.")
                            .append(field.getFieldName()).append("}\">${properties.")
                            .append(field.getFieldName()).append("}</span>\n");
                    html.append("    </div>\n");
                }
            }
        } else {
            // Add placeholder content if no dialog fields
            html.append("    <div>Component content goes here</div>\n");
        }

        html.append("</div>\n");

        return html.toString();
    }

    /**
     * Creates the .content.xml file for the component
     *
     * @param data the form data
     * @return the XML content
     */
    private String createContentXml(FormDataDTO data) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<jcr:root xmlns:cq=\"http://www.day.com/jcr/cq/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n");
        xml.append("    jcr:primaryType=\"cq:Component\"\n");
        xml.append("    jcr:title=\"").append(data.getComponentTitle()).append("\"\n");

        if (data.getComponentDescription() != null && !data.getComponentDescription().isEmpty()) {
            xml.append("    jcr:description=\"").append(data.getComponentDescription()).append("\"\n");
        }

        xml.append("    componentGroup=\"").append(data.getComponentGroup()).append("\"/>\n");

        return xml.toString();
    }

    /**
     * Creates the _cq_dialog/.content.xml file with dialog definition
     *
     * @param data the form data
     * @param dialogValues the dialog values
     * @return the XML content
     */
    private String createDialogXml(FormDataDTO data, List<DialogValueDTO> dialogValues) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<jcr:root xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" xmlns:cq=\"http://www.day.com/jcr/cq/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\"\n");
        xml.append("    jcr:primaryType=\"nt:unstructured\"\n");
        xml.append("    jcr:title=\"").append(data.getComponentTitle()).append("\"\n");
        xml.append("    sling:resourceType=\"cq/gui/components/authoring/dialog\">\n");
        xml.append("    <content\n");
        xml.append("        jcr:primaryType=\"nt:unstructured\"\n");
        xml.append("        sling:resourceType=\"granite/ui/components/coral/foundation/container\">\n");
        xml.append("        <items jcr:primaryType=\"nt:unstructured\">\n");
        xml.append("            <tabs\n");
        xml.append("                jcr:primaryType=\"nt:unstructured\"\n");
        xml.append("                sling:resourceType=\"granite/ui/components/coral/foundation/tabs\"\n");
        xml.append("                maximized=\"true\">\n");
        xml.append("                <items jcr:primaryType=\"nt:unstructured\">\n");
        xml.append("                    <properties\n");
        xml.append("                        jcr:primaryType=\"nt:unstructured\"\n");
        xml.append("                        jcr:title=\"Properties\"\n");
        xml.append("                        sling:resourceType=\"granite/ui/components/coral/foundation/container\">\n");
        xml.append("                        <items jcr:primaryType=\"nt:unstructured\">\n");
        xml.append("                            <column\n");
        xml.append("                                jcr:primaryType=\"nt:unstructured\"\n");
        xml.append("                                sling:resourceType=\"granite/ui/components/coral/foundation/container\">\n");
        xml.append("                                <items jcr:primaryType=\"nt:unstructured\">\n");

        // Add dialog fields
        for (DialogValueDTO field : dialogValues) {
            xml.append("                                    <").append(field.getFieldName()).append("\n");
            xml.append("                                        jcr:primaryType=\"nt:unstructured\"\n");
            xml.append("                                        sling:resourceType=\"granite/ui/components/coral/foundation/form/").append(mapFieldTypeToGraniteUI(field.getFieldType())).append("\"\n");
            xml.append("                                        fieldLabel=\"").append(field.getFieldLabel()).append("\"\n");
            xml.append("                                        name=\"./").append(field.getFieldName()).append("\"\n");

            if (field.isFieldRequired()) {
                xml.append("                                        required=\"true\"\n");
            }

            xml.append("                                    />\n");
        }

        xml.append("                                </items>\n");
        xml.append("                            </column>\n");
        xml.append("                        </items>\n");
        xml.append("                    </properties>\n");
        xml.append("                </items>\n");
        xml.append("            </tabs>\n");
        xml.append("        </items>\n");
        xml.append("    </content>\n");
        xml.append("</jcr:root>\n");

        return xml.toString();
    }

    /**
     * Maps a dialog field type to a Java type
     *
     * @param fieldType the dialog field type
     * @return the corresponding Java type
     */
    private String mapFieldTypeToJavaType(String fieldType) {
        // Updated switch to remove textfield, textarea, pathfield and select so that it defaults
        return switch (fieldType) {
            case "checkbox" -> "boolean";
            case "numberfield" -> "int";
            default -> "String";
        };
    }

    /**
     * Maps a dialog field type to a Granite UI component
     *
     * @param fieldType the dialog field type
     * @return the corresponding Granite UI component
     */
    private String mapFieldTypeToGraniteUI(String fieldType) {
        return switch (fieldType) {
            case "textarea" -> "textarea";
            case "checkbox" -> "checkbox";
            case "numberfield" -> "numberfield";
            case "pathfield" -> "pathfield";
            case "select" -> "select";
            default -> "textfield";
        };
    }

    /**
     * Capitalizes the first letter of a string
     *
     * @param str the string to capitalize
     * @return the capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Extracts the component name directly from the FormDataDTO
     *
     * @param data the form data
     * @return the component name or null if not found
     */
    private String extractComponentName(FormDataDTO data) {
        return data.getComponentName();
    }

    /**
     * Extracts the applicationId directly from the FormDataDTO
     *
     * @param data the form data
     * @return the applicationId or null if not found
     */
    private String extractAppId(FormDataDTO data) {
        return data.getApplicationId();
    }
}
