import os
import shutil
import sys
from pathlib import Path

import pytest

# Ensure sibling modules in the python directory are importable when tests
# are collected from the repository root.
PYTHON_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_DB_PATH = PROJECT_ROOT / "revExpenseData.db"

if str(PYTHON_DIR) not in sys.path:
    sys.path.insert(0, str(PYTHON_DIR))


@pytest.fixture(scope="module", autouse=True)
def isolated_test_database(tmp_path_factory, request):
    module_name = request.module.__name__.split(".")[-1]
    db_copy = tmp_path_factory.mktemp("expense-db") / f"{module_name}.db"
    shutil.copy2(DEFAULT_DB_PATH, db_copy)
    os.environ["EXPENSE_DB_PATH"] = str(db_copy)
    yield str(db_copy)

