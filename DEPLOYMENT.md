# Deployment Guide - Test Case Management System

This guide explains how to deploy the Test Case Management (TCM) system to DigitalOcean using the JAR + Nginx approach. This method is simple, easy to understand, and doesn't require containerization knowledge.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [DigitalOcean Setup](#digitalocean-setup)
3. [Server Configuration](#server-configuration)
4. [Database Setup](#database-setup)
5. [Build the Application](#build-the-application)
6. [Deploy the Backend](#deploy-the-backend)
7. [Deploy the Frontend](#deploy-the-frontend)
8. [Configure Nginx](#configure-nginx)
9. [Set Up SSL with Let's Encrypt](#set-up-ssl-with-lets-encrypt)
10. [Set Up Systemd Service](#set-up-systemd-service)
11. [Environment Variables](#environment-variables)
12. [Test the Deployment](#test-the-deployment)
13. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools
- **Git**: For cloning the repository
- **Maven**: For building the backend JAR
- **Node.js 21+**: For building the frontend
- **DigitalOcean Account**: For hosting the application
- **Domain Name**: For SSL certificate (optional but recommended)

### DigitalOcean Plan
- **Droplet**: Ubuntu 22.04 LTS
- **RAM**: 2GB minimum, 4GB recommended
- **CPU**: 1 vCPU minimum, 2 vCPUs recommended
- **Storage**: 40GB SSD
- **Cost**: ~$6-12/month (student discount may apply)

---

## DigitalOcean Setup

### 1. Create a DigitalOcean Account
1. Go to https://www.digitalocean.com/
2. Sign up or log in
3. Apply for the Student Pack if eligible

### 2. Create a Droplet
1. Click "Create" → "Droplets"
2. Choose **Ubuntu 22.04 LTS**
3. Select your plan (Basic, $6/month recommended for testing)
4. Choose a region closest to your users
5. Set authentication (SSH key recommended)
6. Add your domain name (optional)
7. Click "Create Droplet"

### 3. Connect to Your Droplet
```bash
ssh root@your-droplet-ip
```

Or using your SSH key:
```bash
ssh -i /path/to/your/key root@your-droplet-ip
```

---

## Server Configuration

### Update System Packages
```bash
apt update && apt upgrade -y
```

### Install Required Software
```bash
# Install Java 17
apt install openjdk-17-jdk -y

# Install Nginx
apt install nginx -y

# Install Maven
apt install maven -y

# Install Node.js 21 (using NVM for latest version)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc
nvm install 21
nvm use 21

# Install Git
apt install git -y
```

### Configure Firewall
```bash
# Allow SSH, HTTP, and HTTPS
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

### Verify Installation
```bash
java -version      # Should show openjdk 17
mvn -version       # Should show Maven 3.x
node -v            # Should show Node.js 21
nginx -v           # Should show Nginx version
```

---

## Database Setup

### Option 1: DigitalOcean Managed MySQL (Recommended)

1. Go to DigitalOcean Dashboard
2. Click "Create" → "Databases"
3. Choose MySQL
4. Select plan (Basic, $15/month for 1GB RAM)
5. Configure:
   - Database name: `testcasedb`
   - User: `tcm_user`
   - Region: Same as droplet
6. Create the database

### Option 2: Self-Hosted MySQL (Free)

```bash
# Install MySQL Server
apt install mysql-server -y

# Secure MySQL installation
mysql_secure_installation

# Create database and user
mysql -u root -p
```

Run these SQL commands:
```sql
CREATE DATABASE testcasedb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'tcm_user'@'localhost' IDENTIFIED BY 'your_strong_password_here';
GRANT ALL PRIVILEGES ON testcasedb.* TO 'tcm_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

---

## Build the Application

### Clone the Repository
```bash
cd /opt
git clone https://github.com/your-username/test-case-management-system.git tcm
cd tcm
```

### Build the Backend JAR
```bash
# Using local Maven (if in project directory)
./apache-maven-3.9.8/bin/mvn clean package -DskipTests

# Or using system Maven
mvn clean package -DskipTests
```

This creates `target/tcm-0.0.1-SNAPSHOT.jar`

### Build the Frontend
```bash
cd tcm-frontend
npm install
npm run build
```

This creates the `dist/` folder with built frontend files.

---

## Deploy the Backend

### Move JAR to a Permanent Location
```bash
cd /opt/tcm
mkdir -p /var/lib/tcm
cp target/tcm-0.0.1-SNAPSHOT.jar /var/lib/tcm/tcm.jar
chmod +x /var/lib/tcm/tcm.jar
```

### Create Log Directory
```bash
mkdir -p /var/log/tcm
chown -R $USER:$USER /var/log/tcm
```

### Create Environment File
```bash
# Copy the example .env file
cp /opt/tcm/.env.example /var/lib/tcm/.env

# Edit the environment file
nano /var/lib/tcm/.env
```

Fill in your values:
```env
DB_URL=jdbc:mysql://localhost:3306/testcasedb?useSSL=true&serverTimezone=UTC
DB_USERNAME=tcm_user
DB_PASSWORD=your_database_password
JWT_SECRET=your_random_256_bit_secret
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_admin_password
ADMIN_EMAIL=admin@yourdomain.com
FRONTEND_URL=https://yourdomain.com
```

Generate a JWT secret:
```bash
openssl rand -base64 32
```

---

## Deploy the Frontend

### Move Frontend Files to Nginx
```bash
# Remove default Nginx welcome page
rm -rf /var/www/html/*

# Copy Angular build files
cp -r /opt/tcm/tcm-frontend/dist/* /var/www/html/

# Set permissions
chown -R www-data:www-data /var/www/html
chmod -R 755 /var/www/html
```

---

## Configure Nginx

### Create Nginx Configuration
```bash
nano /etc/nginx/sites-available/tcm
```

Add this configuration:
```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    root /var/www/html;
    index index.html;

    # Frontend routes
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API proxy
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_http_version 1.1;
        proxy_cache_bypass $http_upgrade;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
```

### Enable the Configuration
```bash
ln -s /etc/nginx/sites-available/tcm /etc/nginx/sites-enabled/
rm /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
```

---

## Set Up SSL with Let's Encrypt

### Install Certbot
```bash
apt install certbot python3-certbot-nginx -y
```

### Obtain SSL Certificate
```bash
certbot --nginx -d your-domain.com -d www.your-domain.com
```

Follow the prompts:
1. Enter your email
2. Agree to terms
3. Choose to redirect HTTP to HTTPS

### Auto-Renew SSL
```bash
# Test auto-renewal
certbot renew --dry-run

# Certbot is configured to auto-renew automatically
```

---

## Set Up Systemd Service

### Create Systemd Service File
```bash
nano /etc/systemd/system/tcm.service
```

Add this configuration:
```ini
[Unit]
Description=Test Case Management System Backend
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/var/lib/tcm
Environment="SPRING_PROFILES_ACTIVE=prod"
EnvironmentFile=/var/lib/tcm/.env
ExecStart=/usr/bin/java -jar /var/lib/tcm/tcm.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/tcm/tcm.log
StandardError=append:/var/log/tcm/tcm-error.log

[Install]
WantedBy=multi-user.target
```

### Enable and Start the Service
```bash
systemctl daemon-reload
systemctl enable tcm
systemctl start tcm
systemctl status tcm
```

---

## Environment Variables

### Loading Environment Variables
The systemd service automatically loads variables from `/var/lib/tcm/.env`

### Key Variables
- `DB_URL`: MySQL connection string
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: Secret key for JWT tokens
- `ADMIN_USERNAME`: Default admin username
- `ADMIN_PASSWORD`: Default admin password
- `ADMIN_EMAIL`: Default admin email
- `FRONTEND_URL`: Frontend URL for invitations

---

## Test the Deployment

### Check Backend Health
```bash
# Check if backend is running
curl http://localhost:8080/api/test

# Or check systemd logs
journalctl -u tcm -f
```

### Check Frontend
1. Open your browser
2. Go to `https://your-domain.com`
3. You should see the TCM login page

### Test Login
1. Use the admin credentials from your `.env` file
2. Login and verify the application works

---

## Troubleshooting

### Backend Won't Start
```bash
# Check service status
systemctl status tcm

# Check logs
journalctl -u tcm -n 50

# Check application logs
tail -f /var/log/tcm/tcm-error.log
```

### Database Connection Issues
```bash
# Test database connection
mysql -u tcm_user -p -h localhost testcasedb

# Check MySQL is running
systemctl status mysql
```

### Nginx 502 Bad Gateway
```bash
# Check if backend is running
curl http://localhost:8080

# Check Nginx error logs
tail -f /var/log/nginx/error.log

# Restart Nginx
systemctl restart nginx
```

### Permission Issues
```bash
# Fix file permissions
chown -R root:root /var/lib/tcm
chmod -R 755 /var/lib/tcm
```

### SSL Certificate Issues
```bash
# Renew SSL certificate manually
certbot renew --force-renewal

# Check Nginx SSL configuration
nginx -t
```

---

## Maintenance

### Update the Application
```bash
cd /opt/tcm
git pull
mvn clean package -DskipTests
cp target/tcm-0.0.1-SNAPSHOT.jar /var/lib/tcm/tcm.jar
systemctl restart tcm
```

### Update Frontend
```bash
cd /opt/tcm/tcm-frontend
git pull
npm install
npm run build
cp -r dist/* /var/www/html/
systemctl reload nginx
```

### Backup Database
```bash
# Backup database
mysqldump -u tcm_user -p testcasedb > tcm_backup_$(date +%Y%m%d).sql

# Restore database
mysql -u tcm_user -p testcasedb < tcm_backup_20240101.sql
```

---

## Cost Summary

### DigitalOcean Monthly Cost
- **Droplet (2GB RAM)**: $6/month
- **Managed MySQL (1GB)**: $15/month
- **Total**: ~$21/month

### Free Alternative
- **Droplet (2GB RAM)**: $6/month
- **Self-Hosted MySQL**: Free
- **Total**: ~$6/month

---

## Next Steps

1. Set up regular backups
2. Configure monitoring (DigitalOcean Monitoring)
3. Set up email notifications
4. Configure custom domain (if not done)
5. Test all features end-to-end
6. Train users on the system

---

## Support

For issues or questions:
- Check application logs: `/var/log/tcm/tcm-error.log`
- Check Nginx logs: `/var/log/nginx/error.log`
- Check systemd logs: `journalctl -u tcm`

---

**Last Updated**: January 31, 2026
**Version**: 1.0.0