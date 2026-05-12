Prerequisites
Java 21 JDK
Maven 3.9.15
PostgreSQL 16 or later

Database Creation
Create the database

CREATE DATABASE bank;

Create the tables

create table account (
id INTEGER GENERATED ALWAYS AS IDENTITY primary key not null,
first_name VARCHAR(50) not null,
last_name VARCHAR(50) not null,
balance DECIMAL(20, 2) not null
);


create table transaction (
id INTEGER GENERATED ALWAYS AS IDENTITY primary key not null,
transaction_type VARCHAR(25) not null,
amount DECIMAL(20, 2) not null,
time TIMESTAMP default NOW(),
description VARCHAR(255),
account_id INTEGER references account(id) on delete CASCADE
);


Grant the appropriate privileges to a user to perform read/write operations on the database

CREATE USER your_user WITH PASSWORD ‘your_password’;

GRANT ALL PRIVILEGES ON DATABASE bank to your_user;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_user;

Configuration

In the application.properties file in the source directory, a few adjustments must be made to allow the PostgreSQL driver to communicate with JPARepository.

Replace spring.datasource.username and spring.datasource.password with the username and password provided on setup.
Running the app

bash
mvn clean install -U
mvn spring-boot:run

Running the tests

To run the integration tests, you must have an instance of Docker Desktop open so that the PostgreSQL Testcontainer can be made.


bash
mvn test

API reference
Get All Accounts
GET /accounts
Response 200 OK:
json
[your example here]

Get Account by ID
GET /accounts/{id}
[your description]
Response 200 OK:
json
[your example here]
Errors:
404 — [when]

Create Account
POST /accounts
[your description]
Request Body:
json
[your example here]
Response 201 Created:
json
[your example here]
Errors:
400 — [when]

Deposit
POST /accounts/{id}/deposit
[your description]
Request Body:
json
[your example here]
Response 200 OK:
json
[your example here]
Errors:
400 — [when]
404 — [when]

Withdraw
POST /accounts/{id}/withdraw
[your description]
Request Body:
json
[your example here]
Response 200 OK:
json
[your example here]
Errors:
400 — [when]
404 — [when]
409 — [when]

Transfer
POST /accounts/{id}/transfer
[your description]
Request Body:
json
[your example here]
Response 200 OK:
json
[your example here]
Errors:
400 — [when]
404 — [when]
409 — [when]

Get Transaction History
GET /accounts/{id}/transactions
[your description]
Query Parameters: page, size (optional)
Response 200 OK:
json
[your example here]
Errors:
404 — [when]

Close Account
DELETE /accounts/{id}
[your description]
Response 204 No Content
Errors:
404 — [when]
409 — [when]



