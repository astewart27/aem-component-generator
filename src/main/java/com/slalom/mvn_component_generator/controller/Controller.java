package com.slalom.mvn_component_generator.controller;

import com.slalom.mvn_component_generator.dto.FormDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        logger.info("Application Id: {}", data.getApplicationId());
        logger.info("Application Title: {}", data.getApplicationTitle());
        return ResponseEntity.ok(data);
    }
}
