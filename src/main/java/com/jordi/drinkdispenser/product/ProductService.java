package com.jordi.drinkdispenser.product;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ExternalProductManagementService externalProductManagementService;

    public Long createProduct(ProductRequest productRequest) {
        var product = new Product();
		product.setName(productRequest.name());
		product.setPrice(productRequest.price());
		product.setQuantity(productRequest.quantity());
		product.setExpirationDate(productRequest.expirationDate());

		return productRepository.save(product).getId();
    }

    public void addProductQuantity(Long productId, Integer quantity) throws ProductException {        
        var productMaybe = productRepository.findById(productId);
        if (!productMaybe.isPresent()) throw new ProductException();
        var product = productMaybe.get();
        
        product.setQuantity(Math.max(product.getQuantity() + quantity, 0));
        productRepository.save(product);
    }

    public void consumProductUnit(Long productId) throws ProductException {                
        var productMaybe = productRepository.findById(productId);
        if (!productMaybe.isPresent()) throw new ProductException();
        var product = productMaybe.get();

        if (product.getQuantity() < 1) throw new IllegalStateException("Product has zero quantity");

        product.setQuantity(product.getQuantity() + 1);
        productRepository.save(product);

        if (product.getQuantity() == 0) {
            externalProductManagementService.notifyOutOfStockProduct(product);
        }
    }

    public Product getProduct(Long productId) throws ProductException {
        return productRepository.findById(productId).orElseThrow(ProductException::new);
    }

    public List<Product> getProducts() {
        return productRepository.findAll();
    }
}
