# Simple Reverse Proxy Service for Hubitat [![Donate](https://img.shields.io/badge/donate-PayPal-blue.svg?logo=paypal&style=plastic)](https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J)

## SUMMARY
  This application provides a service that manages multiple Simple Proxy Provider child apps that act as reverse micro-proxies. Each child app exposes a Hubitat-managed `/proxy` endpoint (**HTTP GET** only) that forwards requests to your configured **target URL** and returns the upstream response. This allows you to expose selected resources from your private network to trusted external clients without port-forwarding internal services.

See [QUICK_START.md](QUICK_START.md) for step-by-step setup.

## BENEFITS
  Using a reverse proxy enables selective exposure of internal resources, adding a layer of abstraction. The caller only talks to Hubitat's proxy endpoint, and Hubitat fetches the upstream content on your behalf.

## SECURITY DISCLAIMER
  Enabling remote access for this proxy may expose your Hubitat hub and connected network to significant security risks if proper precautions are not taken.
  
  Treat each child app's remote endpoint URL (it includes an access token) as a secret, and treat **target URL** as privileged configuration (the hub will make requests to whatever you configure).
  
  Consider VPN/firewall restrictions in addition to this app's authentication. By using this service, you assume full responsibility for any unauthorized access or security breaches that may occur. Use this feature only on trusted networks and at your own risk.


## Features

 - [Simple Reverse Proxy Service](app/simple-reverse-proxy-service.groovy)
   - This is the main app for managing the reverse micro-proxy endpoints exposed by each child app.
   - Each child app exposes a single upstream target via its `/proxy` endpoint.
   - Use it to bridge between networks (remote clients -> Hubitat -> upstream service) when Hubitat can reach both networks.
 - [Simple Proxy Provider](app/simple-proxy-provider.groovy)
   - Each reverse micro-proxy can be provided a unique name
   - Each reverse micro-proxy must define a fully qualified **target URL** (http/https)
   - Optional switch to control whether the caller's query parameters are merged into the upstream request
   - Optional switch to control whether request headers are forwarded to the upstream host
   - The micro-proxy will not work until OAuth is enabled for this app (required to create/authorize the access token)
   - The micro-proxy also requires the activation of the remote endpoint to function properly  

## Request / Response Behavior
- The child app maps **`GET /proxy`** to an upstream HTTP GET.
- If `targetUrl` includes a query string, those upstream query values are treated as the base; when merging caller query params, the upstream values take precedence for matching keys.
- Header forwarding (when enabled) filters out hop-by-hop/sensitive headers such as `access_token`.
- The response returns the upstream body and content type (defaulting to `application/json` if the upstream response omits it).

## Vision / Future Enhancements
The goal of this project is to leverage the Hubitat hub as a means of accessing select internal resources. As this project matures, additional features will be added to further protect against bad actors.

At this time, the feature set is intentionally simple: it behaves as a real **fetch-then-return** proxy. The consumer never talks directly to the upstream web endpoint; Hubitat downloads the upstream response first, then returns it. This is not intended to work as a full application reverse proxy (for example, browser-oriented HTML rewriting is out of scope).

This project was created with the assistance of AI, but every bit of this code is original.

## Support the Author
Please consider donating. This app took a lot of work to make.
Any donations received will be used to fund additional Hue based development.

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J)                  

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.  
