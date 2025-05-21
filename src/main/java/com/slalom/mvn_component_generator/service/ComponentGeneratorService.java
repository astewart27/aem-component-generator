package com.slalom.mvn_component_generator.service;

import com.slalom.mvn_component_generator.dto.FormDataDTO;

public interface ComponentGeneratorService {

    byte[] createZipWithFolderStructure(FormDataDTO data);
}
