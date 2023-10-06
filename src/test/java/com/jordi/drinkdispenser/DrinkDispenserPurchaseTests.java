package com.jordi.drinkdispenser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.util.Assert;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItems;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jordi.drinkdispenser.cashbox.CashBox;
import com.jordi.drinkdispenser.cashbox.CashBoxRepository;
import com.jordi.drinkdispenser.outputscreen.OutputScreenService;
import com.jordi.drinkdispenser.product.Product;
import com.jordi.drinkdispenser.product.ProductRepository;
import com.jordi.drinkdispenser.purchase.timeout.PurchaseTimeoutAspect;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class DrinkDispenserPurchaseTests {
	private final String INT_ARRAY_FIELD_PREFIX = "_INT_ARRAY_";
	private final String INT_ARRAY_SEPARATOR = ",";

	@Autowired
  	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private CashBoxRepository cashBoxRepository;
	@Autowired
	private OutputScreenService outputScreenService;

	@Test
	void test_purchase_select_unexisting_product() throws Exception {
		testPurchaseSelect(999L, expect("status","productUnavailable"));
	}

	@Test
	void test_purchase_select_out_of_stock_product() throws Exception {
		var productId = createProduct("Monster", 150, 0, LocalDate.now().plusDays(1));	

		testPurchaseSelect(productId, expect("status","productUnavailable"));
	}

	@Test
	void test_purchase_select_expired_product() throws Exception {
		var productId = createProduct("Orange Juice", 200, 5, LocalDate.now().minusDays(1));	

		testPurchaseSelect(productId, expect("status","productExpired"));
	}

	@Test	
	void test_purchase_with_invalid_coin() throws Exception {
		var productId = createProduct("Diet Coke", 150, 25, LocalDate.now().plusDays(1));		

		var purchaseId = testPurchaseSelect(productId, expect("status","productSelected"), expectPresent("purchaseId"));
		testPurchaseAddCoin(purchaseId, 33, expect("status","productSelected"), expect("purchaseId", purchaseId), expectIntArray("change", 33));		

		expectProductStock(productId, 25);
	}

	@Test
	void test_purchase_product_confirm_no_change_ok() throws Exception {
		var productId = createProduct("Coke", 150, 25, LocalDate.now().plusDays(1));		

		var purchaseId = testPurchaseSelect(productId, expect("status","productSelected"), expectPresent("purchaseId"));
		testPurchaseAddCoin(purchaseId, 100, expect("status","productSelected"), expect("purchaseId", purchaseId));
		testPurchaseAddCoin(purchaseId, 50, expect("status","productSelected"), expect("purchaseId", purchaseId));		
		testPurchaseAction("confirm", purchaseId, expect("status", "completed"), expect("purchaseId", purchaseId), expect("product", "Coke"), expectIntArray("change", new Integer[]{}));

		expectProductStock(productId, 24);
	}

	@Test
	void test_purchase_product_confirm_change_ok() throws Exception {
		setCashBox(5, 3, 10, 3);		
		var productId = createProduct("Red Bull", 185, 1, LocalDate.now().plusDays(1));		

		var purchaseId = testPurchaseSelect(productId, expect("status","productSelected"), expectPresent("purchaseId"));
		expectDisplay("Product selected");
		testPurchaseAddCoin(purchaseId, 100, expect("status","productSelected"), expect("purchaseId", purchaseId));
		expectDisplay("Product selected");
		testPurchaseAddCoin(purchaseId, 100, expect("status","productSelected"), expect("purchaseId", purchaseId));		
		expectDisplay("Product selected");
		testPurchaseAction("confirm", purchaseId, expect("status", "completed"), expect("purchaseId", purchaseId), expect("product", "Red Bull"), expectIntArray("change", 10, 5));
		expectDisplay("Please select product");	
		
		expectProductStock(productId, 0);

		setCashBox(5, 2, 10, 2, 100, 2);
	}

	@Test
	void test_purchase_confirm_insufficient_change() throws Exception {
		setCashBox(10, 5);
		var productId = createProduct("Water", 115, 25, LocalDate.now().plusDays(1));		

		var purchaseId = testPurchaseSelect(productId, expect("status","productSelected"), expectPresent("purchaseId"));
		expectDisplay("Product selected");
		testPurchaseAddCoin(purchaseId, 50, expect("status","productSelected"), expect("purchaseId", purchaseId));
		testPurchaseAddCoin(purchaseId, 100, expect("status","productSelected"), expect("purchaseId", purchaseId));		
		testPurchaseAction("confirm", purchaseId, expect("status", "insufficientChange"), expect("purchaseId", purchaseId), expectIntArray("change", 50, 100));
		expectDisplay("Insufficient change");

		expectProductStock(productId, 25);

		expectCashBox(10, 5);
	}

	@Test
	void test_purchase_insufficient_coins() throws Exception {
		setCashBox(10, 5);
		var productId = createProduct("Fanta Lemon", 115, 33, LocalDate.now().plusDays(1));		

		var purchaseId = testPurchaseSelect(productId, expect("status","productSelected"), expectPresent("purchaseId"));
		testPurchaseAddCoin(purchaseId, 50, expect("status","productSelected"), expect("purchaseId", purchaseId));
		testPurchaseAddCoin(purchaseId, 10, expect("status","productSelected"), expect("purchaseId", purchaseId));		
		testPurchaseAction("confirm", purchaseId, expect("status", "insufficientCoins"), expect("purchaseId", purchaseId), expectIntArray("change", 50, 10));
		expectDisplay("Insufficient credit");

		expectProductStock(productId, 33);

		expectCashBox(10, 5);
	}

	@Test
	void test_purchase_user_cancelled() throws Exception {
		var productId = createProduct("Iced tea", 115, 44, LocalDate.now().plusDays(1));		

		var purchaseId = testPurchaseSelect(productId, expect("status","productSelected"), expectPresent("purchaseId"));
		testPurchaseAddCoin(purchaseId, 10, expect("status","productSelected"), expect("purchaseId", purchaseId));
		testPurchaseAddCoin(purchaseId, 200, expect("status","productSelected"), expect("purchaseId", purchaseId));		
		testPurchaseAction("cancel", purchaseId, expect("status", "userCancelled"), expect("purchaseId", purchaseId), expectIntArray("change", 10, 200));

		expectProductStock(productId, 44);
	}

	@Test
	void test_purchase_confirm_after_cancelling_invalid() throws Exception {
		var productId = createProduct("Cherry juice", 200, 25, LocalDate.now().plusDays(1));		

		var purchaseId = testPurchaseSelect(productId, expect("status","productSelected"), expectPresent("purchaseId"));
		testPurchaseAction("cancel", purchaseId, expect("status", "userCancelled"), expect("purchaseId", purchaseId), expectIntArray("change"));
		testPurchaseInvalidAction("confirm", purchaseId);
	}

	@Test
	void test_purchase_select_timeout() throws Exception {
		var productId = createProduct("Banana shake", 200, 25, LocalDate.now().plusDays(1));		

		PurchaseTimeoutAspect.TEST_TIME_LIMIT = 0;
		testPurchaseSelect(productId, expect("status","timeoutCancelled"), expectPresent("purchaseId"), expectIntArray("change", new Integer[]{}));
		PurchaseTimeoutAspect.TEST_TIME_LIMIT = 5000;
		expectDisplay("Purchase timeout");

		expectProductStock(productId, 25);
	}

	@Test
	void test_purchase_add_coin_timeout() throws Exception {
		var productId = createProduct("Capuccinno", 200, 20, LocalDate.now().plusDays(1));		

		var purchaseId = testPurchaseSelect(productId, expect("status","productSelected"), expectPresent("purchaseId"));
		testPurchaseAddCoin(purchaseId, 10, expect("status","productSelected"), expect("purchaseId", purchaseId));
		PurchaseTimeoutAspect.TEST_TIME_LIMIT = 0;
		testPurchaseAddCoin(purchaseId, 50, expect("status","timeoutCancelled"), expect("purchaseId", purchaseId), expectIntArray("change", 50, 50 ));
		PurchaseTimeoutAspect.TEST_TIME_LIMIT = 5000;
		
		expectDisplay("Purchase timeout");

		expectProductStock(productId, 20);
	}

	@Test
	void test_purchase_confirm_timeout() throws Exception {
		setCashBox(10, 3, 100, 1);		
		var productId = createProduct("Small water", 100, 50, LocalDate.now().plusDays(1));		

		var purchaseId = testPurchaseSelect(productId, expect("status","productSelected"), expectPresent("purchaseId"));
		testPurchaseAddCoin(purchaseId, 50, expect("status","productSelected"), expect("purchaseId", purchaseId));
		testPurchaseAddCoin(purchaseId, 50, expect("status","productSelected"), expect("purchaseId", purchaseId));		
		PurchaseTimeoutAspect.TEST_TIME_LIMIT = 0;
		testPurchaseAction("confirm", purchaseId, expect("status", "timeoutCancelled"), expect("purchaseId", purchaseId), expectIntArray("change", 100));
		PurchaseTimeoutAspect.TEST_TIME_LIMIT = 5000;
		
		expectDisplay("Purchase timeout");

		expectProductStock(productId, 50);

		expectCashBox(10, 3, 50, 2, 100, 0);
	}

	// Helper methods for testing

	private void expectCashBox(Integer ... expectedCashBoxCoins) {
		var cashBox = cashBoxRepository.findAll(Sort.by("coin"));

		Assert.isTrue(cashBox.size() == expectedCashBoxCoins.length / 2, "Different cashbox coins than expected");
		for(int i=0;i < cashBox.size();i++) {
			Assert.isTrue(cashBox.get(i).getCoin() == expectedCashBoxCoins[i*2], "Cashbox coin " + cashBox.get(i).getCoin() + ", expected " + expectedCashBoxCoins[i*2]);
			Assert.isTrue(cashBox.get(i).getQuantity() == expectedCashBoxCoins[i*2 + 1], "Cashbox coin quantity " + cashBox.get(i).getQuantity() + ", expected " + expectedCashBoxCoins[i*2 + 1]);
		}
	}

	private void expectProductStock(Long productId, Integer expectedQuantity) {
		var product = productRepository.findById(productId).get();
		Assert.isTrue(product.getQuantity() == expectedQuantity, "Product quantity: " + product.getQuantity() + ", expected: " + expectedQuantity);
	}

	private void expectDisplay(String message) throws InterruptedException, ExecutionException {		
		var displayMessage = outputScreenService.getLatestDisplay();
		Assert.isTrue(message.equals(displayMessage), "Display message: " + displayMessage + ", expected: " + message);
	}

	private void setCashBox(Integer ... cashboxCoins) { // coin, quantity, coin, quantity, ...
		cashBoxRepository.deleteAll();
		for(int i=0;i<cashboxCoins.length;i+=2) {
			var cashBoxCoin = new CashBox();
			cashBoxCoin.setCoin(cashboxCoins[i]);
			cashBoxCoin.setQuantity(cashboxCoins[i+1]);			
			cashBoxRepository.save(cashBoxCoin);
		}
	}

	private Long testPurchaseSelect(Long productId, String[] ... expectFields) throws Exception {
		var result = mockMvc.perform(post("/purchase")
			.param("productId", productId.toString())
			.accept(MediaType.APPLICATION_JSON))			
			.andDo(MockMvcResultHandlers.print(System.out))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.*",hasSize(expectFields.length)))			
			.andExpectAll(getResultMatchers(expectFields))			
			.andReturn();

		var content = result.getResponse().getContentAsString();
		var jsonObject = new ObjectMapper().readTree(content);
		return jsonObject.has("purchaseId") 
			? Long.parseLong(jsonObject.get("purchaseId").asText()) 
			: null;
	}

	private void testPurchaseAddCoin(Long purchaseId, Integer coinValue, String[] ... expectFields) throws Exception {
		mockMvc.perform(put("/purchase/{purchaseId}", purchaseId)						
			.param("action", "coin")
			.param("coinValue", coinValue.toString())
			.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.*",hasSize(expectFields.length)))			
			.andExpectAll(getResultMatchers(expectFields))								
			.andReturn();
	}

	private void testPurchaseAction(String action, Long purchaseId, String[] ... expectFields) throws Exception {
		mockMvc.perform(put("/purchase/{purchaseId}", purchaseId)						
			.param("action", action)
			.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())			
			.andExpect(jsonPath("$.*", hasSize(expectFields.length)))			
			.andExpectAll(getResultMatchers(expectFields))													
			.andReturn();
	}

	private void testPurchaseInvalidAction(String action, Long purchaseId) throws Exception {
		mockMvc.perform(put("/purchase/{purchaseId}", purchaseId)						
			.param("action", action)
			.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().is(400))
			.andReturn();
	}

	private Long createProduct(String productName, int price, int quantity, LocalDate expirationDate) {
		var product = new Product();
		product.setName(productName);
		product.setPrice(price);
		product.setQuantity(quantity);
		product.setExpirationDate(expirationDate);

		return productRepository.save(product).getId();
	}

	private String[] expect(String field, Object value) {
		return new String[]{ field, value.toString() };
	}

	private String[] expectIntArray(String field, Integer ... value) {
		return new String[]{ INT_ARRAY_FIELD_PREFIX + field, Arrays.stream(value)
			.map(Object::toString).collect(Collectors.joining(INT_ARRAY_SEPARATOR)) };
	}

	private String[] expectPresent(String field) {
		return new String[]{ field };
	}

	private ResultMatcher[] getResultMatchers(String[] ... expectFields) {		
		List<ResultMatcher> resultMatchers = new ArrayList<>();
		for(int i=0; i<expectFields.length; i++) {
			var expectField = expectFields[i];
			if (expectField.length == 1) {
				resultMatchers.add(jsonPath("$." + expectField[0]).exists());
				continue;
			}
			if (expectField[0].startsWith(INT_ARRAY_FIELD_PREFIX)) {
				expectField[0] = expectField[0].substring(INT_ARRAY_FIELD_PREFIX.length());				
				getIntArrayResultMarcher(expectField[0], expectField[1]).forEach(resultMatchers::add);
			} else {
				resultMatchers.add(jsonPath("$." + expectField[0]).value(expectField[1]));
			}
		}

		return resultMatchers.toArray(new ResultMatcher[resultMatchers.size()]);
	}

	private Collection<ResultMatcher> getIntArrayResultMarcher(String fieldName, String arrayValues)
	{
		if (arrayValues.isEmpty()) {
			return List.of(jsonPath("$." + fieldName, hasSize(0)));
		}
		var values = Arrays.stream(arrayValues.split(INT_ARRAY_SEPARATOR))
			.map(Integer::parseInt)
			.toArray(Integer[]::new);

		return List.of(
			jsonPath("$." + fieldName, hasItems(values)),
			jsonPath("$." + fieldName, hasSize(values.length))
		);
	}

}
