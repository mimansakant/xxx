package com.collective404.budget.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import com.collective404.budget.model.Budget;
import com.collective404.budget.model.Category;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Defines budget-related operations.
 */
@Service
public final class BudgetService {

    /** The object mapper. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reads a list from a JSON file.
     *
     * @param <T> the element type
     * @param fileName the file name
     * @param typeRef the type reference
     * @return the list
     * @throws IOException if an I/O error occurs
     */
    private <T> List<T> readList(
            final String fileName,
            final TypeReference<List<T>> typeRef
    ) throws IOException {
        final Path filePath = getDataFilePath(fileName);
        if (Files.size(filePath) == 0) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(filePath.toFile(), typeRef);
    }

    /**
     * Writes a list to a JSON file.
     *
     * @param <T> the element type
     * @param fileName the file name
     * @param list the list to write
     * @throws IOException if an I/O error occurs
     */
    private <T> void writeList(
            final String fileName,
            final List<T> list
    ) throws IOException {
        final Path filePath = getDataFilePath(fileName);
        Files.createDirectories(filePath.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), list);
    }

    /**
     * Resolves a data file while keeping it inside the JSON data directory.
     *
     * @param fileName the file name
     * @return the resolved data file path
     */
    private Path getDataFilePath(final String fileName) {
        final Path dataDirectory = Paths.get("src", "main", "resources", "data")
                .toAbsolutePath()
                .normalize();
        final Path filePath = dataDirectory.resolve(fileName).normalize();
        if (!filePath.startsWith(dataDirectory)) {
            throw new IllegalArgumentException("Data file must be inside the data directory");
        }
        return filePath;
    }

    /**
     * Creates a category in the caller's budget, creating a budget if needed.
     *
     * @param clientId the caller's client identifier
     * @param budgetId the budget identifier, or {@code null} to create a budget
     * @param name the category name
     * @param limit the category spending limit
     * @return the created category
     */
    Category createCategory(String clientId, String budgetId, String name, BigDecimal limit) {
        return null;
    };
}
