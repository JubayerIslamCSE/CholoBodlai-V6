# CholoBodlai V6 — GitHub / Render / Cloudinary Edition

## Change summary

- All V5 booking, bidding, owner approval, tracking, notifications, booking messages, ratings, disputes, booking history, 30-minute new-shipper list, GET-filter scroll restoration, and Night Drive + Dispatch Desk UI remain.
- Registered truck photos now upload to Cloudinary public images when Cloudinary is configured. Secure CDN URLs are returned via an existing authorization-controlled `/trucks/{id}/photo` endpoint.
- Truck registration documents now upload to Cloudinary **authenticated** assets. `/admin/truck-documents/{id}` checks admin authority and generates a short-lived (60-second) signed Cloudinary download URL. Private registration documents are NEVER delivered using anonymous public CDN URLs.
- Existing V5 local-media IDs remain readable on the **same original computer**. They do not magically migrate to Cloudinary: approved owners can re-upload a missing document, and the admin reviews the replacement. An owner can also re-upload their vehicle photo. The original V5 local files are not deleted or transferred automatically.
- Replacing a registration document makes the truck inactive and unverified until the admin checks it, and withdraws outstanding bids to prevent selection with an unverified truck. Existing accounts and trip history remain.
- New HTTPS-based transactional email via Resend (`RESEND_API_KEY`) for public email verification and Forgot Password on Render Free. Existing SMTP and secure local admin-only preview still work in their appropriate environments.
- Public sign-up is blocked with an understandable message when no mail-delivery mechanism is configured, rather than silently creating an account that can never be verified. When email delivery fails after account creation, the UI directs the user to resend verification.
- Dockerfile uses Java 21, a non-root user, and a container-aware JVM heap setting. `server.port=${PORT:8080}` stays valid for local/Render. Added Render HTTPS forwarded headers and secure-cookie toggle.
- Added GitHub Actions CI and optional `render.yaml` blueprint. Secrets never appear in project source.

## Data safety

- V6 does NOT delete MongoDB Atlas data, reset user passwords, drop collections, or execute a destructive migration.
- Existing uploaded photos/documents on local disk **are not** transferred to Cloudinary automatically. Re-upload from their original computer for new persistence.
- Do not open Atlas to the whole internet when allowlisting Render's outbound IP addresses is possible.
- Verify Cloudinary delivery of authenticated PDFs with your account before relying on it. Some accounts restrict PDF delivery; JPG/PNG registration documents are also supported.

## Verification boundary

The release includes new JUnit Cloudinary gateway tests and a Java 21 executable fake-HTTP Cloudinary smoke test; old V5 policy and scroll regressions are retained. Source syntax parsing, policy smoke, Cloudinary request/URL smoke, JavaScript, CSS, and ZIP integrity checks have been executed locally here. Full Maven/JUnit execution, successful authentication against real Cloudinary credentials, real MongoDB Atlas access, real SMTP/Resend delivery, and live Render start remain to be verified on your Mac, GitHub Actions, and the deployed service. See `QA-RESULTS-V6.txt` and the README.
