# URL Shortener API
## Features

- User registration and JWT-based authentication
- Create shortened URLs (authenticated)
- Manage personal shortened URLs
- Fast URL redirection with database indexing
- Input validation and security headers
- PostgreSQL database with Flyway migrations
- OpenAPI/Swagger documentation

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17**
- **Maven 3.6+**
- **PostgreSQL 15+**
- **Docker**

## Quick Start

### Option 1: Local Development Setup

#### 1. Clone the Repository

```bash
git clone <repository-url>
cd url-shortener-api
```

#### 2. Set Up PostgreSQL Database

Create a new PostgreSQL database:

#### 3. Configure Environment Variables

```bash
export DB_USERNAME=u_postgres
export DB_PASSWORD=p_postgres
export JWT_SECRET= ${secret}
export BASE_URL=http://localhost:8080
```

**Important:** For production, use a strong JWT secret (minimum 256 bits). You can generate one using:

#### 4. Build the Application

```bash
mvn clean install
```

#### 5. Run Database Migrations

Flyway migrations will run automatically on application startup, creating the necessary tables and indexes.

#### 6. Start the Application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### Users Table
- `id` (BIGSERIAL PRIMARY KEY)
- `uid` (VARCHAR(46) UNIQUE)
- `email` (VARCHAR(255) UNIQUE)
- `password_hash` (VARCHAR(60))
- `created_at` (TIMESTAMP)

### Shortened URLs Table
- `id` (BIGSERIAL PRIMARY KEY)
- `uid` (VARCHAR(46))
- `short_code` (VARCHAR(8) UNIQUE)
- `original_url` (VARCHAR(2048))
- `user_id` (BIGINT FOREIGN KEY)
- `active` (BOOLEAN)
- `created_at` (TIMESTAMP)

Indexes are created on `email`, `short_code`, `user_id`, and `created_at` for optimal query performance.

## API Documentation

Once the application is running, you can access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

The Swagger UI provides an interactive interface to test all API endpoints.


## API Usage Examples

### Authentication Flow

#### 1. Register a New User

Create a new user account:

```bash
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securepassword123"
  }'
```

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (409 Conflict - Duplicate Email):**
```json
{
   "message":"Email already exists",
   "status":409,
   "timestamp":"2025-11-16T11:48:19.448829"
}                                 
```

**Error Response (400 Bad Request - Validation Error):**
```json
{
  "message": "Password must be at least 8 characters",
  "status": 400,
  "timestamp": "2025-11-16T11:49:11.718336"
}
```

#### 2. Login

Authenticate with existing credentials:

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securepassword123"
  }'
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (401 Unauthorized - Invalid Credentials):**
```json
{
  "message": "Invalid credentials",
  "status": 401,
  "timestamp": "2025-11-16T11:50:28.537321"
}
```

**Note:** Save the JWT token from the response. You'll need it for authenticated endpoints. The token is valid for 24 hours.

### URL Shortening

#### 3. Create a Shortened URL (Authenticated)

Shorten a URL without authentication:

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJyb2xlcyI6WyJST0xFX1VTRVIiXSwidXNlcklkIjoiNTNjYjA2Y2QtM2U1MS00OWJlLThlZGYtMTRlOTZlNjk0MzJhIiwiZW1haWwiOiJNeXJuYTIyQGV4YW1wbGUubmV0Iiwic3ViIjoiTXlybmEyMkBleGFtcGxlLm5ldCIsImlhdCI6MTc2MzI2MzEwMSwiZXhwIjoxNzYzMzQ5NTAxfQ.yPN0S6a8hdKAdiXKGTJM0a2WlWZKlIV4XgE0TfBcmpgY0rQZzDmzF95FPEKPNUcw" \
  -d '{
    "originalUrl": "https://www.macode101.com/very/long/url/path"
  }'
```

**Response (201 Created):**
```json
{
  "shortUrl": "http://localhost:8080/r/aB3xY9",
  "shortCode": "aB3xY9",
  "originalUrl": "https://www.example.com/very/long/url/path"
}
```

**Error Response (400 Bad Request - Invalid URL):**
```json
{
  "message": "Invalid URL format. URL must start with http:// or https://",
  "status": 400,
  "timestamp": "2024-11-14T10:30:00"
}
```

### URL Management (Authenticated Endpoints)

#### 4. List Your Shortened URLs

Retrieve all URLs created by your account:

```bash
curl -X GET http://localhost:8080/api/urls \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "shortCode": "xK9mP2",
    "shortUrl": "http://localhost:8080/r/xK9mP2",
    "originalUrl": "https://github.com/spring-projects/spring-boot",
    "active": true,
    "createdAt": "2024-11-14T10:30:00"
  },
  {
    "id": 2,
    "shortCode": "aB3xY9",
    "shortUrl": "http://localhost:8080/r/aB3xY9",
    "originalUrl": "https://www.example.com/very/long/url/path",
    "active": true,
    "createdAt": "2024-11-14T09:15:00"
  }
]
```

**Error Response (401 Unauthorized - Missing or Invalid Token):**
```json
{
  "message": "Authentication required",
  "status": 401,
  "timestamp": "2024-11-14T10:30:00"
}
```

#### 5. Delete (Deactivate) a Shortened URL

Deactivate one of your shortened URLs:

```bash
curl -X DELETE http://localhost:8080/api/urls/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response (204 No Content):**
```
(Empty response body)
```

**Error Response (403 Forbidden - Not Your URL):**
```json
{
  "message": "You do not have permission to delete this URL",
  "status": 403,
  "timestamp": "2024-11-14T10:30:00"
}
```

**Error Response (404 Not Found - URL Doesn't Exist):**
```json
{
  "message": "URL not found",
  "status": 404,
  "timestamp": "2024-11-14T10:30:00"
}
```

### URL Redirection

#### 6. Access a Shortened URL (Redirect)

Visit a shortened URL to be redirected to the original destination:

```bash
curl -L http://localhost:8080/r/xK9mP2
```

**Response (302 Found):**
```
HTTP/1.1 302 Found
Location: https://github.com/spring-projects/spring-boot
```

The `-L` flag tells curl to follow the redirect. In a browser, this happens automatically.

**Error Response (404 Not Found - Invalid Short Code):**
```json
{
  "message": "Short URL not found",
  "status": 404,
  "timestamp": "2024-11-14T10:30:00"
}
```
