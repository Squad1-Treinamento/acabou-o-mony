# Minimum Viable Context (MVC) - Acabou o Mony Payment Platform

## 1. The Vision and Business Context

**The "Acabou o Mony" Story**
In a vibrant, bustling city where local commerce thrives on innovation, merchants shared a common, crippling pain point: the complexity, slowness, and unreliability of existing payment systems. Enter **"Acabou o Mony"**, a fintech startup with a bold, simple mission: to revolutionize interactions with money by creating a platform capable of processing transactions with unprecedented speed and airtight security. The goal is to remove traditional payment barriers for everyone, from micro-entrepreneurs to large enterprises.

**The Primary Persona: Ana**
Ana is an entrepreneur striving to expand her small clothing business into the digital world by leveraging the highly lucrative but demanding format of **Live Commerce**. 
* **The Pain Point:** Existing payment systems were painfully slow and frequently failed during her live streams, causing high cart abandonment, frustrating her customers, and halting her business growth.
* **The Solution:** Ana adopted "Acabou o Mony". Despite initial skepticism from past failures with other gateways, the new platform delivered on its promise. Transactions processed instantly and integrated flawlessly with her Live Commerce setup. Her customers could finally purchase products seamlessly without stream interruptions.
* **The Impact:** "Acabou o Mony" unlocked new growth opportunities for Ana and countless others, cementing its reputation as a transformative force in digital payments driven by a dedicated technical team.

---

## 2. Core System Requirements

To support a massive volume of transactions and deliver on the promise made to users like Ana, the engineering team must strictly adhere to the following pillars:
* **Performance & Scalability:** The system must process transactions at lightning speed and seamlessly scale horizontally to absorb massive demand spikes (especially during Live Commerce events).
* **Security:** Impermeable protection against fraud, encompassing robust data security for all customer financial information.
* **Reliability:** The system must guarantee high availability, ensuring it is online precisely when clients need it the most.
* **UX/UI:** A frictionless, intuitive user experience that makes completing transactions effortless.
* **Integrations:** Highly efficient and flexible API integrations with third-party platforms, modern payment acquirers (e.g., Mercado Pago), and diverse commerce environments.

---

## 3. BDD Outcome Overview

The combined outcome is a fast, resilient, and trustworthy payment experience that removes friction during high-volume commerce and enables merchants to convert more sales without interruptions.

- **Given** the platform serves merchants and card users during live and conversational commerce, **When** transactions are initiated at scale, **Then** payments are processed in under one second with immediate confirmation, the system scales automatically without end-user performance degradation, and each transaction uses secure connections and secure authentication.
- **Given** customers stay inside live streams or chat interfaces, **When** they complete a purchase, **Then** the payment flow finishes without redirecting them away from the host platform.

## 4. Glossary

- **3DS Engine** — dedicated microservice for 3D Secure 2.x step-up authentication. Owns no relational database; all state lives in Redis.
- **Core Service** — the main payment processing service. Owns PostgreSQL ledger, Mercado Pago integration, and risk evaluation.
- **ACS (Access Control Server)** — the bank's authentication server that presents the 3DS challenge to the cardholder. The Core Service resolves the ACS URL (via BIN lookup or directory service) at initiation time and stores it in the Redis session.
- **Challenge Initiation** — the Core Service resolves the ACS URL, creates the 3DS challenge session in Redis (including `acs_url`), signs the JWT, and returns `3DS_CHALLENGE_REQUIRED` to the merchant. The 3DS Engine never exposes a challenge-initiation endpoint.
- **Landing Page** — a `GET /challenge/{challenge_id}?jwt=<token>` endpoint on the 3DS Engine. Reads the session from Redis (including `acs_url`), verifies the JWT from the query parameter, and HTTP-redirects the cardholder's browser to the bank's ACS URL.
- **3DS Engine Endpoints** — only two: `GET /challenge/{challenge_id}` (landing page, redirects to ACS) and `POST /api/v1/3ds/verify` (receives MFA result from the bank via Nginx reverse proxy).
- **Challenge Processing** — the bank POSTs the MFA result to the 3DS Engine's verify endpoint. The Engine reads the session from Redis, validates the token, writes the auth result, and fires an async callback to Core.
- **Async Callback** — non-blocking HTTP/2 POST from 3DS Engine to Core Service after MFA verification. Failure is recoverable via Core polling Redis.
- **JWT** — signed by the Core Service at initiation time using HS256. Carries `challenge_id`, `transaction_id`, `merchant_id`, `amount`, `exp`, `iat`. The 3DS Engine only **verifies** the JWT (on the landing page). Both services share the same `jwt.secret`.
- **`challenge_id`** — UUID generated by the Core Service at initiation time. Uniquely identifies a 3DS challenge session. Used as the idempotency key.
- **`mfa_token`** — the token the cardholder's bank returns after the user completes the 3DS challenge on the bank's page. Validated by the 3DS Engine: session must exist in Redis and token must be non-empty. No cryptographic validation of the token itself (MVP scope).
