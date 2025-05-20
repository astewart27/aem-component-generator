package com.slalom.mvn_component_generator.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.slalom.mvn_component_generator.dto.DialogValueDTO;
import com.slalom.mvn_component_generator.dto.FormDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/generator")
public class Controller {

    Logger logger = LoggerFactory.getLogger(Controller.class);

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping( value = "/form-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> formData(@ModelAttribute FormDataDTO data) {
        logger.info("Form submission");
        String dialogValuesJson = data.getDialogValues();
        logger.info("Dialog values JSON: {}", dialogValuesJson);
        try {
            Gson gson = new Gson();

            List<DialogValueDTO> dialogValues = gson.fromJson(
                    dialogValuesJson,
                    new TypeToken<List<DialogValueDTO>>() {}.getType()
            );

            logger.info("Dialog values received: {}", dialogValues);

            dialogValues.forEach(value -> {
                logger.info("Field Label: {}", value.getFieldLabel());
            });

            // Use the parsed list as needed
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to parse dialogValues: " + e.getMessage());
        }
    }
}
