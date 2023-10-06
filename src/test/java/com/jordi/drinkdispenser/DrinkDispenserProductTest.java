package com.jordi.drinkdispenser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;

import com.jordi.drinkdispenser.outputscreen.OutputScreenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DrinkDispenserProductTest {

    @Autowired
  	private MockMvc mockMvc;

    @Autowired
    private OutputScreenService outputScreenService;

    @Test
    void test_get_product_quantity() throws Exception {
      var productId = createProduct("Lemon juice", "2024-01-25", 250, 11);

      getProduct(productId, "quantity", "11");
      expectDisplay("Lemon juice quantity 11");
    }

    @Test
    void test_get_product_expiration() throws Exception {
      var productId = createProduct("Strawberry shake", "2024-03-19", 250, 2);

      getProduct(productId, "expiration", "\"2024-03-19\"");
      expectDisplay(String.format("Strawberry shake expiration %tD", LocalDate.of(2024, 03, 19)));
    }

    @Test
    void test_add_product_quantity() throws Exception {
      var productId = createProduct("Coffee Latte", "2024-03-19", 250, 3);
      putProduct(productId, "addQuantity", "6");

      getProduct(productId, "quantity", "9");
      expectDisplay("Coffee Latte quantity 9");
    }

    // Test helper methods

    private void expectDisplay(String message) throws InterruptedException, ExecutionException {		
		  var displayMessage = outputScreenService.getLatestDisplay();
		  Assert.isTrue(message.equals(displayMessage), "Display message: " + displayMessage + ", expected: " + message);
	  }
    
    private Long createProduct(String name, String expirationDate, Integer price, Integer quantity) throws Exception {      
		  var result = mockMvc.perform(put("/product")
          .contentType(MediaType.APPLICATION_JSON)
          .content(String.format("{ \"name\": \"%s\", \"expirationDate\": \"%s\", \"price\": \"%d\", \"quantity\": \"%d\" }", name, expirationDate, price, quantity))
			    .accept(MediaType.APPLICATION_JSON))
			  .andExpect(status().isOk())
			  .andReturn();

      return Long.parseLong(result.getResponse().getContentAsString());
    }

    private void getProduct(Long productId, String field, String expectedValue) throws Exception {
		  mockMvc.perform(get("/product/{productId}/{field}", productId, field)
        .contentType(MediaType.APPLICATION_JSON)
			  .accept(MediaType.APPLICATION_JSON))      
			  .andExpect(status().isOk())
        .andExpect(content().string(expectedValue))
			  .andReturn();
    }

    private void putProduct(Long productId, String field, String value) throws Exception {
		  mockMvc.perform(put("/product/{productId}", productId)
        .param(field, value)
        .contentType(MediaType.APPLICATION_JSON)
			  .accept(MediaType.APPLICATION_JSON))      
			  .andExpect(status().isOk())        
			  .andReturn();
    }
}
