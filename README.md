This is a Spring Boot-based backend application built for a fictional bank to manage customer loans. The API allows bank employees to create, list, and pay off loans and their associated installments.

Create loans with validation (installment count, interest rate, credit limit)
Auto-generate equal monthly installments with proper due dates
List loans per customer
List installments per loan
Pay installments with early/late logic (discounts & penalties)
Admin authentication for all endpoints
Java 17
Spring Boot
Spring Data JPA
Spring Security (Basic Auth)
H2 in-memory DB
Lombok
JUnit (basic test support added)
How to Run

Clone the repo and open it in IntelliJ
Run the project with:
./mvnw spring-boot:run
