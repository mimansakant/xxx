package com.collective404.budget.controller;

import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.model.Purchase;
import com.collective404.budget.service.PurchaseService;
import com.collective404.budget.validation.InputValidator;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** REST endpoint for recording purchases. */
@RestController
@RequestMapping("/purchases")
public final class PurchaseController {

    /** Service for purchase operations. */
    private final PurchaseService purchaseService;

    /**
     * Creates the controller.
     *
     * @param purchaseService service used for purchases
     */
    public PurchaseController(final PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /**
     * Records a purchase after validating its input.
     *
     * @param request purchase information
     * @return the newly created purchase
     * @throws IOException when the datastore cannot be accessed
     */
    @PostMapping
    public ResponseEntity<Purchase> addPurchase(
            @RequestBody final PurchaseRequest request) throws IOException {
        if (!InputValidator.isValidPurchaseRequest(request)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid purchase request");
        }

        final Purchase purchase = purchaseService.createPurchase(
                request, request.getCategory());
        purchaseService.savePurchase(purchase);
        return ResponseEntity.status(HttpStatus.CREATED).body(purchase);
    }
}
