# Smart Queue Management System

A small queue management app with a Java backend and MySQL database.

## Requirements

- JDK 17 or newer
- MySQL Server
- MySQL Connector/J `.jar`

Download Connector/J from the official MySQL site, create a `lib` folder, and place the `.jar` file inside it.

## Database setup

Open MySQL and run:

```sql
SOURCE E:/Git/SQMS/schema.sql;
```

The Java server also creates the database and required tables automatically, but `schema.sql` is included for DBMS practical submission.

## Run the project

In `SQMSServer.java`, update these values if your MySQL username or password is different:

```java
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "root";
```

Compile:

```bash
javac --add-modules jdk.httpserver -cp ".;lib/*" SQMSServer.java
```

Run:

```bash
java --add-modules jdk.httpserver -cp ".;lib/*" SQMSServer
```

Or on Windows:

```bash
run.bat
```

Open:

```text
http://localhost:8000
```

Default login:

```text
Username: admin
Password: 1234
```

## API routes

- `POST /api/login` checks username and password.
- `GET /api/tokens` returns the waiting queue.
- `POST /api/tokens` creates a new token.
- `POST /api/tokens/serve-next` serves the next waiting token.
