/**
 *  Simple Reverse Proxy Service
 *
 *  Parent app that manages multiple Simple Proxy Provider child apps.
 *
 *  SUMMARY:
 *  This service manages multiple Simple Proxy Provider child apps that act as reverse proxies.
 *  Each child proxy forwards incoming web requests to a specified target URL and returns the upstream
 *  response. This allows you to expose content from your private network to trusted external hosts without
 *  directly exposing your network.
 *
 *  BENEFITS:
 *  Using a reverse proxy enables selective exposure of internal resources, adding a layer of abstraction
 *  and security. Trusted external services (e.g., sharptools.io) can securely access specific data on your
 *  hub's network while the proxy handles requests in a controlled manner. This helps maintain security
 *  while providing remote access to designated content.
 *
 *  DISCLAIMER:
 *  Enabling the reverse proxy service may expose your Hubitat hub to significant security risks if proper
 *  precautions are not taken. Before enabling remote access, ensure that you have configured adequate network
 *  security measures (such as firewalls, VPNs, or strict access controls). By using this service, you assume
 *  full responsibility for any unauthorized access or security breaches that may occur. Use this feature only on
 *  trusted networks and at your own risk.
 */
definition(
    name: "Simple Reverse Proxy Service",
    namespace: "apwelsh",
    author: "Armand Welsh",
    description: "Parent app to manage multiple Simple Proxy Provider child apps. Use with caution!",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Simple Reverse Proxy Service", install: true, uninstall: true) {
        section("Overview") {
            paragraph "This service manages multiple Simple Proxy Provider child apps that act as reverse proxies. Each child proxy forwards incoming web requests to a specified target URL and returns the upstream response. This allows you to expose content from your private network to trusted external hosts without directly exposing your network."
            paragraph "Using a reverse proxy enables selective exposure of internal resources, adding a layer of abstraction and security. Trusted external services (e.g., sharptools.io) can securely access specific data on your hub's network while the proxy handles requests in a controlled manner. This helps maintain security while providing remote access to designated content."
        }
        section("Child Apps") {
            app(name: "childApps", appName: "Simple Proxy Provider", namespace: "apwelsh", title: "Add New Simple Proxy Provider", multiple: true)
        }
        section("<hr style='border:2px solid red; margin:10px 0;'>") {
            paragraph "<span style='color:red; font-style:italic;'>WARNING:</span> Enabling the reverse proxy service may expose your Hubitat hub to significant security risks if proper precautions are not taken. Before enabling remote access, ensure that you have configured adequate network security measures (such as firewalls, VPNs, or strict access controls). By using this service, you assume full responsibility for any unauthorized access or security breaches that may occur. Use this feature only on trusted networks and at your own risk."
        }
    }
}
