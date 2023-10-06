package com.jordi.drinkdispenser.cashbox;

import java.util.Date;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class CashBox {

    @Id @GeneratedValue 
    private Long id;

    @Column(unique = true)
    private Integer coin;

    private Integer quantity;

    @LastModifiedDate
    private Date lastModifiedDate;

    public Integer getCoin() {
        return coin;
    }
    public void setCoin(Integer coin) {
        this.coin = coin;
    }
    public Integer getQuantity() {
        return quantity;
    }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

}
