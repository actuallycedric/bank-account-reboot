package com.clarence.bank_api;

import com.clarence.bank_api.dto.AccountResponse;
import com.clarence.bank_api.dto.BalanceChangeRequest;
import com.clarence.bank_api.dto.CreateAccountRequest;
import com.clarence.bank_api.dto.TransferRequest;
import com.clarence.bank_api.service.AccountService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BankApiApplicationTests {


	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@BeforeAll
	static void initDatabase(){
		postgres.start();
	}

	@AfterAll
	static void closeDatabase(){
		postgres.stop();
	}

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private AccountService accountService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private CreateAccountRequest req;
	private CreateAccountRequest req2;
	private BalanceChangeRequest transact;
	private String transactionBody;
	private String requestBody;
	private String requestBodyTwo;

	@BeforeEach
	void init() throws JsonProcessingException {
		req = new CreateAccountRequest();
		req.setFirstName("Clarence");
		req.setLastName("Cesante");
		req.setInitialDeposit(new BigDecimal("437.25"));

		req2 = new CreateAccountRequest();
		req2.setFirstName("Test");
		req2.setLastName("Account");
		req2.setInitialDeposit(new BigDecimal("777.25"));

		requestBody = objectMapper.writeValueAsString(req);

		requestBodyTwo = objectMapper.writeValueAsString(req2);

		transact = new BalanceChangeRequest();
		transact.setAmount(new BigDecimal("49.97"));

		transactionBody = objectMapper.writeValueAsString(transact);
	}


	@Test
	@DisplayName("Creating an account returns 201 Created")
	void testPost201() throws Exception {

		mockMvc.perform(
				post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Creating an account with invalid fields returns 400 Bad Request")
	void testBadInput() throws Exception {
		req.setFirstName(null);
		requestBody = objectMapper.writeValueAsString(req);

		mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isBadRequest());

	}

	@Test
	@DisplayName("Performing a GET request returns the correct account")
	void testGet() throws Exception {
		ResponseEntity<AccountResponse> resp = accountService.createAccount(req);
		int id = resp.getBody().getId();

		mockMvc.perform(get("/accounts/" + id))
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.firstName").value("Clarence"));
	}

	@Test
	@DisplayName("Finding an invalid account returns 404")
	void testNotFound() throws Exception{
		// ensures that an invalid ID returns 404
		mockMvc.perform(get("/accounts/" + Integer.MAX_VALUE)).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Deleting an account and trying to fetch it returns 404")
	void testDeleteThenGet() throws Exception{

		req.setInitialDeposit(new BigDecimal("0.00"));
		requestBody = objectMapper.writeValueAsString(req);

		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		String responseBody = resp.getResponse().getContentAsString();

		int id = JsonPath.parse(responseBody).read("$.id");

		// ensures initial POST request went through properly
		mockMvc.perform(get("/accounts/" + id))
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(status().isOk());

		// deletes account
		mockMvc.perform(delete("/accounts/" + id))
				.andExpect(status().isNoContent());

		// subsequent GET request returns 404
		mockMvc.perform(get("/accounts/" + id))
				.andExpect(status().isNotFound());

	}

	@Test
	@DisplayName("Deleting an invalid account returns 404")
	void deleteInvalidAccount() throws Exception {
		mockMvc.perform(delete("/accounts/" + Integer.MAX_VALUE)).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Valid deposits increase balance")
	void testValidDeposit() throws Exception{
		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();


		DocumentContext jsonBody = JsonPath.parse(resp.getResponse().getContentAsString());

		BigDecimal balance = new BigDecimal(jsonBody.read("$.balance").toString());


		MvcResult newResp = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/deposit")
				.contentType(MediaType.APPLICATION_JSON).content(transactionBody)).andReturn();

		BigDecimal newBalance = balance.add(transact.getAmount()).stripTrailingZeros();

		jsonBody = JsonPath.parse(newResp.getResponse().getContentAsString());

		String readBalance = jsonBody.read("$.balance").toString();

		assertEquals(newBalance, new BigDecimal(readBalance).stripTrailingZeros());
	}

	@Test
	@DisplayName("A withdrawal amount of zero returns 400")
	void testZeroDepositAmount() throws Exception{
		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		DocumentContext jsonBody = JsonPath.parse(resp.getResponse().getContentAsString());

		transact.setAmount(new BigDecimal("0.00"));
		transactionBody = objectMapper.writeValueAsString(transact);

		MvcResult newResp = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/deposit")
						.contentType(MediaType.APPLICATION_JSON).content(transactionBody))
				.andExpect(status().isBadRequest())
				.andReturn();

	}

	@Test
	@DisplayName("A withdrawal amount less than zero returns 400")
	void testNegativeDepositAmount() throws Exception{
		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		DocumentContext jsonBody = JsonPath.parse(resp.getResponse().getContentAsString());

		transact.setAmount(new BigDecimal("-10.00"));
		transactionBody = objectMapper.writeValueAsString(transact);

		MvcResult newResp = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/deposit")
						.contentType(MediaType.APPLICATION_JSON).content(transactionBody))
				.andExpect(status().isBadRequest())
				.andReturn();

	}

	@Test
	@DisplayName("Depositing into an invalid account returns 404")
	void testInvalidDepositAccount() throws Exception{
		mockMvc.perform(post("/accounts/" + Integer.MAX_VALUE + "/deposit")
				.contentType(MediaType.APPLICATION_JSON)
				.content(transactionBody)).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Valid withdrawals decrease the balance")
	void testValidWithdrawal() throws Exception{
		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();


		DocumentContext jsonBody = JsonPath.parse(resp.getResponse().getContentAsString());

		BigDecimal balance = new BigDecimal(jsonBody.read("$.balance").toString());


		MvcResult newResp = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/withdraw")
				.contentType(MediaType.APPLICATION_JSON).content(transactionBody)).andReturn();

		BigDecimal newBalance = balance.subtract(transact.getAmount()).stripTrailingZeros();

		jsonBody = JsonPath.parse(newResp.getResponse().getContentAsString());

		String readBalance = jsonBody.read("$.balance").toString();

		assertEquals(newBalance, new BigDecimal(readBalance).stripTrailingZeros());
	}

	@Test
	@DisplayName("Withdrawing from an invalid account returns 404")
	void testInvalidWithdrawAccount() throws Exception{
		mockMvc.perform(post("/accounts/" + Integer.MAX_VALUE + "/withdraw")
				.contentType(MediaType.APPLICATION_JSON)
				.content(transactionBody)).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("A withdrawal amount equal to zero returns 400")
	void testZeroWithdrawalAmount() throws Exception{
		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		DocumentContext jsonBody = JsonPath.parse(resp.getResponse().getContentAsString());

		transact.setAmount(new BigDecimal("0.00"));
		transactionBody = objectMapper.writeValueAsString(transact);

		MvcResult newResp = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/withdraw")
				.contentType(MediaType.APPLICATION_JSON).content(transactionBody))
				.andExpect(status().isBadRequest())
				.andReturn();
	}

	@Test
	@DisplayName("A withdrawal amount less than zero returns 400")
	void testNegativeWithdrawalAmount() throws Exception{
		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		DocumentContext jsonBody = JsonPath.parse(resp.getResponse().getContentAsString());

		transact.setAmount(new BigDecimal("-10.00"));
		transactionBody = objectMapper.writeValueAsString(transact);

		MvcResult newResp = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/withdraw")
						.contentType(MediaType.APPLICATION_JSON).content(transactionBody))
				.andExpect(status().isBadRequest())
				.andReturn();
	}

	@Test
	@DisplayName("Withdrawing more than the account balance returns 400")
	void testOverdraft() throws Exception{
		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		DocumentContext jsonBody = JsonPath.parse(resp.getResponse().getContentAsString());

		BigDecimal balance = new BigDecimal(jsonBody.read("$.balance").toString());

		balance = balance.add(new BigDecimal("100"));

		transact.setAmount(balance);
		transactionBody = objectMapper.writeValueAsString(transact);

		MvcResult newResp = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/withdraw")
				.contentType(MediaType.APPLICATION_JSON).content(transactionBody))
				.andExpect(status().isConflict())
				.andReturn();
	}

	@Test
	@DisplayName("Valid transfers are atomic and change both balances")
	void testValidTransfer() throws Exception {
		req.setInitialDeposit(new BigDecimal("500.00"));
		requestBody = objectMapper.writeValueAsString(req);

		req2.setInitialDeposit(new BigDecimal("250.00"));
		requestBodyTwo = objectMapper.writeValueAsString(req2);

		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		MvcResult respTwo = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBodyTwo))
				.andExpect(status().isCreated()).andReturn();

		int firstId = JsonPath.parse(resp.getResponse().getContentAsString()).read("$.id");

		int secondId = JsonPath.parse(respTwo.getResponse().getContentAsString()).read("$.id");

		TransferRequest transfer = new TransferRequest();
		BigDecimal amountToTransfer = new BigDecimal("200.00");
		transfer.setAmount(amountToTransfer);
		transfer.setRecipientAccount(secondId);
		String transferBody = objectMapper.writeValueAsString(transfer);

		mockMvc.perform(post("/accounts/" + firstId + "/transfer").contentType(MediaType.APPLICATION_JSON).content(transferBody))
				.andExpect(status().isOk())
				.andReturn();

		resp = mockMvc.perform(get("/accounts/" + firstId)).andReturn();
		respTwo = mockMvc.perform(get("/accounts/" + secondId)).andReturn();

		BigDecimal balance = new BigDecimal(JsonPath.parse(resp.getResponse().getContentAsString()).read("$.balance").toString());
		BigDecimal balance2 = new BigDecimal(JsonPath.parse(respTwo.getResponse().getContentAsString()).read("$.balance").toString());

		assertEquals(balance.stripTrailingZeros(), new BigDecimal("300.00").stripTrailingZeros());
		assertEquals(balance2.stripTrailingZeros(), new BigDecimal("450.00").stripTrailingZeros());

	}

	@Test
	@DisplayName("Invalid transfer doesn't change the balance of either account")
	void testInvalidTransferDestination() throws Exception {
		req.setInitialDeposit(new BigDecimal("500.00"));
		requestBody = objectMapper.writeValueAsString(req);

		req2.setInitialDeposit(new BigDecimal("250.00"));
		requestBodyTwo = objectMapper.writeValueAsString(req2);

		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();


		int firstId = JsonPath.parse(resp.getResponse().getContentAsString()).read("$.id");


		TransferRequest transfer = new TransferRequest();
		BigDecimal amountToTransfer = new BigDecimal("200.00");
		transfer.setAmount(amountToTransfer);
		transfer.setRecipientAccount(Integer.MAX_VALUE);
		String transferBody = objectMapper.writeValueAsString(transfer);

		mockMvc.perform(post("/accounts/" + firstId + "/transfer").contentType(MediaType.APPLICATION_JSON).content(transferBody))
				.andExpect(status().isNotFound())
				.andReturn();

		resp = mockMvc.perform(get("/accounts/" + firstId)).andReturn();


		BigDecimal balance = new BigDecimal(JsonPath.parse(resp.getResponse().getContentAsString()).read("$.balance").toString());

		assertEquals(balance.stripTrailingZeros(), new BigDecimal("500.00").stripTrailingZeros());

	}

	@Test
	@DisplayName("Transferring insufficient funds returns 409 and neither balance changes")
	void transferInsufficientFunds() throws Exception {
		req.setInitialDeposit(new BigDecimal("500.00"));
		requestBody = objectMapper.writeValueAsString(req);

		req2.setInitialDeposit(new BigDecimal("250.00"));
		requestBodyTwo = objectMapper.writeValueAsString(req2);

		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		MvcResult respTwo = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBodyTwo))
				.andExpect(status().isCreated()).andReturn();

		int firstId = JsonPath.parse(resp.getResponse().getContentAsString()).read("$.id");

		int secondId = JsonPath.parse(respTwo.getResponse().getContentAsString()).read("$.id");

		TransferRequest transfer = new TransferRequest();
		BigDecimal amountToTransfer = new BigDecimal("600.00");
		transfer.setAmount(amountToTransfer);
		transfer.setRecipientAccount(secondId);
		String transferBody = objectMapper.writeValueAsString(transfer);

		mockMvc.perform(post("/accounts/" + firstId + "/transfer").contentType(MediaType.APPLICATION_JSON).content(transferBody))
				.andExpect(status().isConflict())
				.andReturn();

		resp = mockMvc.perform(get("/accounts/" + firstId)).andReturn();
		respTwo = mockMvc.perform(get("/accounts/" + secondId)).andReturn();

		BigDecimal balance = new BigDecimal(JsonPath.parse(resp.getResponse().getContentAsString()).read("$.balance").toString());
		BigDecimal balance2 = new BigDecimal(JsonPath.parse(respTwo.getResponse().getContentAsString()).read("$.balance").toString());

		assertEquals(balance.stripTrailingZeros(), new BigDecimal("500.00").stripTrailingZeros());
		assertEquals(balance2.stripTrailingZeros(), new BigDecimal("250.00").stripTrailingZeros());

	}


	@Test
	@DisplayName("Transferring zero funds returns 400")
	void transferZeroFunds() throws Exception {
		req.setInitialDeposit(new BigDecimal("500.00"));
		requestBody = objectMapper.writeValueAsString(req);

		req2.setInitialDeposit(new BigDecimal("250.00"));
		requestBodyTwo = objectMapper.writeValueAsString(req2);

		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		MvcResult respTwo = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBodyTwo))
				.andExpect(status().isCreated()).andReturn();

		int firstId = JsonPath.parse(resp.getResponse().getContentAsString()).read("$.id");

		int secondId = JsonPath.parse(respTwo.getResponse().getContentAsString()).read("$.id");

		TransferRequest transfer = new TransferRequest();
		BigDecimal amountToTransfer = new BigDecimal("0.00");
		transfer.setAmount(amountToTransfer);
		transfer.setRecipientAccount(secondId);
		String transferBody = objectMapper.writeValueAsString(transfer);

		mockMvc.perform(post("/accounts/" + firstId + "/transfer").contentType(MediaType.APPLICATION_JSON).content(transferBody))
				.andExpect(status().isBadRequest())
				.andReturn();

	}

	@Test
	@DisplayName("Transferring negative funds returns 400")
	void transferNegativeFunds() throws Exception {
		req.setInitialDeposit(new BigDecimal("500.00"));
		requestBody = objectMapper.writeValueAsString(req);

		req2.setInitialDeposit(new BigDecimal("250.00"));
		requestBodyTwo = objectMapper.writeValueAsString(req2);

		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		MvcResult respTwo = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBodyTwo))
				.andExpect(status().isCreated()).andReturn();

		int firstId = JsonPath.parse(resp.getResponse().getContentAsString()).read("$.id");

		int secondId = JsonPath.parse(respTwo.getResponse().getContentAsString()).read("$.id");

		TransferRequest transfer = new TransferRequest();
		BigDecimal amountToTransfer = new BigDecimal("-100.00");
		transfer.setAmount(amountToTransfer);
		transfer.setRecipientAccount(secondId);
		String transferBody = objectMapper.writeValueAsString(transfer);

		mockMvc.perform(post("/accounts/" + firstId + "/transfer").contentType(MediaType.APPLICATION_JSON).content(transferBody))
				.andExpect(status().isBadRequest())
				.andReturn();

	}

	@Test
	@DisplayName("Posting a transfer to a non-existing account returns 404")
	void transferNonexistentAccount() throws Exception {

		TransferRequest transfer = new TransferRequest();
		BigDecimal amountToTransfer = new BigDecimal("100.00");
		transfer.setAmount(amountToTransfer);
		transfer.setRecipientAccount(1);
		String transferBody = objectMapper.writeValueAsString(transfer);

		mockMvc.perform(post("/accounts/" + Integer.MAX_VALUE + "/transfer").contentType(MediaType.APPLICATION_JSON).content(transferBody))
				.andExpect(status().isNotFound())
				.andReturn();

	}

	@Test
	@DisplayName("A valid transfer where the sender is the recipient should return 409")
	void testTransferToSelf() throws Exception {
		req.setInitialDeposit(new BigDecimal("500.00"));
		requestBody = objectMapper.writeValueAsString(req);

		req2.setInitialDeposit(new BigDecimal("250.00"));
		requestBodyTwo = objectMapper.writeValueAsString(req2);

		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();


		int firstId = JsonPath.parse(resp.getResponse().getContentAsString()).read("$.id");


		TransferRequest transfer = new TransferRequest();
		BigDecimal amountToTransfer = new BigDecimal("200.00");
		transfer.setAmount(amountToTransfer);
		transfer.setRecipientAccount(firstId);
		String transferBody = objectMapper.writeValueAsString(transfer);

		mockMvc.perform(post("/accounts/" + firstId + "/transfer").contentType(MediaType.APPLICATION_JSON).content(transferBody))
				.andExpect(status().isConflict())
				.andReturn();

		resp = mockMvc.perform(get("/accounts/" + firstId)).andReturn();

	}

	@Test
	@DisplayName("Fetching all transactions linked to one user should only return their transactions")
	void testGetTransactionHistory() throws Exception {
		MvcResult resp = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(requestBody))
				.andExpect(status().isCreated()).andReturn();

		DocumentContext jsonBody = JsonPath.parse(resp.getResponse().getContentAsString());

		MvcResult depositResponse = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/deposit")
				.contentType(MediaType.APPLICATION_JSON).content(transactionBody)).andReturn();

		transact.setAmount(new BigDecimal("500.00"));
		transactionBody = objectMapper.writeValueAsString(transact);

		MvcResult depositResponseTwo = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/deposit")
				.contentType(MediaType.APPLICATION_JSON).content(transactionBody)).andReturn();

		transact.setAmount(new BigDecimal("600.00"));
		transactionBody = objectMapper.writeValueAsString(transact);

		MvcResult withdrawResponse = mockMvc.perform(post("/accounts/" + jsonBody.read("$.id") + "/withdraw")
				.contentType(MediaType.APPLICATION_JSON).content(transactionBody)).andReturn();

        Set<Integer> transactionIds = new HashSet<>();
		// parses all of the valid transactions linked to this account as their ID
		transactionIds.add(JsonPath.parse(depositResponse.getResponse().getContentAsString()).read("$.id"));
		transactionIds.add(JsonPath.parse(depositResponseTwo.getResponse().getContentAsString()).read("$.id"));
		transactionIds.add(JsonPath.parse(withdrawResponse.getResponse().getContentAsString()).read("$.id"));

		MvcResult allTransactions = mockMvc.perform(get("/accounts/" + jsonBody.read("$.id") + "/transactions")).andReturn();

		List<Map<String, Object>> list = JsonPath.parse(allTransactions.getResponse().getContentAsString()).read("$.content");

		assertEquals(list.size(), 3);
	}

	@Test
	@DisplayName("Fetching transactions from an invalid account returns 404")
	void testFetchInvalidAccountTransactions() throws Exception {
		mockMvc.perform(get("/accounts/" + Integer.MAX_VALUE + "/transactions")).andExpect(status().isNotFound());
	}


}
