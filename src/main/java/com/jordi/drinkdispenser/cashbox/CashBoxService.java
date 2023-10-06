package com.jordi.drinkdispenser.cashbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CashBoxService {
    public static List<Integer> VALID_COINS = List.of(5, 10, 20, 50, 100, 200);
    
    @Autowired
    private CashBoxRepository cashBoxRepository;

    public List<Integer> addCoinsAndReturnChange(List<Integer> newCoins, Integer changeNeeded) throws InsufficientChangeException {
        List<CashBox> cashBoxCoinsSortDesc = cashBoxRepository.findAll(Sort.by(Sort.Direction.DESC, "coin"));
        var newCoinsSortDesc = new ArrayList<>(newCoins);
        Collections.sort(newCoinsSortDesc);
        Collections.reverse(newCoinsSortDesc);

        addCoins(cashBoxCoinsSortDesc, newCoinsSortDesc);
        
        if (changeNeeded == 0) {
            cashBoxCoinsSortDesc.forEach(cashBoxRepository::save);
            return List.of();
        }

        var change = returnChange(cashBoxCoinsSortDesc, changeNeeded);
        cashBoxCoinsSortDesc.forEach(cashBoxRepository::save);
        return change;
    }

    private void addCoins(List<CashBox> cashBoxCoinsSortDesc, List<Integer> newCoinsSortDesc) {
        int newCoinIndex = 0; 
        int cashBoxCoinIndex = 0;
        
        while (newCoinIndex < newCoinsSortDesc.size()) {
            var newCoin = newCoinsSortDesc.get(newCoinIndex);
            
            if (cashBoxCoinsSortDesc.size() <= cashBoxCoinIndex 
                || cashBoxCoinsSortDesc.get(cashBoxCoinIndex).getCoin() < newCoin) {
                var cashBoxCoin = new CashBox();
                cashBoxCoin.setCoin(newCoin);
                cashBoxCoin.setQuantity(1);
                cashBoxCoinsSortDesc.add(cashBoxCoinIndex, cashBoxCoin);
                newCoinIndex++;                
                continue;
            } 

            var cashBoxCoin = cashBoxCoinsSortDesc.get(cashBoxCoinIndex);

            if (cashBoxCoin.getCoin() == newCoin) {
                cashBoxCoin.setQuantity(cashBoxCoin.getQuantity() + 1);
                newCoinIndex++;
                continue;
            }

            cashBoxCoinIndex++;            
        }
    }

    private List<Integer> returnChange(List<CashBox> cashBoxCoinsSortDesc, Integer changeNeeded) throws InsufficientChangeException {        
        List<Integer> returnedChange = new ArrayList<Integer>();

        for(var cashBoxCoin : cashBoxCoinsSortDesc) {
            if (changeNeeded == 0) break;

            var coinsNeeded = changeNeeded / cashBoxCoin.getCoin();
            if (coinsNeeded > 0) {
                var coinsReturned = Math.min(coinsNeeded, cashBoxCoin.getQuantity());            

                cashBoxCoin.setQuantity(cashBoxCoin.getQuantity() - coinsReturned);
                changeNeeded -= coinsReturned * cashBoxCoin.getCoin();

                IntStream.range(0, coinsReturned).forEach(ignore -> returnedChange.add(cashBoxCoin.getCoin()));
            }       
        }

        if (changeNeeded > 0) {
            throw new InsufficientChangeException();
        }

        return returnedChange;
    }
}
