#!/bin/bash
# Script để tạo multiple databases trong PostgreSQL container

set -e
set -u

function create_user_and_database() {
	local database=$1
	local user=$2
	local password=$3
	echo "Creating user '$user' and database '$database'"
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	    CREATE USER $user WITH PASSWORD '$password';
	    CREATE DATABASE $database;
	    GRANT ALL PRIVILEGES ON DATABASE $database TO $user;
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
	echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
	
	# Create OnlyOffice database
	create_user_and_database $POSTGRES_ONLYOFFICE_DB $POSTGRES_ONLYOFFICE_USER $POSTGRES_ONLYOFFICE_PASSWORD
	
	echo "Multiple databases created"
fi