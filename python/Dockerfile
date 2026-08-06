FROM python:3.12-slim

WORKDIR /app

COPY python/requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY python/ python/

COPY revExpenseData.db .

EXPOSE 5000

CMD ["python", "python/api/app.py"]