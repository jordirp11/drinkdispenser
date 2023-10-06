# Drink dispenser

Some remarks about the implemented solution
- There are integration tests that cover all normal success paths and expected error cases (like insufficient change, timeout 5s, etc.). But unit tests are not present because everything is pretty much covered by integration tests. In a normal production application adding also unit tests to test at least the most logic-intensive services (CashboxService and PurchaseService) wouldn't be omitted.
- I decided not to implement the machine status (out of order, etc) endpoints because lack of time, it's unconnected to the rest of the functionalities and, in my opinion, it doesn't add anything meaningful to the challenge.
- Similar to the previous point, I only implemented the LDC screen integration, but I didn't implement the stock management integration, because the two are involve more or less the same idea: simulating asynchronous calls to an external service.
- Nothing is mentioned about concurrency in the challenge document, and since it simulates a vending machine I assume only one user is using at a time. If concurrency was to be taken into account, the access to the product stock and machine cash box would have to be re-designed to support it.

I hope you understand the reasons for such omissions, mainly due to lack of time. Thanks for your time in reviewing this solution.