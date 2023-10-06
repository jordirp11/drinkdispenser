package com.jordi.drinkdispenser.purchase;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jordi.drinkdispenser.cashbox.CashBoxService;
import com.jordi.drinkdispenser.cashbox.InsufficientChangeException;
import com.jordi.drinkdispenser.product.Product;
import com.jordi.drinkdispenser.product.ProductException;
import com.jordi.drinkdispenser.product.ProductService;
import com.jordi.drinkdispenser.purchase.PurchaseException.PurchaseError;
import com.jordi.drinkdispenser.purchase.timeout.PurchaseTimeout;

@Service
public class PurchaseService {
    private static final int PURCHASE_STEP_TIMEOUT = 5000;

    private static final List<PurchaseStatus> notCancelledPurchaseStatuses = List.of(
        PurchaseStatus.completed, PurchaseStatus.productSelected
    );

    @Autowired
    private CashBoxService cashBoxService;
    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @PurchaseTimeout(PurchaseService.PURCHASE_STEP_TIMEOUT)
    public PurchaseResult selectPurchaseProduct(Long productId) {
        Product product = null;  
        try { 
            product = productService.getProduct(productId); 
        } catch (ProductException e) {
            return new PurchaseResult(PurchaseStatus.productUnavailable);
        }        
        if (product.getQuantity() == 0) {
            return new PurchaseResult(PurchaseStatus.productUnavailable);
        }        
        if (product.getExpirationDate().isBefore(LocalDate.now())) {
            return new PurchaseResult(PurchaseStatus.productExpired);
        }

        var purchase = new Purchase();
        purchase.setProduct(product);
        purchase.setStatus(PurchaseStatus.productSelected);
        purchaseRepository.save(purchase);

        return new PurchaseResult(purchase.getId(), purchase.getStatus());
    }

    @PurchaseTimeout(PurchaseService.PURCHASE_STEP_TIMEOUT)
    public PurchaseResult addPurchaseCoin(Long purchaseId, Integer coinValue) throws PurchaseException {        
        var purchase = getActivePurchase(purchaseId); 

        if (!CashBoxService.VALID_COINS.contains(coinValue)) { // Return invalid coins
            return new PurchaseResult(purchase.getId(), purchase.getStatus(), coinValue);
        }

        var coins = purchase.getCoins() != null ? purchase.getCoins() + "," + coinValue : coinValue.toString();
        purchase.setCoins(coins);
        purchaseRepository.save(purchase);

        return new PurchaseResult(purchase.getId(), purchase.getStatus());
    }

    @PurchaseTimeout(PurchaseService.PURCHASE_STEP_TIMEOUT)
    public PurchaseResult confirmPurchase(Long purchaseId) throws PurchaseException {
        var purchase = getActivePurchase(purchaseId);

        List<Integer> purchaseCoinsList = getPurchaseCoinsList(purchase);

        var totalPurchaseCoins = getTotal(purchaseCoinsList);
        
        var changeNeeded = totalPurchaseCoins - purchase.getProduct().getPrice();
        if (changeNeeded < 0) {
            return new PurchaseResult(purchaseId, PurchaseStatus.insufficientCoins, purchaseCoinsList);
        }

        try {                               
            var changeCoinsList = cashBoxService.addCoinsAndReturnChange(purchaseCoinsList, changeNeeded);
            productService.addProductQuantity(purchase.getProduct().getId(), -1);

            purchase.setStatus(PurchaseStatus.completed);
            purchaseRepository.save(purchase);
            return new PurchaseResult(purchase.getId(), purchase.getStatus(), purchase.getProduct().getName(), changeCoinsList);
        } catch (ProductException e) {
            throw new IllegalStateException("Purchase product not found " + purchase.getProduct().getId());
        } catch (InsufficientChangeException e) {            
            return new PurchaseResult(purchaseId, PurchaseStatus.insufficientChange, purchaseCoinsList);
        }
    }

    private Integer getTotal(List<Integer> purchaseCoinsList) {
        return purchaseCoinsList.stream().reduce((n1, n2) -> n1 + n2).orElse(0);
    }

    public PurchaseResult cancelPurchase(Long purchaseId) throws PurchaseException {
        var purchase = getActivePurchase(purchaseId);
        purchase.setStatus(PurchaseStatus.userCancelled);
        purchaseRepository.save(purchase);

        List<Integer> purchaseCoinsList = getPurchaseCoinsList(purchase);        

        return new PurchaseResult(purchaseId, PurchaseStatus.userCancelled, purchaseCoinsList);
    }

    public PurchaseResult purchaseTimeout(PurchaseResult timedOutPurchaseResult) {
        if (!notCancelledPurchaseStatuses.contains(timedOutPurchaseResult.status())) {
            // No need to cancel purchases that are effectively already cancelled
            return timedOutPurchaseResult;
        }
        var purchaseMaybe = purchaseRepository.findById(timedOutPurchaseResult.purchaseId());
        if (!purchaseMaybe.isPresent()) {
            throw new IllegalStateException("Timed out purchase does not exist " + timedOutPurchaseResult.purchaseId());
        }
        var purchase = purchaseMaybe.get();

        List<Integer> returnedCoins = null;
        
        if (purchase.getStatus() == PurchaseStatus.productSelected) {
            returnedCoins = getPurchaseCoinsList(purchase);
        } else if (purchase.getStatus() == PurchaseStatus.completed){
            try {
                productService.addProductQuantity(purchase.getProduct().getId(), 1);            
            } catch (ProductException e) {
                throw new IllegalStateException("Purchase product not found " + purchase.getProduct().getId());
            }
            try {
                returnedCoins = cashBoxService.addCoinsAndReturnChange(timedOutPurchaseResult.change(), getTotal(getPurchaseCoinsList(purchase)));
            } catch (InsufficientChangeException e) {                
                throw new IllegalStateException("Not enough change in cashbox to cancel the purchase");
            }
        } else {
            throw new IllegalStateException("Purchase status " + purchase.getStatus().name());
        }

        purchase.setStatus(PurchaseStatus.timeoutCancelled);
        purchaseRepository.save(purchase);

        return new PurchaseResult(purchase.getId(), purchase.getStatus(), returnedCoins);
    }

    private List<Integer> getPurchaseCoinsList(Purchase purchase) {
        return purchase.getCoins() == null || purchase.getCoins().isEmpty() 
            ? List.of()
            : Arrays.stream(purchase.getCoins().split(","))
                .map(Integer::parseInt)
                .toList();
    }

    private Purchase getActivePurchase(Long purchaseId) throws PurchaseException {
        var purchaseMaybe = purchaseRepository.findById(purchaseId);
        if (!purchaseMaybe.isPresent()) {
            throw new PurchaseException(PurchaseError.purchaseNotFound);
        }
        if (purchaseMaybe.get().getStatus() != PurchaseStatus.productSelected) {
            throw new PurchaseException(PurchaseError.invalidPurchaseStatus);
        }
        return purchaseMaybe.get();
    }
}
