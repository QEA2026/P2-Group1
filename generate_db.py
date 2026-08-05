import sqlite3
from pathlib import Path

root = Path(__file__).resolve().parent
schema_path = root / 'expense-app-managers' / 'src' / 'test' / 'resources' / 'test-schema.sql'
data_path = root / 'expense-app-managers' / 'src' / 'test' / 'resources' / 'test-data.sql'
db_path = root / 'revExpenseData.db'

if not schema_path.exists() or not data_path.exists():
    raise FileNotFoundError(f'Schema or data SQL file missing: {schema_path}, {data_path}')

print('Generating', db_path)

with sqlite3.connect(db_path) as conn:
    cur = conn.cursor()
    cur.executescript(schema_path.read_text())
    cur.executescript(data_path.read_text())

print('Done. Tables:', sqlite3.connect(db_path).cursor().execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall())
