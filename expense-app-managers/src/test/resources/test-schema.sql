DROP TABLE IF EXISTS approvals;
DROP TABLE IF EXISTS expenses;
DROP TABLE IF EXISTS users;
CREATE TABLE IF NOT EXISTS "users" (
	"id"	INT,
	"username"	TEXT,
	"password"	TEXT,
	"role"	TEXT,
	PRIMARY KEY("id"),
	UNIQUE("username")
);
CREATE TABLE IF NOT EXISTS "expenses" (
	"id"	INT,
	"user_id"	INT,
	"amount"	REAL,
	"description"	TEXT,
	"date"	TEXT,
	"category"	TEXT,
	PRIMARY KEY("id"),
	FOREIGN KEY("user_id") REFERENCES "users"("id")
);
CREATE TABLE IF NOT EXISTS "approvals" (
	"id"	INT,
	"expense_id"	INT,
	"status"	TEXT,
	"reviewer"	INT,
	"comment"	TEXT,
	"review_date"	TEXT,
	PRIMARY KEY("id"),
	FOREIGN KEY("expense_id") REFERENCES "expenses"("id")
);
