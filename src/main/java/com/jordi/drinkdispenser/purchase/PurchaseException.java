package com.jordi.drinkdispenser.purchase;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class PurchaseException extends Exception {

    enum PurchaseError {
        purchaseNotFound,
        invalidPurchaseStatus
    }

    private PurchaseError error = null;

    PurchaseException(PurchaseError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return error.name();
    }
    
}
