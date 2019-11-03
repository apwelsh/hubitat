/**
 * Advanced Philips Hue Bridge Integration application
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a parent application for locating your Philips Hue Bridges, and installing
 * the Advanced Hue Bridge Controller application
  *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *-------------------------------------------------------------------------------------------------------------------
 **/
definition(
    name:"Advanced Hue Bridge Integration",
    namespace: "apwelsh",
    author: "Armand Welsh",
    description: "Find and install your Philips Hue Bridge systems",
    category: "Convenience",
    //importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/app/hue-bridge-integration.groovy",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)
    
preferences {
    page(name: "routerPage")
	page(name: "mainPage")
    page(name: "bridgeDiscovery", title: "Device Discovery", content: "bridgeDiscovery", refreshTimeout:10)
    page(name: "addDevice", title: "Add Hue Bridge", content: "addDevice")
	page(name: "bridgeBtnPush", title:"Linking with your Hue", content:"bridgeLinking", refreshTimeout:5)
    page(name: "configurePDevice")
    page(name: "findGroups", title: "Group Discovery Started!", content: "findGroups", refreshTimeout:10)
    page(name: "addGroups", title: "Add Group")
    page(name: "findScenes", title: "Scene Discovery Started!", content: "findScenes", refreshTimeout:10)
    page(name: "addScenes", title: "Add Scene")

}

def routerPage() {
    if (!state) {
        return dynamicPage(name: "routerPage", title: "Advanced Hue Bridge Integration", nextPage: null, uninstall: true, install: true) {
            section {
                paragraph "Hit Done to to install the Hue Bridge Integration.\nRe-open to setup."
            }
        }
    } else if (!state.bridgeHost) {
        return bridgeDiscovery()
    } else {
         return mainPage()   
    }
}

def mainPage(params=[:]) {
    
    def hub = getHubs().find { it.value.mac == selectedDevice }?.value
    if (params[nextPage]=="bridgeBtnPush") {
        return bridgeLinking()
    }
    
    def title
    if (selectedDevice) {
        discoveredHubs()[selectedDevice]
        title=discoveredHubs()[selectedDevice]
    } else {
        title="Find Bridges"
    }
    
	dynamicPage(name: "mainPage", title: "Manage your linked Hue Bridge", nextPage: null, uninstall: true, install: true) {
        if (selectedDevice == null) {
            section("Setup"){
                paragraph """ To begin, select Find Bridges to start searching for your Hue Bride."""
                href "bridgeDiscovery", title:"Find Bridges", description:""//, params: [pbutton: i]
            }
        } else {
            section("Configure"){
                
                href "findGroups", title:"Find Groups", description:""
                href "findScenes", title:"Find Scenes", description:""
                href "bridgeDiscovery", title:title, description:"", state:selectedDevice? "complete" : null //, params: [nextPage: "bridgeBtnPush"]

               // getChildDevices().sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).each {
                    //if(it.typeName == "Advanced Hue Bridge"){
                    //    href "configurePDevice", title:"$it.label", description:"", params: [did: it.deviceNetworkId]
                    //}

                //}
            }    
        }
    }
}

def bridgeDiscovery(params=[:]) {
	log.debug "Searching for Hub additions and updates"
	def hubs = discoveredHubs()
    
	int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
	state.deviceRefreshCount = deviceRefreshCount + 1
	def refreshInterval = 10
    
	def options = hubs ?: []
	def numFound = options.size() ?: 0

	//if ((numFound == 0 && state.deviceRefreshCount > 25) || params.reset == "true") {
    //	log.trace "Cleaning old device memory"
    //	clearDiscoveredHubs()
    //    state.deviceRefreshCount = 0
    //}

	ssdpSubscribe()

	//bridge discovery request every 5th refresh, retry discovery
	if((deviceRefreshCount % 5) == 0) {
		ssdpDiscover()
	}

	return dynamicPage(name:"bridgeDiscovery", title:"Discovery Started!", nextPage:"bridgeBtnPush", refreshInterval:refreshInterval, uninstall: true) {
		section("Please wait while we discover your Hue Bridge. Note that you must first configure your Hue Bridge and Lights using the Philips Hue application. Discovery can take five minutes or more, so sit back and relax, the page will reload automatically! Select your Hue Bridge below once discovered.") {
			input "selectedDevice", "enum", required:false, title:"Select Hue Bridge (${numFound} found)", multiple:false, options:options
		}
	}
}

def bridgeLinking() {
    ssdpUnsubscribe()
    
	int linkRefreshcount = !state.linkRefreshcount ? 0 : state.linkRefreshcount as int
	state.linkRefreshcount = linkRefreshcount + 1
	def refreshInterval = 3

	def nextPage = ""
	def title = "Linking with your Hue"
    def paragraphText
    
	if (selectedDevice) {
		paragraphText = "Press the button on your Hue Bridge to setup a link. "
        
        def hub = getHubForMac(selectedDevice)
        if (hub?.username) { //if discovery worked
            log.debug "Hub linking completed for ${hub.name}"
            return addDevice(hub)
        }
        if (hub?.networkAddress) {
            state.bridgeHost = hub.networkAddress
        }

        if (hub) {
            if((linkRefreshcount % 2) == 0 && !state.username) {
                requestHubAccess(selectedDevice)
            }
        }

    } else {
    	paragraphText = "You haven't selected a Hue Bridge, please Press \"Done\" and select one before clicking next."
    }

	return dynamicPage(name:"bridgeBtnPush", title:title, nextPage:nextPage, refreshInterval:refreshInterval) {
		section("") {
			paragraph """${paragraphText}"""
		}
	}
}

def addDevice(device) {
    def sectionText = "Linking to your hub was a success! Please click 'Next'!\r\n"
    def title = "Success"
    def dni = deviceNetworkId(device.mac)
    
    def d
    if (device) {
        d = getChildDevices()?.find {
            it.deviceNetworkId == dni
        }
        state.bridgeHost = "${device.networkAddress}:${device.deviceAddress}"
        state.username = "${device.username}"
        getHubStatus()
    }

    if (!d && device != null) {
        log.debug "Creating Hue Bridge device with dni: ${dni}"
        try {
            addChildDevice("apwelsh", "AdvancedHueBridge", dni, null, ["label": device.name])
        } catch (ex) {
            if (ex.message =~ "A device with the same device network ID exists.*") {
                sectionText = "Cannot add hub.  A device with the same device network ID already exists."
                title = "Problem detected"
                state.remove("bridgeHost")
            }
        }
    }
        
    
    return dynamicPage(name:"addDevice", title:title, nextPage:"mainPage",  uninstall: false) {
        section() {
            paragraph sectionText
        }
    }    
}

def findGroups(params){
    enumerateGroups()
    
    def installed = getInstalledGroups().collect { it.label }
    def dnilist = getInstalledGroups().collect { it.deviceNetworkId }

    def options = [:]
    def groups = state.groups
    if (groups) {
        groups.each {key, value ->
            def lights = value.lights ?: []
            if ( lights.size()  == 0 ) return
            if ( dnilist.find { it == networkIdForGroup(key) }) return
            options["${key}"] = "${value.name} (${value.type})"
        }
    }

    def numFound = options.size()
    def refreshInterval = numFound == 0 ? 30 : 120

	return dynamicPage(name:"findGroups", title:"Group Discovery Started!", nextPage:"addGroups", refreshInterval:refreshInterval, uninstall: true) {
		section("""Let's find some groups.  Please click the ""Refresh Group Discovery"" Button if you aren't seeing your Groups / Rooms.""") {
			input "selectedGroups", "enum", required:false, title:"Select additional rooms / zones to add (${numFound} available)", multiple:true, options:options
            if (!installed.isEmpty()) {
                paragraph "Previously added Hue Groups"
                paragraph "[${installedGroups.join(', ')}]"
            }
		}
	}

}

def addGroups(params){

    if (!selectedGroups)
        return findGroups()
    
    def subject = selectedGroups.size == 1 ? "Group" : "Groups"
    
    def title = ""
    def sectionText = ""
    
    def appId = app.getId()
    def groups = selectedGroups.collect { it }
    
    selectedGroups.each { groupId -> 
        def name = state.groups[groupId].name
        def dni = networkIdForGroup(groupId)
        try {
            
            addChildDevice("apwelsh", "AdvancedHueGroup", dni, null, ["label": "${name}"])
            
            groups.remove(groupId)
            
        } catch (ex) {
            if (ex.message =~ "A device with the same device network ID exists.*") {
                sectionText = """\nA device with the same device network ID (${dni}) already exists; cannot add Group [${name}]"""
            } else {
                sectionText += """\nFailed to add group [${name}]; see logs for details"""
                log.error "${ex}"
            }
        }
    }
    
    if (groups.size() == 0)
        app.removeSetting("selectedGroups")
    
    if (!sectionText) {
        title = "Adding ${subject} to Hubitat"
        sectionText = """Added Groups"""
    } else {
        title = "Failed to add Group"
    }
    
	return dynamicPage(name:"addGroups", title:title, nextPage:"mainPage", refreshInterval:refreshInterval, uninstall: true) {
		section() {
            paragraph sectionText
		}
	}

}

def findScenes(params){
    enumerateScenes()
    
    def groupOptions = [:]
    getInstalledGroups().each { groupOptions[deviceIdNode(it.deviceNetworkId)] = it.label }
    
    //def installed = getInstalledScenes().collect { it.label }
    //def dnilist = getInstalledScenes().collect { it.deviceNetworkId }
    //log.info "${dnilist}"

    def options = [:]
    def scenes
    def group
    def installed
    def dnilist
    if (selectedGroup) {
        group = getChildDevice(networkIdForGroup(selectedGroup))
        if (group) {
            group.getChildDevices().each {log.info it}
            installed = group.getChildDevices().collect { it.label }
            dnilist = group.getChildDevices()?.collect { it.deviceNetworkId }
        }
        scenes = state.scenes?.findAll { it.value.type == "GroupScene" && it.value.group == selectedGroup }
    }    
    if (scenes) {
        scenes.each {key, value ->
            def groupName = groupOptions."${selectedGroup}"
            def lights = value.lights ?: []
            if ( lights.size()  == 0 ) return
            if ( dnilist.find { it == networkIdForScene(selectedGroup, key) } ) return
            options["${key}"] = "${value.name} (${value.type} [${groupName}])"
        }
    }

    def numFound = options.size()
    def refreshInterval = numFound == 0 ? 30 : 120

	return dynamicPage(name:"findScenes", title:"Scene Discovery Started!", nextPage:"addScenes", refreshInterval:refreshInterval, uninstall: true) {
		section("""Let's find some scenes.  Please click the ""Refresh Scene Discovery"" Button if you aren't seeing your Scenes.""") {
            input "selectedGroup",  "enum", required:true,  title:"Select the group to add scenes to (${groupOptions.size()} installed)", multiple:false, options:groupOptions, submitOnChange: true
            if (selectedGroup) {
                input "selectedScenes", "enum", required:false, title:"Select additional scenes to add (${numFound} available)", multiple:true, options:options
                if (!installed.isEmpty()) {
                    paragraph "Previously added Hue Scenes for ${group.label}"
                    paragraph "[${installed.join(', ')}]"
                }
            }
		}
	}

}


def addScenes(params){

    if (!selectedScenes)
        return findScenes()
    
    def group = getChildDevice(networkIdForGroup(selectedGroup))
    
    def subject = selectedScenes.size == 1 ? "Scene" : "Scenes"
    
    def title = ""
    def sectionText = ""
    
    def appId = app.getId()
    def scenes = selectedScenes.collect { it }
    
    selectedScenes.each { sceneId -> 
        def name = "${group.label} - ${state.scenes[sceneId].name}"
        def dni = networkIdForScene(selectedGroup, sceneId)
        try {
            
            group.addChildDevice("apwelsh", "AdvancedHueScene", "${dni}", ["label": "${name}"])
            
            scenes.remove(sceneId)
            
        } catch (ex) {
            if (ex.message =~ "A device with the same device network ID exists.*") {
                sectionText = """\nA device with the same device network ID (${dni}) already exists; cannot add Scene [${name}]"""
            } else {
                sectionText += """\nFailed to add scene [${name}]; see logs for details"""
                log.error "${ex}"
            }
        }
    }
    
    if (scenes.size() == 0)
        app.removeSetting("selectedScenes")
    
    if (!sectionText) {
        title = "Adding ${subject} to Hubitat"
        sectionText = """Added Scenes"""
    } else {
        title = "Failed to add Scene"
    }
    
	return dynamicPage(name:"addScenes", title:title, nextPage:"mainPage", refreshInterval:refreshInterval, uninstall: true) {
		section() {
            paragraph sectionText
		}
	}

}



// Life Cycle Functions

def installed() {
    log.debug "Installed with settings: ${settings}"
    state.bridgeRefreshCount = 0
    state.bulbRefreshCoun    = 0
    state.inBulbDiscovery    = 0
}

def uninstalled() {
    unsubscribe()
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    ssdpSubscribe()
    runEvery5Minutes("ssdpDiscover")    
}

def subscribe() {
    // Add message subscriptions for devices, if needed
}


/*
 * SSDP Device Discover
 */

void ssdpSubscribe() {
    subscribe(location, "ssdpTerm.upnp:rootdevice", ssdpHandler)
}

void ssdpUnsubscribe() {
    unsubscribe(ssdpHandler)
}


void ssdpDiscover() {
    sendHubCommand(new hubitat.device.HubAction("lan discovery upnp:rootdevice", hubitat.device.Protocol.LAN))

}

def ssdpHandler(evt) {
    def description = evt.description
    def parsedEvent = parseLanMessage(description)    
    def ssdpPath = parsedEvent.ssdpPath
    
    // The Hue bridge publishes the device information in /description.xml, so if this ssdpPath does not match, skip this device.
    if (ssdpPath != "/description.xml")
        return
    
    def hub = evt?.hubId
    if (parsedEvent.networkAddress) {
        parsedEvent << ["hub":hub, 
                        "networkAddress": convertHexToIP(parsedEvent.networkAddress),
                        "deviceAddress": convertHexToInt(parsedEvent.deviceAddress)]

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
        device << ["networkAddress": parsedEvent.networkAddress,
                   "deviceAddress": parsedEvent.deviceAddress]
        
        log.debug "Discovered hub address update: ${device.name}"
    }
}

void verifyDevice(parsedEvent) {
    def ssdpPath = parsedEvent.ssdpPath
    
    // The Hue bridge publishes the device information in /description.xml, so if this ssdpPath does not match, skip this device.
    if (ssdpPath != "/description.xml")
        return

    // Using the httpGet method, and arrow function, perform the validation check w/o the need for a callback function.
    def url = "http://${parsedEvent.networkAddress}:${parsedEvent.deviceAddress}${ssdpPath}"
    asynchttpGet(verifyDeviceHandler, [uri: url], parsedEvent)
}

def verifyDeviceHandler(response, parsedEvent) {
    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return
    
    def data = response.getXml()
    if (data) {
        def device = data.device
        def model = device.modelName
        def ssdpUSN = "${parsedEvent.ssdpUSN.toString()}"

        log.debug "Identified model: ${model}"
        if (model =~ 'Philips hue bridge.*') {
            def hubId = "${parsedEvent.mac}"[-6..-1]
            def name = "${device.friendlyName}".replaceAll(~/\(.*\)/, "(${hubId})")

            parsedEvent << ["url":"${data.URLBase}", 
                            "name":"${name}", "serialNumber":"${device.serialNumber}"]

            def hubs = getHubs()
            hubs << ["${ssdpUSN}": parsedEvent]
            log.debug "Discovered new hub: ${name}"
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
    def deviceType = "{\"devicetype\": \"AdvanceHueBridgeLink#Hubitat\"}"
    
    asynchttpPost(requestHubAccessHandler,
                  [uri: "http://${device.networkAddress}/api",
                   contentType: "application/json",
                   requestContentType: "application/json",
                   body: deviceType], [device: device])

}

def requestHubAccessHandler(response, args) {
    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return
    
    def data = response.getJson()

    if (data) {
        if (data.error?.description) {
            log.error "${data.error.description[0]}"
        } else if (data.success) {
            if (data.success.username) {
                def device = getHubForMac(args.device.mac)
                device << [username: "${data.success.username[0]}"]
                log.debug "Obtained credentials: ${device}"
            } else {
                log.error "Problem with hub linking.  Received response: ${data}"
            }
        } else {
            log.error $data
        }

    }
}
private getHubStatus() {
    
    def url = "http://${state.bridgeHost}/api/${state.username}"
    
    asynchttpGet(getHubStatusHandler,
                  [uri: url,
                   contentType: "application/json",
                   requestContentType: "application/json"])

}

def getHubStatusHandler(response, args) {
    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return
    
    def data = response.getJson()
    if (data) {
        if (data.error?.description) {
            log.error "${data.error.description[0]}"
        } else {
            parseStatus(data)
        }
    }
}

private enumerateGroups() {
    
    def url = "http://${state.bridgeHost}/api/${state.username}/groups"
    //log.debug "${url}"
    
    asynchttpGet(enumerateGroupsHandler,
                  [uri: url,
                   contentType: "application/json",
                   requestContentType: "application/json"])

}

def enumerateGroupsHandler(response, args) {
    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return
    
    def data = response.getJson()
    if (data) {
        if (data.error?.description) {
            log.error "${data.error.description[0]}"
        } else {
            if (data) state.groups = data
            parseGroups(data)
        }
    }
}

private enumerateScenes() {
    
    def url = "http://${state.bridgeHost}/api/${state.username}/scenes"
    //log.debug "${url}"
    
    asynchttpGet(enumerateScenesHandler,
                  [uri: url,
                   contentType: "application/json",
                   requestContentType: "application/json"])

}

def enumerateScenesHandler(response, args) {
    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return
    
    def data = response.getJson()
    if (data) {
        if (data.error?.description) {
            log.error "${data.error.description[0]}"
        } else {
            if (data) state.scenes = data
            parseScenes(data)
        }
    }
}

private enumerateLights() {
    
    def url = "http://${state.bridgeHost}/api/${state.username}/lights"
    //log.debug "${url}"
    
    asynchttpGet(enumerateScenesHandler,
                  [uri: url,
                   contentType: "application/json",
                   requestContentType: "application/json"])

}

def enumerateLightsHandler(response, args) {
    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return
    
    def data = response.getJson()
    if (data) {
        if (data.error?.description) {
            log.error "${data.error.description[0]}"
        } else {
            if (data) state.lights = data
            parseLights(data)
        }
    }
}

def setDeviceState(child, deviceState) {

    def deviceNetworkId = child.device.deviceNetworkId
    def hubId = deviceIdHub(deviceNetworkId)
    if ("${hubId}" != "${app.getId()}") {
        log.warn "Received setDeviceState request for invalid hub ID ${hubId}.  Current hub ID is ${app.getId()}"
        return
    }
    def type = deviceIdType(deviceNetworkId)
    def node = deviceIdNode(deviceNetworkId)
    
    def url = "http://${state.bridgeHost}/api/${state.username}/${type}/${node}/action"
    log.debug "URL: ${url}"
    log.debug "args: ${deviceState}"
    asynchttpPut(setDeviceStateHandler,
                  [uri: url,
                   contentType: "application/json",
                   requestContentType: "application/json", 
                   body: deviceState],
                  ["childDevice": child,
                   "type": type,
                   "node": node])
}

def setDeviceStateHandler(response, args) {
    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return
    
    def data = response.getJson()
    if (data) {
//        log.info data
        data.each { 
            if (it.error?.description) {
                log.error "${it.error.description[0]}"
            }
            if (it.success) {
                log.info it.success
                it.success.each { key, value ->
                    
                    def result = key.split('/')
                    def nid
                    switch (result[1]) {
                        case "groups":  nid=networkIdForGroup(result[2]); break;
                        case "lights":  nid=networkIdForLight(result[2]); break;
                        case "scenes":  nid=networkIdForScene(result[2]); break;
                        default:
                            log.warn "Unhandled device state handler repsonse: ${result}"
                        return;
                    }
                    def device = getChildDevice(nid)
                    device.setHueProperty(result[4],value)
                }
            }
        }
    }
}


/*
 *
 * Private helper functions
 *
 */

private networkIdForGroup(id) {
    "hueGroup:${app.getId()}/${id}"
}
private networkIdForLight(id) {
    "${app.getId()}/${id}"
}
private networkIdForScene(groupId,sceneId) {
    "hueScene:${app.getId()}/${groupId}/${sceneId}"
}



private getInstalledGroups() {
    getChildDevices().findAll { it.deviceNetworkId =~ 'hueGroup:.*' }
}

private getInstalledScenes() {
    getChildDevices().findAll { it.deviceNetworkId =~ 'hueScene:.*' }
}


private String deviceIdType(deviceNetworkId) {
    switch (deviceNetworkId) {
        case ~/hueGroup:.*/:    "groups"; break
        case ~/hueScene:.*/:    "scenes"; break
        case ~/hueBulb:.*/:
        case ~/hueBulbRGBW:.*/: "lights"; break
    }
}

private String deviceIdNode(deviceNetworkId) {
    deviceNetworkId.replaceAll(~/.*\//,"")
}

private String deviceIdHub(deviceNetworkId) {
    deviceNetworkId.replaceAll(~/.*\:/,"").replaceAll(~/\/.*/,"")
}


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

/*
 *
 * Hub Device Utility Functions
 *
 */

def deviceNetworkId(mac) {
    mac ? "hue-${mac}" : null
}

private getHubForNetworkId(deviceNetworkId) {
    getHubs().find { it.value.deviceNetworkId == deviceNetworkId }   
}

private getChildDeviceForMac(mac) {
    getChildDevices()?.find {it.deviceNetworkId == t(mac)  }    
}

private clearDiscoveredHubs() {
    state.hubs = getHubs().find { getChildDeviceForMac(it.value.mac) != null }
}

Map discoveredHubs() {
	def vhubs = getHubs()
	def map = [:]
    vhubs.each {
		def value = "${it.value?.name}"
		def key = "${it.value?.mac}"
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

def refreshHubStatus(dni) {
    def hub = getHubs().find { deviceNetworkId(it.value.mac) == dni }.value
    if (!hub) {
        return
    }
    def url = "${hub.url}/api/${hub.username}"
    httpGet(url) { response ->
        if (response.data) {
            getChildDevice(dni).parse(response.data)
        }
    }
}

/*
 *
 * Device Parsers
 *
 */

def parseStatus(data) {
    
    def groups = data.groups
    def lights = data.lights
    def scenes = data.scenes
    
    if (groups) parseGroups(groups)
    if (lights) parseLights(lights)
    if (scenes) parseScenes(scenes)
    
}

def parseGroups(data) {
    
    data.each { id, value -> 
        //log.debug "${id}"
        // Add code to update all installed groups state
    }
}

def parseLights(data) {
    data.each { id, value -> 
        //log.debug "${id}"
        
    }
}

def parseScenes(data) {
    data.each { id, value -> 
        //log.debug "${id}"
        
    }
}

def findScene(group, scene) {
    if (state.scenes."${scene}") {
        return scene
    } else {
        return state.scenes.find{ it.value.group == group && it.value.name == scene }?.key
    }
}



