#!/bin/bash
# Cleanup script

echo "Cleaning up E-Form environment..."

# Stop and remove containers
docker-compose down

# Remove volumes (optional - commented out for safety)
# echo "This will delete all data! Uncomment to enable:"
# docker-compose down -v
# docker volume prune -f

# Remove images (optional)
# docker-compose down --rmi all

# Clean up logs
rm -rf logs/*.log

echo "Cleanup completed!"