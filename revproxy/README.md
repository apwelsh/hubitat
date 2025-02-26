# Simple Reverse Proxy Service for Hubitat [![Donate](https://img.shields.io/badge/donate-PayPal-blue.svg?logo=paypal&style=plastic)](https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J)

## SUMMARY
  This application provides a service that manages multiple Simple Proxy Provider child apps that act as reverse mico-proxies. Each child proxy forwards incoming web requests to a specified target URL and returns the upstream response. This allows you to expose content from your private network to trusted external hosts without directly exposing your network, and without the need for complex VPNs.

## BENEFITS
  Using a reverse proxy enables selective exposure of internal resources, adding a layer of abstraction and security. Trusted external services (e.g., sharptools.io) can securely access specific data on your hub's network while the proxy handles requests in a controlled manner. This helps maintain security while providing remote access to designated content.

## SECURITY DISCLAIMER
  Enabling the reverse proxy service may expose your Hubitat hub and connected network to significant security risks if proper precautions are not taken. Before enabling remote access, ensure that you have configured adequate network security measures (such as firewalls, VPNs, or strict access controls). By using this service, you assume full responsibility for any unauthorized access or security breaches that may occur. Use this feature only on trusted networks and at your own risk.


## Features

 - [Simple Reverse Proxy Service](app/simple-reverse-proxy-service.groovy)
   - This is the main app for managing the various reverse micro-proxy endpoints.
   - Each endpoint can expose only one specific URL to the consumer of the service.
   - This is not just for exposing private network resources to the internet, it can also be used to enable access between IoT networks and home networks, when the Hubitat Elevation hub has visibility to both networks.
 - [Simple Proxy Provider](app/simple-proxy-provider.groovy)
   - Each reverse micro-proxy can be provided a unique name
   - Each reverse micro-proxy must define a fully quialified target URL
   - Optional switch to turn on/off support to forward query parameters
   - Optional switch to turn on/off support to forward http request headers
   - The micro-proxy will not work until the OAuth has been enabled for this driver in the Dev / Apps Code section of hubitat for this driver
   - The micro-proxy also requires the activation of the remote endpoint to function properly  

## Vision / Future Enhancements
The goal of this project is to leverage the Hubitat Hub as a means of accessing select internal resources. As this project matures, additional features will be added to further provide protection from bad actors.  At this time, the features is very simple, but it is in all accounts a real proxy.  At no time does the consumer of this server talk directly to the back-end web endpoint.  All data is downloaded from the back-end (upstream) endpoint before it is sent to the consumer.  This is not intended to work as an application reverse proxy, as it is not a simple URL redirector.

This project was created with the assistance of AI, but every bit of this code it original code.

## Support the Author
Please consider donating. This app took a lot of work to make.
Any donations received will be used to fund additional Hue based development.

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J)                  

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.  
