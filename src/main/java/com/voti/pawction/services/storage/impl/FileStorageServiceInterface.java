package com.voti.pawction.services.storage.impl;

import org.springframework.web.multipart.MultipartFile;
import com.voti.pawction.exceptions.PetExceptions.ValidationException;

public interface FileStorageServiceInterface {
        /**
         * Stores a file in the storage system.
         *
         * @param file the multipart file to store
         * @return the path or URL where the file is stored
         * @throws ValidationException if the file is empty or exceeds size limits
         * @throws RuntimeException if the storage operation fails
         */
         String store(MultipartFile file);
}


