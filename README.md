# Community Giving — St. Thomas Church CNI, Ghugus

A donation, membership, election, and church-administration system for St. Thomas Church CNI, Ghugus. Two Angular front ends (member and admin portals) share one codebase and talk to a single Spring Boot backend.

## Layout

- `BE/` — Spring Boot 4.1.1 REST API (Java 21, Maven). Spring Security (session + CSRF), Spring Data JPA, PostgreSQL in Docker (file-backed H2 for local `mvn spring-boot:run`).
- `BEUI/` — admin Angular entry point (port 4200 in Docker).
- `FEUI/` — member Angular entry point (port 4201 in Docker).
- `shared/` — the actual application: `app.ts` (component logic) and `app.html` (template) are shared by both portals; `styles.css` is the shared stylesheet. Which portal you get is decided purely by `PORTAL_ROLE` injected in each entry point's `main.ts` (`ADMIN` for BEUI, `MEMBER` for FEUI) — the same component branches its UI (and its public marketing site) on `role`.
- `deploy/` — `nginx.conf` (served by both UI containers) and Kubernetes manifests.

Both Angular apps build independently from this root workspace via `ng build BEUI` / `ng build FEUI`.

## Running locally

```sh
./run.sh stop   # tear down
./run.sh        # build + start everything (Docker Compose)
```

This builds and starts four containers: `postgres`, `backend`, `admin` (BEUI on :4200), `member` (FEUI on :4201). Credentials for the bootstrapped admin account are written to `.runtime/local.env` on first run (`ADMIN_USERNAME`, `ADMIN_PASSWORD`, `DB_PASSWORD`).

Alternative modes: `./run.sh k8s` (Docker Desktop Kubernetes), `./run.sh build`, `./run.sh db`. See `deploy/README.md` for kubectl details.

To iterate on backend Java code only, without a full `./run.sh`:
```sh
cd /Users/admin/Documents/dev/Personal/Expense_Management
docker compose build backend && docker compose up -d backend
```

### A note on schema migrations

`spring.jpa.hibernate.ddl-auto` is `update` — Hibernate adds new columns/tables automatically on boot. **This has bitten us twice in this project**: adding a Java `boolean` field (which maps to a `NOT NULL` column with no default) to an entity that already has rows fails silently — Hibernate logs a `WARN`, the ALTER never runs, and every future query against that table 500s. If you add a new non-nullable primitive field to an existing entity, either give existing rows a value first or fix it up immediately after deploy:
```sql
ALTER TABLE <table> ADD COLUMN IF NOT EXISTS <col> boolean NOT NULL DEFAULT false;
```
Check `docker logs em-backend-1 | grep "GenerationTarget encountered"` after any schema change to catch this early. New tables (no existing rows) are never affected.

## Architecture at a glance

- **One shared Angular component, two roles.** `shared/app.ts` / `shared/app.html` implement the entire UI for both portals. `role` (`'ADMIN'` | `'MEMBER'`, from `PORTAL_ROLE`) gates almost every branch: nav items, the public marketing site vs. plain admin login, which actions are visible, etc.
- **Public vs. authenticated content.** Anything reachable before login (member portal home page, About, Committee, Contact, election candidates) is served by `/api/public/**` and a couple of specifically-permitted plain paths (`/api/pastors`, `/api/csrf`, `/api/login`) — see `Security.java`. Everything else requires a session; `/api/admin/**` additionally requires `ROLE_ADMIN`.
- **File uploads are real files, not base64-in-the-database.** `POST /api/admin/uploads` (multipart, 5MB cap, admin-only) writes to a directory on disk and returns just a filename (e.g. `a1b2c3.jpg`). Entities store that filename in a `*Image` field (`profilePicImage`, `attachmentImage`, `foundationStoneImage`, church carousel `images`). The upload directory is a **Docker named volume (`uploads-data`) shared read-write with `backend` and read-only with both `admin` and `member`** — nginx serves `/uploads/<filename>` directly as a static file in both portals, no round-trip through Java. Client-side, `onFile()` resizes profile-picture-type uploads to 480px JPEG before sending; other attachments (cheques, slips, church photos) upload as-is.
- **Cash denomination integrity.** Every cash `Donation` must have `denominations` (face-value → quantity) summing exactly to the amount; non-cash payments require a `reference` and forbid denominations. This is enforced server-side in `Api.createDonation`.
- **Day-End Closing lifecycle.** A `DayEndClosing` row per calendar date drives `PENDING → APPROVED → VERIFIED → DEPOSITED`. Once a date is `APPROVED` or later, `createDonation` rejects new donations for that date. The summary endpoint (`GET /api/admin/day-end/{date}`) computes everything live from `Donation` rows grouped by category (member-tied categories get a "N members" count) plus a denomination roll-up — nothing is snapshotted except who/when approved/verified/deposited and which bank account.
- **Elections** have Candidates (nominated pre-election), a `winner` flag (declared manually by admin per category/subcategory, enforced one-winner-per-seat), and a separate `CommitteeMember` table for people appointed to the committee **without** an election. The public "Current Committee" page merges both sources. Candidates also track `feePaid` for the nomination form fee, recorded as a real `Donation` (category `Election Form Fee`) via `ElectionApi.payFee`, which calls `Api.createDonation` directly as a Java method — see the Hibernate proxy gotcha below if you touch this path.

### A Hibernate gotcha worth knowing about

`ElectionApi.payFee` loads a `Candidate` (which has a `@ManyToOne(fetch=LAZY) Member member` on the same `member_id` column) and then, in the *same transaction*, calls `Api.createDonation` which does `members.findById(...)`. Because this codebase uses **plain public fields, not getters**, an uninitialized Hibernate proxy returns `null` for every field when accessed directly — and JPA's `find()` can hand back exactly such a cached proxy if one was already registered in the session (which happens here because the Candidate's lazy `member` association silently registers one). The fix, already applied in `Api.createDonation`, is `Hibernate.unproxy(...)` on the loaded `Member` before reading its fields. If you see a donation with a real `memberId` but a blank `memberName`, this is almost certainly the cause — some other cross-entity same-transaction call path triggered it.

## Feature map

**Public site (no login, `localhost:4201`, `role==='MEMBER'` + `publicView`)**
- Hero photo carousel (falls back to `assets/church-*.png` if no Church images are uploaded)
- Promotional banners, Upcoming Events (real elections + fixed weekly services), "Meet the Candidates" carousel (`/api/public/elections`)
- About (`/api/public/church`), Current Bishop/Pastor/Committee (`/api/public/committee-members` + `/api/pastors`), Contact Us form (`/api/public/contact`)
- "Member login" opens a modal instead of replacing the page

**Admin portal (`localhost:4200`)**
- Overview / Contributions with month + **day** filters, Member-cash vs. Common-offerings-cash split
- Record a donation: member or "collection box, no specific member" (Sunday Worship Offerings / Sunday School / etc.), cash denominations or reference, cheque/transaction-slip upload for non-cash
- Members, Pastors (with **Is Bishop** checkbox, one bishop enforced server-side), Elections (nominate, declare winner, **download bilingual Hindi nomination form** as a print-to-PDF page, mark form fee paid with denomination/attachment capture)
- Pastor & Committee: appoint committee members directly (no election needed), assign a post (Secretary/Treasurer/etc., free text)
- **Day End**: date picker, line-item + denomination breakdown, Approve → Verify → **Deposit to bank** (with a Church-configured bank account)
- **Church**: profile (name/title/description/founded year & by/foundation stone photo/carousel images), multiple Bank Accounts
- **Contact Us**: inbox of public enquiries, mark read/unread

## Data model notes

- `Member`, `Pastor`, `Candidate`, `CommitteeMember` all have a `profilePicImage` (filename only). `Donation` has `attachmentImage` for cheque/slip photos.
- Master/reference lists (categories, events, denominations, payment types, genders, election categories/subcategories) live in the `master` table, seeded by `MasterDefaults` on boot (idempotent — only inserts values that don't already exist). The `masters()` API endpoint is what both portals read dropdowns from; edit `MasterDefaults.java` to add options.
- `Church` is effectively a singleton in the current UI (one profile edited in place), but the schema doesn't prevent creating more than one row — `GET /api/public/church` just returns the first one found. `BankAccount.churchId` exists but nothing sets it yet (see TODO).

## Validation

```sh
mvn -f BE/pom.xml test
npm run build
```

## Key API surface

- `GET /api/csrf`, `POST /api/login`, `POST /api/logout`
- `GET /api/me`, `/api/masters`, `/api/donations`, `/api/donations/{id}/receipt`, `/api/pastors`, `/api/elections`, `/api/committee-members`, `/api/churches`, `/api/bank-accounts`
- Public (no auth): `/api/public/elections`, `/api/public/committee-members`, `/api/public/church`, `POST /api/public/contact`
- Admin: `/api/admin/members`, `/api/admin/donations`, `/api/admin/pastors`, `/api/admin/elections`, `/api/admin/elections/{id}/candidates`, `PUT .../candidates/{id}/winner|post|fee`, `/api/admin/committee-members`, `/api/admin/day-end/{date}` + `/approve` `/verify` `/deposit`, `/api/admin/contact`, `/api/admin/churches`, `/api/admin/bank-accounts`, `POST /api/admin/uploads`
- Every route is also available under `/api/v1`. A machine-readable contract lives in `BE/src/main/resources/openapi.yaml` (not updated for everything added this session — see TODO).

## Deployment considerations

Serve each UI and `/api` through a same-origin HTTPS reverse proxy in production; set secure session cookies. Use `DB_URL`/`DB_USER`/`DB_PASSWORD` for Postgres. Replace `ddl-auto: update` with versioned migrations (Flyway/Liquibase) before production — see the gotcha above for why.

Election rules are explicit and intentionally simple: all active members can vote in every open seat, one vote per seat, votes are attributable to the authenticated member (no secret ballot). Nomination withdrawal, eligibility ages, tie-breaking, and multi-winner seats are not implemented.

## TODO / future development

Roughly in order of likely usefulness:

- **Church ↔ Bank Account linking.** `BankAccount.churchId` is unused; once there's a real need for more than one church record, wire the Church admin page to filter/scope bank accounts by church and stop treating Church as an implicit singleton.
- **Day-End "reopen".** Once a day is `APPROVED`, there's no way back to `PENDING` short of editing the database directly — worth a guarded "Reopen" action for admin mistakes (but `VERIFIED`/`DEPOSITED` should probably stay one-way).
- **Cash-in-hand summary across dates.** Day End currently shows one date at a time; a rollup of all `VERIFIED`-but-not-yet-`DEPOSITED` days (total cash still physically in hand) would close the loop on the bank-deposit lifecycle.
- **Orphaned `profile_pic` columns.** `Pastor`, `Member`, `Candidate`, `CommitteeMember` all still carry their original base64 `profile_pic` column from before the filename-based upload rework; nothing reads them anymore. Safe to drop once you're sure no historical data needs it.
- **OpenAPI spec drift.** `openapi.yaml` predates this session's additions (uploads, Day End, Church/Bank Accounts, Contact Us, election fee/post/winner, committee appointments). Regenerate or hand-update it before relying on it for client generation.
- **Election nomination form is Hindi-only and hardcoded to this church.** `printForm` in `app.ts`/`app.html` renders a fixed bilingual (Hindi) nomination form matching St. Thomas Church's paper form via the browser's print-to-PDF. If this app is ever used by another church/diocese, this needs to become a configurable template rather than inline HTML.
- **No email/SMS notifications.** Contact Us enquiries, new committee appointments, election results, and day-end approvals are all silent — nobody gets notified except by checking the admin UI.
- **Password reset / self-service.** Members can't reset their own password; only an admin can create/update member accounts.
- **PDF receipts are Latin-only.** `BE`'s donation receipt PDF (PDFBox, `Api.receipt`) still strips non-ASCII characters to `?` — unlike the nomination form (which sidesteps this by using the browser's print-to-PDF instead of PDFBox), receipts have no Unicode font embedded.
- **No audit trail for edits.** Most admin mutations overwrite in place (pastor edits, church profile edits) with only a single `updatedBy`/`updatedAt` pair — no history of prior values.
- **Bulk member import** and **login throttling / brute-force protection** remain unimplemented, as before this session.
