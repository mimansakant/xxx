package com.collective404.budget.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides synchronized access to the budget service's JSON data files.
 */
@Component
public final class BudgetDataStore {

    /** JSON mapper used for datastore files. */
    private final ObjectMapper objectMapper;
    /** Directory containing the JSON datastore files. */
    private final Path dataDirectory;
    /** Lock shared by all operations on this store. */
    private final Object lock = new Object();

    /**
     * Creates a store using the default data directory.
     *
     * @param objectMapper the JSON mapper
     */
    @Autowired
    public BudgetDataStore(final ObjectMapper objectMapper) {
        this(objectMapper, Paths.get("src", "main", "resources", "data"));
    }

    /**
     * Creates a store using the specified data directory.
     *
     * @param objectMapper the JSON mapper
     * @param dataDirectory the directory containing the data files
     */
    public BudgetDataStore(
            final ObjectMapper objectMapper,
            final Path dataDirectory
    ) {
        this.objectMapper = objectMapper;
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
    }

    /**
     * Runs a multi-step data operation while preventing other store operations
     * from reading or writing the files.
     *
     * @param <T> the operation result type
     * @param operation the operation to run
     * @return the operation result
     * @throws IOException if a file operation fails
     */
    public <T> T withExclusiveLock(
            final DataStoreOperation<T> operation
    ) throws IOException {
        synchronized (lock) {
            return operation.execute();
        }
    }

    /**
     * Reads a list from a JSON file.
     *
     * @param <T> the element type
     * @param fileName the file name
     * @param typeRef the type reference
     * @return the list
     * @throws IOException if an I/O error occurs
     */
    public <T> List<T> readList(
            final String fileName,
            final TypeReference<List<T>> typeRef
    ) throws IOException {
        synchronized (lock) {
            final Path filePath = getDataFilePath(fileName);
            if (Files.size(filePath) == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(filePath.toFile(), typeRef);
        }
    }

    /**
     * Writes a list to a JSON file.
     *
     * @param <T> the element type
     * @param fileName the file name
     * @param list the list to write
     * @throws IOException if an I/O error occurs
     */
    public <T> void writeList(
            final String fileName,
            final List<T> list
    ) throws IOException {
        synchronized (lock) {
            final Path filePath = getDataFilePath(fileName);
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(filePath.toFile(), list);
        }
    }

    private Path getDataFilePath(final String fileName) {
        final Path filePath = dataDirectory.resolve(fileName).normalize();
        if (!filePath.startsWith(dataDirectory)) {
            throw new IllegalArgumentException(
                    "Data file must be inside the data directory");
        }
        return filePath;
    }

    @FunctionalInterface
    public interface DataStoreOperation<T> {
        /**
         * Executes the data operation.
         *
         * @return the operation result
         * @throws IOException if a file operation fails
         */
        T execute() throws IOException;
    }
}
