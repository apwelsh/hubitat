# revproxy app documentation

This folder contains the two Hubitat apps that make up the `Simple Reverse Proxy Service` package.

## 1) `simple-reverse-proxy-service.groovy` (Parent)

Purpose:
 - Parent app that manages multiple `Simple Proxy Provider` child apps.

What it does:
 - Provides the app UI to add/remove child apps.

Endpoints:
 - The parent app itself does not expose the proxy endpoint.
 - Each child app exposes its own Hubitat-managed `/proxy` endpoint.

## 2) `simple-proxy-provider.groovy` (Child)

Purpose:
 - A single upstream forwarder (“micro-proxy”) configured with one `targetUrl`.

Endpoint:
 - Maps `GET /proxy` to `handleProxy`.

Request behavior:
 - Validates that `targetUrl` is `http://` or `https://`.
 - Builds the upstream URL:
   - If `targetUrl` includes a query string, those upstream query values act as the base.
   - Optionally merges client query parameters into the upstream request.
 - Optionally forwards most request headers upstream (filtered to avoid hop-by-hop/sensitive headers).

Response behavior:
 - Performs an HTTP GET to the upstream URL and returns:
   - Upstream `Content-Type` (or defaults to `application/json`)
   - Response body
   - Upstream headers (filtered to remove hop-by-hop/sensitive headers)

Remote endpoint control:
 - The child app includes a toggle that enables/disables the remote endpoint.
 - When enabled, the child app displays `localEndpointURL` and `remoteEndpointURL` values to use for callers.

## Notes
 - This is designed for simple web calls (not a full application reverse proxy for browsers/HTML rewriting).
 - Be careful with `targetUrl`: the hub will make requests to whatever you configure.

