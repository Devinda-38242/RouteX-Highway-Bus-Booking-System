#  RouteX – Highway Bus Booking System

A production-quality full-stack web application for booking highway bus tickets in Sri Lanka.  
**Built with Spring Boot 3.5 + Java 21 + MySQL + HTML/CSS/JS** | NSBM Green University Group Project 2026

---

## 📋 Table of Contents

- [Project Overview](#project-overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [ER Diagram](#er-diagram)
- [Project Structure](#project-structure)
- [Setup & Run](#setup--run)
- [Default Credentials](#default-credentials)
- [API Documentation](#api-documentation)
- [Member Task Division](#member-task-division)

---

## 📖 Project Overview

RouteX solves the real-world problem of inefficient highway bus ticket booking in Sri Lanka.  
Currently most passengers queue at bus stands or call operators directly. RouteX provides:

- 🔍 Online route search by origin, destination, and date
- 💺 Real-time seat selection and booking
- 📡 Live seat availability updates via WebSocket (no page refresh needed)
- 📧 Email OTP verification for accounts and bookings (via Gmail SMTP)
- 🎫 QR-code boarding passes generated as PDF e-tickets
- 🤖 Smart chatbot (RouteBot) for passenger route queries
- 🛠️ Full admin dashboard for route, booking, and user management

**Focus Route:** Makumbura ↔ Katharagama (Bus Bay B-6) with real timetable data,  
expanded to **16+ routes** island-wide (Colombo → Kandy, Galle, Jaffna, Matara, Anuradhapura, Trincomalee, Badulla, Batticaloa and more).

---

## ✅ Features

### Core Requirements Met

| Requirement | Implementation |
|---|---|
| User Auth (Signup / Login / Logout) | JWT + HttpOnly Cookie |
| Role-based Authorization | USER / ADMIN roles via Spring Security |
| Core Entities with Relationships | User → Booking → Schedule → BusRoute |
| Full CRUD for Main Entity | Bus Routes (Admin CRUD) |
| Pagination & Sorting | All list endpoints paginated + sorted |
| Bean Validation | `@Valid` on all DTOs + friendly UI error messages |
| Centralized Error Handling | `GlobalExceptionHandler` → proper HTTP codes |
| Sessions & Cookies | HttpOnly JWT cookie (`routex_token`) |
| CSRF Protection | Disabled for `/api/**` (stateless JWT REST API) |
| JPA + Seed Data | All entities JPA mapped, `DataSeeder` runs on startup |

### 🚀 Beyond-CRUD Features

| Feature | Details |
|---|---|
| Real-time Seat Updates | WebSocket/STOMP broadcasts seat changes to all connected clients instantly |
| Email Notifications | OTP verification + HTML booking confirmation via Gmail SMTP |
| QR Code Boarding Pass | ZXing generates 300×300 QR codes, stored as Base64, embedded in emails |
| PDF E-Ticket | iText generates downloadable PDF tickets with embedded QR code |
| Caching | `@Cacheable` on `getLocations()` to avoid repeated DB queries |
| Soft Deletes | Bookings & Users have `deleted` flag — data preserved for audit trail |
| Advanced Route Search | Paginated search with origin/destination filter and sort by departure time |
| AI Chatbot (RouteBot) | Pattern-matched responses for common passenger queries using live DB data |
| Swagger / OpenAPI Docs | Auto-generated at `/swagger-ui.html` |
| Demo Accounts | Pre-seeded admin and user accounts for instant demo |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5, Java 21 |
| Security | Spring Security 6, JWT (jjwt 0.11.5), HttpOnly Cookies |
| Database | MySQL 8 + Spring Data JPA / Hibernate |
| Email | Gmail SMTP via Spring Mail (JavaMailSender) |
| QR Code | ZXing 3.5.2 |
| PDF Tickets | iText PDF 5.5.13.3 |
| WebSocket | STOMP over SockJS |
| Caching | Spring Cache (Simple in-memory) |
| API Docs | SpringDoc OpenAPI 2.3.0 |
| Frontend | HTML5, CSS3, Vanilla JavaScript |
| Build Tool | Maven 3.8+ |

---

## 🗄️ ER Diagram

```
┌─────────────────────┐         ┌──────────────────────────┐
│         USER        │         │        BUS_ROUTE          │
├─────────────────────┤         ├──────────────────────────┤
│ id (PK)             │         │ id (PK)                   │
│ fullName            │         │ origin                    │
│ email (UNIQUE)      │         │ destination               │
│ password (BCrypt)   │         │ departureTime             │
│ phone               │         │ arrivalTime               │
│ role (USER/ADMIN)   │         │ busType (ENUM)            │
│ enabled             │         │ operatorName              │
│ otp                 │         │ contactNumber             │
│ otpExpiry           │         │ busBay                    │
│ deleted             │         │ price                     │
└────────┬────────────┘         │ totalSeats                │
         │ 1                    │ active                    │
         │                      └──────────┬───────────────┘
         │ M                               │ 1
         │                                 │ M
┌────────▼────────────┐         ┌──────────▼───────────────┐
│       BOOKING       │         │         SCHEDULE          │
├─────────────────────┤         ├──────────────────────────┤
│ id (PK)             │  M   1  │ id (PK)                   │
│ bookingReference    ├────────►│ busRoute_id (FK)          │
│ user_id (FK)        │         │ travelDate                │
│ schedule_id (FK)    │         │ bookedSeats (CSV)         │
│ selectedSeats (CSV) │         │ availableSeats            │
│ passengerName       │         │ status (OPEN/FULL/CANCELLED)|
│ passengerEmail      │         └──────────────────────────┘
│ passengerPhone      │
│ totalAmount         │
│ bookingStatus (ENUM)│
│ paymentStatus (ENUM)│
│ qrCode (LONGTEXT)   │
│ deleted             │
└─────────────────────┘
```

**Relationships:**
- `User` → `Booking` : One-to-Many
- `BusRoute` → `Schedule` : One-to-Many
- `Schedule` → `Booking` : One-to-Many

---

## 📁 Project Structure

```
RouteX_Full_Project/
└── backend/
    ├── pom.xml
    └── src/main/
        ├── java/com/routex/
        │   ├── RouteXApplication.java
        │   ├── entity/
        │   │   ├── User.java
        │   │   ├── BusRoute.java
        │   │   ├── Schedule.java
        │   │   └── Booking.java
        │   ├── repository/
        │   │   ├── UserRepository.java
        │   │   ├── BusRouteRepository.java
        │   │   ├── ScheduleRepository.java
        │   │   └── BookingRepository.java
        │   ├── service/
        │   │   ├── AuthService.java
        │   │   ├── BusRouteService.java
        │   │   ├── BookingService.java
        │   │   └── EmailService.java
        │   ├── controller/
        │   │   ├── AuthController.java
        │   │   ├── BusRouteController.java
        │   │   ├── BookingController.java
        │   │   └── AdminController.java
        │   ├── security/
        │   │   ├── JwtUtil.java
        │   │   ├── JwtAuthFilter.java
        │   │   └── UserDetailsServiceImpl.java
        │   ├── config/
        │   │   ├── SecurityConfig.java
        │   │   ├── WebSocketConfig.java
        │   │   ├── GlobalExceptionHandler.java
        │   │   └── DataSeeder.java
        │   ├── dto/
        │   │   ├── AuthDTOs.java
        │   │   ├── BookingDTOs.java
        │   │   ├── UserAdminResponse.java
        │   │   └── ApiResponse.java
        │   ├── qr/
        │   │   └── QRCodeService.java
        │   └── bot/
        │       ├── ChatBotService.java
        │       └── ChatBotController.java
        └── resources/
            ├── application.properties
            └── static/
                ├── index.html
                ├── login.html
                ├── register.html
                ├── results.html
                ├── booking.html
                ├── my-bookings.html
                ├── admin.html
                ├── Payment.html
                ├── Ticket.html
                ├── css/
                │   ├── home.css
                │   ├── auth.css
                │   ├── results.css
                │   ├── booking.css
                │   ├── PaymentStyles.css
                │   └── TicketStyles.css
                ├── js/
                │   ├── home.js
                │   ├── booking.js
                │   ├── Payment.js
                │   └── Ticket.js
                └── images/
                    ├── Logo.png
                    ├── hero-image.png
                    └── ...
```

---

## ⚙️ Setup & Run

### Prerequisites
- Java 21+
- Maven 3.8+
- MySQL 8+ (running on port **3307** — or change in `application.properties`)

### Step 1 — Clone the Repository
```bash
git clone https://github.com/hasiniumandi0405-cell/RouteX
cd routex/backend
```

### Step 2 — Database Setup
The app auto-creates the database on first run via:
```
spring.datasource.url=jdbc:mysql://localhost:3307/routex_db?createDatabaseIfNotExist=true
```
Just make sure MySQL is running. Or create manually:
```sql
CREATE DATABASE routex_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 3 — Configure `application.properties`
Edit `src/main/resources/application.properties`:
```properties
# MySQL password
spring.datasource.password=

# Brevo API key (for OTP + booking confirmation emails)
brevo.api.key=<YOUR_BREVO_API_KEY>
```

### Step 4 — Build & Run
```bash
mvn clean install
mvn spring-boot:run
```
The app auto-creates all tables and seeds demo data on first run.

### Step 5 — Access the App

| URL | Description |
|---|---|
| `http://localhost:8080` | Home Page |
| `http://localhost:8080/login.html` | Login |
| `http://localhost:8080/register.html` | Register |
| `http://localhost:8080/admin.html` | Admin Dashboard |
| `http://localhost:8080/my-bookings.html` | My Bookings |
| `http://localhost:8080/swagger-ui.html` | API Documentation (Swagger) |

---

## 🔑 Default Credentials

| Role | Email | Password |
|---|---|---|
| Admin | `admin@routex.lk` | `Admin@2026` |
| Demo User | `user@routex.lk` | `User@2026` |

> **Demo OTP:** Both demo accounts accept OTP `123456` for quick demo purposes.

---

## 📡 API Documentation

Full interactive Swagger UI available at: `http://localhost:8080/swagger-ui.html`

### Auth Endpoints
```
POST /api/auth/register       → Register new user (sends OTP email)
POST /api/auth/verify-otp     → Verify OTP and activate account
POST /api/auth/resend-otp     → Resend OTP to email
POST /api/auth/login          → Login (returns JWT + sets HttpOnly cookie)
POST /api/auth/logout         → Clear auth cookie
```

### Routes (Public)
```
GET  /api/routes/locations                          → All origins & destinations
GET  /api/routes/search?origin=&destination=&date=  → Search routes (paginated)
GET  /api/routes/{routeId}/schedule?date=           → Get or create schedule for date
GET  /api/routes/schedules/{id}/seats               → Real-time seat availability
```

### Bookings (Authenticated)
```
POST /api/bookings                  → Create booking
GET  /api/bookings/my               → My bookings (paginated)
GET  /api/bookings/{ref}            → Get booking by reference
PUT  /api/bookings/{ref}/cancel     → Cancel booking
POST /api/bookings/send-otp         → Send booking verification OTP
POST /api/bookings/verify-otp       → Verify booking OTP
```

### Admin (ADMIN role only)
```
GET    /api/admin/routes            → All routes (paginated)
POST   /api/admin/routes            → Create new route
PUT    /api/admin/routes/{id}       → Update route
DELETE /api/admin/routes/{id}       → Deactivate route

GET    /api/admin/bookings          → All bookings (paginated)

GET    /api/admin/users             → All users (paginated)
PUT    /api/admin/users/{id}        → Update user details
PUT    /api/admin/users/{id}/role   → Change user role
PUT    /api/admin/users/{id}/status → Enable / disable user
DELETE /api/admin/users/{id}        → Soft delete user
```

### Chatbot
```
POST /api/bot/message   → { "message": "hello" } → RouteBot response
```

---

## 👥 Member Task Division

| # | Member           | Module | Key Files |
|---|------------------|---|---|
| 1 | Member 1- 38239  | Frontend — Home Page & Route Search UI | `index.html`, `home.css`, `home.js` |
| 2 | Member 2- 38242  | Frontend — Booking UI, Seat Map & Auth Pages | `booking.html`, `booking.css`, `booking.js`, `login.html`, `register.html`, `auth.css` |
| 3 | Member 3- 38050  | Database Design & Booking OTP System | `User.java`, `BusRoute.java`, `Schedule.java`, `Booking.java`, OTP logic in `BookingController.java` |
| 4 | Member 4- 37915  | JWT Token System | `JwtUtil.java`, `JwtAuthFilter.java`, `UserDetailsServiceImpl.java` |
| 5 | Member 5 - 38756 | Spring Security & Auth Flow | `SecurityConfig.java`, `AuthController.java`, `AuthService.java` |
| 6 | Member 6 - 37926 | Bus Route Service & Schedule Logic | `BusRouteService.java`, `BusRouteController.java`, `BusRouteRepository.java`, `ScheduleRepository.java` |
| 7 | Member 7 - 38537 | Booking Service & Real-Time WebSocket | `BookingService.java`, `BookingRepository.java`, `WebSocketConfig.java` |
| 8 | Member 8 - 38025 | Email Service & Brevo API Integration | `EmailService.java` |
| 9 | Member 9 - 38730 | QR Code Generation & Data Seeder | `QRCodeService.java`, `DataSeeder.java` |
| 10 | Member 10 -38451 | Admin Dashboard Backend & Chatbot | `AdminController.java`, `ChatBotService.java`, `ChatBotController.java` |

---

## 📄 License

MIT License — Built for educational purposes.  
Group Project, NSBM Green University, 2026.