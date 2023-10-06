package com.jordi.drinkdispenser.cashbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CashBoxRepository extends JpaRepository<CashBox, String> {

    CashBox findByCoin(Integer coin);
    
}
