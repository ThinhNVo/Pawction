package com.voti.pawction.services.storage;

import com.voti.pawction.exceptions.PetExceptions.StorageException;
import com.voti.pawction.exceptions.PetExceptions.ValidationException;
import com.voti.pawction.services.storage.impl.FileStorageServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService  implements FileStorageServiceInterface {

    private final Path rootLocation = Paths.get("uploads");

    /**
     * Initializes the storage service by ensuring the "uploads" directory exists.
     *
     * @throws RuntimeException if the directory cannot be created
     */
    public FileStorageService() {
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    /**
     * Stores a file in the storage system.
     *
     * @param file the multipart file to store
     * @return the path or URL where the file is stored
     * @throws ValidationException if the file is empty or exceeds size limits
     * @throws RuntimeException if the storage operation fails
     */
    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new ValidationException("Failed to store empty file.");
            }

            // Check file size (300 KB = 300 * 1024 bytes)
            if (file.getSize() > 300 * 1024) {
                throw new ValidationException("File size must not exceed 300 KB.");
            }

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path destinationFile = rootLocation.resolve(filename).normalize().toAbsolutePath();

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + filename;
        } catch (IOException e) {
            // Use domain-specific StorageException instead of generic RuntimeException
            throw new StorageException("Failed to store file: " + e.getMessage());
        }
    }
}
