package com.jordi.drinkdispenser.purchase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jordi.drinkdispenser.outputscreen.OutputScreenService;

import jakarta.annotation.PostConstruct;

@RestController
public class PurchaseController {

    private enum PurchaseAction {
        coin, confirm, cancel
    } 

    @Autowired
    private PurchaseService purchaseService;
    @Autowired
    private OutputScreenService outputScreen;

    @PostConstruct
    public void init() {
        outputScreen.display("Please select product");
    }
    
    @PostMapping("/purchase")
    public PurchaseResult selectPurchaseProduct(@RequestParam Long productId)
    {
        return displayAndReturn(purchaseService.selectPurchaseProduct(productId));
    }

    @PutMapping("/purchase/{purchaseId}")
    public PurchaseResult addPurchaseCoin(@PathVariable Long purchaseId, @RequestParam PurchaseAction action, 
        @RequestParam(required = false) Integer coinValue)
    {
        try {
            if (action == PurchaseAction.coin) {
                if (coinValue == null) throw new IllegalArgumentException("Required parameter 'coinValue' is not present");
                return displayAndReturn(purchaseService.addPurchaseCoin(purchaseId, coinValue));        
            } else if (action == PurchaseAction.confirm) {
                return displayAndReturn(purchaseService.confirmPurchase(purchaseId));
            } else if (action == PurchaseAction.cancel) {
                return displayAndReturn(purchaseService.cancelPurchase(purchaseId));
            }
        } catch (PurchaseException e) {
            throw new IllegalStateException(e);
        }
            
        throw new IllegalArgumentException("Invalid parameter action");
    }

    private PurchaseResult displayAndReturn(PurchaseResult purchaseResult) {
        switch (purchaseResult.status()) {
            case productSelected -> outputScreen.display("Product selected");
            case productUnavailable -> outputScreen.display("Product unavailable");
            case completed -> outputScreen.display("Please select product");
            case insufficientChange -> outputScreen.display("Insufficient change");
            case insufficientCoins -> outputScreen.display("Insufficient credit");
            case productExpired -> outputScreen.display("Product expired");
            case userCancelled -> outputScreen.display("Please select product");
            case timeoutCancelled -> outputScreen.display("Purchase timeout");
            default -> throw new IllegalArgumentException("Unexpected value: " + purchaseResult.status());            
        }
        return purchaseResult;
    }

}
