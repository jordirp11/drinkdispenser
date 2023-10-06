package com.jordi.drinkdispenser.purchase;

import java.sql.Date;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.data.annotation.CreatedDate;

import com.jordi.drinkdispenser.product.Product;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
class Purchase {

    private @Id @GeneratedValue Long id;

    @ManyToOne
    @Fetch(FetchMode.SELECT)    
    private Product product;

    private String coins;

    @Enumerated(EnumType.STRING)
    private PurchaseStatus status;
    
    @CreatedDate    
    private Date createdAt;

    public Long getId() {
        return id;
    }
    public Product getProduct() {
        return product;
    }
    public void setProduct(Product product) {
        this.product = product;
    }
    public String getCoins() {
        return coins;
    }
    public void setCoins(String coins) {
        this.coins = coins;
    }
    public PurchaseStatus getStatus() {
        return status;
    }
    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    
}
