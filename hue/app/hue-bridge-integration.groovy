/**
 * Advanced Philips Hue Bridge Integration application
 * Version 2.0.0
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a parent application for locating your Philips Hue Bridges, and installing
 * the Advanced Hue Bridge Controller application
 *-------------------------------------------------------------------------------------------------------------------
 * Copyright 2022 Armand Peter Welsh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the 'Software'), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *-------------------------------------------------------------------------------------------------------------------
 **/

 import groovy.transform.Field
 import java.util.concurrent.ConcurrentHashMap

definition(
    name:'Advanced Hue Bridge Integration',
    namespace: 'apwelsh',
    author: 'Armand Welsh',
    description: 'Find and install your Philips Hue Bridge systems',
    category: 'Convenience',
    //importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/app/hue-bridge-integration.groovy',
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: ''
)

@Field static final Integer PAGE_REFRESH_TIMEOUT = 10

@Field static final String PAGE_MAINPAGE = 'mainPage'
@Field static final String PAGE_BRIDGE_DISCOVERY = 'bridgeDiscovery'
@Field static final String PAGE_ADD_BRIDGE = 'addBridge'

preferences {
    page(name: PAGE_MAINPAGE)
    page(name: PAGE_BRIDGE_DISCOVERY, title: 'Device Discovery', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_ADD_BRIDGE,       title: 'Add Hue Bridge')
}

public String getBridgeHost() {
    String host = state.bridgeHost
    if (host?.endsWith(':80')) {
        host = "${host.substring(0,host.size()-3)}:443"
        state.bridgeHost = host
    }
    return host
}

public void setBridgeHost(String host) {
    if (host?.endsWith(':80')) {
        host = "${host.substring(0,host.size()-3)}:443"
    }    
    state.bridgeHost = host
}

def installationCompletePage() {
    return dynamicPage(name: PAGE_MAINPAGE, title: '', nextPage: null, uninstall: true, install: true) {
        section (getFormat("title", "Advanced Hue Bridge")) {
            paragraph getFormat("subtitle", "Installing new Hue Bridge Integration")
            paragraph getFormat("line")
            paragraph 'Click the Done button to install the Hue Bridge Integration.'
            paragraph 'Re-open the app to setup your device'
        }
    }
}

def mainPage(Map params=[:]) {
    if (app.installationState == 'INCOMPLETE') {
        return installationCompletePage()
    }

    Boolean uninstall = bridgeHost ? false : true

    return dynamicPage(name: PAGE_MAINPAGE, title: '', nextPage: null, uninstall: uninstall, install: true) {
        section (getFormat("title", "Advanced Hue Bridge")) {
            paragraph getFormat("subtitle", "Manage your linked Hue Bridge")

            href PAGE_BRIDGE_DISCOVERY, title:'Add new Hue bridge', description:''//, params: [pbutton: i]
            paragraph getFormat("line")
            
            paragraph ''
            getChildDevices.each { 
                log.info "$it.device.type" 
                //href selectedDevice ? PAGE_ADD_BRIDGE : PAGE_BRIDGE_DISCOVERY, title:title, description:'', state:selectedDevice? 'complete' : null //, params: [nextPage: PAGE_ADD_BRIDGE]
            }

        }
        section('Options') {
            input name: 'logEnable',  type: 'bool', defaultValue: true,  title: 'Enable informational logging'
            input name: 'dbgEnable',  type: 'bool', defaultValue: false, title: 'Enable debug logging'
        }
        section() {
            paragraph getFormat("line")
            paragraph '''<div style='color:#1A77C9;text-align:center'>Advanced Hue Bridge
                |
                |<a href='https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J' target='_blank'><img src='https://img.shields.io/badge/donate-PayPal-blue.svg?logo=paypal&style=plastic' border='0' alt='Donate'></a>
                |
                |Please consider donating. This app took a lot of work to make.
                |Any donations received will be used to purchase additional Hue products to further the development of new device support
                |</div>'''.stripMargin()
        }      
    }
}

def getFormat(type, myText=""){            // Borrowed from @dcmeglio HPM code
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
    if(type == "subtitle") return "<h3 style='color:#1A77C9;font-weight: normal'>${myText}</h3>"
}

@Field static final Integer DEVICE_REFRESH_DICOVER_INTERVAL = 3
@Field static final Integer DEVICE_REFRESH_MAX_COUNT = 30

def bridgeDiscovery(Map params=[:]) {
    if (selectedDevice) {
        
    }
    if (logEnable) { log.debug 'Searching for Hub additions and updates' }
    Map hubs = discoveredHubs()

    Integer deviceRefreshCount = Integer.valueOf(state.deviceRefreshCount ?: 0)
    state.deviceRefreshCount = deviceRefreshCount + 1
    Integer refreshInterval = PAGE_REFRESH_TIMEOUT

    Map options = hubs ?: [:]
    Integer numFound = options.size()

    if (!options && state.deviceRefreshCount > DEVICE_REFRESH_MAX_COUNT) {
        /* groovylint-disable-next-line DuplicateNumberLiteral */
        state.deviceRefreshCount = 0
        ssdpSubscribe()
    }


    //bridge discovery request every 5th refresh, retry discovery
    if (!(deviceRefreshCount % DEVICE_REFRESH_DICOVER_INTERVAL)) {
        ssdpDiscover()
    }

    String nextPage = selectedDevice ? PAGE_ADD_BRIDGE : null

    return dynamicPage(name:PAGE_BRIDGE_DISCOVERY, title:'Discovery Started!', nextPage:nextPage, refreshInterval:refreshInterval, uninstall:!bridgeLinked) {
        section('Please wait while we discover your Hue Bridge. Note that you must first configure your Hue Bridge and Lights using the Philips Hue application. Discovery can take five minutes or more, so sit back and relax, the page will reload automatically! Select your Hue Bridge below once discovered.') {
            input 'selectedDevice', 'enum', required:false, title:"Select Hue Bridge (${numFound} found)", multiple:false, options:options, submitOnChange: true
        }
    }
}

// Returns true is bridgeHost has a value, or if a child app is defined
Boolean isBridgeLinked() {
    return (bridgeHost || getChildApps())
}

def addBridge() {
    def hub = getHubForMac(selectedDevice)
    def controller = addChildApp('apwelsh', 'Advanced Hue Bridge Controller', hub.name)
    controller.setHub(hub)
    return mainPage()
}

// Life Cycle Functions

def installed() {
    app.updateSetting('logEnable', true)
    app.updateSetting('dbgEnable', false)
    ssdpSubscribe()
    ssdpDiscover()
}

def uninstalled() {
    unsubscribe()
    childDevices.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updated() {
    if (debug)  { app.updateSetting('dbgEnable', debug) }  // temporary cleanup code
    if (logNew) { app.updateSetting('newEnable', logNew) } // temporary cleanup code
    app.removeSetting('debug')  // temporary cleanup code
    app.removeSetting('logNew') // temporary cleanup code
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    volatileAtomicStateByDeviceId.clear()
    ssdpDiscover()
    runEvery5Minutes('ssdpDiscover')
}

def subscribe() {
    // Add message subscriptions for devices, if needed
}


/*
 * SSDP Device Discover
 */

void ssdpSubscribe() {
    subscribe(location, 'ssdpTerm.upnp:rootdevice', ssdpHandler)
}

void ssdpUnsubscribe() {
    unsubscribe(ssdpHandler)
}


void ssdpDiscover() {
    sendHubCommand(new hubitat.device.HubAction('lan discovery upnp:rootdevice', hubitat.device.Protocol.LAN))

}

def ssdpHandler(evt) {
    def description = evt.description
    def parsedEvent = parseLanMessage(description)
    def ssdpPath = parsedEvent.ssdpPath

    // The Hue bridge publishes the device information in /description.xml, so if this ssdpPath does not match, skip this device.
    if (ssdpPath != '/description.xml') { return }

    def hub = evt?.hubId
    if (parsedEvent.networkAddress) {
        parsedEvent << ['hub':hub,
                        'networkAddress': convertHexToIP(parsedEvent.networkAddress),
                        'deviceAddress': convertHexToInt(parsedEvent.deviceAddress)]

        def ssdpUSN = parsedEvent.ssdpUSN.toString()

        def hubs = getHubs()
        if (!hubs."${ssdpUSN}") {
            verifyDevice(parsedEvent)
        } else {
            updateDevice(parsedEvent)
        }
    }
}

void updateDevice(parsedEvent) {
    def ssdpUSN = parsedEvent.ssdpUSN.toString()
    def hubs = getHubs()
    def device = hubs["${ssdpUSN}"]

    if (device.networkAddress != parsedEvent.networkAddress || device.deviceAddress != parsedEvent.deviceAddress) {
        device << ['networkAddress': parsedEvent.networkAddress,
                   'deviceAddress': parsedEvent.deviceAddress]

        if (logEnable) { log.debug "Discovered hub address update: ${device.name}" }
    }
}

void verifyDevice(parsedEvent) {
    def ssdpPath = parsedEvent.ssdpPath

    // The Hue bridge publishes the device information in /description.xml, so if this ssdpPath does not match, skip this device.
    if (ssdpPath != '/description.xml') { return }

    // Using the httpGet method, and arrow function, perform the validation check w/o the need for a callback function.
    httpGet("http://${parsedEvent.networkAddress}:${parsedEvent.deviceAddress}${ssdpPath}") { response ->
        if (!response.isSuccess()) {return}

        def data = response.data
        if (data) {
            def device = data.device
            String model = device.modelName
            String ssdpUSN = "${parsedEvent.ssdpUSN.toString()}"

            if (logEnable) { log.debug "Identified model: ${model}" }
            if (model =~ 'Philips hue bridge.*') {
                def hubId = "${parsedEvent.mac}"[-6..-1]
                String name = "${device.friendlyName}".replaceAll(~/\(.*\)/, "(${hubId})")

                parsedEvent << ['url':          "${data.URLBase}",
                                'name':         "${name}",
                                'serialNumber': "${device.serialNumber}"]

                def hubs = getHubs()
                hubs << ["${ssdpUSN}": parsedEvent]
                if (logEnable) { log.debug "Discovered new hub: ${name}" }
            }
        }

    }
}

/*
 *
 * Hue Hub API Integration Functions
 *
 */

private requestHubAccess(mac) {
    def device = getHubForMac(mac)
    def deviceType = '{"devicetype": "AdvanceHueBridgeLink#Hubitat", "generateclientkey": true}'

    asynchttpPost(requestHubAccessHandler,
                  [uri: "http://${device.networkAddress}/api",
                   contentType: 'application/json',
                   requestContentType: 'application/json',
                   body: deviceType], [device: device])

}

def requestHubAccessHandler(response, args) {
    def status = response.getStatus();
    if (status < 200 || status >= 300) { return }

    def data = response.json
    if (data) {
        if (data.error?.description) {
            if (logEnable) { log.error "${data.error.description[0]}" }
        } else if (data.success) {
            if (data.success.username && data.success.clientkey) {
                def device = getHubForMac(args.device.mac)
                device.remove("clientkey")
                device << [username: "${data.success.username[0]}"] << [clientkey: "${data.success.clientkey[0]}"]
                if (logEnable) { log.debug "Obtained credentials: ${device}" }
            } else {
                log.error "Problem with hub linking.  Received response: ${data}"
            }
        } else {
            if (logEnable) { log.error $data }
        }

    }
}

String getApiUrl() {
    "https://${bridgeHost}/api/${state.username}/"
}

String getApiV2Url() {
        uri: "https://${bridgeHost}/clip/v2"
}

Map getApiV2Header() {
    ['hue-application-key': state.username]
}

void refreshHubStatus() {

    def url = apiUrl
    httpGet([uri: url,
             contentType: 'application/json',
             ignoreSSLIssues: true, 
             requestContentType: 'application/json']) { response ->

        if (!response.isSuccess()) { return }

        def data = response.data
        if (data) {
            if (data.error?.description) {
                if (logEnable) { log.error "${data.error.description[0]}" }
            } else {
                parseStatus(data)
            }
        }

    }
}


/*
 *
 * Private helper functions
 *
 */


private String convertHexToIP(hex) {
    [hubitat.helper.HexUtils.hexStringToInt(hex[0..1]),
     hubitat.helper.HexUtils.hexStringToInt(hex[2..3]),
     hubitat.helper.HexUtils.hexStringToInt(hex[4..5]),
     hubitat.helper.HexUtils.hexStringToInt(hex[6..7])].join('.')
}

private Integer convertHexToInt(hex) {
    hubitat.helper.HexUtils.hexStringToInt(hex)
}

/*
 *
 * Hub Device Utility Functions
 *
 */

def deviceNetworkId(mac) {
    mac ? "hue-${mac}" : null
}

Map discoveredHubs() {
    Map vhubs = getHubs()
    Map map = [:]
    vhubs.each {
        String value = "${it.value?.name}"
        String key = "${it.value?.mac}"
        map["${key}"] = value
    }
    map
}

def getSelectedHub() {
    getHubs().find{ key, value -> value.mac == selectedDevice}?.value
}

def getHubForMac(mac) {
    getHubs().find{ key, value -> value.mac == mac}?.value
}

def getHubs() {
    state.hubs = state.hubs ?: [:]
}

def getDeviceApp(child) {
    String dni = (child.device ?: child).deviceNetworkId
    return getChildApps().find { it.getChildDevice(dni) }
}

// Child app delegated functions


public networkIdForSensor(child, sensorId) {
    getDeviceApp(child).networkIdForSensor(sensorId)
}

void componentOn(child) {
	getDeviceApp(child).componentOn(child)
}

void componentOff(child) {
	getDeviceApp(child).componentOff(child)
}

void componentRefresh(child) {
	getDeviceApp(child).componentRefresh(child)
}

void componentSetLevel(child, level, duration=null) {
	getDeviceApp(child).componentSetLevel(child, level, duration)
}

void componentStartLevelChange(child, direction) {
	getDeviceApp(child).componentStartLevelChange(child, direction)
}

void componentStopLevelChange(def child) {
	getDeviceApp(child).componentStopLevelChange(child)
}

void componentSetColor(def child, Map colormap) {
	getDeviceApp(child).componentSetColor(child, colormap)
}

void componentSetHue(def child, hue) {
	getDeviceApp(child).componentSetHue(child, hue)
}

void componentSetSaturation(def child, saturation) {
	getDeviceApp(child).componentSetSaturation(child, saturation)
}

void componentSetColorTemperature(def child, colortemperature, level = null, transitionTime = null) {
	getDeviceApp(child).componentSetColorTemperature(child, colortemperature, level, transitionTime)
}
