# Job Application Tracker

A Spring Boot REST API and drag and drop browser board for job seekers who want to track applications through a pipeline with enforced stage rules, notes, follow ups and simple analytics.

Portfolio: https://cv.omnitest.dev/

I built this because job hunting got messy fast. Once applications start moving through assessments, interviews and waiting rooms, a notes app is not enough. I wanted one place I could open, edit, and actually trust.

This is my Spring Boot workspace for that: a REST API plus a browser dashboard where I can drag applications between stages, leave notes, and see how the search is going.

The files are meant to stay open. If I want a stricter pipeline or a different board, I change the Java or the static page, there is no hidden frontend build.

## About

I'm Tehilla. I used the tools I already knew and wanted to get better at:

- **Java 21** and **Spring Boot** for the API
- **Spring Web**, **Spring Data JPA** and **Jakarta Validation** for HTTP, persistence and request checks
- **H2** so the app runs locally without installing a database
- **Maven** to build and test
- **JUnit** and **MockMvc** for the tests
- **springdoc-openapi** for Swagger
- plain **HTML, CSS and JavaScript** for the dashboard, so I could restyle it without npm
- **GitHub Actions** to run the tests on every push
- **Docker** when I want a container instead of a local Maven run

If you clone it, you should be able to run it, click around, and start editing the same way I do.

## What you get

- Create, edit, move and delete applications from the dashboard or the API
- Pipeline rules that block jumps such as `SAVED → OFFER`
- Timeline events for creates, status moves and notes
- Search, filters, pagination and CSV export
- Analytics: conversion funnel, interview rate, follow-ups and source mix
- OpenAPI / Swagger UI
- Seeded sample data so the board is not empty on first run
- GitHub Actions CI

## Tech stack

- Java 21
- Spring Boot 4.1.1
- Spring Web, Data JPA, Validation, Actuator
- springdoc-openapi
- H2
- Maven
- JUnit 5 and MockMvc
- Vanilla HTML / CSS / JS dashboard

## Run

```bash
mvn spring-boot:run
```

Then open:

| Surface | URL |
|---|---|
| Dashboard | http://localhost:8080 |
| Applications API | http://localhost:8080/api/applications |
| Analytics | http://localhost:8080/api/applications/stats |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 console | http://localhost:8080/h2-console |

H2 JDBC URL: `jdbc:h2:mem:jobtracker`

```bash
mvn test
```

```bash
docker build -t job-tracker-api .
docker run -p 8080:8080 job-tracker-api
```

## Real output

From a local run on 1 November 2026 (Temurin 21, Maven 3.9), using the seeded sample board. The sample companies are made up for the demo data.

```text
$ curl -s localhost:8080/api/applications/stats
{"total":8,"active":6,"offers":1,"accepted":0,"rejected":1,"withdrawn":1,
 "interviewRatePercent":25.0,"offerRatePercent":12.5,"averageDaysOpen":15.0,
 "funnel":[{"stage":"SAVED","count":8},{"stage":"APPLIED","count":7},
           {"stage":"ASSESSMENT","count":3},{"stage":"INTERVIEW","count":2},
           {"stage":"OFFER","count":1},{"stage":"ACCEPTED","count":0}], ...}

$ curl -s -X PATCH -H 'Content-Type: application/json' \
    -d '{"status":"OFFER"}' localhost:8080/api/applications/3/status
{"status":409,"error":"Invalid pipeline transition",
 "message":"Cannot move an application from SAVED to OFFER",
 "from":"SAVED","to":"OFFER","allowed":["WITHDRAWN","APPLIED"], ...}
```

Output is trimmed and wrapped for reading. The seed dates are relative to the day you start the app, so your numbers for days open and follow ups will differ.

## What was verified

| Check | Result |
| --- | --- |
| `mvn test` | 9 tests pass (3 test classes) |
| `mvn package` then `java -jar` | starts, dashboard returns 200 |
| Stats, paged list and an illegal status move | ran as shown above |
| GitHub Actions CI | green on the latest push |

Not checked by hand: the drag and drop board in a browser, CSV export, Swagger UI, the H2 console and the Docker image.

## Dashboard

The home page is a live workspace, not a placeholder.

- **Board**: drag a card to another stage. Illegal moves come back as `409` and the UI explains what is allowed
- **Table**: sortable-looking list with filters and click-to-edit
- **Insights**: funnel, sources, companies and follow-ups
- **Drawer**: full create / edit form, delete, and a timeline you can append notes to

Edit `src/main/resources/static/` if you want to change layout, copy or behaviour. No frontend build step.

## Example request

```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "company": "Northstar Digital",
    "role": "Java Developer",
    "status": "APPLIED",
    "appliedDate": "2026-09-12",
    "location": "London",
    "salaryMin": 42000,
    "salaryMax": 52000,
    "source": "LINKEDIN",
    "priority": "HIGH",
    "nextFollowUp": "2026-09-19",
    "tags": ["java", "spring"],
    "notes": "Applied after a referral."
  }'
```

## API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/applications` | Search and page. Query: `q`, `status`, `company`, `source`, `priority`, `followUpDue`, `page`, `size`, `sort` |
| GET | `/api/applications/{id}` | One record plus timeline and allowed next stages |
| POST | `/api/applications` | Create |
| PUT | `/api/applications/{id}` | Replace |
| PATCH | `/api/applications/{id}/status` | Move stage |
| POST | `/api/applications/{id}/notes` | Append a timeline note |
| DELETE | `/api/applications/{id}` | Delete |
| GET | `/api/applications/stats` | Pipeline analytics |
| GET | `/api/applications/export.csv` | CSV export |

Errors use a consistent body: timestamp, status, message, path, and field or transition details when they apply.

## Architecture

```text
dashboard (static)
    → controller
        → service
            → pipeline rules
            → repository
                → H2
```

Controllers stay on HTTP. `JobApplicationService` owns salary checks, tagging, events and analytics. `PipelineRules` is the only place that decides which status moves are legal.

```text
src/main/java/com/victrom/jobtracker/
  controller/     HTTP
  service/        business rules
  pipeline/       status transitions
  dto/            request and response records
  model/          JPA entities and enums
  repository/     Spring Data
  exception/      API errors
  config/         OpenAPI, CORS, seed data
```

## Open edits and improvements

Useful first changes:

1. **Relax or tighten the pipeline**: edit the map in `pipeline/PipelineRules.java`, then add a test in `PipelineRulesTest`.
2. **Add a field**: extend `JobApplication`, `CreateApplicationRequest`, `ApplicationResponse`, `ApplicationMapper` and the drawer form in `static/index.html` / `static/app.js`.
3. **Change the sample board**: edit `config/SampleDataConfig.java`.
4. **Restyle the workspace**: `static/styles.css` is the whole look.
5. **Swap H2 for Postgres**: change the datasource and add the driver. The rest of the code can stay.

Larger follow-ons that fit the current shape: JWT auth, reminders, file attachments, a React client against the same API, or PostgreSQL in Docker Compose.

## Pipeline rules

Happy path: `SAVED → APPLIED → ASSESSMENT → INTERVIEW → OFFER → ACCEPTED`.

Rejections and withdrawals can happen from most active stages. Closed applications can be reopened to `SAVED` or `APPLIED`, except `ACCEPTED`, which is terminal.

## Limitations and status

A personal tool, version 0.1, no known users besides me.

- H2 runs in memory, so every restart wipes your data and reloads the sample board. Point the datasource at a file or Postgres before relying on it.
- There is no authentication. Anyone who can reach the port can read and change everything, and the H2 console is enabled. Keep it on localhost.
- Swagger UI and the API docs are on by default.

## Licence

MIT. See `LICENSE`.
