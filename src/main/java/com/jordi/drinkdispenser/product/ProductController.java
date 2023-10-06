package com.jordi.drinkdispenser.product;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jordi.drinkdispenser.outputscreen.OutputScreenService;

@RestController
public class ProductController {

    @Autowired
    private ProductService productService;
    @Autowired
    private OutputScreenService outputScreen;

    @PutMapping("/product")    
    public Long createProduct(@RequestBody ProductRequest product)
    {
        return productService.createProduct(product);
    }

    @PutMapping("/product/{productId}")
    public void addProductQuantity(@PathVariable Long productId, @RequestParam Integer addQuantity) throws ProductException
    {
        productService.addProductQuantity(productId, addQuantity);
    }

    @GetMapping("/product/{productId}/quantity")
    public Integer getProductQuantity(@PathVariable Long productId) throws ProductException
    {
        var product = productService.getProduct(productId);
        outputScreen.display("%s quantity %d", product.getName(), product.getQuantity());
        return product.getQuantity();
    }

    @GetMapping("/product/{productId}/expiration")
    public LocalDate getProductExpiration(@PathVariable Long productId) throws ProductException
    {
        var product = productService.getProduct(productId);
        outputScreen.display("%s expiration %tD", product.getName(), product.getExpirationDate());
        return product.getExpirationDate();
    }

    @GetMapping("/product")
    public List<Product> getProducts()
    {
        return productService.getProducts();
    }
    
}
