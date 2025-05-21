package com.slalom.mvn_component_generator.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.slalom.mvn_component_generator.dto.DialogValueDTO;
import com.slalom.mvn_component_generator.dto.FormDataDTO;
import com.slalom.mvn_component_generator.service.ComponentGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/generator")
public class Controller {

    Logger logger = LoggerFactory.getLogger(Controller.class);

    private final ComponentGeneratorService componentGeneratorService;

    public Controller(ComponentGeneratorService componentGeneratorService) {
        this.componentGeneratorService = componentGeneratorService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping( value = "/form-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> formData(@ModelAttribute FormDataDTO data) {
        logger.info("Form submission");
        try {
            // Pass the files and form fields to the service
            byte[] zipBytes = componentGeneratorService.createZipWithFolderStructure(data);

            // Return the zip file as a response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "aem-component.zip");

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(("Failed to parse dialogValues: " + e.getMessage()).getBytes());
        }
    }
}
