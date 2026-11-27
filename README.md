# Smart Pantry

A household pantry tracker with a Spring Boot REST API and a responsive, dependency-free web frontend. The backend and frontend are separate projects so they can be developed in parallel against the API contract below.

## Project layout

```text
backend/    Java 17+, Spring Boot, Spring Web, Spring Data JPA, validation, H2
frontend/   HTML, CSS, and JavaScript ES modules
```

## Run locally

### Backend

Requirements: Java 17 or newer and Maven 3.8+.

```powershell
cd backend
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. On first startup it creates an H2 database at `backend/pantrydb` and inserts example items. The H2 console is at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./pantrydb`, user `sa`, blank password).

Run backend tests with `mvn test` from `backend/`.

The database can be changed through environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:h2:file:./pantrydb;DB_CLOSE_ON_EXIT=FALSE` | JDBC URL; use a PostgreSQL JDBC URL to connect PostgreSQL |
| `DATABASE_USERNAME` | `sa` | Database user |
| `DATABASE_PASSWORD` | empty | Database password |
| `SERVER_PORT` | `8080` | API port |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000,http://127.0.0.1:5500` | Comma-separated frontend origins |

For PostgreSQL, add the PostgreSQL JDBC driver dependency to `backend/pom.xml`, set `DATABASE_URL` to a `jdbc:postgresql://...` URL, and set the username and password. Hibernate uses `spring.jpa.hibernate.ddl-auto=update`; use migrations and a deliberate schema strategy before production deployment.

### Frontend

The default API URL is set in `frontend/config.js` as `http://localhost:8080/api/v1`. Change `window.PANTRY_API_BASE_URL` there to point at another API host.

Serve `frontend/` from a local static web server (do not open the HTML as a `file://` URL). For example, with Python installed:

```powershell
cd frontend
py -m http.server 5173
```

Then open `http://localhost:5173`. The default frontend origin is enabled for CORS by the backend. The frontend can also be hosted on port `3000`; set `APP_CORS_ALLOWED_ORIGINS` if it is served from a different origin.

### Authentication

The app supports email/password registration and sign-in on separate `/register.html` and `/login.html` pages. Passwords must be 12–128 characters and are stored as PBKDF2 hashes. Authentication uses a server-side session cookie and CSRF protection; the frontend requests `/api/v1/auth/csrf` before sending state-changing requests.

`POST /api/v1/auth/register` creates an account without signing it in; the user is sent to the sign-in page to start a session. `POST /api/v1/auth/login` starts a session, `GET /api/v1/auth/me` returns the signed-in account, and `POST /api/v1/auth/logout` ends the session. All `/api/v1/pantry-items` endpoints require authentication, and each account can access only its own items. Existing items without an owner are assigned to the first account registered after this feature is enabled.

Use HTTPS and a production-ready database/session configuration when deploying outside local development.

## REST API contract

Base URL: `http://localhost:8080/api/v1`

Dates use ISO-8601 `YYYY-MM-DD`; timestamps use ISO local date-time. A pantry item response has this shape:

```json
{
  "id": 1,
  "name": "Oat milk",
  "category": "Beverages",
  "quantity": 2.0,
  "unit": "L",
  "expiryDate": "2026-11-29",
  "status": "AVAILABLE",
  "updatedAt": "2026-11-27T09:30:00"
}
```

Status is resolved when returning an item: quantity `<= 0` is `USED_UP`; otherwise a past expiry date is `EXPIRED`; otherwise quantity `<= 1` is `LOW_STOCK`; all other items are `AVAILABLE`. This comparison uses the item's entered unit. Expiry today is not expired. Status precedence is `USED_UP`, `EXPIRED`, `LOW_STOCK`, then `AVAILABLE`.

Validation errors return HTTP `400` with field-specific messages:

```json
{
  "timestamp": "2026-11-27T09:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed.",
  "fieldErrors": {
    "name": "Name is required.",
    "quantity": "Quantity must be greater than zero."
  }
}
```

Not-found errors return HTTP `404` in the same envelope, for example:

```json
{
  "timestamp": "2026-11-27T09:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Pantry item with id 999 was not found.",
  "fieldErrors": {}
}
```

### List and filter

`GET /pantry-items`

All filters are optional and can be combined: `category` (case-insensitive exact match), `status` (`AVAILABLE`, `LOW_STOCK`, `EXPIRED`, `USED_UP`), `expiringWithinDays` (non-negative integer; from today through the inclusive end date), and `name` (case-insensitive substring search).

Request:

```http
GET /api/v1/pantry-items?category=Beverages&status=AVAILABLE&expiringWithinDays=3&name=oat
```

Response `200 OK`:

```json
[
  {
    "id": 1,
    "name": "Oat milk",
    "category": "Beverages",
    "quantity": 2.0,
    "unit": "L",
    "expiryDate": "2026-11-29",
    "status": "AVAILABLE",
    "updatedAt": "2026-11-27T09:30:00"
  }
]
```

### Fetch one item

`GET /pantry-items/{id}`

Request: `GET /api/v1/pantry-items/1`

Response `200 OK`: one pantry item in the response shape above. An unknown ID returns `404 Not Found` using the error envelope.

### Create an item

`POST /pantry-items`

Request `Content-Type: application/json`:

```json
{
  "name": "Rolled oats",
  "category": "Grains",
  "quantity": 2,
  "unit": "kg",
  "expiryDate": "2027-06-30"
}
```

`name`, `category`, and `unit` must be non-blank; `quantity` is required and must be greater than zero; `expiryDate` is optional (use `null` or omit it). Response `201 Created` includes a `Location` header ending in the created ID and the created pantry item:

```json
{
  "id": 5,
  "name": "Rolled oats",
  "category": "Grains",
  "quantity": 2.0,
  "unit": "kg",
  "expiryDate": "2027-06-30",
  "status": "AVAILABLE",
  "updatedAt": "2026-11-27T09:30:00"
}
```

Invalid JSON field values return `400 Bad Request` with the validation error envelope.

### Replace/update an item

`PUT /pantry-items/{id}`

Request `PUT /api/v1/pantry-items/5` with the same full JSON shape as create. All required fields must be supplied; `expiryDate` remains optional. Response `200 OK` returns the updated pantry item. An unknown ID returns `404`; invalid values return `400`.

```json
{
  "name": "Rolled oats",
  "category": "Grains",
  "quantity": 1,
  "unit": "kg",
  "expiryDate": "2027-06-30"
}
```

### Consume/decrement quantity

`PATCH /pantry-items/{id}/consume`

Request `PATCH /api/v1/pantry-items/5/consume`:

```json
{
  "quantity": 0.5
}
```

`quantity` is optional; when omitted or `null`, one unit is consumed. The supplied quantity must be greater than zero and cannot exceed the current amount. Response `200 OK` returns the updated item; consuming its remaining amount returns status `USED_UP` and quantity `0`. Over-consumption returns `400`; an unknown ID returns `404`.

Response example:

```json
{
  "id": 5,
  "name": "Rolled oats",
  "category": "Grains",
  "quantity": 1.5,
  "unit": "kg",
  "expiryDate": "2027-06-30",
  "status": "AVAILABLE",
  "updatedAt": "2026-11-27T09:35:00"
}
```

### Delete an item

`DELETE /pantry-items/{id}`

Request: `DELETE /api/v1/pantry-items/5`

Response `204 No Content` with an empty body. An unknown ID returns `404`.

### Pantry summary

`GET /pantry-items/summary`

Counts include all stored items; low-stock uses the status rules above, and expiring-soon counts items with expiry dates from today through three days from today (inclusive).

Response `200 OK`:

```json
{
  "totalItems": 4,
  "lowStockItems": 2,
  "expiringSoonItems": 1
}
```
