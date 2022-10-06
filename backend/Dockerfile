FROM python:3.6
WORKDIR /code/backend
RUN apt-get update && apt-get install -y python3-pyodbc binutils libproj-dev gdal-bin freetds-dev freetds-bin unixodbc-dev tdsodbc
COPY base_requirements.txt base_requirements.txt
COPY requirements.txt requirements.txt
COPY odbcinst.ini /etc/odbcinst.ini
RUN pip install -r requirements.txt
EXPOSE 8000
CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]
