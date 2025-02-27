import java.util.regex.Pattern

/**
 *  Simple Proxy Provider
 *
 *  Child app acting as a reverse proxy that forwards incoming requests to a specified target URL.
 *  It can optionally pass client request headers and append client query parameters to the upstream request.
 *  The remote endpoint functionality can be toggled on and off.
 */
definition(
    name: "Simple Proxy Provider",
    namespace: "apwelsh",
    author: "Armand Welsh",
    description: "Child app acting as a simple proxy for a single web call with options to pass request headers and query parameters.",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    parent: "apwelsh:Simple Reverse Proxy Service"
)

preferences {
    page(name: "mainPage")
}

def mainPage(params) {

    if (params?.action == "enable") {
        initializeAppEndpoint()
    } else if (params?.action == "disable") {
        revokeAccessToken()
    }

    dynamicPage(name: "mainPage", title: "Simple Proxy Provider", install: true, uninstall: true) {
        // Built-in label input to update the app's label.
        section("") {
            label title: "Name", required: true
        }
        section("Proxy Settings") {
            input "targetUrl", "text", title: "Target URL", required: true, submitOnChange: true
        }
        section("Proxy Options") {
            input "passRequestHeaders", "bool", title: "Pass Request Headers to Upstream Host", required: true, defaultValue: true
            input "appendClientQueryParams", "bool", title: "Append Client Query Parameters to Upstream URL", required: true, defaultValue: true
            input "enableUsageLogging", "bool", title: "Enable Usage Logging", required: true, defaultValue: false
        }
        section("Remote Endpoint Control") {
            if (!state.endpoint) {
                paragraph "Remote endpoint is currently disabled."
                href(name: "initializeAppEndpoint", title: "Enable Remote Endpoint", description: "Tap to enable", params: [action: "enable"], page: "mainPage")
            } else {
                paragraph "Local Endpoint:\n<a href='${state.localEndpointURL}' target='_blank'>${state.localEndpointURL}</a>"
                paragraph "Remote Endpoint:\n<a href='${state.remoteEndpointURL}' target='_blank'>${state.remoteEndpointURL}</a>"
                href(name: "revokeAccessToken", title: "Disable Remote Endpoint", description: "Tap to disable", params: [action: "disable"], page: "mainPage")
            }
        }
    }
}

mappings {
    path("/proxy") {
        action: [
            GET: "handleProxy"
        ]
    }
}

def isValidUrl(url) {
    try {
        def uri = new URI(url)
        return uri.scheme in ["http", "https"] && uri.host != null
    } catch (URISyntaxException e) {
        return false
    }
}

def sanitizeQueryParam(value) {
    return URLEncoder.encode(value, "UTF-8")
}

def sanitizeUrl(url) {
    try {
        return new URI(url).toASCIIString()
    } catch (Exception e) {
        return ""
    }
}

def handleProxy() {
    if (!targetUrl || !isValidUrl(targetUrl)) {
        log.warn "Invalid or missing target URL: ${targetUrl}" + (targetUrl ? " - Fails validation" : " - Target URL is empty")
        render contentType: "application/json", data: [error: "Invalid or missing target URL. Must be a valid HTTP/HTTPS URL."]
        return
    }
    
    def baseUrl = targetUrl
    def upstreamQueryMap = [:]
    if (targetUrl.contains("?")) {
        def parts = targetUrl.split(/\?/, 2)
        baseUrl = parts[0]
        def queryString = parts[1]
        queryString.split("&").each { pair ->
            def kv = pair.split("=")
            upstreamQueryMap[kv[0]] = (kv.size() > 1 ? sanitizeQueryParam(kv[1]) : "")
        }
    }
    
    def mergedParams = [:]
    mergedParams.putAll(upstreamQueryMap)
    if (appendClientQueryParams && params != null) {
        params.each { key, value ->
            if (key.toLowerCase() != "access_token" && !upstreamQueryMap.containsKey(key)) {
                mergedParams[key] = sanitizeQueryParam(value.toString())
            }
        }
    }
    
    def queryStringFinal = mergedParams.collect { key, value ->
        "${URLEncoder.encode(key, 'UTF-8')}=${URLEncoder.encode(value, 'UTF-8')}"
    }.join("&")
    
    def upstreamUrl = sanitizeUrl(baseUrl + (queryStringFinal ? "?" + queryStringFinal : ""))
    
    // Define the list of headers to ignore.
    def ignoreHeaders = ["connection", "access_token", "cookie", "host", "upgrade-insecure-requests"]
    
    // Build request headers if the toggle is enabled.
    def requestHeaders = [:]
    if (passRequestHeaders && request?.headers) {
        if (request.headers instanceof Map) {
            request.headers.each { key, value ->
                if (!ignoreHeaders.contains(key.toLowerCase())) {
                    requestHeaders[key] = value
                }
            }
        } else if (request.headers instanceof List) {
            request.headers.each { header ->
                def key = header.getName()
                if (!ignoreHeaders.contains(key.toLowerCase())) {
                    requestHeaders[key] = header.getValue()
                }
            }
        }
    }
    
    // Log usage details if enabled
    if (enableUsageLogging) {

        String reconstructedUrl = (request.requestSource == 'local' ? localApiServerUrl : apiServerUrl)
        reconstructedUrl += '/proxy/' + (params ? '?' + params.collect { key, value -> "${URLEncoder.encode(key, 'UTF-8')}=${URLEncoder.encode(value.toString(), 'UTF-8')}" }.join('&') : '')
        log.info "${app.getLabel()} (${request.requestSource})  ${reconstructedUrl}"
    }
    
    try {
        httpGet(uri: upstreamUrl, headers: requestHeaders) { resp ->
            def headerMap = [:]
            if (resp.headers instanceof Map) {
                resp.headers.each { key, value ->
                    if (!ignoreHeaders.contains(key.toLowerCase())) {
                        headerMap[key] = value
                    }
                }
            } else if (resp.headers instanceof List) {
                resp.headers.each { header ->
                    def key = header.getName()
                    if (!ignoreHeaders.contains(key.toLowerCase())) {
                        headerMap[key] = header.getValue()
                    }
                }
            }
            
            // If no Content-Disposition header exists, deduce a filename from targetUrl.
            if (!headerMap['Content-Disposition']) {
                def segments = targetUrl.tokenize("/")
                def fileName = segments ? segments[-1].split('\\?')[0] : null
                if (fileName) {
                    headerMap['Content-Disposition'] = "attachment; filename=\"${fileName}\""
                }
            }
            
            if (resp.status == 200) {
                def ct = headerMap['Content-Type'] ?: "application/json"
                def dataOut = resp.data
                if (dataOut instanceof java.io.ByteArrayInputStream) {
                    dataOut = new String(dataOut.bytes, "UTF-8")
                }
                render contentType: ct, data: dataOut, headers: headerMap
            } else {
                render contentType: "application/json", data: [error: "HTTP call returned status ${resp.status}"], headers: headerMap
            }
        }
    } catch (Exception e) {
        render contentType: "application/json", data: [error: "Exception: ${e.message}"]
    }
}

/**
 * Initializes the remote endpoint by creating an access token and storing endpoint URLs.
 * The hub automatically stores the access token in state.accessToken.
 * The access token is appended as a query parameter named "apikey".
 */
def initializeAppEndpoint() {
    if (!state.endpoint) {
        try {
            def token = createAccessToken()
            if (token) {
                state.endpoint = true
                state.localEndpointURL = getFullLocalApiServerUrl() + "/proxy?access_token=${state.accessToken}"
                state.remoteEndpointURL = getFullApiServerUrl() + "/proxy?access_token=${state.accessToken}"
            }
        } catch(e) {
            state.endpoint = null
        }
    }
    return state.endpoint
}

/**
 * Revokes the remote endpoint by clearing the stored token and endpoint URLs.
 */
def revokeAccessToken() {
    try {
        state.endpoint = false
        state.localEndpointURL = null
        state.remoteEndpointURL = null
        state.accessToken = null
    } catch(e) {
    }
}

/**
 * Called when the app's preferences are updated.
 */
def updated() {
    // No additional label update is necessary since the built-in label is used.
}
