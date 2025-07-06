#!/bin/bash
# Setup script for E-Form with OnlyOffice

echo "Setting up E-Form with OnlyOffice..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo "Creating .env file..."
    cp .env.example .env
    echo ".env file created. Please review and update the values."
    echo "Important: Update JWT secret and passwords in .env file!"
fi

# Create necessary directories
echo "Creating directories..."
mkdir -p backend/src
mkdir -p frontend/src
mkdir -p nginx
mkdir -p scripts
mkdir -p logs
mkdir -p ssl

# Make scripts executable
chmod +x scripts/*.sh

# Build and start services
echo "Building and starting services..."
docker-compose up -d --build

# Wait for services to start
echo "Waiting for services to start..."
sleep 30

# Check OnlyOffice health
echo "Checking OnlyOffice health..."
if curl -f http://localhost:8081/healthcheck > /dev/null 2>&1; then
    echo "OnlyOffice Document Server is running"
else
    echo "OnlyOffice Document Server is not responding"
fi

# Check backend health
echo "Checking backend health..."
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "E-Form Backend is running"
else
    echo "E-Form Backend is not responding"
fi

# Check frontend
echo "Checking frontend..."
if curl -f http://localhost:3000 > /dev/null 2>&1; then
    echo "E-Form Frontend is running"
else
    echo "E-Form Frontend is not responding"
fi

echo ""
echo "Setup completed!"
echo ""
echo "Access URLs:"
echo "   Frontend: http://localhost:3000"
echo "   Backend:  http://localhost:8080"
echo "   OnlyOffice: http://localhost:8081"
echo ""
echo "Management Commands:"
echo "   View logs: docker-compose logs -f"
echo "   Stop all:  docker-compose down"
echo "   Restart:   docker-compose restart"
echo ""