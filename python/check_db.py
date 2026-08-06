import sqlite3

conn = sqlite3.connect('revExpenseData.db')
cur = conn.cursor()
print(cur.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall())
try:
    cur.execute("SELECT * FROM users LIMIT 1")
    print('users exists')
except Exception as e:
    print('users error', e)
conn.close()
