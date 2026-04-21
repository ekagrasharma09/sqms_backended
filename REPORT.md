# Smart Queue Management System

## Project Report

### Submitted By

Name: Ekagra Sharma  
Roll No./Enrollment No.: PIET24CA015  
Course: B. Tech CS(AI)  
Semester/Year: 4th Sem, 2nd Year  
Section: F1  

### Submitted To

Faculty Name: Dr. Buddhesh Kanwar  
Subject: DBMS  
Department: CS (AI & DS)  
College/Institute: Poornima Institute of Engineering and Technology, Jaipur  
Academic Session: 2025-26

---

## Certificate

This is to certify that Ekagra Sharma, Roll No./Enrollment No. PIET24CA015, has successfully completed the project titled "Smart Queue Management System" as part of the DBMS practical work.

The project demonstrates the use of Java backend connectivity with a MySQL database using JDBC. It includes database creation, table creation, insertion, selection, and update operations.

Faculty Signature: ____________________

---

## Acknowledgement

I would like to express my sincere gratitude to my faculty and department for giving me the opportunity to work on this project. This project helped me understand the practical implementation of DBMS concepts, Java backend development, JDBC connectivity, and SQL database operations.

I am also thankful to my classmates and mentors for their guidance and support during the development of this project.

---

### 1. Title

Smart Queue Management System using Java Backend and MySQL Database

### 2. Introduction

The Smart Queue Management System is a web-based project designed to manage service queues digitally. In many real-life places such as banks, hospitals, college offices, and service centers, people wait in queues for different services. This project provides a simple token-based queue system where users can generate a token, view the current queue, and serve the next token.

The project uses HTML, CSS, and JavaScript for the frontend, Java for the backend server, and MySQL as the SQL database. The Java backend communicates with the MySQL database using JDBC.

### 3. Objectives

- To create a simple web-based queue management system.
- To connect a frontend application with a backend server.
- To store queue data permanently in a SQL database.
- To demonstrate DBMS concepts such as tables, primary keys, insert, select, and update queries.
- To manage queue tokens based on first-come, first-served order.

### 4. Technologies Used

| Component | Technology |
| --- | --- |
| Frontend | HTML, CSS, JavaScript |
| Backend | Java |
| Database | MySQL |
| Database Connectivity | JDBC |
| Server | Java built-in HTTP server |

### 5. System Architecture

The project follows a three-layer architecture:

```text
Frontend UI
    |
    | HTTP API requests
    v
Java Backend Server
    |
    | JDBC SQL queries
    v
MySQL Database
```

The frontend sends requests to the Java backend. The backend processes the request, performs SQL operations using JDBC, and sends a response back to the frontend.

### 6. Project Modules

#### 6.1 Login Module

The login module allows an admin user to access the queue management page. The frontend sends the username and password to the Java backend using the `/api/login` API route. The backend checks the user record from the `users` table.

#### 6.2 Token Generation Module

This module allows the user to generate a new token by entering a name and service type. The backend inserts the token details into the `tokens` table with the default status `waiting`.

#### 6.3 View Queue Module

This module displays all waiting tokens from the database. The backend fetches tokens using a `SELECT` query and returns them to the frontend.

#### 6.4 Serve Next Token Module

This module serves the first waiting token in the queue. The backend finds the earliest waiting token and updates its status from `waiting` to `served`.

### 7. Database Design

Database name:

```sql
sqms_db
```

#### 7.1 Users Table

The `users` table stores admin login details.

```sql
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL
);
```

| Column | Description |
| --- | --- |
| id | Unique user ID |
| username | Admin username |
| password_hash | Hashed password value |

#### 7.2 Tokens Table

The `tokens` table stores queue token details.

```sql
CREATE TABLE IF NOT EXISTS tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    service VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'waiting',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    served_at TIMESTAMP NULL
);
```

| Column | Description |
| --- | --- |
| id | Unique token number |
| name | Name of the person/customer |
| service | Type of service requested |
| status | Token status: waiting or served |
| created_at | Token creation time |
| served_at | Time when token was served |

### 8. SQL Operations Used

#### 8.1 Create Database

```sql
CREATE DATABASE IF NOT EXISTS sqms_db;
```

#### 8.2 Insert Token

```sql
INSERT INTO tokens (name, service) VALUES (?, ?);
```

#### 8.3 View Waiting Queue

```sql
SELECT id, name, service, status, created_at, served_at
FROM tokens
WHERE status = 'waiting'
ORDER BY id;
```

#### 8.4 Serve Next Token

```sql
SELECT id
FROM tokens
WHERE status = 'waiting'
ORDER BY id
LIMIT 1 FOR UPDATE;
```

```sql
UPDATE tokens
SET status = 'served', served_at = ?
WHERE id = ?;
```

### 9. API Routes

| API Route | Method | Purpose |
| --- | --- | --- |
| `/api/login` | POST | Checks admin login |
| `/api/tokens` | GET | Displays waiting tokens |
| `/api/tokens` | POST | Creates a new token |
| `/api/tokens/serve-next` | POST | Serves the next waiting token |

### 10. Working Process

1. The user opens the application in the browser.
2. The Java server serves `login.html`.
3. The admin enters login details.
4. The frontend sends the login details to the backend.
5. The backend checks the `users` table in MySQL.
6. After successful login, the user is redirected to the queue page.
7. When a token is generated, the backend inserts it into the `tokens` table.
8. When the queue is viewed, the backend fetches all waiting tokens from MySQL.
9. When the next token is served, the backend updates its status to `served`.

### 11. Screenshots

#### 11.1 Login Page

![Login Page](screenshots/login-page.png)

#### 11.2 Queue Page

![Queue Page](screenshots/queue-page.png)

#### 11.3 MySQL Tables

![MySQL Tables](screenshots/mysql-tables.png)

### 12. Advantages

- Simple and easy to use.
- Uses a real SQL database.
- Stores queue data permanently.
- Demonstrates backend and database connectivity.
- Shows practical DBMS operations such as insert, select, and update.

### 13. Limitations

- The project currently supports a simple admin login only.
- It is intended for practical/demo use, not production deployment.
- The frontend design is basic and can be improved further.
- It requires MySQL and JDBC Connector/J to be installed before running.

### 14. Future Scope

- Add multiple admin users.
- Add customer login or registration.
- Add token priority based on service type.
- Add token search and service history.
- Add dashboard charts for daily token statistics.
- Add export report feature.

### 15. Conclusion

The Smart Queue Management System successfully demonstrates how a frontend web application can communicate with a Java backend and store data in a MySQL database. It uses important DBMS concepts such as table creation, primary keys, insertion, selection, and updating records. The project is suitable for demonstrating backend connectivity and SQL database usage in a DBMS practical.
