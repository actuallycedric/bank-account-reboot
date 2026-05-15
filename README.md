# Prerequisites
- Java 21 JDK
- Maven 3.9.15
- PostgreSQL 16 or later

# Database Creation
1. Create the database

```
CREATE DATABASE your_database_name;
```

2. Create the tables

```
create table account (
id INTEGER GENERATED ALWAYS AS IDENTITY primary key not null,
first_name VARCHAR(50) not null,
last_name VARCHAR(50) not null,
balance DECIMAL(20, 2) not null
);
```

```
create table transaction (
id INTEGER GENERATED ALWAYS AS IDENTITY primary key not null,
transaction_type VARCHAR(25) not null,
amount DECIMAL(20, 2) not null,
time TIMESTAMP default NOW(),
description VARCHAR(255),
account_id INTEGER references account(id) on delete CASCADE
);
```

3. Grant the appropriate privileges to a user to perform read/write operations on the database
```
CREATE USER your_user WITH PASSWORD ‘your_password’;

GRANT ALL PRIVILEGES ON DATABASE bank to your_user;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_user;
```

# Configuration

In the application.properties file in the source directory, a few adjustments must be made to allow the PostgreSQL driver to communicate with JPARepository.

Replace spring.datasource.username and spring.datasource.password with the username and password provided on setup.

```text
spring.application.name=bank-rest-api
spring.datasource.url=jdbc:postgresql://localhost:5432/{your_database_name}
spring.datasource.username={your_username}
spring.datasource.password={your_password}
```
# Running the app

``` bash
mvn clean install -U
mvn spring-boot:run
```

# Running integration tests

To run the integration tests, you must have an instance of Docker Desktop open so that the PostgreSQL Testcontainer can be initialized.


``` bash
mvn test
```

# API reference
## Get All Accounts

```
GET /accounts
```

Returns a list of all accounts in the database

Response 200 OK:
``` json
{
    "id":1,
    "firstName":"John",
    "lastName":"Doe",
    "balance":500.00
}
```

Optional: Returns a list of all accounts in the database paginated,
using the request parameters `/accounts?page={page_number}&size={size_of_page}`


Response 200 OK:
``` json
{
    "totalElements": 5,
    "currentPage": 0,
    "leftoverPages": 1,
    "content": [
        {
            "id": 1,
            "firstName": "John",
            "lastName": "Doe",
            "balance": 1100.00
        },
        {
            "id": 2,
            "firstName": "Jane",
            "lastName": "Smith",
            "balance": 2450.50
        },
        {
            "id": 5,
            "firstName": "David",
            "lastName": "Bowie",
            "balance": 150.00
        }
    ],
    "hasNext": true
}
```

## Get Account by ID
```
GET /accounts/{id}
```
Returns a specific account based on the `id` path variable

Response 200 OK:

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "balance": 1100.00
}
```
Errors:

404 — Occurs when no account matches the specified ID

## Create Account
```
POST /accounts
```

Creates an account. The request body must provide the first name, last name, and initial deposit.

Request Body:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "initialDeposit": 400.00
}
```
Response 201 Created:
```json
{
  "id": 10,
  "firstName": "John",
  "lastName": "Doe",
  "balance": 400.00
}
```
Errors:

400 — Any malformed input can lead to a 400. The deposit must be greater than zero, and the first and last name cannot be left blank.


## Deposit
```
POST /accounts/{id}/deposit
```
Creates a deposit request. The request body must include the amount to deposit, as well as an optional description, to be logged in a Transaction object.
Request Body:
```json
{
  "amount":400.00,
  "description":"Adding more money to my savings account!"
}
```

The response body returns the account data with the updated balance.

Response 200 OK:
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "balance": 1500.00
}
```
Errors:

400 — Occurs when the input data is malformed, like the deposit field not being a double.

404 — Occurs when the ID of the target account does not match an existing account.

409 — Occurs when the input data is malformed, such as a negative withdrawal amount.
## Withdraw
```
POST /accounts/{id}/withdraw
```
Creates a withdraw request, which has the same behaviour as the deposit request (positive deposit amount, optional description).

Request Body:
```json
{
  "amount":400.00,
  "description":"Withdrawing some money for a fun trip!"
}
```
Response 200 OK:
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "balance": 1100.00
}
```
Errors:

400 — Occurs when the input data is malformed, like the withdrawal amount not being a double.

404 — Occurs when the ID of the target account does not match an existing account.

409 — Occurs when the input data is malformed, such as a negative withdrawal amount.

## Transfer
```
POST /accounts/{id}/transfer
```
Creates a transfer request. The request body should include the amount to transfer, an optional description, and the ID of the recipient account.

Request Body:
```json
{
  "amount":400,
  "description":"Sending some money to Jane!",
  "recipientAccount": 2
}
```

The response body returns the account data for both the sender and the recipient.

Response 200 OK:
```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "balance": 700.00
  },
  {
    "id": 2,
    "firstName": "Jane",
    "lastName": "Smith",
    "balance": 2850.50
  }
]
```
Errors:
400 — Occurs when input data is malformed.

404 — Occurs when the sender or recipient account ID does not match an existing account.

409 — Occurs if the account is trying to transfer negative money, zero money, or if the recipient ID is the same as the sender's ID.

## Get Transaction History
```
GET /accounts/{id}/transactions
```

Returns a list of all transactions linked to a specific account, with optional pagination parameters (page, size).

Response 200 OK:
```json
{
  "totalElements": 3,
  "currentPage": 0,
  "leftoverPages": 0,
  "content": [
    {
      "amount": 400.00,
      "description": "Adding more money to my savings account!",
      "id": 9,
      "time": "2026-05-13T17:13:29.44821",
      "type": "DEPOSIT"
    },
    {
      "amount": 400.00,
      "description": "Withdrawing some money for a fun trip!",
      "id": 10,
      "time": "2026-05-13T17:15:43.661744",
      "type": "WITHDRAW"
    },
    {
      "amount": 400.00,
      "description": "Sending some money to Jane!",
      "id": 11,
      "time": "2026-05-13T17:19:15.437373",
      "type": "TRANSFER"
    }
  ],
  "hasNext": false
}
```
Errors:
404 — Occurs when the target ID doesn't match an existing account.

## Close Account
```
DELETE /accounts/{id}
```

Closes an account by deleting its data as well as any transaction history linked to the account.

Response 204 No Content

Errors:

404 — Occurs when the target ID doesn't match an existing account.

409 — Occurs when the account to be closed has an outstanding balance/debt.



