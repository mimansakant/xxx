package com.collective404.budget.controller;

import com.collective404.budget.service.PurchaseService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.dto.PurchaseDeleteResponse;
import com.collective404.budget.model.Purchase;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.collective404.budget.validation.InputValidator
    
@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }
    @PostMapping
public ResponseEntity<Purchase> addPurchase(
        @RequestBody PurchaseRequest request) throws IOException {

    // Temporary: category selection will be implemented next.
    String categoryId = request.getCategory();

    Purchase purchase = purchaseService.createPurchase(request, categoryId);
    purchaseService.savePurchase(purchase);

    return ResponseEntity.status(HttpStatus.CREATED).body(purchase);
}
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleInvalidAmount(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
}

    /**
     * Deletes a purchase that belongs to the given client and budget.
     *
     * <p>Returns 200 with the recalculated category totals on success, 400 if
     * any id is not a valid UUID, 404 if the client, budget, or purchase is not
     * found (or the purchase does not belong to the budget), and 500 if the
     * datastore cannot be read.</p>
     *
     * @param id the id of the purchase to delete
     * @param clientId the caller's client id
     * @param budgetId the budget the purchase belongs to
     * @return the recalculated totals or an error body in the {status, reason} shape
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePurchase(
            @PathVariable("id") final String id,
            @RequestParam("clientId") final String clientId,
            @RequestParam("budgetId") final String budgetId) {
        // Reuse the shared helpers defined in BudgetController (same package).
        if (!InputValidator.isValidUuid(clientId)) {
            return BudgetController.error(HttpStatus.BAD_REQUEST, "clientId must be a valid UUID");
        }
        if (!InputValidator.isValidUuid(budgetId)) {
            return BudgetController.error(HttpStatus.BAD_REQUEST, "budgetId must be a valid UUID");
        }
        if (!InputValidator.isValidUuid(id)) {
            return BudgetController.error(HttpStatus.BAD_REQUEST, "purchase id must be a valid UUID");
        }
        try {
            final PurchaseDeleteResponse result =
                    purchaseService.deletePurchase(clientId, budgetId, id);
            if (result == null) {
                return BudgetController.error(
                        HttpStatus.NOT_FOUND,
                        "No purchase " + id + " found for the given client and budget");
            }
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return BudgetController.error(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Purchase data is unavailable");
        }
    }
}
