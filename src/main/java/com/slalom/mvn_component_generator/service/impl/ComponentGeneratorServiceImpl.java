package com.slalom.mvn_component_generator.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.slalom.mvn_component_generator.controller.Controller;
import com.slalom.mvn_component_generator.dto.DialogValueDTO;
import com.slalom.mvn_component_generator.dto.FormDataDTO;
import com.slalom.mvn_component_generator.service.ComponentGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComponentGeneratorServiceImpl implements ComponentGeneratorService {

    Logger logger = LoggerFactory.getLogger(ComponentGeneratorServiceImpl.class);

    /**
     * @param data the component form data
     * @return byte[] of generated component file structure
     */
    @Override
    public byte[] createZipWithFolderStructure(FormDataDTO data) {
        String dialogValuesJson = data.getDialogValues();
        logger.info("Dialog values JSON: {}", dialogValuesJson);

        try {
            Gson gson = new Gson();
            List<DialogValueDTO> dialogValues = gson.fromJson(
                    dialogValuesJson,
                    new TypeToken<List<DialogValueDTO>>() {}.getType()
            );
            logger.info("Dialog values received: {}", dialogValues);

            dialogValues.forEach(dialogField -> logger.info("Field Required: {}", dialogField.isFieldRequired()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(("Failed to parse dialogValues: " + e.getMessage()).getBytes()).getBody();
        }
        return new byte[0];
    }
}
