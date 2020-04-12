
definition(
    name: "aaa Roku Connect",
    namespace: "apwelsh",
    author: "Armand Welsh (apwelsh)",
    description: "Service Manager for Roku devices",
    category: "Convenience",
    //importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/roku/app/hue-connect.groovy",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
	page(name: "mainPage")
    page(name: "deviceDiscovery", title: "Device Discover", content:"deviceDiscovery", refreshTimeout:10)
    page(name: "addSelectedDevices")
    page(name: "configureDevice")
    page(name: "changeName")
    page(name: "appDiscovery")
    page(name: "installApps", content:"installApps")
    page(name: "manageApp")
}

/*
 * Life Cycle Functions
 */

def installed() {
    state.discovered=[:]
    ssdpSubscribe()
    initialize()
}

def uninstalled() {
    unschedule()
    unsubscribe()
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updated() {
    unschedule()
    initialize()
}

def initialize() {
    ssdpDiscover()
    runEvery5Minutes("ssdpDiscover")    
}

def subscribe() {
    // Add message subscriptions for devices, if needed
}


/*
 * Application Screens
 */

def routerPage() {
    if (!state) {
        return dynamicPage(name: "routerPage", title: "Roku Connect", nextPage: null, uninstall: true, install: true) {
            section {
                paragraph "Hit Done to to install the Roku Connect Integration.\nRe-open to setup."
            }
        }
    } else {
         return mainPage()   
    }
}

def mainPage() {

    if (!state) {
        return dynamicPage(name: "mainPage", title: "Roku Connect", nextPage: null, uninstall: true, install: true) {
            section {
                paragraph "Hit Done to to install the Roku Connect Integration.\nRe-open to setup."
            }
        }
    } else {
        return dynamicPage(name: "mainPage", title: "Manage your Roku connected devices", nextPage: null, uninstall: true, install: true) {
            section(){
                href "deviceDiscovery", title:"Discover New Devices", description:""
            }
            section("Installed Devices"){
                getChildDevices().sort({ a, b -> a["label"] <=> b["label"] }).each {
                    def desc = it.label != it.name ? it.name : ""
                    href "configureDevice", title:"$it.label", description:desc, params: [netId: it.deviceNetworkId]
                }
            }
            section("Options") {
                input name: "logEnable",   type: "bool", defaultValue: true, title: "Enable logging"
            }

        }
    }

    
}
def deviceDiscovery() {
	if (logEnable) log.debug "Searching for Hub additions and updates"
    def refreshInterval = 30
    
    // Make sure we initiate a new search for Roku devices.
    ssdpSubscribe()
    ssdpDiscover()

    def installed = getChildDevices().collect { it.deviceNetworkId }
    def discovered = getDiscovered()
    
    def options = [:]
    if (discovered) {
        discovered.each {key, value ->
            if (installed.find { it == value.mac }) return
            options["${key}"]= "${value.name}"
        }
    }

    def numFound = options.size() ?: 0
        
    def uninstall = false
    def nextPage = selectedDevices ? "addSelectedDevices" : null
    log.info "Rendering Page with options: ${options}"
    
	return dynamicPage(name:"deviceDiscovery", title:"Discovery Started!", nextPage:nextPage, refreshInterval:refreshInterval, uninstall:uninstall) {
		section("Please wait while we discover your Roku devices.  Discovery can take a few minutes, so sit back and relax. The page will reload automatically! Select your Roku below once discovered.") {
			input "selectedDevices", "enum", required:false, title:"Select Roku Devices to install (${numFound} found)", multiple:true, options:options, submitOnChange: true
		}
	}    
}

def addSelectedDevices(params) {
    if (!selectedDevices)
        return deviceDiscovery()
    
    def subject = selectedDevices.size == 1 ? "device" : "devices"
    def title = ""
    def sectionText = ""
    
    def appId = app.getId()
    def devices = selectedDevices.collect { it }  // clone the list, so as to not accidentally modify it
    def deviceCount = devices.size()
    
    selectedDevices.each { rokuId -> 
        def name = state.discovered[rokuId].name
        def dni = state.discovered[rokuId].mac
        try {
            
            def child = addChildDevice("apwelsh", "Roku TV", dni, ["name": "${name}", "label": "${name}"])
            child.updateSetting("deviceIp", state.discovered[rokuId].networkAddress)
            child.updateSetting("autoManage", true)
            child.updateSetting("manageApps", false)
            child.updateSetting("manageApps", false)
            devices.remove(rokuId)
            
        } catch (ex) {
            if (ex.message =~ "A device with the same device network ID exists.*") {
                sectionText = """\nA device with the same device network ID (${dni}) already exists; cannot add Group [${name}]"""
            } else {
                sectionText += """\nFailed to add group [${name}]; see logs for details"""
                if (logEnable) log.error "${ex}"
            }
        }
    }

    if (devices.size() == 0)
        app.removeSetting("selectedDevices")
    
    if (!sectionText) {
        title = "Adding ${deviceCount} Roku ${subject} to Hubitat"
        sectionText = """Added Roku ${subject}"""
    } else {
        title = "Failed to add Roku ${subject}"
    }
    
	return dynamicPage(name:"addSelectedDevices", title:title, install: false, uninstall: flase, nextPage:"mainPage") {
		section() {
            paragraph sectionText
		}
	}

}

def configureDevice(params) {
    log.info "Configure Device: params = ${params}"
    def networkId = params?.netId
    if (!networkId) return mainPage()
    
    def child = getChildDevice(networkId)
    if (!child)
        return mainPage()
    
    def label = child.label
    def newLabel = settings["${networkId}_label"]
    if (newLabel) {
        if (label != settings["${networkId}_label"]) { 
            renameChildDevice(this, networkId, settings["${networkId}_label"])
            app.removeSetting("${networkId}_label")
            label = settings["${networkId}_label"]
        }
    }
    
    return dynamicPage(name:"configureDevice", title:"Configure device", nextPage:null, params: params) {
        section() {
            paragraph "Use this section to configure your Roku device settings"
            input "${networkId}_label", "text", title: "Device name", defaultValue:label, submitOnChange: true
        }
        section("Installed Applications") {
            href "appDiscovery", title:"Add/Remove Roku Apps", description:"", params: params
            paragraph "Manage installed apps"
            child.getChildDevices().sort({ a, b -> a["label"] <=> b["label"] }).each {
                def desc = it.label != it.name ? it.name : ""
                href "manageApp", title:desc, description:"", params: [netId: networkId, appId: it.deviceNetworkId]
            }
        }
    }
}

def manageApp(params) {
    log.info "Params: ${params}"
    def networkId = params?.netId
    def appId = params?.appId
    if (!networkId || !appId) return configureDevice(params)
    
    def child = getChildDevice(networkId)
    def app = child?.getChildDevice(appId)
    log.info app.name
    log.info app.label
    
    def label = app.label
    def newLabel = settings["${appId}_label"]
    if (newLabel) {
        if (label != settings["${appId}_label"]) { 
            renameChildDevice(child, appId, settings["${appId}_label"])
            app.removeSetting("${appId}_label")
            label = settings["${appId}_label"]
        }
    }
    
    return dynamicPage(name: "manageApp", title:"Manage Installed Apps for ${child.label}", nextPage:"configureDevice", params: [netId: networkId]) {
        section() {
            paragraph "Use this section to set the ${app.name} application name for ${child.label}"
            input "${appId}_label", "text", title: "Application name", defaultValue:app.label, submitOnChange: true
        }    
    }
    
}

def appDiscovery(params) {
    def networkId = params?.netId
    if (!networkId) return mainPage()
    
    def child = getChildDevice(networkId)
    if (!child)
        return mainPage()
    
    def apps = child.getInstalledApps()

    log.debug "Sending: ${params}"
    return dynamicPage(name:"appDiscovery", title:"Discover Apps", nextPage:"installApps", params: params) {
        section() {
            paragraph ""
        }
        section("${child.label} Applications") {
            input "${networkId}_selectedApps", "enum", title: "Select Roku Apps to publish as switches", required: flase, multiple: true, options: apps, submitOnChange: true
        }
    }
}

def installApps(args) {
    // For some reason, params always arrive as null in this function
    // def networkId = params?.netId
    // if (!networkId) return mainPage()
    log.debug "Received: ${args}"
    def nids = apps.collect { key, value -> key}.findAll { child.getChildDevice(it) != null }

    def selected = settings["selectedApps"]
    selected.each { networkID ->
        
    }
    
    return dynamicPage(name:"installApps", title:"Updating child devices", nextPage:"mainPage") {
        section() {
            paragraph "Nothing to do"
        }
    }
}

/*
 * SSDP Device Discover
 */

void ssdpSubscribe() {
    subscribe(location, "ssdpTerm.roku:ecp", ssdpHandler)
}

void ssdpUnsubscribe() {
    unsubscribe(ssdpHandler)
}


void ssdpDiscover() {
    sendHubCommand(new hubitat.device.HubAction("lan discovery roku:ecp", hubitat.device.Protocol.LAN))
}

def ssdpHandler(event) {
    
    def parsedEvent = parseLanMessage(event.description)    
    def ssdpPath = parsedEvent.ssdpPath
    
    def roku = parsedEvent?.ssdpUSN.replaceAll(~/.*\:/,"")
    if (parsedEvent.networkAddress) {
        parsedEvent << ["roku":roku, 
                        "networkAddress": convertHexToIP(parsedEvent.networkAddress),
                        "deviceAddress": convertHexToInt(parsedEvent.deviceAddress)]

        def ssdpUSN = parsedEvent.ssdpUSN.toString()
        
        def discovered = getDiscovered()
        if (!discovered."${ssdpUSN}") {
            verifyDevice(parsedEvent)
        } else {
            updateDevice(parsedEvent)
        }
    }
}

private verifyDevice(event) {
   def ssdpPath = event.ssdpPath
    
    log.info "Verifying ${event.networkAddress}"
    
    // Using the httpGet method, and arrow function, perform the validation check w/o the need for a callback function.
    httpGet("http://${event.networkAddress}:${event.deviceAddress}${ssdpPath}") { response ->
        
        if (!response.isSuccess()) {return}

        def data = response.data
        if (data) {
            def device = data.device
            def model = device.modelName
            def ssdpUSN = "${event.ssdpUSN.toString()}"

            if (logEnable) log.debug "Identified model: ${model}"
            def hubId = "${event.mac}"[-6..-1]
            def name = "${device.friendlyName} (${hubId})"

            event << ["url":"${data.URLBase}", 
                            "name":"${name}", "serialNumber":"${device.serialNumber}"]

            def discovered = getDiscovered()
            discovered << ["${ssdpUSN}": event]
            if (logEnable) log.debug "Discovered new Roku: ${name}"
        }

    }}

private updateDevice(event) {
    def ssdpUSN = event.ssdpUSN.toString()
    def discovered = getDiscovered()
    def roku = discovered["${ssdpUSN}"]

    if (roku.networkAddress != event.networkAddress || roku.deviceAddress != event.deviceAddress) {
        def oldAddress = roku.deviceAddress
        roku << ["networkAddress": event.networkAddress,
                   "deviceAddress": event.deviceAddress]
        
        if (logEnable) log.debug "Detected roku address update: ${device.name} from ${oldAddress} to ${event.deviceAddress}"
    }
    
    def child = getChildDevice(roku.mac)
    if (child) {
        child.updateSetting("deviceIp", roku.networkAddress)
    }
}

private getDiscovered() {
    state.discovered = state.discovered ?: [:]
}

def getRokuForMac(mac) {
    getDiscovered().find{ key, value -> value.mac == mac}?.value
}

def renameChildDevice(parent, networkId, name) {
    if (networkId) {
        def child = parent.getChildDevice(networkId)
        if (logEnable) log.info "Renaming ${child.label} to ${name}"
        child.label = settings["${networkId}_label"]
    }
}



/*
 * Device Helpers
 */

private String convertHexToIP(hex) {
	[hubitat.helper.HexUtils.hexStringToInt(hex[0..1]),
     hubitat.helper.HexUtils.hexStringToInt(hex[2..3]),
     hubitat.helper.HexUtils.hexStringToInt(hex[4..5]),
     hubitat.helper.HexUtils.hexStringToInt(hex[6..7])].join(".")
}

private Integer convertHexToInt(hex) {
    hubitat.helper.HexUtils.hexStringToInt(hex)
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}




