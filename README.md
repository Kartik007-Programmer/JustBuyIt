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
- **Role-based authorization** (`USER` / `ADMIN`)
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
- Update / delete products
- **User management**: paged + searchable user list, ban / unban users
- View all admins

### ⚡ Engineering
- **Two-tier caching** — Caffeine (L1) → Redis (L2) → MySQL
- **Automatic cache invalidation** on product mutations via `@CacheEvict`
- **JWT token + user caching** in Redis for fast auth
- **Lettuce connection pool** tuned for resilience
- **HikariCP** tuned for cloud MySQL (Aiven)
- Environment-variable driven configuration — no secrets in source

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
│  │             Controllers                │   │
│  │  Auth · Product · Cart · Admin         │   │
│  └────────────────┬───────────────────────┘   │
│                   ▼                           │
│  ┌────────────────────────────────────────┐   │
│  │              Services                  │   │
│  │  @Cacheable / @CachePut / @CacheEvict  │   │
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
which clears **both** tiers through the `LayeredCache` wrapper.

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
- Admin routes require `ADMIN` authority
- User routes require `USER` or `ADMIN`

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
    │   │   │   ├── CartItemRequest.java
    │   │   │   ├── CartItemResponse.java
    │   │   │   ├── PaymentRequest.java
    │   │   │   ├── PaymentResponse.java
    │   │   │   ├── ProductDTO.java
    │   │   │   ├── ProductPageDTO.java
    │   │   │   └── ShoppingCartResponse.java
    │   │   │
    │   │   ├── Models/
    │   │   │   ├── CartItem.java
    │   │   │   ├── Category.java
    │   │   │   ├── Product.java
    │   │   │   ├── QuantityUnit.java
    │   │   │   ├── Role.java
    │   │   │   ├── ShoppingCart.java
    │   │   │   └── Users.java
    │   │   │
    │   │   ├── Repository/
    │   │   │   ├── CategoryRepo.java
    │   │   │   ├── ProductRepo.java
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
    │   │       ├── SecurityService.java
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
    │           ├── admin/
    │           │   ├── AddProduct.html
    │           │   ├── AdminDashboad.html
    │           │   ├── AdminOneProduct.html
    │           │   ├── AdminUsers.html
    │           │   ├── UpdateProduct.html
    │           │   └── ViewAllProduct.html
    │           │
    │           ├── users/
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

The repo ships with a `docker-compose.yml` that starts a Redis container:

```bash
docker compose up -d
```

Verify it's running:

```bash
docker ps
# Look for: justbuyitapp-redis   (healthy)
```

Redis listens on **localhost:6379**.

> 💡 `application.properties` already has `spring.data.redis.host=localhost` and `port=6379`, so no extra config is needed for local Docker Redis.

### 3️⃣ Import into IntelliJ

1. **File → Open** → select the cloned `JustBuyIt` folder.
2. Wait for Maven to download dependencies (first run takes a few minutes).
3. Set the SDK to **Java 17**: **File → Project Structure → Project → SDK → 17**.

### 4️⃣ Configure environment variables

Create a `.env` file at the project root (git-ignored) — or set them directly in IntelliJ's run configuration:

```properties
DB_URL=jdbc:mysql://<your-mysql-host>:<port>/JustBuyItDb?zeroDateTimeBehavior=convertToNull
DB_USERNAME=<your-db-user>
DB_PASSWORD=<your-db-password>

EMAIL_ADDRESS=<your-gmail-address>
EMAIL_APP_PASSWORD=<gmail-app-password>
```

**Setting env vars in IntelliJ:**

1. **Run → Edit Configurations…**
2. Select **JustBuyItApplication**
3. Under **Environment variables**, click the folder icon → add each key/value
4. **Apply → OK**

### 5️⃣ Load the database schema

Import the provided `JustBuyIt.sql` into your MySQL instance:

```bash
mysql -h <host> -u <user> -p JustBuyItDb < JustBuyIt.sql
```

This creates all tables **and** seeds sample products / categories.

If you prefer, let Hibernate auto-generate the schema (`ddl-auto=update`) on first run, then import only the **INSERT** statements.

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

To promote a user to **ADMIN**, update the `role` column directly in the `users` table:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';
```

Log out and back in to pick up the new role.

---

## ⚙ Environment Configuration

`src/main/resources/application.properties` reads everything from environment variables:

```properties
spring.application.name=JustBuyIt

# DataSource
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.hikari.max-lifetime=300000
spring.datasource.hikari.idle-timeout=240000
spring.datasource.hikari.keepalive-time=120000
spring.datasource.hikari.connection-test-query=SELECT 1

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=10000ms
spring.data.redis.lettuce.shutdown-timeout=200ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2
spring.data.redis.lettuce.pool.time-between-eviction-runs=30s

# Cache
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
spring.cache.redis.cache-null-values=false
spring.cache.redis.use-key-prefix=true
spring.cache.redis.key-prefix=RedisDbForJustBuyIt:

# Mail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_ADDRESS}
spring.mail.password=${EMAIL_APP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# App
app.base-url=http://localhost:8080
app.verification.token-expiry-minutes=1440
app.password-reset.token-expiry-minutes=30

spring.docker.compose.enabled=true
```

> ⚠️ **Never commit real credentials.** Use a `.env` file (git-ignored) or IntelliJ run-config env vars.

---

## 🗄 Database Setup

1. Create a database named **`JustBuyItDb`** on any MySQL 8 instance (Aiven, Railway, Docker MySQL, etc.).
2. Import `JustBuyIt.sql` from the repo root:
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
| `GET` | `/products?page=&size=&sortby=&direction=&category=` | USER | Paged, sortable, filterable list |
| `GET` | `/products/{id}` | USER | Single product (cached) |
| `GET` | `/products/{id}/image` | USER | Product image bytes |
| `GET` | `/products/search?keyword=&page=&size=` | USER | Keyword search |
| `GET` | `/products/categories` | USER | All category names |
| `POST` | `/products` | ADMIN | Add product (JSON + image URL) |
| `POST` | `/products/upload` | ADMIN | Add product (multipart) |
| `POST` | `/multi_products` | ADMIN | Batch add products |
| `PUT` | `/products/{id}` | ADMIN | Update product (JSON) |
| `PUT` | `/products/{id}/upload` | ADMIN | Update product (multipart) |
| `DELETE` | `/products/{id}` | ADMIN | Delete product |

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
| `GET` | `/admin` | ADMIN | Current admin details |
| `GET` | `/admin/all` | ADMIN | List all admins |
| `GET` | `/admin/users?q=&page=&size=` | ADMIN | Paged + searchable user list |
| `PATCH` | `/admin/users/{id}/ban` | ADMIN | Ban user |
| `PATCH` | `/admin/users/{id}/unban` | ADMIN | Unban user |

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

---

## 🧠 Lessons Learned

Building JustBuyIt was a deep dive into production-style Spring Boot patterns:

1. **Layered caching is subtle.** Wrapping Caffeine + Redis behind Spring's `CacheManager` requires careful handling of `ValueWrapper`. Returning the wrapper instead of the unwrapped value causes cryptic `ClassCastException`s on every `@Cacheable` call.

2. **Free-tier managed Redis is fragile.** Redis Cloud's free tier aggressively closes idle connections, causing repeated `ConnectionWatchdog` reconnects. **Upstash** was more resilient thanks to its serverless-friendly protocol handling.

3. **JWT + Redis is a strong pairing.** Caching `token → username` in Redis lets every authenticated request skip a DB lookup while still allowing instant revocation on logout.

4. **Environment variables are non-negotiable.** Moving all credentials (`DB_*`, `EMAIL_*`, `REDIS_*`) out of `application.properties` means the repo is safe to publish and easy to configure per environment.

5. **HikariCP + Lettuce tuning matters.** Setting `keepalive-time`, `max-lifetime`, and pool `min-idle` prevents connection starvation against cloud databases that aggressively reap idle sockets.

6. **Spring Boot's `spring-boot-docker-compose` integration is a game-changer.** It auto-detects `docker-compose.yml`, starts Redis on app boot, and injects connection details — no native Redis install required for contributors.

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
