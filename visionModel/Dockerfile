FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY testModel.py .
COPY models/ models/

RUN mkdir -p output

CMD ["python", "testModel.py"]
