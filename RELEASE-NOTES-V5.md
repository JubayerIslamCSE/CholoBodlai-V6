# CholoBodlai V5 — Release notes

**Selected visual direction:** D — Night Drive homepage + Dispatch Desk dashboards.

## UI
- Bespoke dark freight-network homepage: geometric route illustration, neon-lime highlights, clear shipper and owner entry paths, responsive typography.
- Authenticated application navigation redesigned around a persistent charcoal sidebar and light workspace. Mobile drawer closes with its button, backdrop or Escape.
- Uniform treatment for admin, shipper, owner, truck, trip, reviews, notifications, history and tracking pages; maintain the previous customer-feedback card fix.

## Workflow fixes
- GET search/filter/sort and same-path query links preserve reading position; same-path POST and reload still preserve it. Different pages start at top.
- Input validation: phone 11 digits, names 120 chars, email 180, password 10–100, truck ID 5–35, review 500, bid 0.01–50 million (2 decimal places), cargo 0.1–50,000 kg and truck capacity, routes up to 180 chars, goods up to 1,500 chars, filter 80 chars, status choices enumerated.
- Role-policy tests cover shipper/owner/admin visibility, matched contact security, truck ownership, and bid acceptance permission.
- Booking pickup still requires at least 48 hours notice in Asia/Dhaka, both form and service.

## Compatibility
- MongoDB Atlas, PORT/8080, application Java package and existing data unchanged.
- Mail preview remains LOCAL-ONLY, disabled by default; no real GPS or real email without SMTP.
- Same `CholoBodlaiApplication` main class and IntelliJ run configuration as prior V4.

## Verification
See `QA-RESULTS.txt` for exactly what ran, what remains unverified and mandatory checks to perform in IntelliJ.
