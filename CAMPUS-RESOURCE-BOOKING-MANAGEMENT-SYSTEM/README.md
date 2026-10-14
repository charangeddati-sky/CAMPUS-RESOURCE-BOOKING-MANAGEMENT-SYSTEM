# CampusOS — College Resource Booking Portal

Digital campus twin + booking + Groq AI assistant for **Vasireddy Venkatadri Institute of Technology (VVIT)**.

Stack: **React (JavaScript) + Vite + Tailwind + Spring Boot + JWT + JPA + MySQL + Groq**.

The aerial campus photo and architecture diagram drive an interactive campus map (Loyalty 1–4, Wisdom, Honesty/Siemens, Truth, Ground).

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- MySQL 8 (`root` / `root` by default) with database `college_resource_booking`

## Configure

Copy `backend/.env.example` to `backend/.env` and set:

```
GROQ_API_KEY=your_key_here
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=change-me
DEMO_PASSWORD=Password@123
```

Never expose `GROQ_API_KEY` to the browser. All AI calls go React → Spring Boot → Groq.

## Run

From `backend/`:

```
mvn spring-boot:run
```

From `frontend/`:

```
npm install
npm run dev
```

Open http://localhost:5173

Windows helper (repo root):

```
.\start-dev.ps1
```

## Demo accounts

Password for all: `Password@123`

| Role | Email |
| --- | --- |
| Student | student@example.com |
| Professor | professor@example.com |
| Admin | admin@example.com |

## Demo story

1. Sign in as student → Explore Campus → Block A / Loyalty 1 → floor map → room → book.
2. Sign in as professor → Approvals → approve.
3. Sign in as admin → Approvals → confirm.
4. Student opens the booking and checks in from the page (no QR).
5. AI Booking: *“I need a seminar hall for 100 people tomorrow from 10 AM to 1 PM with a projector.”*

## API

Backend: http://localhost:8080  
Health: `GET /api/health`
