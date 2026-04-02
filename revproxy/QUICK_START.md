# Simple Reverse Proxy Service - Quick Start

## Goal
Expose a specific internal web resource (on your LAN/private network) to an external caller by using your Hubitat hub as a gateway.

This package provides:
- A parent app (`Simple Reverse Proxy Service`) to manage multiple endpoints
- A child app (`Simple Proxy Provider`) that forwards HTTP **GET** requests to a configured upstream `targetUrl`

## Steps

1. Install the `Simple Reverse Proxy Service` package.
2. Open the parent app: `Simple Reverse Proxy Service`.
3. Add one or more child apps: `Simple Proxy Provider`.
4. For each child:
   - Set a unique **Name**
   - Set **Target URL** to the upstream resource you want Hubitat to fetch (must be `http://` or `https://`)
   - Decide whether to:
     - **Pass Request Headers to Upstream Host**
     - **Append Client Query Parameters to Upstream URL**
   - Enable the **Remote Endpoint** (the child app shows local/remote endpoint URLs once enabled)
5. From your external client/service, call the **Remote Endpoint URL** shown in the child app.
   - That URL includes an access token; treat it as a secret.
6. If you need to stop access, disable the Remote Endpoint in the child app.

## Important Notes / Limitations
- The child app exposes a Hubitat-managed `/proxy` endpoint and supports **GET only**.
- Hubitat authenticates access using its access token mechanism (so the remote URL must include the token).
- Because this is a generic fetcher, `targetUrl` is privileged configuration. Only set it to endpoints you trust.

