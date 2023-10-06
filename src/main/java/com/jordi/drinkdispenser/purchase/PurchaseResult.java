package com.jordi.drinkdispenser.purchase;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record PurchaseResult (Long purchaseId, PurchaseStatus status, String product, List<Integer> change) {

    public PurchaseResult(PurchaseStatus status) {
        this(null, status, null, null);
    }

    public PurchaseResult(Long purchaseId, PurchaseStatus status) {
        this(purchaseId, status, null, null);
    }

    public PurchaseResult(Long purchaseId, PurchaseStatus status, Integer change) {
        this(purchaseId, status, null, List.of(change));
    }

    public PurchaseResult(Long purchaseId, PurchaseStatus status, List<Integer> change) {
        this(purchaseId, status, null, change);
    }

};
