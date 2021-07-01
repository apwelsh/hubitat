/**
 * Advanced Philips Hue Bridge Integration application
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a parent application for locating your Philips Hue Bridges, and installing
 * the Advanced Hue Bridge Controller application
 *-------------------------------------------------------------------------------------------------------------------
 * Copyright 2020 Armand Peter Welsh
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
@Field static final String PAGE_ADD_DEVICE = 'addDevice'
@Field static final String PAGE_BRIDGE_LINKING = 'bridgeLinking'
@Field static final String PAGE_FIND_LIGHTS = 'findLights'
@Field static final String PAGE_ADD_LIGHTS = 'addLights'
@Field static final String PAGE_FIND_GROUPS = 'findGroups'
@Field static final String PAGE_ADD_GROUPS = 'addGroups'
@Field static final String PAGE_FIND_SCENES = 'findScenes'
@Field static final String PAGE_ADD_SCENES = 'addScenes'

@Field static Map volatileAtomicStateByDeviceId = new ConcurrentHashMap()

preferences {
    page(name: PAGE_MAINPAGE)
    page(name: PAGE_BRIDGE_DISCOVERY, title: 'Device Discovery', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_ADD_DEVICE,       title: 'Add Hue Bridge')
    page(name: PAGE_BRIDGE_LINKING,   title: 'Linking with your Hue', refreshTimeout:5)
    page(name: PAGE_FIND_LIGHTS,      title: 'Light Discovery Started!', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_ADD_LIGHTS,       title: 'Add Light')
    page(name: PAGE_FIND_GROUPS,      title: 'Group Discovery Started!', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_ADD_GROUPS,       title: 'Add Group')
    page(name: PAGE_FIND_SCENES,      title: 'Scene Discovery Started!', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_ADD_SCENES,       title: 'Add Scene')

}

synchronized Map getVolatileAtomicState(def device) {
    Integer deviceId = device.deviceId ?: device.device.deviceId
    Map result = volatileAtomicStateByDeviceId.get(deviceId)
    if (result == null) {
        result = new ConcurrentHashMap()
        volatileAtomicStateByDeviceId[deviceId] = result
    }
    return result
}

def mainPage(Map params=[:]) {

    if (app.installationState == 'INCOMPLETE') {
        return dynamicPage(name: PAGE_MAINPAGE, title: 'Advanced Hue Bridge Integration', nextPage: null, uninstall: true, install: true) {
            section {
                paragraph 'Hit Done to to install the Hue Bridge Integration.\nRe-open to setup.'
            }
        }
    }
    if (!state.bridgeHost) {
        return bridgeDiscovery()
    }

    if (params[nextPage]==PAGE_BRIDGE_LINKING) {
        return bridgeLinking()
    }

    String title
    if (selectedDevice) {
        discoveredHubs()[selectedDevice]
        title=discoveredHubs()[selectedDevice]
    } else {
        title='Find Bridges'
    }

    Boolean uninstall = (state.bridgehost) ? false : true

    return dynamicPage(name: PAGE_MAINPAGE, title: '', nextPage: null, uninstall: uninstall, install: true) {
            section (getFormat("title", "Advanced Hue Bridge")) {
                paragraph getFormat("subtitle", "Manager your linked Hue Bridge")
                paragraph getFormat("line")
            }

        if (selectedDevice == null) {
            section('Setup'){
                paragraph 'To begin, select Find Bridges to start searching for your Hue Bride.'
                href PAGE_BRIDGE_DISCOVERY, title:'Find Bridges', description:''//, params: [pbutton: i]
            }
        } else {
            section('Configure'){

                href PAGE_FIND_LIGHTS, title:'Find Lights', description:''
                href PAGE_FIND_GROUPS, title:'Find Groups', description:''
                href PAGE_FIND_SCENES, title:'Find Scenes', description:''
                href PAGE_BRIDGE_DISCOVERY, title:title, description:'', state:selectedDevice? 'complete' : null //, params: [nextPage: PAGE_BRIDGE_LINKING]

            // childDevices.sort({ a, b -> a['deviceNetworkId'] <=> b['deviceNetworkId'] }).each {
                    //if(it.typeName == 'Advanced Hue Bridge'){
                    //    href 'configurePDevice', title:"$it.label", description:'', params: [did: it.deviceNetworkId]
                    //}

                //}
            }
            section('Options') {
                input name: 'logEnable', type: 'bool', defaultValue: true,  title: 'Enable informational logging'
                input name: 'debug',     type: 'bool', defaultValue: false, title: 'Enable debug logging'
            }
            section() {
                paragraph getFormat("line")
                paragraph "<div style='color:#1A77C9;text-align:center'>Advanced Hue Bridge<br><a href='https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Please consider donating. This app took a lot of work to make.<br>If you find it valuable, I'd certainly appreciate it!</div>"
            }      
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

    Boolean uninstall = state.bridgehost ? false : true
    String nextPage = selectedDevice ? PAGE_BRIDGE_LINKING : null

    return dynamicPage(name:PAGE_BRIDGE_DISCOVERY, title:'Discovery Started!', nextPage:nextPage, refreshInterval:refreshInterval, uninstall:uninstall) {
        section('Please wait while we discover your Hue Bridge. Note that you must first configure your Hue Bridge and Lights using the Philips Hue application. Discovery can take five minutes or more, so sit back and relax, the page will reload automatically! Select your Hue Bridge below once discovered.') {
            input 'selectedDevice', 'enum', required:false, title:"Select Hue Bridge (${numFound} found)", multiple:false, options:options, submitOnChange: true
        }
    }
}

def bridgeLinking() {
    ssdpUnsubscribe()

    /* groovylint-disable-next-line DuplicateNumberLiteral */
    Integer linkRefreshcount = state.linkRefreshcount ?: 0
    state.linkRefreshcount = linkRefreshcount + 1
    Integer refreshInterval = 3

    String nextPage = ''
    String title = 'Linking with your Hue'
    String paragraphText

    if (selectedDevice) {
        paragraphText = 'Press the button on your Hue Bridge to setup a link. '

        def hub = getHubForMac(selectedDevice)
        if (hub?.username) { //if discovery worked
            if (logEnable) { log.debug "Hub linking completed for ${hub.name}" }
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
        paragraphText = 'You haven\'t selected a Hue Bridge, please Press \'Done\' and select one before clicking next.'
    }

    def uninstall = state.bridgehost ? false : true

    return dynamicPage(name:PAGE_BRIDGE_LINKING, title:title, nextPage:nextPage, refreshInterval:refreshInterval, uninstsall: uninstall) {
        section('') {
            paragraph "${paragraphText}"
        }
    }
}

def addDevice(device) {
    String sectionText = 'Linking to your hub was a success! Please click \'Next\'!\r\n'
    String title = 'Success'
    String dni = deviceNetworkId(device?.mac)

    if (logEnable) { log.info "Adding Bridge device with DNI: ${dni}" }

    def d
    if (device) {
        d = childDevices?.find { dev -> dev.deviceNetworkId == dni }
        state.bridgeHost = "${device.networkAddress}:${device.deviceAddress}"
        state.username = "${device.username}"
        refreshHubStatus()
    }

    if (!d && device != null) {
        if (logEnable) { log.debug "Creating Hue Bridge device with dni: ${dni}" }
        try {
            addChildDevice('apwelsh', 'AdvancedHueBridge', dni, null, ['label': device.name])
        } catch (ex) {
            if (ex.message =~ 'A device with the same device network ID exists.*') {
                sectionText = 'Cannot add hub.  A device with the same device network ID already exists.'
                title = 'Problem detected'
                state.remove('bridgeHost')
            }
        }
    }


    return dynamicPage(name:PAGE_ADD_DEVICE, title:title, nextPage:PAGE_MAINPAGE) {
        section() {
            paragraph sectionText
        }
    }
}

def findLights(){
    enumerateLights()

    List installed = getInstalledLights().collect { it.label }
    List dnilist   = getInstalledLights().collect { it.deviceNetworkId }

// TODO:
    Map options = [:]
    Map lights = state.lights
    if (lights) {
        lights.each {key, value ->
            // def lights = value.lights ?: []
            // if ( lights.size()  == 0 ) { return }
            if ( dnilist.find { dni -> dni == networkIdForLight(key) }) { return }
            options["${key}"] = "${value.name} (${value.type})"
        }
    }

    Integer numFound = options.size()
    Integer refreshInterval = numFound == 0 ? 30 : 120
    String nextPage = selectedLights ? PAGE_ADD_LIGHTS : null

    return dynamicPage(name:PAGE_FIND_LIGHTS, title:'Light Discovery Started!', nextPage:nextPage, refreshInterval:refreshInterval) {
        section('Let\'s find some groups.') {
            input 'selectedLights', 'enum', required:false, title:"Select additional lights to add (${numFound} available)", multiple:true, options:options, submitOnChange: true
            if (!installed.isEmpty()) {
                paragraph 'Previously added Hue Lights'
                paragraph "[${installedLights.join(', ')}]"
            }
        }
    }

}

def addLights(Map params=[:]){

    if (!selectedLights) {
        return findLights()
    }

    String subject = selectedLights.size == 1 ? 'Light' : 'Lights'

    String title = ''
    String sectionText = ''

    List lights = selectedLights.collect { it }

    selectedLights.each { lightId ->
        String name = state.lights[lightId].name
        String dni = networkIdForLight(lightId)
        String type = bulbTypeForLight(lightId)
        try {

            // addChildDevice('apwelsh', 'AdvancedHueGroup', dni, null, ['label': "${name}"])
            def child = addChildDevice('hubitat', "Generic Component ${type}", "${dni}",
            [label: "${name}", isComponent: false, name: 'AdvancedHueBulb'])
            child.updateSetting('txtEnable', false)
            lights.remove(lightId)

        } catch (ex) {
            if (ex.message =~ 'A device with the same device network ID exists.*') {
                sectionText = "\nA device with the same device network ID (${dni}) already exists; cannot add Light [${name}]"
            } else {
                sectionText += "\nFailed to add light [${name}]; see logs for details"
                log.error "${ex}"
            }
        }
    }

    if (lights.size() == 0) {
         app.removeSetting('selectedLights')
    }

    if (!sectionText) {
        title = "Adding ${subject} to Hubitat"
        sectionText = "Added ${subject}"
    } else {
        title = "Failed to add ${subject}"
    }

    return dynamicPage(name:PAGE_ADD_LIGHTS, title:title, nextPage:PAGE_MAINPAGE) {
        section() {
            paragraph sectionText
        }
    }

}

def findGroups(params){
    enumerateGroups()

    def installed = getInstalledGroups().collect { it.label }
    def dnilist = getInstalledGroups().collect { it.deviceNetworkId }

    Map options = [:]
    def groups = state.groups
    if (groups) {
        groups.each {key, value ->
            List lights = value.lights ?: []
            if ( !lights ) { return }
            if ( dnilist.find { dni -> dni == networkIdForGroup(key) }) { return }
            options["${key}"] = "${value.name} (${value.type})"
        }
    }

    def numFound = options.size()
    def refreshInterval = numFound == 0 ? 30 : 120
    def nextPage = selectedGroups ? PAGE_ADD_GROUPS : null

    return dynamicPage(name:PAGE_FIND_GROUPS, title:'Group Discovery Started!', nextPage:nextPage, refreshInterval:refreshInterval) {
        section('Let\'s find some groups.') {
            input 'selectedGroups', 'enum', required:false, title:"Select additional rooms / zones to add (${numFound} available)", multiple:true, options:options, submitOnChange: true
            if (!installed.isEmpty()) {
                paragraph 'Previously added Hue Groups'
                paragraph "[${installed.join(', ')}]"
            }
        }
    }

}

def addGroups(params){

    if (!selectedGroups) { return findGroups() }

    def subject = selectedGroups.size == 1 ? 'Group' : 'Groups'

    def title = ''
    def sectionText = ''

    def groups = selectedGroups.collect { it }

    selectedGroups.each { groupId ->
        String name = state.groups[groupId].name
        String dni = networkIdForGroup(groupId)
        try {

            addChildDevice('apwelsh', 'AdvancedHueGroup', dni, null, ['label': "${name}"])

            groups.remove(groupId)

        } catch (ex) {
            if (ex.message =~ 'A device with the same device network ID exists.*') {
                sectionText = "\nA device with the same device network ID (${dni}) already exists; cannot add Group [${name}]"
            } else {
                sectionText += "\nFailed to add group [${name}]; see logs for details"
                log.error "${ex}"
            }
        }
    }

    if (groups.size() == 0) { app.removeSetting('selectedGroups') }

    if (!sectionText) {
        title = "Adding ${subject} to Hubitat"
        sectionText = 'Added Groups'
    } else {
        title = 'Failed to add Group'
    }

    return dynamicPage(name:PAGE_ADD_GROUPS, title:title, nextPage:PAGE_MAINPAGE) {
        section() {
            paragraph sectionText
        }
    }

}

def scenesForGroupId(groupNetworkId) {
    def groupId = deviceIdNode(groupNetworkId)
    state.scenes?.findAll { it.value.type == 'GroupScene' && it.value.group == groupId }
}

def findScenes(params){
    enumerateScenes()

    Map groupOptions = [:]
    getInstalledGroups().each { groupOptions[deviceIdNode(it.deviceNetworkId)] = it.label }

    Map options = [:]
    def scenes
    def group
    def installed
    def dnilist
    if (selectedGroup) {
        group = getChildDevice(networkIdForGroup(selectedGroup))
        if (group) {
            installed = group.childDevices.collect { it.label }
            dnilist = group.childDevices?.collect { it.deviceNetworkId }
        }
        scenes = scenesForGroupId(selectedGroup)
    }
    if (scenes) {
        scenes.each {key, value ->
            def groupName = groupOptions."${selectedGroup}"
            def lights = value.lights ?: []
            if ( lights.size()  == 0 ) { return }
            if ( dnilist.find { dni -> dni == networkIdForScene(selectedGroup, key) } ) { return }
            options["${key}"] = "${value.name} (${value.type} [${groupName}])"
        }
    }

    def numFound = options.size()
    def refreshInterval = numFound == 0 ? 30 : 120

    def nextPage = selectedGroup && selectedScenes ? PAGE_ADD_SCENES : null

    return dynamicPage(name:PAGE_FIND_SCENES, title:'Scene Discovery Started!', nextPage:nextPage, refreshInterval:refreshInterval) {
        section('Let\'s find some scenes.  Please click the \'Refresh Scene Discovery\' Button if you aren\'t seeing your Scenes.') {
            input 'selectedGroup',  'enum', required:true,  title:"Select the group to add scenes to (${groupOptions.size()} installed)", multiple:false, options:groupOptions, submitOnChange: true
            if (selectedGroup) {
                input 'selectedScenes', 'enum', required:false, title:"Select additional scenes to add (${numFound} available)", multiple:true, options:options, submitOnChange: true
                if (installed && !installed.isEmpty()) {
                    paragraph "Previously added Hue Scenes for ${group.label}"
                    paragraph "[${installed.join(', ')}]"
                }
            }
        }
    }

}


def addScenes(params){

    if (!selectedScenes) { return findScenes() }

    def group = getChildDevice(networkIdForGroup(selectedGroup))

    def subject = selectedScenes.size == 1 ? 'Scene' : 'Scenes'

    def title = ''
    def sectionText = ''

    def scenes = selectedScenes.collect { it }

    selectedScenes.each { sceneId ->
        String name = "${group.label} - ${state.scenes[sceneId].name}"
        String dni = networkIdForScene(selectedGroup, sceneId)
        try {

            // group.addChildDevice('apwelsh', 'AdvancedHueScene', "${dni}', ['label': '${name}"])
            def child = group.addChildDevice('hubitat', 'Generic Component Switch', "${dni}",
            [label: "${name}", isComponent: false, name: 'AdvancedHueScene'])
            child.updateSetting('txtEnable', false)
            scenes.remove(sceneId)

        } catch (ex) {
            if (ex.message =~ 'A device with the same device network ID exists.*') {
                sectionText = "\nA device with the same device network ID (${dni}) already exists; cannot add Scene [${name}]"
            } else {
                sectionText += "\nFailed to add scene [${name}]; see logs for details"
                log.error "${ex}"
            }
        }
    }

    if (scenes.size() == 0) { app.removeSetting('selectedScenes') }

    if (!sectionText) {
        title = "Adding ${subject} to Hubitat"
        sectionText = 'Added Scenes'
    } else {
        title = 'Failed to add Scene'
    }

    return dynamicPage(name:PAGE_ADD_SCENES, title:title, nextPage:PAGE_MAINPAGE) {
        section() {
            paragraph sectionText
        }
    }

}



// Life Cycle Functions

def installed() {
    app.updateSetting('logEnable', true)
    app.updateSetting('debug', false)
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
    def deviceType = '{"devicetype": "AdvanceHueBridgeLink#Hubitat"}'

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
            if (data.success.username) {
                def device = getHubForMac(args.device.mac)
                device << [username: "${data.success.username[0]}"]
                if (logEnable) { log.debug "Obtained credentials: ${device}" }
            } else {
                log.error "Problem with hub linking.  Received response: ${data}"
            }
        } else {
            if (logEnable) { log.error $data }
        }

    }
}
void refreshHubStatus() {

    def url = "http://${state.bridgeHost}/api/${state.username}/"

    httpGet([uri: url,
             contentType: 'application/json',
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

private enumerateGroups() {

    def url = "http://${state.bridgeHost}/api/${state.username}/groups"
    //if (debug) { log.debug "${url}" }

    httpGet([uri: url,
            contentType: 'application/json',
            requestContentType: 'application/json']) { response ->
        if (!response.isSuccess()) { return }

        def data = response.data
        if (debug) { log.debug "enumerateGroups: ${data}" }
        if (data) {
            if (data.error?.description) {
                if (logEnable) { log.error "${data.error.description[0]}" }
            } else {
                if (data) { state.groups = data }
                parseGroups(data)
            }
        }

    }

}

private enumerateScenes() {

    def url = "http://${state.bridgeHost}/api/${state.username}/scenes"
    //if (debug) { log.debug "${url}" }

    httpGet([uri: url,
             contentType: 'application/json',
             requestContentType: 'application/json']) { response ->
        if (!response.isSuccess()) { return }

        def data = response.data
        if (data) {
            if (data.error?.description) {
                if (logEnable) { log.error "${data.error.description[0]}" }
            } else {
                if (data) { state.scenes = data }
                parseScenes(data)
            }
        }

    }

}

private enumerateLights() {

    def url = "http://${state.bridgeHost}/api/${state.username}/lights"
    //if (debug) { log.debug "${url}" }

    httpGet([uri: url,
             contentType: 'application/json',
             requestContentType: 'application/json']) { response ->
        if (!response.isSuccess()) { return }

        def data = response.data
        if (data) {
            if (data.error?.description) {
                if (logEnable) { log.error "${data.error.description[0]}" }
            } else {
                if (data) { state.lights = data }
                parseLights(data)
            }
        }
    }

}

@Field static Map eventQueue = null

void setDeviceState(def child, Map deviceState) {

    String deviceNetworkId = child.device.deviceNetworkId
    // Establish the eventQueue for the device.
    eventQueue = eventQueue ?: [(deviceNetworkId): deviceState]
    eventQueue[(deviceNetworkId)] = (eventQueue[(deviceNetworkId)] ?: [:]) + deviceState

    // If device state does not contain the value on, then determine if we must queue the state for a future call
    if (!deviceState.on && ! deviceState.scene) {
        if ((currentValue(child, 'switch') ?: 'on') == 'off') {
            return
        }
    }

    Map newState = deviceState.scene ? deviceState : eventQueue[(deviceNetworkId)]
    eventQueue.remove(deviceNetworkId)
    eventQueue = eventQueue ?: null // flush memory is list is empty, allows for garbage collection

    String hubId = deviceIdHub(deviceNetworkId)
    String type
    String node
    String action='action'

    if ( deviceNetworkId ==~ /hue\-[0-9A-F]{12}/ ) {
        type = 'groups'
        node = '0'
    } else if ("${hubId}" != "${app.getId()}") {
        log.warn "Received setDeviceState request for invalid hub ID ${hubId}.  Current hub ID is ${app.getId()}"
        return
    } else {
        type = "${deviceIdType(deviceNetworkId)}s"
        node = deviceIdNode(deviceNetworkId)
        if (type == 'lights') {
            action = 'state'
        }
    }

    // Disabling scheduled refreshes for hub.
    def hub = getChildDeviceForMac(selectedDevice)
    hub.resetRefreshSchedule()

    String url = "http://${state.bridgeHost}/api/${state.username}/${type}/${node}/${action}"
    if (debug) { log.debug "URL: ${url}" }
    if (debug) { log.debug "args: ${deviceState}" }
    httpPut([uri: url,
             contentType: 'application/json',
             requestContentType: 'application/json',
             body: newState]) { response ->

        if (!response.success) { return }

        response.data?.each { item ->
            if (item.error?.description) {
                if (debug) { log.error "set state: ${item.error.description[0]}" }

            }
            item.success?.each { key, value ->
                List result = key.split('/')
                type = result[1].replaceFirst('.$', '')
                String nid
                switch (type) {
                    case 'group':  nid = networkIdForGroup(result[2]); break
                    case 'light':  nid = networkIdForLight(result[2]); break
                    case 'scene':  nid = networkIdForScene(result[2]); break
                    default:
                        if (logEnable) { log.warn "Unhandled device state handler repsonse: ${result}" }
                        return
                }
                def device = type == 'group' && node == '0' ? getChildDevice(child.device.deviceNetworkId) : getChildDevice(nid)
                setHueProperty(device, [(result[3]): [(result[4]): value]])
                hub.runInMillis(500, 'refresh', [overwrite: true, misfire:'ignore'])

            }
        }
    }
}


void getDeviceState(def child) {
    String deviceNetworkId = child.device.deviceNetworkId
    String hubId = deviceIdHub(deviceNetworkId)
    String type
    String node

    if ( deviceNetworkId ==~ /hue\-[0-9A-F]{12}/ ) {
        type = 'groups'
        node = '0'
    } else if ("${hubId}" != "${app.getId()}") {
        log.warn "Received getDeviceState request for invalid hub ID ${hubId}.  Current hub ID is ${app.getId()}"
        return
    } else {
        type = "${deviceIdType(deviceNetworkId)}s"
        node = deviceIdNode(deviceNetworkId)
    }
    String url = "http://${state.bridgeHost}/api/${state.username}/${type}/${node}"
    if (debug) { log.debug "URL: ${url}" }
    httpGet([uri: url,
             contentType: 'application/json',
             requestContentType: 'application/json']) { response ->

        if (!response.success) { return }

        def data = response.data
        if (data) {
            if (debug) { log.debug "getDeviceState received data: ${data}" }  // temporary until lights are added, and all groups/all lights
            if ( data.error ) {
                if (logEnable) { log.error "${it.error.description[0]}" }
                return
            }
            if (node) {
                child = getChildDevice(deviceNetworkId)
                if (type == 'groups') {
                    child.resetRefreshSchedule()
                    data.state.remove('on')
                    data.action.remove('on')
                }
                setHueProperty(child, [state: data.state, action: data.action])
            } else {
                if (debug) { log.debug "Received unknown response: ${it}" }  // temporary to catch unknown result state
            }
        }

    }
}



/*
 *
 * Private helper functions
 *
 */

private String networkIdForGroup(id) {
    "hueGroup:${app.getId()}/${id}"
}

private String networkIdForLight(id) {
    def type=bulbTypeForLight(id)
    "hueBulb${type}:${app.getId()}/${id}"
}
String networkIdForScene(groupId,sceneId) {
    "hueScene:${app.getId()}/${groupId}/${sceneId}"
}

private String bulbTypeForLight(id) {
    if (!state.lights) { return null }
    def type=state.lights[id]?.type
    switch (type) {
        case 'Dimmable light':
            return 'Dimmer'
        case 'Extended color light':
            return 'RGBW'
        case 'Color temperature light':
            return 'CT'
        default:
            return 'RGB'
    }
}

private List getInstalledLights() {
    return childDevices.findAll { light -> light.deviceNetworkId =~ /hueBulb\w*:.*/ }
}

private List getInstalledGroups() {
    return childDevices.findAll { group -> group.deviceNetworkId =~ /hueGroup:.*/ }
}

// private getInstalledScenes() {
//     childDevices.findAll { it.deviceNetworkId =~ /hueScene:.*/ }
// }


private String deviceIdType(deviceNetworkId) {
    switch (deviceNetworkId) {
        case ~/hueGroup:.*/:    'group'; break
        case ~/hueScene:.*/:    'scene'; break
        case ~/hueBulb\w*:.*/:  'light'; break
        case ~/hue\-\w+/:         'hub'; break
    }
}

String deviceIdNode(deviceNetworkId) {
    deviceNetworkId.replaceAll(~/.*\//, '')
}

private String deviceIdHub(deviceNetworkId) {
    deviceNetworkId.replaceAll(~/.*\:/, '').replaceAll(~/\/.*/, '')
}


private String convertHexToIP(hex) {
    [hubitat.helper.HexUtils.hexStringToInt(hex[0..1]),
     hubitat.helper.HexUtils.hexStringToInt(hex[2..3]),
     hubitat.helper.HexUtils.hexStringToInt(hex[4..5]),
     hubitat.helper.HexUtils.hexStringToInt(hex[6..7])].join('.')
}

private Integer convertHexToInt(hex) {
    hubitat.helper.HexUtils.hexStringToInt(hex)
}

// private String convertIPtoHex(ipAddress) {
//     String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
//     return hex
// }

/*
 *
 * Hub Device Utility Functions
 *
 */

def deviceNetworkId(mac) {
    mac ? "hue-${mac}" : null
}

// private getHubForNetworkId(deviceNetworkId) {
//     getHubs().find { it.value.deviceNetworkId == deviceNetworkId }
// }

private getChildDeviceForMac(mac) {
    //childDevices?.find {it.deviceNetworkId == deviceNetworkId(mac)  }
    getChildDevice(deviceNetworkId(mac))
}

// private clearDiscoveredHubs() {
//     state.hubs = getHubs().findAll { getChildDeviceForMac(it.value.mac) != null }
// }

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


/*
 *
 * Device Parsers
 *
 */

def parseStatus(data) {

    def groups = data.groups
    def lights = data.lights
    def scenes = data.scenes

    if (groups) { parseGroups(groups) }
    if (lights) { parseLights(lights) }
    if (scenes) { parseScenes(scenes) }

}

void parseGroups(json) {
    json.each { id, data ->
        // Add code to update all installed groups state
        def group = getChildDevice(networkIdForGroup(id))
        if (group) {
            if (debug) { log.debug "Parsing response: $data for $id" } // temporary
            group.resetRefreshSchedule()
            if (data.state?.all_on || data.state?.any_on) data.state.remove('on')
            if (data.state?.all_on || data.state?.any_on) data.action.remove('on')
            setHueProperty(group, [state: data.state, action: data.action])
        }
    }
}

void parseLights(json) {
    json.each { id, data ->
        // Add code to update all installed lights state
        def light = getChildDevice(networkIdForLight(id))
        if (light) {
            if (debug) { log.debug "Parsing response: $data for $id" } // temporary
            light.unschedule()
            setHueProperty(light, [state: data.state, action: data.action])
        }
    }
}

void parseScenes(data) {
    //data.each { id, value ->
    //    if (debug) { log.debug "${id}" }
    //}
}

def findGroup(String groupId) {
    if (state.groups."${groupId}") {
        return state.groups."${groupId}"
    } else {
        return state.groups.find{ it.value.name == groupId }
    }
}

def findScene(String groupId, String sceneId) {
    if (state.scenes."${sceneId}") {
        return state.scenes."${sceneId}"
    } else {
        return state.scenes.find{ it.value.group == groupId && it.value.name == sceneId }
    }
}

Object getDeviceForChild(def child) {
    def nid = (child.device?:child).deviceNetworkId
    def dev = getChildDevice(nid)
    if (dev) {
        return dev
    }
    return getChildDevice(networkIdForGroup(nid.split('/')[1])).getChildDevice(nid)
}

Object currentValue(def child, String attributeName) {
    def nid = (child.device?:child).deviceNetworkId
    def device = getDeviceForChild(child)

    if (!device.hasAttribute(attributeName)) {
        return null
    }
    def result = getVolatileAtomicState(child)[attributeName]
    if (result != null) {
        return result
    }
    result = (device.deviceId ? device : device.device).currentValue(attributeName)
    if (result != null) {
        getVolatileAtomicState(device)[attributeName] = result
    }
    return result
}

void sendChildEvent(def child, Map event) {
    def nid = (child.device?:child).deviceNetworkId
    def device = getDeviceForChild(child)

    if (!device.hasAttribute(event.name)) { return }

    // Supress repetative updates, this reduces the load on the event bus.
    if (currentValue(device, event.name) == event.value) { return }

    // Send the update to the event bus
    child.sendEvent(event)

    getVolatileAtomicState(device.device)[event.name] = event.value

    // Log a message that the value was updated
    if (child.logEnable) {
        if (event.name == 'switch') {
            String type = deviceIdType(nid) ?: 'device'
            child.log.info "${type} ($child) turned ${event.value}"
        } else {
            child.log.info "Set ($child) ${event.name}: ${event.value}"
        }
    }
}

void setHueProperty(def child, Map args) {
    String type = deviceIdType(child.device.deviceNetworkId) ?: 'device'

    Map devstate = args.state

    if (type ==~ /group|hub/) {
        // Groups report any_on and all_on in state
        if (devstate) {
            if (devstate['any_on']) { child.setHueProperty([name: 'any_on', value: devstate.any_on]) }
            if (devstate['all_on']) { child.setHueProperty([name: 'all_on', value: devstate.all_on]) }
        }
        // Groups report all other values in action
        devstate = args.action
    }    

    if (devstate) {
        if (type == 'group' && devstate.scene) {
            child.setHueProperty([name: "scene", value: devstate.scene])
            return
        }
    }


    if (devstate.containsKey('on'))        { sendChildEvent(child, [name: 'switch',           value: devstate.on  ? 'on' : 'off']) }
    if (devstate.containsKey('colormode')) { sendChildEvent(child, [name: 'colorMode',        value: convertHBColorMode(devstate.colormode)]) }
    if (devstate.containsKey('bri'))       { sendChildEvent(child, [name: 'level',            value: convertHBLevel(devstate.bri)]) }
    if (devstate.containsKey('hue'))       { sendChildEvent(child, [name: 'hue',              value: convertHBHue(devstate.hue)]) }
    if (devstate.containsKey('sat'))       { sendChildEvent(child, [name: 'saturation',       value: convertHBSaturation(devstate.sat)]) }
    if (devstate.containsKey('ct'))        { sendChildEvent(child, [name: 'colorTemperature', value: convertHBColortemp(devstate.ct)]) }
}

// Component Dimmer delegates

void componentOn(child) {
    setDeviceState(child, ['on': true])
}

void componentOff(child) {
    setDeviceState(child, ['on': false])
}

void componentRefresh(child){
    getDeviceState(child)
}

void componentSetLevel(child, level, duration=null){
    Integer heLevel = convertHELevel(level as int)
    Map args = ['on': (heLevel > 0), 'bri': heLevel]
    if (duration != null) { args['transitiontime'] = (duration as int) * 10 }

    setDeviceState(child, args)
}

void componentStartLevelChange(child, direction) {
    Integer level = 0
    if (direction == 'up')        { level =  254 }
    else if (direction == 'down') { level = -254 }

    setDeviceState(child, ['bri_inc': level, 'transitiontime': transitionTime() * 10])

}

void componentStopLevelChange(def child) {
    setDeviceState(child, ['bri_inc': 0])
}

void componentSetColor(def child, Map colormap) {
    if (colormap?.hue == null)        { colormap.hue        = currentValue(child, 'hue') ?: 0 }
    if (colormap?.saturation == null) { colormap.saturation = currentValue(child, 'saturation') ?: 50 }
    if (colormap?.level == null)      { colormap.level      = currentValue(child, 'level') ?: 100 }

    Map args = ['colormode': 'hs',
                'hue': convertHEHue(colormap.hue as int),
                'sat': convertHESaturation(colormap.saturation as int),
                'bri': convertHELevel(colormap.level as int)]

    setDeviceState(child, args)

}

void componentSetHue(def child, hue) {
    setDeviceState(child, ['colormode': 'hs', 'hue': convertHEHue(hue as int)])
}

void componentSetSaturation(def child, saturation) {
    setDeviceState(child, ['colormode': 'hs', 'sat': convertHESaturation(saturation as int)])
}

void componentSetColorTemperature(def child, colortemperature, level = null, transitionTime = null) {
    Map values = ['colormode': 'ct', 'ct': convertHEColortemp(colortemperature as int)]
    if (level)          { values.['bri']           = convertHELevel(level) }
    if (transitionTime) { values['transitiontime'] = (transitionTime as int) * 10}

    setDeviceState(child, values)
}

Integer transitionTime() {
    2
}

//
// Utility Functions
//

private Number valueBetween(Number value, Number min, Number max) {
    return Math.min(Math.max(value, min), max)
}

Integer convertHBLevel(Integer value) {
    Math.round(value / 2.54)
}

Integer convertHELevel(Integer value) {
    valueBetween(Math.round(value * 2.54), 1, 254)
}

Number convertHBHue(Number value) {
    Math.round(value / 655.35)
}

Number convertHEHue(Number value) {
    valueBetween(Math.round(value * 655.35), 0, 65535)
}

Number convertHBSaturation(Number value) {
    Math.round(value / 2.54)
}

Number convertHESaturation(Number value) {
    valueBetween(Math.round(value * 2.54), 0, 254)
}

Number convertHBColortemp(Number value) {
    Math.round(((500 - value) * (4500 / 347)) + 2000 )
}

Number convertHEColortemp(Number value) {
    valueBetween(Math.round(500 - ((value - 2000) / (4500 / 347))), 153, 500)
}

String convertHBColorMode(String value) {
    if (value == 'hs') { return 'RGB'  }
    if (value == 'ct') { return 'CT' }
    if (value == 'xy') { return 'RGB' }
    return ''
}
