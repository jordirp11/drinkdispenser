package com.jordi.drinkdispenser.purchase;

public enum PurchaseStatus {
    productUnavailable,
    productExpired,
    productSelected,
    userCancelled,        
    completed,
    insufficientCoins,
    insufficientChange,
    timeoutCancelled
}
