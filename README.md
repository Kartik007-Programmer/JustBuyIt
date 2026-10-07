# JustBuyIt 🛒

A full-stack e-commerce web application built with **Spring Boot 4**, featuring JWT authentication, email verification, password reset, product management, shopping cart, PDF receipt generation, admin user management, and a **two-tier caching system** using **Caffeine + Redis**.

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8-blue?logo=mysql)
![Redis](https://img.shields.io/badge/Redis-alpine-red?logo=redis)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Roles & Permissions](#-roles--permissions)
- [Demo Credentials](#-demo-credentials)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Caching Strategy](#-caching-strategy)
- [Security Model](#-security-model)
- [Project Structure](#-project-structure)
- [Getting Started (IntelliJ + Docker)](#-getting-started-intellij--docker)
- [Environment Configuration](#-environment-configuration)
- [Database Setup](#-database-setup)
- [API Reference](#-api-reference)
- [Screenshots](#-screenshots)
- [Lessons Learned](#-lessons-learned)
- [Author](#-author)

---

## 🧭 Overview

**JustBuyIt** is a learning-focused, production-style e-commerce application built entirely in **Spring Boot 4**. It demonstrates:

- Stateless **JWT authentication** stored in HttpOnly cookies
- **Email verification** and **password reset** flows using Spring Mail
- **Role-based authorization** with **three roles**: `USER`, `SECONDARY_ADMIN`, and `ADMIN`
- A **two-tier cache** (in-JVM Caffeine + remote Redis) layered behind Spring's `CacheManager` abstraction
- **PDF receipt generation** using OpenPDF
- **Admin user management** with ban/unban capabilities

The app is designed to run locally in **IntelliJ IDEA** while **MySQL** and **Redis** run externally (MySQL on Aiven, Redis in Docker) — so contributors don't need to install database servers natively.

---

## ✨ Features

### 👤 Authentication & Account
- User registration with **email verification** (SMTP link)
- **Forgot password** / **reset password** flow with expiring tokens
- Login issues a **JWT** stored in an **HttpOnly cookie** (XSS-safe)
- Logout invalidates both the cookie and the Redis-cached token
- **BCrypt** password hashing

### 🛍️ Shopping (USER role)
- Browse products with **pagination, sorting, and category filter**
- **Search products** by keyword
- View single product with image
- **Shopping cart**: add, update quantity, remove, clear
- **Fake payment checkout** with transaction ID
- **Download PDF receipt** for completed orders

### 🛠️ Administration (ADMIN role)
- **Admin dashboard**
- Add products (JSON + image URL, or multipart file upload)
- Batch product upload
- Update / delete **any** product
- **User management**: paged + searchable user list, ban / unban users
- View all admins

### 🧑‍💼 Secondary Administration (SECONDARY_ADMIN role)
- Access to the same admin dashboard UI
- **Can add up to 3 products** (hard limit enforced server-side)
- **Can update / delete only the products they added** — cannot touch other admins' products
- **Cannot ban or unban users**
- **Cannot access the full admin list** (`GET /admin/all`)
- **Cannot use batch import** (`POST /multi_products`)
- Attempts to exceed the limit return a clean `403 Forbidden` with a clear message

### ⚡ Engineering
- **Two-tier caching** — Caffeine (L1) → Redis (L2) → MySQL
- **Automatic cache invalidation** on product mutations via `@CacheEvict`
- **JWT token + user caching** in Redis for fast auth
- **Lettuce connection pool** tuned for resilience
- **HikariCP** tuned for cloud MySQL (Aiven)
- **`GlobalExceptionHandler`** returns consistent JSON error payloads (`ApiError`) for every failure mode
- **Multipart upload limit** configurable via `application.properties` (currently 5 MB per file / 10 MB per request)
- Environment-variable driven configuration — no secrets in source

---

## 🔑 Roles & Permissions

The app enforces **two layers** of authorization:

1. **URL-level** — `SecurityConfig` restricts endpoints by authority.
2. **Service-level** — `SecurityService.assertCanCreateProduct` and `assertCanModifyOrDeleteProduct` enforce ownership and quotas that URL patterns can't express.

| Action | USER | SECONDARY_ADMIN | ADMIN |
|---|---|---|---|
| Browse products | ✅ | ✅ | ✅ |
| Manage own cart / checkout | ✅ | ✅ | ✅ |
| Access admin dashboard UI | ❌ | ✅ | ✅ |
| Add product | ❌ | ✅ (max 3, own) | ✅ |
| Update product | ❌ | ✅ (own only) | ✅ (any) |
| Delete product | ❌ | ✅ (own only) | ✅ (any) |
| Batch product import (`/multi_products`) | ❌ | ❌ | ✅ |
| List all admins (`/admin/all`) | ❌ | ❌ | ✅ |
| Ban / unban users | ❌ | ❌ | ✅ |
| Manage users page | ❌ | ❌ | ✅ |

**Product ownership** is tracked via `Product.addedBy` (`@ManyToOne` to `Users`). Whenever a Secondary Admin tries to modify or delete a product they did not create, the service throws `IllegalStateException`, which `GlobalExceptionHandler` converts to a `403 Forbidden` with a clear message.

**3-product quota** is enforced at creation time by counting `ProductRepo.countByAddedById(actor.getId())` and rejecting the request when it would exceed the limit.

---

## 🧪 Demo Credentials

For quick testing of the two admin tiers:

| Role | Email | Password |
|---|---|---|
| **ADMIN** | `Admin@gmail.com` | `K@1234` |
| **SECONDARY_ADMIN** | `Admin2@gmail.com` | `K@1234` |

> ⚠️ These are **demo-only** accounts. Change the passwords (or delete the accounts) before deploying anywhere public.

To test as a **USER**, register a fresh account via `RegistrationForm.html` and verify the email.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 4.1.1 |
| **Web** | Spring MVC, Tomcat 11 |
| **Security** | Spring Security 7 + JWT (`jjwt 0.11.5`) |
| **Persistence** | Spring Data JPA, Hibernate 7 |
| **Database** | MySQL 8 (Aiven in production) |
| **Cache L1** | Caffeine 3.2.4 |
| **Cache L2** | Redis (Upstash / Docker) via Lettuce |
| **PDF Generation** | OpenPDF 1.3.30 |
| **Email** | Spring Boot Starter Mail (SMTP) |
| **Build** | Maven |
| **Frontend** | HTML / CSS / vanilla JavaScript |
| **Local Dev** | Docker Compose (Redis) + IntelliJ |

---

## 🏗 Architecture

```
┌──────────────────────────┐
│        Browser           │
│  (HTML / CSS / JS SPA)   │
└────────────┬─────────────┘
             │ HTTP + JWT cookie
             ▼
┌───────────────────────────────────────────────┐
│              Spring Boot App                  │
│                                               │
│  ┌────────────────────────────────────────┐   │
│  │  JwtAuthFilter (OncePerRequestFilter)  │   │
│  │  → validates cookie                    │   │
│  │  → checks Redis token cache            │   │
│  │  → sets SecurityContext                │   │
│  └────────────────┬───────────────────────┘   │
│                   ▼                           │
│  ┌────────────────────────────────────────┐   │
│  │   SecurityConfig — URL-level rules     │   │
│  │   USER · SECONDARY_ADMIN · ADMIN       │   │
│  └────────────────┬───────────────────────┘   │
│                   ▼                           │
│  ┌────────────────────────────────────────┐   │
│  │             Controllers                │   │
│  │  Auth · Product · Cart · Admin         │   │
│  └────────────────┬───────────────────────┘   │
│                   ▼                           │
│  ┌────────────────────────────────────────┐   │
│  │              Services                  │   │
│  │  @Cacheable / @CachePut / @CacheEvict  │   │
│  │  SecurityService — ownership + quota   │   │
│  └────────────────┬───────────────────────┘   │
│                   ▼                           │
│  ┌────────────────────────────────────────┐   │
│  │        LayeredCacheManager             │   │
│  │   ┌──────────┐        ┌──────────┐     │   │
│  │   │ Caffeine │  ───►  │  Redis   │     │   │
│  │   │   (L1)   │        │   (L2)   │     │   │
│  │   └──────────┘        └──────────┘     │   │
│  └────────────────┬───────────────────────┘   │
│                   ▼                           │
│  ┌────────────────────────────────────────┐   │
│  │        Spring Data JPA / Hibernate     │   │
│  └────────────────┬───────────────────────┘   │
└───────────────────┼───────────────────────────┘
                    ▼
            ┌───────────────┐
            │   MySQL 8     │
            └───────────────┘
```

---

## ⚡ Caching Strategy

JustBuyIt uses a **two-tier cache** to balance latency, consistency, and durability.

| Tier | Tech | Location | Default TTL | Purpose |
|---|---|---|---|---|
| **L1** | Caffeine | In-JVM heap | 2 min | Hot data, microsecond reads |
| **L2** | Redis | Upstash / Docker | 5–30 min | Shared cache, survives restarts |

### Read path
```
getProducts()
      │
      ▼
[Caffeine L1] ── hit ──► return (≈ 100 ns)
      │ miss
      ▼
[Redis L2] ── hit ──► backfill L1 → return (≈ 1–5 ms)
      │ miss
      ▼
[MySQL] ──► populate L2 → populate L1 → return
```

### Cached regions

| Cache | Methods | Redis TTL | Caffeine TTL |
|---|---|---|---|
| `products` | `getProducts`, `getProductById` | 30 min | 2 min |
| `cart` | `getCartByUser`, cart mutations | 10 min | 2 min |
| `user` | user lookups during auth | 5 min | 2 min |

### Invalidation
Every product mutation is annotated with:
```java
@CacheEvict(value = "products", allEntries = true)
```
which clears **both** tiers through the `LayeredCache` wrapper. The product version key (`products:version`) is also bumped so that composite cache keys change on every mutation.

### Redis-only data (not layered)
JWT → username mappings and cached `Users` objects use `RedisCacheService` **directly** via `RedisTemplate`. These **must not** be in Caffeine because they need to be:
- shared across instances,
- instantly invalidated on logout,
- durable across JVM restarts.

---

## 🔐 Security Model

- **Stateless** authentication via JWT
- Token stored in an **HttpOnly cookie** (prevents XSS token theft)
- Token TTL: **1 hour**
- Custom **`JwtAuthFilter`** runs before `UsernamePasswordAuthenticationFilter`
- **BCrypt** password hashing
- **Email verification required** before login
- **Password reset** uses short-lived signed tokens
- Public routes: `/auth/**`, `/LoginForm.html`, `/RegistrationForm.html`, `/ForgotPassword.html`, `/ResetPassword.html`
- `/Admin_Pages/**` and `/admin`, `/admin/users` → `ADMIN` or `SECONDARY_ADMIN`
- `/admin/all`, `/admin/users/*/ban`, `/admin/users/*/unban` → `ADMIN` only
- `/Users_Pages/**` and `/users/**` → `USER` only
- All other routes → authenticated

### Authorization at two levels

1. **URL matching** in `SecurityConfig` — coarse-grained, blocks whole endpoint families.
2. **Service-level checks** in `SecurityService` — fine-grained:
   - `assertCanCreateProduct(actor)` → enforces the 3-product limit for `SECONDARY_ADMIN`
   - `assertCanModifyOrDeleteProduct(existing, actor)` → verifies the caller owns the product (for `SECONDARY_ADMIN`)

Any `IllegalStateException` thrown by these guards is caught by `GlobalExceptionHandler` and returned as a clean `403 Forbidden` JSON payload.

### Auth flow
```
1. POST /auth/v1/register        → user created (unverified)
2. Email sent with verification link
3. GET  /auth/v1/verify-email    → account verified → redirect to login
4. POST /auth/v1/login           → JWT generated, cached in Redis,
                                   set as HttpOnly cookie
5. Any request                   → JwtAuthFilter validates cookie,
                                   checks Redis cache, sets SecurityContext
6. POST /auth/v1/logout          → Redis token entry deleted, cookie cleared
```

---

## 📁 Project Structure

```
JustBuyIt/
│
├── .gitattributes
├── .gitignore
├── JustBuyIt.sql                    # Full schema + seed data
├── docker-compose.yml               # Redis container for local dev
├── mvnw
├── mvnw.cmd
├── pom.xml
│
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
│
└── src/
    ├── main/
    │   ├── java/com/example/JustBuyIt/
    │   │   ├── JustBuyItApplication.java
    │   │   │
    │   │   ├── Configurations/
    │   │   │   ├── AppConfig.java
    │   │   │   ├── GlobalExceptionHandler.java     # JSON error responses
    │   │   │   ├── JwtAuthFilter.java
    │   │   │   ├── LayeredCache.java
    │   │   │   ├── LayeredCacheConfig.java
    │   │   │   ├── LayeredCacheManager.java
    │   │   │   ├── RedisConfig.java
    │   │   │   └── SecurityConfig.java
    │   │   │
    │   │   ├── Controllers/
    │   │   │   ├── AdminController.java
    │   │   │   ├── AuthController.java
    │   │   │   ├── ProductController.java
    │   │   │   └── ShoppingCartController.java
    │   │   │
    │   │   ├── DTOs/
    │   │   │   ├── ApiError.java                   # Uniform error body
    │   │   │   ├── CartItemRequest.java
    │   │   │   ├── CartItemResponse.java
    │   │   │   ├── PaymentRequest.java
    │   │   │   ├── PaymentResponse.java
    │   │   │   ├── ProductDTO.java
    │   │   │   ├── ProductPageDTO.java
    │   │   │   ├── ShoppingCartResponse.java
    │   │   │   └── UserPrincipalDto.java
    │   │   │
    │   │   ├── Models/
    │   │   │   ├── CartItem.java
    │   │   │   ├── Category.java
    │   │   │   ├── Product.java
    │   │   │   ├── QuantityUnit.java
    │   │   │   ├── Role.java                       # USER · SECONDARY_ADMIN · ADMIN
    │   │   │   ├── ShoppingCart.java
    │   │   │   └── Users.java
    │   │   │
    │   │   ├── Repository/
    │   │   │   ├── CategoryRepo.java
    │   │   │   ├── ProductRepo.java                # + countByAddedById
    │   │   │   ├── ShoppingCartRepo.java
    │   │   │   └── UsersRepo.java
    │   │   │
    │   │   └── Services/
    │   │       ├── EmailService.java
    │   │       ├── ImageService.java
    │   │       ├── JwtService.java
    │   │       ├── PaymentService.java
    │   │       ├── ProductService.java
    │   │       ├── ReceiptService.java
    │   │       ├── RedisCacheManagementService.java
    │   │       ├── RedisCacheService.java
    │   │       ├── SecurityService.java            # ownership + quota guards
    │   │       ├── ShoppingCartService.java
    │   │       └── UsersService.java
    │   │
    │   └── resources/
    │       ├── application.properties
    │       │
    │       └── static/
    │           ├── ForgotPassword.html
    │           ├── HomePage.html
    │           ├── LoginForm.html
    │           ├── OneProduct.html
    │           ├── RegistrationForm.html
    │           ├── ResetPassword.html
    │           │
    │           ├── Admin_Pages/
    │           │   ├── AddProduct.html
    │           │   ├── AdminDashboad.html
    │           │   ├── AdminOneProduct.html
    │           │   ├── AdminUsers.html
    │           │   ├── UpdateProduct.html
    │           │   └── ViewAllProduct.html
    │           │
    │           ├── Users_Pages/
    │           │   ├── PaymentCheckout.html
    │           │   └── ShoppingCart.html
    │           │
    │           ├── css/
    │           │   ├── admin/
    │           │   │   ├── admin.css
    │           │   │   ├── components.css
    │           │   │   ├── layout.css
    │           │   │   ├── reset.css
    │           │   │   ├── tokens.css
    │           │   │   ├── utilities.css
    │           │   │   └── pages/
    │           │   │       ├── addUpdateProduct.css
    │           │   │       ├── product.css
    │           │   │       └── viewAll.css
    │           │   │
    │           │   └── users/
    │           │       ├── common.css
    │           │       └── pages/
    │           │           ├── auth.css
    │           │           ├── cart.css
    │           │           ├── checkout.css
    │           │           ├── home.css
    │           │           └── product.css
    │           │
    │           └── js/
    │               └── popup.js
    │
    └── test/java/com/example/JustBuyIt/
        └── JustBuyItApplicationTests.java
```

---

## 🚀 Getting Started (IntelliJ + Docker)

This project runs **without installing MySQL or Redis natively**:
- **MySQL** — hosted (Aiven free tier) or local
- **Redis** — runs in **Docker** via the included `docker-compose.yml`

> **Prerequisites**
> - Java 17
> - IntelliJ IDEA (Community or Ultimate)
> - Docker Desktop
> - A MySQL 8 database (Aiven free tier works great)
> - A Gmail account with an **App Password** (for SMTP)

### 1️⃣ Clone the repository

```bash
git clone https://github.com/Kartik007-Programmer/JustBuyIt.git
cd JustBuyIt
```

### 2️⃣ Start Redis with Docker

```bash
docker compose up -d
docker ps
# Look for: justbuyitapp-redis   (healthy)
```

Redis listens on **localhost:6379**.

### 3️⃣ Import into IntelliJ

1. **File → Open** → select the cloned `JustBuyIt` folder.
2. Wait for Maven to download dependencies.
3. Set the SDK to **Java 17**: **File → Project Structure → Project → SDK → 17**.

### 4️⃣ Configure environment variables

The app reads all secrets from environment variables. Set them either in a `.env` file at the project root (git-ignored) or directly in IntelliJ's run configuration:

| Variable | Purpose |
|---|---|
| `DB_URL` | Full JDBC URL to your MySQL instance |
| `DB_USERNAME` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `EMAIL_ADDRESS` | Gmail address used to send verification / reset emails |
| `EMAIL_APP_PASSWORD` | Gmail **App Password** (not your normal password) |
| `JWT_SECRET` | A long, random string used to sign JWT tokens |

Example:

```properties
DB_URL=jdbc:mysql://<your-mysql-host>:<port>/JustBuyItDb?zeroDateTimeBehavior=convertToNull
DB_USERNAME=<your-db-user>
DB_PASSWORD=<your-db-password>
EMAIL_ADDRESS=<your-gmail-address>
EMAIL_APP_PASSWORD=<gmail-app-password>
JWT_SECRET=<a-long-random-string-at-least-32-chars>
```

**Setting env vars in IntelliJ:**

1. **Run → Edit Configurations…**
2. Select **JustBuyItApplication**
3. Under **Environment variables**, click the folder icon → add each key/value
4. **Apply → OK**

> 💡 Generate a strong `JWT_SECRET` with:
> ```bash
> openssl rand -base64 48
> ```

### 5️⃣ Load the database schema

```bash
mysql -h <host> -u <user> -p JustBuyItDb < JustBuyIt.sql
```

This creates all tables **and** seeds sample products / categories.

### 6️⃣ Run the app in IntelliJ

Open:

```
src/main/java/com/example/JustBuyIt/JustBuyItApplication.java
```

Right-click → **Run 'JustBuyItApplication'**.

The app starts at **http://localhost:8080**.

### 7️⃣ First-time use

1. Open **http://localhost:8080/RegistrationForm.html**
2. Register a user → check the email inbox for the verification link
3. Click the link → redirected to login with `?verified=success`
4. Log in → you're in!

To promote a user to **ADMIN**:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';
```

To create a **SECONDARY_ADMIN**:

```sql
UPDATE users SET role = 'SECONDARY_ADMIN' WHERE email = 'secondary@example.com';
```

Log out and back in to pick up the new role.

### 8️⃣ Quick test with demo accounts

Use the built-in demo credentials to skip registration:

| Role | Email | Password |
|---|---|---|
| **ADMIN** | `Admin@gmail.com` | `K@1234` |
| **SECONDARY_ADMIN** | `Admin2@gmail.com` | `K@1234` |

---

## ⚙ Environment Configuration

`src/main/resources/application.properties` reads all secrets from environment variables and configures the rest with sensible defaults.

```properties
spring.application.name=JustBuyIt

# DataSource Configuration
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.hikari.max-lifetime=300000
spring.datasource.hikari.idle-timeout=240000
spring.datasource.hikari.keepalive-time=120000
spring.datasource.hikari.connection-test-query=SELECT 1

# JPA / Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true

# Redis Configuration
#spring.data.redis.url=${REDIS_URL}
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=10000ms
spring.data.redis.lettuce.shutdown-timeout=200ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2
spring.data.redis.lettuce.pool.time-between-eviction-runs=30s

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
spring.cache.redis.cache-null-values=false
spring.cache.redis.use-key-prefix=true
spring.cache.redis.key-prefix=RedisDbForJustBuyIt:

# Mail (use Gmail app password or your SMTP provider)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_ADDRESS}
spring.mail.password=${EMAIL_APP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# App config
app.base-url=http://localhost:8080
app.verification.token-expiry-minutes=1440
app.password-reset.token-expiry-minutes=30

# Multipart upload limits
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB

spring.docker.compose.enabled=true

jwt.secret=${JWT_SECRET}
```

> ⚠️ **Never commit real credentials.** Use a `.env` file (git-ignored) or IntelliJ run-config env vars.

### Notes on each section

- **DataSource** — Hikari is tuned for cloud MySQL (Aiven) that aggressively reaps idle connections.
- **Redis** — `lettuce.pool.*` properties keep the client resilient to transient network blips. The commented `spring.data.redis.url` line is left in place so you can switch to a managed Redis (Upstash, Redis Cloud) by uncommenting it and setting `REDIS_URL`.
- **Cache** — Redis is the L2 store; Caffeine L1 is configured in Java code via `LayeredCacheConfig`.
- **Mail** — Uses Gmail SMTP with STARTTLS. Requires a Gmail **App Password** (2FA must be enabled on the Google account).
- **Multipart** — `max-file-size=5MB` and `max-request-size=10MB` are the current limits. Increase both if you need larger product images.
- **JWT** — `jwt.secret` must be a long, random string. `JwtService` fails to start if it's missing or too short.

---

## 🗄 Database Setup

1. Create a database named **`JustBuyItDb`** on any MySQL 8 instance (Aiven, Railway, Docker MySQL, etc.).
2. Import `JustBuyIt.sql`:
   ```bash
   mysql -h <host> -u <user> -p JustBuyItDb < JustBuyIt.sql
   ```
3. The schema and seed data will be created automatically.

---

## 🌐 API Reference

### 🔐 Auth — `/auth/v1`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/auth/v1/register` | Public | Register a new user |
| `POST` | `/auth/v1/login` | Public | Login (form-encoded `username`, `password`) |
| `POST` | `/auth/v1/logout` | Auth | Clear cookie + invalidate Redis token |
| `GET`  | `/auth/v1/verify-email?token=` | Public | Verify email; redirects to login |
| `POST` | `/auth/v1/forgot-password?email=` | Public | Send reset link |
| `POST` | `/auth/v1/reset-password?token=&password=` | Public | Reset password |

### 🛍 Products

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/products?page=&size=&sortby=&direction=&category=` | Public | Paged, sortable, filterable list |
| `GET` | `/products/{id}` | Public | Single product (cached) |
| `GET` | `/products/{id}/image` | Public | Product image bytes |
| `GET` | `/products/search?keyword=&page=&size=` | Public | Keyword search |
| `GET` | `/products/categories` | Public | All category names |
| `POST` | `/products` | ADMIN, SECONDARY_ADMIN | Add product (JSON + image URL) |
| `POST` | `/products/upload` | ADMIN, SECONDARY_ADMIN | Add product (multipart) |
| `POST` | `/multi_products` | **ADMIN only** | Batch add products |
| `PUT` | `/products/{id}` | ADMIN, SECONDARY_ADMIN | Update product (JSON) |
| `PUT` | `/products/{id}/upload` | ADMIN, SECONDARY_ADMIN | Update product (multipart) |
| `DELETE` | `/products/{id}` | ADMIN, SECONDARY_ADMIN | Delete product |

> ⚠️ `SECONDARY_ADMIN` calls to `POST`/`PUT`/`DELETE /products/**` are further validated in `ProductService` — the caller must own the product (for update/delete) and must not exceed 3 products (for create).

### 🛒 Cart — `/users/cart`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/users/cart` | USER | Get current user's cart |
| `POST` | `/users/cart/items` | USER | Add item |
| `DELETE` | `/users/cart/items/{cartItemId}` | USER | Remove item |
| `DELETE` | `/users/cart` | USER | Clear cart |
| `POST` | `/users/cart/payment/checkout` | USER | Fake payment |
| `POST` | `/users/cart/payment/checkout/receipt` | USER | Download PDF receipt |

### 🛠 Admin — `/admin`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/admin` | ADMIN, SECONDARY_ADMIN | Current admin details |
| `GET` | `/admin/all` | **ADMIN only** | List all admins |
| `GET` | `/admin/users?q=&page=&size=` | ADMIN, SECONDARY_ADMIN | Paged + searchable user list |
| `PATCH` | `/admin/users/{id}/ban` | **ADMIN only** | Ban user |
| `PATCH` | `/admin/users/{id}/unban` | **ADMIN only** | Unban user |

### ❗ Error responses

Every error response uses a single JSON shape:

```json
{
  "error": "FORBIDDEN",
  "message": "Secondary admins can only add up to 3 products."
}
```

| HTTP Status | `error` | When |
|---|---|---|
| `400` | `BAD_REQUEST` | Invalid input |
| `401` | `UNAUTHORIZED` | Not logged in / expired token |
| `403` | `FORBIDDEN` | Insufficient role, ownership, or quota |
| `404` | `NOT_FOUND` | Missing resource |
| `413` | `PAYLOAD_TOO_LARGE` | Upload exceeds the configured multipart limit |
| `500` | `SERVER_ERROR` | Unexpected server error (message sanitized) |

---

## 📸 Screenshots

> _Screenshots coming soon._

| Page | Preview |
|---|---|
| Login / Register | `docs/screenshots/login.png` |
| Home / Product Grid | `docs/screenshots/home.png` |
| Single Product | `docs/screenshots/product.png` |
| Shopping Cart | `docs/screenshots/cart.png` |
| Checkout & Receipt | `docs/screenshots/receipt.png` |
| Admin Dashboard | `docs/screenshots/admin-dashboard.png` |
| Admin User Management | `docs/screenshots/admin-users.png` |
| Secondary Admin — 3-product limit | `docs/screenshots/secondary-admin-limit.png` |

---

## 🧠 Lessons Learned

Building JustBuyIt was a deep dive into production-style Spring Boot patterns:

1. **Layered caching is subtle.** Wrapping Caffeine + Redis behind Spring's `CacheManager` requires careful handling of `ValueWrapper`. Returning the wrapper instead of the unwrapped value causes cryptic `ClassCastException`s on every `@Cacheable` call.

2. **Never put `CascadeType.ALL` on a `@ManyToOne`.** We learned this the hard way — `Product.category` had `cascade = CascadeType.ALL`, so deleting one product cascaded a `REMOVE` to its `Category`, and via `orphanRemoval` on the inverse side, took every sibling product in that category down with it. Cascades on `@ManyToOne` (child → shared parent) are almost always wrong.

3. **Two-layer authorization is essential.** URL matchers in `SecurityConfig` can't express "you may only edit products you added" or "you may only add 3 products". Service-level assertions (`assertCanCreateProduct`, `assertCanModifyOrDeleteProduct`) enforce the business rules that URL rules can't.

4. **Free-tier managed Redis is fragile.** Redis Cloud's free tier aggressively closes idle connections, causing repeated `ConnectionWatchdog` reconnects. **Upstash** was more resilient thanks to its serverless-friendly protocol handling.

5. **JWT + Redis is a strong pairing.** Caching `token → username` in Redis lets every authenticated request skip a DB lookup while still allowing instant revocation on logout.

6. **Environment variables are non-negotiable.** Moving all credentials (`DB_*`, `EMAIL_*`, `JWT_SECRET`, `REDIS_*`) out of `application.properties` means the repo is safe to publish and easy to configure per environment.

7. **A single `GlobalExceptionHandler` beats scattered try/catch.** Wrapping every controller method in `try { ... } catch (Exception e) { return 500 }` hides the true cause of errors. Letting exceptions bubble to a `@RestControllerAdvice` produces consistent status codes, consistent JSON shapes, and clean frontend messages.

8. **HikariCP + Lettuce tuning matters.** Setting `keepalive-time`, `max-lifetime`, and pool `min-idle` prevents connection starvation against cloud databases that aggressively reap idle sockets.

9. **Spring Boot's `spring-boot-docker-compose` integration is a game-changer.** It auto-detects `docker-compose.yml`, starts Redis on app boot, and injects connection details — no native Redis install required for contributors.

---

## 👨‍💻 Author

**Kartik Irabatti**

- GitHub: [@Kartik007-Programmer](https://github.com/Kartik007-Programmer)
- Project: [JustBuyIt](https://github.com/Kartik007-Programmer/JustBuyIt)

---

## 🙏 Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Caffeine](https://github.com/ben-manes/caffeine)
- [Redis](https://redis.io/) / [Upstash](https://upstash.com/)
- [Aiven](https://aiven.io/) for hosted MySQL
- [jjwt](https://github.com/jwtk/jjwt)
- [OpenPDF](https://github.com/LibrePDF/OpenPDF)

---

_This project is a personal learning effort and is not affiliated with any commercial entity._
