package com.collective404.budget.controller;

import com.collective404.budget.service.PurchaseService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.collective404.budget.dto.PurchaseRequest;
import com.collective404.budget.model.Purchase;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    /** The service used by the controller */
    private final PurchaseService purchaseService;

    /** Constructs a PurchaseController
     *
     * @param purchaseService the service used by the controller
     */
    public PurchaseController(final PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /**
     * Posts a purchase to the client's requested category
     *
     * @param request the request with purchase information to be added
     * @return HTTP status 201 if successful, or 400 if required information is not included in the request
     * @throws IOException
     */
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
    public ResponseEntity<String> handleInvalidAmount(final IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
