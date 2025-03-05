/**
* Advanced Philips Hue Bridge Integration application
* Version 1.6.2
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
import java.math.RoundingMode
import hubitat.helper.ColorUtils
import groovy.json.JsonOutput

definition(
    name:'Advanced Hue Bridge Integration',
    namespace: 'apwelsh',
    author: 'Armand Welsh',
    description: 'Find and install your Philips Hue Bridge systems',
    category: 'Convenience',
    //importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/app/hue-bridge-integration.groovy', // Commented out because the import URL is not needed for this version
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: ''
)

@Field static final Integer PAGE_REFRESH_TIMEOUT = 10

@Field static final String PAGE_MAINPAGE = 'mainPage'
@Field static final String PAGE_BRIDGE_DISCOVERY = 'bridgeDiscovery'
@Field static final String PAGE_HUB_REFRESH = 'hubRefresh'
@Field static final String PAGE_ADD_DEVICE = 'addDevice'
@Field static final String PAGE_BRIDGE_LINKING = 'bridgeLinking'
@Field static final String PAGE_UNLINK = 'unlink'
@Field static final String PAGE_FIND_LIGHTS = 'findLights'
@Field static final String PAGE_ADD_LIGHTS = 'addLights'
@Field static final String PAGE_FIND_GROUPS = 'findGroups'
@Field static final String PAGE_ADD_GROUPS = 'addGroups'
@Field static final String PAGE_FIND_SCENES = 'findScenes'
@Field static final String PAGE_ADD_SCENES = 'addScenes'
@Field static final String PAGE_FIND_SENSORS = 'findSensors'
@Field static final String PAGE_ADD_SENSORS = 'addSensors'

@Field static Map atomicStateByDeviceId = new ConcurrentHashMap()
@Field static Map atomicQueueByDeviceId = new ConcurrentHashMap()
@Field static Map refreshQueue = new ConcurrentHashMap()

@Field static final Integer DEVICE_REFRESH_DISCOVER_INTERVAL = 3
@Field static final Integer DEVICE_REFRESH_MAX_COUNT = 60

preferences {
    page(name: PAGE_MAINPAGE)
    page(name: PAGE_BRIDGE_DISCOVERY, title: 'Device Discovery', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_HUB_REFRESH,      title: 'Refresh Hub Metadata', refreshTimeout:1)
    page(name: PAGE_ADD_DEVICE,       title: 'Add Hue Bridge')
    page(name: PAGE_UNLINK,           title: 'Unlink your Hue')
    page(name: PAGE_BRIDGE_LINKING,   title: 'Linking with your Hue', refreshTimeout:5)
    page(name: PAGE_FIND_LIGHTS,      title: 'Light Discovery Started!', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_ADD_LIGHTS,       title: 'Add Light')
    page(name: PAGE_FIND_GROUPS,      title: 'Group Discovery Started!', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_ADD_GROUPS,       title: 'Add Group')
    page(name: PAGE_FIND_SCENES,      title: 'Scene Discovery Started!', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_ADD_SCENES,       title: 'Add Scene')
    page(name: PAGE_FIND_SENSORS,     title: 'Sensor Discovery Started!', refreshTimeout:PAGE_REFRESH_TIMEOUT)
    page(name: PAGE_ADD_SENSORS,      title: 'Add Sensor')

}


synchronized Map getAtomicState(def device) {
    Integer deviceId = device.deviceId ?: device.device.deviceId
    Map result = atomicStateByDeviceId.get(deviceId)
    if (result == null) {
        result = new ConcurrentHashMap()
        atomicStateByDeviceId[deviceId] = result
    }
    return result
}

synchronized Map getAtomicQueue(def device) {
    Integer deviceId = device.deviceId ?: device.device.deviceId
    Map result = atomicQueueByDeviceId.get(deviceId)
    if (result == null) {
        result = new ConcurrentHashMap()
        atomicQueueByDeviceId[deviceId] = result
    }
    return result
}

synchronized Map getRefreshQueue() {
    Integer appId = app.getId()
    Map result = refreshQueue.get(appId)
    if (result == null) {
        result = new ConcurrentHashMap()
        refreshQueue[appId] = result
    }
    return result
}

public String getBridgeHost() {
    String host = settings.bridgeHost
    if (!host) {
        host = state.remove('bridgeHost')
        if (!host) { return null }
        setBridgeHost(host)
    }
    host = host.replaceAll(/:80$/, ":443").replaceAll(/(?<!:\d+)\z/, ":443")
    setBridgeHost(host)
    return host
}

public void setBridgeHost(String host) {
    if (!host) {
        app.removeSetting('bridgeHost')
        return
    } else {
        if (host?.endsWith(':80')) {
            host = "${host.substring(0,host.size()-3)}:443"
        }
        app.updateSetting("bridgeHost", [type: "text", value: host])
    }
}

def mainPage(Map params=[:]) {
    if (app.installationState == 'INCOMPLETE') {
        return dynamicPage(name: PAGE_MAINPAGE, title: '', nextPage: null, uninstall: true, install: true) {
            section (getFormat("title", "Advanced Hue Bridge")) {
                paragraph getFormat("subtitle", "Installing new Hue Bridge Integration")
                paragraph getFormat("line")
                paragraph 'Click the Done button to install the Hue Bridge Integration.'
                paragraph 'Re-open the app to setup your device'
            }
        }
    }

    if (!selectedDevice) {
        return bridgeDiscovery()
    }

    if (params.nextPage==PAGE_BRIDGE_LINKING) {
        return bridgeLinking()
    }

    String title
    if (selectedDevice) {
        discoveredHubs()[selectedDevice]
        title=discoveredHubs()[selectedDevice]
        if (!getHubForMac(selectedDevice)) {
            ssdpSubscribe()
            ssdpDiscover()
        } else {
            ssdpUnsubscribe()
        }
    } else {
        title='Find Bridge'
    }

    Boolean uninstall = getBridgeHost() ? false : true

    return dynamicPage(name: PAGE_MAINPAGE, title: '', nextPage: null, uninstall: uninstall, install: true) {
            section (getFormat("title", "Advanced Hue Bridge")) {
                paragraph getFormat("subtitle", "Manage your linked Hue Bridge")
                paragraph getFormat("line")
            }

        if (selectedDevice == null) {
            section('Setup') {
                paragraph 'To begin, select Find Bridge to start searching for your Hue Bride.'
                href PAGE_BRIDGE_DISCOVERY, title:'Find Bridge', description:''//, params: [pbutton: i]
            }
        } else {
            section('Configure') {

                href PAGE_FIND_LIGHTS, title:'Find Lights', description:''
                href PAGE_FIND_GROUPS, title:'Find Groups', description:''
                href PAGE_FIND_SCENES, title:'Find Scenes', description:''
                href PAGE_FIND_SENSORS, title:'Find Sensors', description:''
                if (state.clientkey) {
                    href PAGE_HUB_REFRESH, title:title, description:'', state:selectedDevice ? 'complete' : null , params: [reset: true]
                    paragraph 'Your hub is linked.  Pressing this button will pause all hub activity, refresh the metadata for your hub, then resume listening.'
                } else {
                    href PAGE_BRIDGE_LINKING, title:title, description:'', state:selectedDevice ? 'complete' : null
                }
            }
            section('Options') {
                input name: 'logEnable',  type: 'bool', defaultValue: true,  title: 'Enable informational logging'
                input name: 'dbgEnable',  type: 'bool', defaultValue: false, title: 'Enable debug logging'
                input name: 'newEnable',  type: 'bool', defaultValue: false, title: 'Enable detection, and logging of new device types'
                input name: 'autorename', type: 'bool', defaultValue: false, title: 'Automatically track, and rename installed devices to match Hue defined names'
                href PAGE_UNLINK, title: 'Unlink hub', description:'Use this to unlink your hub and force a new hub link'
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
}

def getFormat(type, myText="") {            // Borrowed from @dcmeglio HPM code
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
    if(type == "subtitle") return "<h3 style='color:#1A77C9;font-weight: normal'>${myText}</h3>"
}

def hubRefresh(Map params=[:]) {
    log.debug "hubRefresh called with params: ${params}"

    String nextPage = ''
    String title = 'Hue Hub Metadata Refresh'
    String paragraphText
    Integer refreshInterval = 0

    // Invalidate the bridgeHost, so we can find the new one.
    if (params.reset) {
        def hub = getChildDeviceForMac(selectedDevice)
        hub.disconnect() // un-subscribe from the hub events
        refreshInterval = 1
        app.removeSetting('bridgeHost') // remove the bridgeHost setting, to force re-discovery of host address
        params.remove('reset')          // remove the reset param, so we don't keep resetting the bridgeHost
        params.sync = true

        paragraphText = 'Pausing hub event stream, and initiating search for hub on network.'

    } else if (!getBridgeHost()) {
        refreshInterval = 2
        nextPage = PAGE_HUB_REFRESH
        ssdpSubscribeUpdate()
        ssdpDiscover()
        paragraphText = 'Looking for hub on network.'
        params.sync = true
    } else {
        if (params.sync) {
            refreshInterval = 2
            nextPage = PAGE_HUB_REFRESH
            paragraphText = 'Found hub on network.  Proceeding with metadata refresh.'
            if (logEnable) { log.debug paragraphText }
            params.remove('sync')
        } else {
            enumerateLights()
            enumerateGroups()
            enumerateScenes()
            enumerateDevices()
            enumerateSensors()
            enumerateLightsV2()
            enumerateGroupsV2()
            enumerateScenesV2()

            def hub = getChildDeviceForMac(selectedDevice)
            hub.connect() // re-subscribe to the hub events
            hub.refresh() // for a hub refresh

            nextPage = PAGE_MAINPAGE
            paragraphText = 'Metadata refresh completed.  Press Next to return to the main page.'
        }
    }

    log.debug "$title: $paragraphText"

    return dynamicPage(name:PAGE_HUB_REFRESH, title:title, nextPage:nextPage, refreshInterval: refreshInterval, params:params) {
        section('') {
            paragraph "${paragraphText}"
        }
    }
}

def bridgeDiscovery(Map params=[:]) {
    if (selectedDevice) {

    }
    if (logEnable) { log.debug 'Searching for Hub additions and updates' }
    Map hubs = discoveredHubs() // pull app state for known hubs

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
    if (!(deviceRefreshCount % DEVICE_REFRESH_DISCOVER_INTERVAL)) {
        ssdpDiscover()
    }

    Boolean uninstall = getBridgeHost() ? false : true
    String nextPage = selectedDevice ? PAGE_BRIDGE_LINKING : null

    return dynamicPage(name:PAGE_BRIDGE_DISCOVERY, title:'Discovery Started!', nextPage:nextPage, refreshInterval:refreshInterval, uninstall:uninstall) {
        section('Please wait while we discover your Hue Bridge. Note that you must first configure your Hue Bridge and Lights using the Philips Hue application. Discovery can take five minutes or more, so sit back and relax, the page will reload automatically! Select your Hue Bridge below once discovered.') {
            input 'selectedDevice', 'enum', required:false, title:"Select Hue Bridge (${numFound} found)", multiple:false, options:options, submitOnChange: true
        }
    }
}

def unlink() {
    state.remove('hub')
    state.remove('username')
    state.remove('clientkey')
    app.removeSetting('bridgeHost')
    if (selectedDevice) {
        def hub = getHubForMac(selectedDevice)
        hub.remove('username')
        hub.remove('clientkey')
    }
    selectedDevice = ''

    return dynamicPage(name:PAGE_UNLINK, title:'Unlink bridge', nextPage:null, uninstall: false) {
        section('') {
            paragraph "Your hub has been unlinked.  Use hub linking to re-link your hub."
        }
    }

}

def bridgeLinking() {

    String nextPage = ''
    String title = 'Linking with your Hue'
    Integer refreshInterval = 2
    String paragraphText

    // If bridge IP is undefined, wait for IP discovery before starting linking.
    if (selectedDevice && !getBridgeHost()) {
        ssdpSubscribeUpdate()
        ssdpDiscover()
        paragraphText = 'Looking for hub on network.'

    } else {
        ssdpUnsubscribe()  // force-stop all SSDP discovery, to reduce network chatter

        /* groovylint-disable-next-line DuplicateNumberLiteral */
        Integer linkRefreshcount = state.linkRefreshcount ?: 0
        state.linkRefreshcount = linkRefreshcount + 1

        if (selectedDevice) {
            paragraphText = 'Press the button on your Hue Bridge to setup a link. '

            def hub = getHubForMac(selectedDevice)
            if (hub?.username && hub?.clientkey) { //if discovery worked
                if (logEnable) { log.debug "Hub linking completed for ${hub.name}" }
                return addDevice(hub)
            }
            if (hub?.networkAddress) {
                setBridgeHost("${hub.networkAddress}:${hub.deviceAddress}")
            }

            if (hub) {
                if((linkRefreshcount % 2) == 0 && (!state.username || !state.clientkey)) {
                    requestHubAccess(selectedDevice)
                }
            }

        } else {
            paragraphText = 'You haven\'t selected a Hue Bridge, please Press \'Done\' and select one before clicking next.'
        }

    }

    def uninstall = getBridgeHost() ? false : true

    return dynamicPage(name:PAGE_BRIDGE_LINKING, title:title, nextPage:nextPage, refreshInterval:refreshInterval, uninstall: uninstall) {
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
        setBridgeHost("${device.networkAddress}:${device.deviceAddress}")
        state.username = "${device.username}"
        state.clientkey = "${device.clientkey}"
        refreshHubStatus()
    }

    String nextPage = PAGE_HUB_REFRESH
    Map params = [sync: true]
    if (!d && device != null) {
        if (logEnable) { log.debug "Creating Hue Bridge device with dni: ${dni}" }
        try {
            addChildDevice('apwelsh', 'AdvancedHueBridge', dni, null, ['label': device.name])
        } catch (ex) {
            if (ex.message =~ 'A device with the same device network ID exists.*') {
                sectionText = 'Cannot add hub.  A device with the same device network ID already exists.'
                title = 'Problem detected'
                app.removeSetting('bridgeHost')
                nextpage = PAGE_MAINPAGE
                params = [:]
            }
        }
    }

    return dynamicPage(name:PAGE_ADD_DEVICE, title:title, nextPage:nextPage, params: params) {
        section() {
            paragraph sectionText
        }
    }
}

def findLights() {
    enumerateLights()
    enumerateDevices()
    enumerateLightsV2()

    List installed = getInstalledLights().collect { it.label ?: it.name }
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
        section('Let\'s find some lights.') {
            input 'selectedLights', 'enum', required:false, title:"Select additional lights to add (${numFound} available)", multiple:true, options:options, submitOnChange: true
        }
        if (selectedLights) {
            section {
                paragraph "Click the Next button to add the selected light${selectedLights.size() > 1 ? 's' : ''} to your Hubitat hub."
            }
        } else if (installed) {
                section('Installed lights') {
                installedLights.each { child -> buttonLink child }
            }
        }
    }

}

def addLights(Map params=[:]) {

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

            def child = addChildDevice('hubitat', "Generic Component ${type}", "${dni}",
            [label: "${name}", isComponent: false, name: 'AdvancedHueBulb'])
            child.updateSetting('txtEnable', false)
            lights.remove(lightId)
            child.refresh()

            } catch (ex) {
                if (ex.message =~ 'A device with the same device network ID exists.*') {
                    sectionText += "\nA device with the same device network ID (${dni}) already exists; cannot add Light [${name}]"
                } else {
                    sectionText += "\nFailed to add light [${name}]; see logs for details"
                    log.error "${ex}"
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

        return dynamicPage(name:PAGE_ADD_LIGHTS, title:title, nextPage:null) {
            section() {
                paragraph sectionText
            }
        }
    }
}

def findGroups(params){
    enumerateGroups()
    enumerateDevices()
    enumerateGroupsV2()

    def installed = getInstalledGroups().collect { it.label ?: it.name }
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
        }
        if (selectedGroups) {
            section {
                paragraph "Click the Next button to add the selected group${selectedGroups.size() > 1 ? 's' : ''} to your Hubitat hub."
            }
        } else if (installed) {
                section('Installed groups') {
                installedGroups.each { child -> buttonLink child }
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

            def child = addChildDevice('apwelsh', 'AdvancedHueGroup', dni, null, ['label': "${name}"])
            groups.remove(groupId)
            child.refresh()

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

    return dynamicPage(name:PAGE_ADD_GROUPS, title:title, nextPage:null) {
        section() {
            paragraph sectionText
        }
    }

}

Map scenesForGroupId(groupNetworkId) {
    def groupId = deviceIdNode(groupNetworkId)
    state.scenes?.findAll { it.value.type == 'GroupScene' && it.value.group == groupId }
}

def findScenes(params){
    enumerateScenes()
    enumerateDevices()
    // enumerateScenesV2()

    Map groupOptions = [:]
    getInstalledGroups().each { groupOptions[deviceIdNode(it.deviceNetworkId)] = it.label ?: it.name }

    Map options = [:]
    Map scenes
    def group
    List installed
    List dnilist
    if (selectedGroup) {
        group = getChildDevice(networkIdForGroup(selectedGroup))
        if (group) {
            installed = group.getChildDevices()?.collect { it.label ?: it.name }
            dnilist = group.getChildDevices()?.collect { it.deviceNetworkId }
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
            }
        }
        if (selectedScenes) {
            section {
                paragraph "Click the Next button to add the selected Scene${selectedScenes.size() > 1 ? 's' : ''} to your Hubitat hub."
            }
        } else if (installed && selectedGroup) {
                section('Installed scenes') {
                group.getChildDevices().each { child -> buttonLink child }
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
        String name = "${group.label ?: group.name} - ${state.scenes[sceneId].name}"
        String dni = networkIdForScene(selectedGroup, sceneId)
        try {

            def child = group.addChildDevice('hubitat', 'Generic Component Switch', "${dni}",
            [label: "${name}", isComponent: false, name: 'AdvancedHueScene'])
            child.updateSetting('txtEnable', false)
            scenes.remove(sceneId)
            child.refresh()

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

    return dynamicPage(name:PAGE_ADD_SCENES, title:title, nextPage:null) {
        section() {
            paragraph sectionText
        }
    }

}

def findSensors(){
    enumerateSensors()
    enumerateDevices()

    List installed = getInstalledSensors().collect { it.label ?: it.name }
    List dnilist   = getInstalledSensors().collect { it.deviceNetworkId }

// TODO:
    Map options = [:]
    Map sensors = state.sensors
    if (sensors) {
        sensors.each {key, value ->
            // def sensors = value.sensors ?: []
            // if ( sensors.size()  == 0 ) { return }
            if ( dnilist.find { dni -> dni == networkIdForSensor(key) }) { return }
            options["${key}"] = "${value.name} (${value.productname})"
        }
    }

    Integer numFound = options.size()
    Integer refreshInterval = numFound == 0 ? 30 : 120
    String nextPage = selectedSensors ? PAGE_ADD_SENSORS : null

    return dynamicPage(name:PAGE_FIND_SENSORS, title:'Sensor Discovery Started!', nextPage:nextPage, refreshInterval:refreshInterval) {
        section('Let\'s find some sensors.') {
            input 'selectedSensors', 'enum', required:false, title:"Select additional sensors to add (${numFound} available)", multiple:true, options:options, submitOnChange: true
        }
        if (selectedSensors) {
            section {
                paragraph "Click the Next button to add the selected sensor${selectedSensors.size() > 1 ? 's' : ''} to your Hubitat hub."
            }
        } else if (!installed.isEmpty()) {
                section('Installed sensors') {
                installedSensors.each { child -> buttonLink child }
            }
        }
    }

}

private buttonLink(child) {
    Map map = stateForNetworkId(child.device.deviceNetworkId)
    Boolean ena = map?.config?.on ?: map?.state?.reachable
    paragraph """<button type="button" class="btn btn-default btn-lg btn-block hrefElem ${ena ? 'btn-state-complete' : '' } mdl-button--raised mdl-shadow--2dp" style="text-align:left;width:100%" onclick="window.location.href='/device/edit/${child.device.id}'">
                    <span style="text-align:left;white-space:pre-wrap">${child.label ?: child.name} (${child.name})</span>
                </button>"""
}

def addSensors(Map params=[:]){

    if (!selectedSensors) {
        return findSensors()
    }

    String subject = selectedSensors.size == 1 ? 'Sensor' : 'Sensors'

    String title = ''
    String sectionText = ''

    List sensors = selectedSensors.collect { it }

    selectedSensors.each { sensorId ->
        String name = state.sensors[sensorId].name
        String dni = networkIdForSensor(sensorId)
        String type = driverTypeForSensor(sensorId)
        String model = state.sensors[sensorId].productname ?: "Advanced Hue ${type} Sensor"
        if (!type) {
            sectionText = "\nCannot install sensor ${name}; a compatible driver is not available.\n"
            sectionText += "\nTo request support for the sensor, open a support ticket on GitHub, and include the following device details:\n\n${state.sensors[sensorId]}"
            log.error "Failed to add sensor [${name}]; not supported"
        } else {
            try {

                def child = addChildDevice('apwelsh', "AdvancedHue${type}Sensor", "${dni}",
                [label: "${name}", isComponent: false, name: "${model}"])
                sensors.remove(sensorId)
                child.refresh()

            } catch (ex) {
                if (ex.message =~ 'A device with the same device network ID exists.*') {
                    sectionText = "\nA device with the same device network ID (${dni}) already exists; cannot add sensor [${name}]"
                } else {
                    sectionText += "\nFailed to add sensor [${name}]; see logs for details"
                    log.error "${ex}"
                }
            }
        }
    }

    if (sensors.size() == 0) {
        app.removeSetting('selectedSensors')
    }

    if (!sectionText) {
        title = "Adding ${subject} to Hubitat"
        sectionText = "Added ${subject}"
    } else {
        title = "Failed to add ${subject}"
    }

    return dynamicPage(name:PAGE_ADD_SENSORS, title:title, nextPage:null) {
        section() {
            paragraph sectionText
        }
    }

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
    unschedule()
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
    atomicStateByDeviceId.clear()
    if (!settings.bridgeHost && state.bridgeHost) {  // migrate bridgeHost from state to setting\
        setBridgeHost(bridgeHost)
        state.remove('bridgeHost')
    }
    if (selectedDevice) {
        ssdpSubscribeUpdate() // Setup listener to update the configured hub details
    } else {
        ssdpSubscribe() // Setup listener to find all hue hubs
    }
    ssdpDiscover()
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

void ssdpSubscribeUpdate() {
    subscribe(location, 'ssdpTerm.upnp:rootdevice', ssdpUpdateHandler)
}

void ssdpUnsubscribe() {
    unsubscribe(ssdpHandler)
    unsubscribe(ssdpUpdateHandler)
    unschedule(ssdpDiscover)
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
                        'networkAddress': convertToHexToIP(parsedEvent.networkAddress),
                        'deviceAddress': convertToHexToInt(parsedEvent.deviceAddress)]

        def ssdpUSN = parsedEvent.ssdpUSN.toString()

        def hubs = getHubs()
        if (!hubs."${ssdpUSN}") {
            verifyDevice(parsedEvent)
        } else {
            updateDevice(parsedEvent)
        }
    }
}

def ssdpUpdateHandler(evt) {
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
        if ("${parsedEvent.mac}" == "${selectedDevice}") {
            def hubs = getHubs()
            if (hubs."${ssdpUSN}") {
                log.info "${parsedEvent.mac}: Autodetect IP address ${parsedEvent.networkAddress}"
                updateDevice(parsedEvent)
                setBridgeHost("${parsedEvent.networkAddress}:${parsedEvent.deviceAddress}")
            }
            ssdpUnsubscribe()
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
            if (logEnable) { log.error "${data}" }
        }

    }
}

String getApiUrl() {
    "https://${getBridgeHost()}/api/${state.username}"
}

String getApiV2Url() {
        uri: "https://${getBridgeHost()}/clip/v2"
}

Map getApiV2Header() {
    ['hue-application-key': state.username]
}

void refreshHubStatus() {

    def url = apiUrl
    if (url == null) {
        log.warn "Hub communications are offline, due to missing bridge host."
        return
    }

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

private renameInstalledDevices(type, data) {
    if (!autorename) { return }
    data.each { key, value ->
        String nid = ''
        switch (type) {
            case 'groups':
                nid = networkIdForGroup(key);  break
            case 'sensors':
                nid = networkIdForSensor(key); break
            case 'scenes':
                nid = networkIdForScene(key);  break
            case 'lights':
                nid = networkIdForLight(key);  break
            default:
                return
        }

        def child = getChildDevice(nid)
        if (!child) { return }

        if (child.label != value.name) {
            child.label = value.name
        }
        if (child.name != value.productname && value.productname) {
            child.name = value.productname
        }
    }
}

private renameInstalledDevicesV2(type, data) {
    if (!autorename) { return }
    data.each { key, value ->
        String nid = ''

        switch (type) {
            case 'zones':
                nid = networkIdForZone(key);  break
            case 'rooms':
                nid = networkIdForRoom(key);  break
            case 'sensors':
                nid = networkIdForSensor(key); break
            case 'scenes':
                nid = networkIdForScene(key);  break
            case 'lights':
                nid = networkIdForLight(key);  break
            default:
                return
        }

        def child = getChildDevice(nid)
        if (!child) { return }

        if (child.label != value.metadata.name) {
            child.label = value.metadata.name
        }
        if (child.name != value.productname && value.productname) {
            child.name = value.productname
        }
    }
}

private enumerateGroups() {

    if (apiUrl == null) {
        log.warn "Hub communications are offline, due to missing bridge host."
        return
    }
    def url = "${apiUrl}/groups"

    httpGet([uri: url,
            contentType: 'application/json',
            ignoreSSLIssues: true,
            requestContentType: 'application/json']) { response ->
        if (!response.isSuccess()) { return }

        def data = response.data
        if (dbgEnable) { log.debug "enumerateGroups: ${data}" }
        if (data) {
            if (data.error?.description) {
                if (logEnable) { log.error "${data.error.description[0]}" }
            } else {
                if (data) {
                    state.groups = data
                    renameInstalledDevices('groups', state.groups)
                }
                parseGroups(data)
            }
        }

    }

}

private enumerateGroupsV2() {
    enumerateRooms()
    enumerateZones()
    enumerateGroupedLights()
}

private enumerateRooms() {
    def data = enumerateResourcesV2('/room')  // Get the definition of the room
    if (data) {
        Map rooms = [:]
        List keys = ['id_v1', 'children', 'metadata', 'services']
        data.each { it ->
            String id = it.id
            rooms[id] = it.subMap(keys)

            // update lights V1 state with ID of V2 light
            state.groups.(it.id_v1.replaceFirst('/groups/', '')).id = it.id
            state.groups.(it.id_v1.replaceFirst('/groups/', '')).id_type = 'room'
        }
        state.rooms = rooms
        renameInstalledDevicesV2('rooms', rooms)
        //parseGroupsV2(data)
    }
}

private enumerateZones() {
    def data = enumerateResourcesV2('/zone')  // Get the definition of the room

    if (data) {
        Map zones = [:]
        List keys = ['id_v1', 'children', 'metadata', 'services']
        data.each { it ->
            String id = it.id
            zones[id] = it.subMap(keys)

            // update lights V1 state with ID of V2 light
            state.groups.(it.id_v1.replaceFirst('/groups/', '')).id = it.id
            state.groups.(it.id_v1.replaceFirst('/groups/', '')).id_type = 'zone'
        }
        state.zones = zones
        renameInstalledDevicesV2('zones', zones)
        //parseGroupsV2(data)
    }
}

private enumerateGroupedLights() {
    def data = enumerateResourcesV2('/grouped_light')  // Get the status of a group (room or zone)

    if (data) {
        Map groups = [:]
        List keys = ['id_v1', 'owner', 'signaling']
        data.each { it ->
            String id = it.id
            groups[id] = it.subMap(keys)
        }
        state.grouped_lights = groups
        //parseGroupsV2(data)
    }
}

private enumerateDevices() {
    def data = enumerateResourcesV2('/device')  // Get the status of a group (room or zone)

    if (data) {
        Map devices = [:]
        List keys = ['id_v1', 'services', 'product_data', 'metadata']
        data.each { it ->
            String id = it.id
            devices[id] = it.subMap(keys)
        }
        state.devices = devices
        //parseSensorsV2(data)
    }
}

private enumerateLights() {

    if (apiUrl == null) {
        log.warn "Hub communications are offline, due to missing bridge host."
        return
    }
    def url = "${apiUrl}/lights"

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
                if (data) {
                    state.lights = data
                    renameInstalledDevices('lights', state.lights)
                }
                parseLights(data)
            }
        }
    }
}

private enumerateLightsV2() {
    def data = enumerateResourcesV2('/light')
    if (data) {
        Map lights = [:]
        List keys = ['id_v1', 'owner', 'metadata']
        data.each { it ->
            String id = it.id
            lights[id] = it.subMap(keys)
            if (it.dimming?.min_dim_level)  { lights[id].dimming = [min_dim_level: it.dimming.min_dim_level] }
            if (it.dynamics?.status_values) { lights[id].dynamics = [status_values: it.dynamics.status_values] }
            if (it.color?.gamut || it.color?.gamut_type) { lights[id].color = [:] }
            if (it.color?.gamut)            { lights[id].color << [gamut: it.color.gamut] }
            if (it.color?.gamut_type)       { lights[id].color << [gamut_type: it.color.gamut_type] }
            if (it.effects) {
                    lights[id].effects = it.effects.findAll { it.key != 'status'}
            }
            if (it.color_temperature?.mirek_schema) {
                lights[id].color_temperatures = [mirek_schema: it.color_temperature.mirek_schema]
            }
            lights[id].type = bulbTypeForLightV2(it)

            // update lights V1 state with ID of V2 light
            state.lights.(it.id_v1.replaceFirst('/lights/', '')).id = it.id
        }
        state.lights_v2 = lights
        renameInstalledDevicesV2('lights', lights)
        // parseLightsV2(data)
    }
}

private enumerateScenes() {

    if (apiUrl == null) {
        log.warn "Hub communications are offline, due to missing bridge host."
        return
    }
    def url = "${apiUrl}/scenes"

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
                if (data) {
                    state.scenes = data
                    renameInstalledDevices('scenes', state.scenes)
                }
                parseScenes(data)
            }
        }

    }
}

private enumerateScenesV2() {
    def data = enumerateResourcesV2('/scene')
    if (data) {
        Map scenes = [:]
        List keys = ['id_v1', 'group', 'metadata', 'status', 'auto_dynamic']
        data.each { it ->
            String id = it.id
            scenes[id] = it.subMap(keys)
            // if (it.dimming?.min_dim_level)  { scenes[id].dimming = [min_dim_level: it.dimming.min_dim_level] }
            // if (it.dynamics?.status_values) { scenes[id].dynamics = [status_values: it.dynamics.status_values] }
            // if (it.color?.gamut || it.color?.gamut_type) { scenes[id].color = [:] }
            // if (it.color?.gamut)            { scenes[id].color << [gamut: it.color.gamut] }
            // if (it.color?.gamut_type)       { scenes[id].color << [gamut_type: it.color.gamut_type] }
            // if (it.effects) {
            //         scenes[id].effects = it.effects.findAll { it.key != 'status'}
            // }
            // if (it.color_temperature?.mirek_schema) {
            //     scenes[id].color_temperatures = [mirek_schema: it.color_temperature.mirek_schema]
            // }
            // scenes[id].type = bulbTypeForLightV2(it)

            // update lights V1 state with ID of V2 light
            // state.scenes.(it.id_v1.replaceFirst('/scenes/', '')).id = it.id

            //def ridValues = it.actions.collect { it.target.rid }
            //log.info "scene: $id,  RIDs=$ridValues"
            //'167f8930-ee97-4982-a7f7-9ba436425500'
//TODO:  Finish working out the logic for parsing the scene data
            // if (id == 'fe4c565f-4f1d-4d50-970f-13e7bf5b72ea')
            //     log.info(JsonOutput.toJson(it))
        }
        state.scenes_v2 = scenes
        //renameInstalledDevicesV2('scenes', state.scenes_v2)
        // parseLightsV2(data)

    }


}

private enumerateSensors() {

    if (apiUrl == null) {
        log.warn "Hub communications are offline, due to missing bridge host."
        return
    }
    def url = "${apiUrl}/sensors"

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
                if (data) {
                    state.sensors = fixupSensorNames(data)
                    renameInstalledDevices(sensors, state.sensors)
                }

                parseSensors(data)
            }
        }
    }
}

private List enumerateResourcesV2(String type) {

    def url = "${apiV2Url}/resource"
    if (type) { url = "${url}/${type}" }

    httpGet([uri: url,
            contentType: 'application/json',
            ignoreSSLIssues: true,
            //  requestContentType: 'application/json',
            headers: apiV2Header]) { response ->

        if (!response.isSuccess()) { return }

        def data = response.data
        if (data) {
            if (logEnable) {
                data.errors.each { log.error "${it}" }
            }
            return data.data
        }
    }
}

private fixupSensorNames(data) {
    Map uniqueids = data.findAll { key, value -> value.uniqueid ==~ /^(\p{XDigit}{2}:){7}\p{XDigit}{2}.*/ && !value.name.startsWith(value.productname)}
                        .sort { a, b -> (a.key as int) <=> (b.key as int) }
                        .collectEntries { entry ->
                            String mac = uniqueIdMAC(entry.value.uniqueid)
                            [(mac): entry.value.name.replaceFirst(/(?i: \w+ sensor)$/, '')]
                        }
    data.each { key, value ->
        if (value.uniqueid?.size() >= 23) {
            String mac = uniqueIdMAC(value.uniqueid)
            if (uniqueids[mac]) {
                String suffix = value.productname.replaceFirst(/^.* (?i:)(\S+ sensor)$/, /$1/)
                if (suffix) { value.name = "${uniqueids[mac]} ${suffix}" }
            }
        }
    }
    return data
}

private String uniqueIdMAC(String uniqueid) {
    uniqueid.replaceFirst(/^(.{23}).*/, /$1/)
}

void setDeviceConfig(def child, Map deviceConfig) {

    if (apiUrl == null) {
        log.warn "Hub communications are offline, due to missing bridge host."
        return
    }

    String deviceNetworkId = child.device.deviceNetworkId

    String hubId = deviceIdHub(deviceNetworkId)
    String action='config'

    if ("${hubId}" != "${app.getId()}") { // hub validation by validating the device belongs to current app instance
        log.warn "Received setDeviceState request for invalid hub ID ${hubId}.  Current hub ID is ${app.getId()}"
        return
    }

    // all other updates
    String type = "${deviceIdType(deviceNetworkId)}s"
    switch (type) {
        case 'hubs':
        case 'groups':
        case 'scenes':
            return
    }
    String node = deviceIdNode(deviceNetworkId)

    String url = "${apiUrl}/${type}/${node}/${action}"
    if (dbgEnable) { log.debug "URL: ${url}" }
    if (dbgEnable) { log.debug "args: ${deviceConfig}" }
    httpPut([uri: url,
            contentType: 'application/json',
            ignoreSSLIssues: true,
            requestContentType: 'application/json',
            body: deviceConfig]) { response ->

        if (!response.success) { return }

        response.data?.each { item ->
            if (item.error?.description) {
                if (dbgEnable) { log.error "set state: ${item.error.description[0]}" }

            }
            item.success?.each { key, value ->
                List result = key.split('/')
                state.(result[1]).(result[2]).(result[3]).(result[4]) = value
            }
        }
    }
}

void setDeviceState(def child, Map deviceState) {
    if (apiUrl == null) {
        log.warn "Hub communications are offline, due to missing bridge host."
        return
    }

    String deviceNetworkId = child.device.deviceNetworkId
    // Establish the eventQueue for the device.
    Map eventQueue = getAtomicQueue(child)
    eventQueue.putAll(deviceState)

    // If device state does not contain the value on, then determine if we must queue the state for a future call
    if (!deviceState.on && !deviceState.scene) {
        if ((currentValue(child, 'switch') ?: 'on') == 'off') {
            return
        }
    }

    Map newState = [:] + (deviceState.scene ? deviceState : eventQueue)
    eventQueue.clear()
    if (newState.scene) { newState['on'] = true } // Hue no longer turns lights on when scene is activated.
    if (newState.colormode) { newState.remove('colormode')}

    if (newState.containsKey('hue') || newState.containsKey('sat')) {
        if (!newState.containsKey('hue') && currentValue(child, 'hue') != null) { newState.hue = convertToHueHue(currentValue(child, 'hue')) }
        if (!newState.containsKey('sat') && currentValue(child, 'saturation') != null) { newState.sat = convertToHueSaturation(currentValue(child, 'saturation')) }
    }

    String hubId = deviceIdHub(deviceNetworkId)
    String type
    String node
    String action='action'

    if ( deviceNetworkId ==~ /hue\-[0-9A-F]{12}/ ) { // All lights (group 0) doesn't have app ID identified in nid
        type = 'groups'
        node = '0'
    } else if ("${hubId}" != "${app.getId()}") { // hub validation by validating the device belongs to current app instance
        log.warn "Received setDeviceState request for invalid hub ID ${hubId}.  Current hub ID is ${app.getId()}"
        return
    } else {  // all other updates
        type = "${deviceIdType(deviceNetworkId)}s"
        node = deviceIdNode(deviceNetworkId)
        if (type == 'lights') { // defined url to use action for lights, instead of state
            action = 'state'
        }
    }

    // Disabling scheduled refreshes for hub.
    def hub = getChildDeviceForMac(selectedDevice)

    String url = "${apiUrl}/${type}/${node}/${action}"
    if (dbgEnable) { log.debug "URL: ${url}" }
    if (dbgEnable) { log.debug "args: ${deviceState}" }
    httpPut([uri: url,
            contentType: 'application/json',
            ignoreSSLIssues: true,
            requestContentType: 'application/json',
            body: newState]) { response ->

        if (!response.success) { return }

        response.data?.each { item ->
            if (item.error?.description) {
                if (dbgEnable) { log.error "set state: ${item.error.description[0]}" }

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
                //setHueProperty(device, [(result[3]): [(result[4]): value]])
            }
        }
    }
}

void queueDeviceRefresh(List deviceIds) {
    unschedule(dispatchRefresh)
    Map queue = refreshQueue
    deviceIds.each { id -> queue[(id)] = true }
    runIn(1, dispatchRefresh)
}

void dispatchRefresh() {
    List nids = refreshQueue.keySet() as ArrayList
    refreshQueue.clear()
    if (nids.size() > 4) {
        refreshHubStatus()
    } else {
        nids.each { nid ->
            def child = getChildDevice(nid)
            if (child) {
                getDeviceState(child)
            }
        }
    }
}

void getDeviceState(def child) {
    if (apiUrl == null) {
        log.warn "Hub communications are offline, due to missing bridge host."
        return
    }
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
    String url = "${apiUrl}/${type}/${node}"
    if (dbgEnable) { log.debug "URL: ${url}" }
    httpGet([uri: url,
            contentType: 'application/json',
            ignoreSSLIssues: true,
            requestContentType: 'application/json']) { response ->

        if (!response.success) { return }

        def data = response.data
        if (data) {
            if (dbgEnable) { log.debug "getDeviceState received data: ${data}" }  // temporary until lights are added, and all groups/all lights
            if ( data.error ) {
                if (logEnable) { log.error "${it.error.description[0]}" }
                return
            }
            if (node) {
                child = getChildDevice(deviceNetworkId)
                if (type == 'groups') {
                    data.state.remove('on')
                    data.action.remove('on')
                }

                if (!(deviceNetworkId ==~ /hue\-\w+/) ) { stateForNetworkId(deviceNetworkId).putAll(data) }
                setHueProperty(child, [state: data.state ?: [:], action: data.action ?: [:], config: data.config ?: [:]])

            } else {
                if (dbgEnable) { log.debug "Received unknown response: ${it}" }  // temporary to catch unknown result state
            }
        }

    }
}


/*
*
* Private helper functions
*
*/


public String networkIdForGroup(id) {
    "hueGroup:${app.getId()}/${id}"
}

public String networkIdForZone(id) {
    "hueGroup:${app.getId()}/${state.zones[id].id_v1.replaceFirst('/groups/', '')}"
}

public String networkIdForRoom(id) {
    "hueGroup:${app.getId()}/${state.rooms[id].id_v1.replaceFirst('/groups/', '')}"
}

public String networkIdForLight(id) {
    String type = bulbTypeForLight(id)
    "hueBulb${type}:${app.getId()}/${id}"
}

public networkIdForScene(sceneId) {
    String groupId = state.scenes[sceneId]?.group
    "hueScene:${app.getId()}/${groupId}/${sceneId}"
}

public networkIdForScene(groupId, sceneId) {
    "hueScene:${app.getId()}/${groupId}/${sceneId}"
}

public networkIdForSensor(sensorId) {
    "hueSensor:${app.getId()}/${sensorId}"
}

public stateForNetworkId(deviceNetworkId) {
    def (String type, String address) = deviceNetworkId.split(':', 2)
    String hueId = address.split('/').last()
    switch (type) {
        case 'hueGroup':   return state.groups[hueId]
        case ~/hueBulb.*/: return state.lights[hueId]
        case 'hueScene':   return state.scenes[hueId]
        case 'hueSensor':  return state.sensors[hueId]
    }
    return null
}

public getChildDeviceById(Long id) {
    getChildDevices().find { child -> child.device.deviceId == id }
}

private String bulbTypeForLight(String id) {
    if (state.lights_v2?.id?.type) {
        return state.lights_v2.(id).type
    }
    if (!state.lights) { return null }
    def type = state.lights[id]?.type
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

private String bulbTypeForLightV2(Map light) {
    if (!light) { return null }

    if (light?.color) {
        return 'RGBW'
    }
    if (!light?.color_temperature) {
        return 'CT'
    }
    if (light?.dimming) {
        return 'Dimmer'
    }
    return 'RGB'
}

private String driverTypeForSensor(id) {
    // All supported sensors are here,
    // https://developers.meethue.com/develop/hue-api/supported-devices/

    if (!state.sensors) { return null }
    def type=state.sensors[id]?.type
    def modelid=state.sensors[id]?.modelid // Since the RunLessWires Friends of Hue switch still says it's a ZGPSwitch gotta use modelid to find it
    switch (type) {
        case 'ZGPSwitch':
        if (modelid == 'FOHSWITCH') {
            return 'RunLessWires'
        }
        else {
            return 'Tap' // 4 button controller (push only)
        }
        case 'ZLLSwitch':
            return 'Dimmer' // 4 button controller (PHR)
        case 'ZLLPresence':
            return 'Motion'  // presence as boolean (true/false)
        case 'ZLLTemperature':  // temperature in degrees C, 3000 = 30.00 (reported in int32)
            return 'Temperature'
        case 'ZLLLightLevel':   // reports lightlevel in 10000 log10(lux) + 1. daylight, dark as booleans.
            return 'Light'
        // case '':
        //     return ''
        default:
            return ''
    }
}

private List getInstalledLights() {
    // return childDevices.findAll { light -> light.deviceNetworkId =~ /hueBulb\w*:.*/ }
    return getInstalledDevices(/hueBulb\w*:.*/)
}

private List getInstalledGroups() {
    // return childDevices.findAll { group -> group.deviceNetworkId =~ /hueGroup:.*/ }
    return getInstalledDevices(/hueGroup:.*/)
}

private List getInstalledSensors() {
    return getInstalledDevices(/hueSensor:.*/)
}

private List getInstalledDevices(pattern) {
    return childDevices.findAll { child -> child.deviceNetworkId =~ pattern }
}

// private getInstalledScenes() {
//     childDevices.findAll { it.deviceNetworkId =~ /hueScene:.*/ }
// }

private String deviceIdType(deviceNetworkId) {
    switch (deviceNetworkId) {
        case ~/hueGroup:.*/:    'group'; break
        case ~/hueScene:.*/:    'scene'; break
        case ~/hueBulb\w*:.*/:  'light'; break
        case ~/hueSensor:.*/:  'sensor'; break
        case ~/hue\-\w+/:         'hub'; break
    }
}

String deviceIdNode(deviceNetworkId) {
    deviceNetworkId.replaceAll(~/.*\//, '')
}

private String deviceIdHub(deviceNetworkId) {
    deviceNetworkId.replaceAll(~/.*\:/, '').replaceAll(~/\/.*/, '')
}

/*
*
* Hub Device Utility Functions
*
*/

def deviceNetworkId(mac) {
    mac ? "hue-${mac}" : null
}

private getChildDeviceForMac(mac) {
    getChildDevice(deviceNetworkId(mac))
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

/*
*
* Device Parsers
*
*/

def parseStatus(data) {

    def groups  = data.groups
    def lights  = data.lights
    def scenes  = data.scenes
    def sensors = data.sensors

    if (groups)  { parseGroups(groups)   }
    if (lights)  { parseLights(lights)   }
    if (scenes)  { parseScenes(scenes)   }
    if (sensors) { parseSensors(sensors) }

}

void parseGroups(json) {
    json.each { id, data ->
        // Add code to update all installed groups state
        def group = getChildDevice(networkIdForGroup(id))
        if (group) {
            if (dbgEnable) { log.debug "$group; Parsing response: $data for $id" }
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
            if (dbgEnable) { log.debug "$light; Parsing response: $data for $id" }
            setHueProperty(light, [state: data.state, action: data.action])
        }
    }
}

void parseLightsV2(json) {
    json.each { data ->
        String id = data.id

        // Add code to update all installed lights state
        def light = getChildDevice(networkIdForLight(id))
        if (!light) {
            light = getChildDevice(networkIdForLight(data.id_v1.replaceFirst('/lights/','')))
        }

        if (light) {
            if (dbgEnable) { log.debug "$light; Parsing response: $data for $id" }
            Map state = [:]
            Map events = parseDeviceHandler(data)
            setHueProperty(light, [state: events.state, action: events.action])
        }
    }
}

void parseScenes(json) {
    //json.each { id, value ->
    //    if (dbgEnable) { log.debug "${id}" }
    //}
}

/**
 * Parses the JSON response containing sensor data and updates the state of installed sensors.
 *
 * @param json The JSON object containing sensor data, where each key is a sensor ID and each value is the sensor data.
 */
void parseSensors(json) {
    json.each { id, data ->
        // Add code to update all installed groups state
        def sensor = getChildDevice(networkIdForSensor(id))
        if (sensor) {
            if (dbgEnable) { log.debug "$sensor; Parsing response: $data for $id" }
            setHueProperty(sensor, [state: data.state, action: data.action])
        }
    }
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
    def result = getAtomicState(child)[attributeName]
    if (result != null) {
        return result
    }
    result = (device.deviceId ? device : device.device).currentValue(attributeName)
    if (result != null) {
        getAtomicState(device)[attributeName] = result
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

    getAtomicState(device.device)[event.name] = event.value

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

    if (devstate.containsKey('colormode')) {
        switch (devstate.colormode) {
            case 'ct':
                devstate.remove('hue')
                devstate.remove('sat')
                devstate.remove('xy')
                break;
            case 'hs':
                devstate.remove('ct')
                devstate.remove('mirek')
                devstate.remove('xy')
                break;
            case 'xy':
                devstate.remove('hue')
                devstate.remove('sat')
                devstate.remove('mirek')
                break;
        }
    }

    // transform hue attributes and values to device attributes and values
    def events = devstate.collectEntries {  key, value ->
        switch (key) {
            case 'on':          ['switch':           value ? 'on' : 'off']; break
            case 'colormode':   ['colorMode':        convertFromHueColorMode(value)]; break
            case 'bri':         ['level':            convertFromHueLevel(value) as int]; break
            case 'brightness':  ['level':            convertFromHueLevel((value * 2.55) as int)]; break
            case 'hue':         ['hue':              convertFromHueHue(value)]; break
            case 'sat':         ['saturation':       convertFromHueSaturation(value)]; break
            case 'ct':          ['colorTemperature': convertFromHueColorTemp(value)]; break
            case 'mirek':       ['colorTemperature': convertFromHueColorTemp(value)]; break
            case 'presence':    ['motion':           value ? 'active' : 'inactive']; break
            case 'lightlevel':  ['illuminance':      convertToHueLightLevel(value)]; break
            case 'temperature': ['temperature':      convertFromHueTemperature(value)]; break
            default:           [key, value]
        }
    }.findAll { key, value -> child.hasAttribute("$key") }

    // If the location temperature scale is not Celsius, then convert the temperature to Fahrenheit
    if (location.temperatureScale != 'C' && events.temperature != null) {
        events.temperature = (events.temperature * 1.8 + 32).setScale(1, RoundingMode.HALF_UP)
    }

    if (args.config) {
        events << args.config.collectEntries { key, value ->
            switch (key) {
                case 'on':          ['status':           value ? 'enabled'   : 'disabled']; break
                case 'reachable':   ['health':           value ? 'reachable' : 'unreachable']; break
                case 'battery':     ['battery':          value]; break
                default: [key, value]
            }
        }.findAll {key, value -> child.hasAttribute("$key") }
    }

    // log.debug "Received State ($child): $devstate"
    // log.debug "Translated Events ($child): $events"

    if (child.hasAttribute('colorMode') && devstate.colormode == 'xy') {
        if (devstate.containsKey('xy')) {
            List xy = devstate.xy
            double bri
            if (events.level != null) {
                bri = events.level as double
            } else {
                bri = child.currentValue('level') as double
            }

            Map gamut = state.lights_v2[args.id]?.color?.gamut
            if (!gamut) {
                gamut = [
                    "red":   ["x":1.0, "y":0.0],
                    "green": ["x":0.0, "y":1.0],
                    "blue":  ["x":0.0, "y":0.0]
                ]
            }

            String gamutType = state.lights_v2[args.id]?.color?.gamut_type
            List<Double> rawCalib
            switch (gamutType) {
                case "C":
                    rawCalib = [5.0, 13.0, 28.0, 43.0, 59.0, 84.0]
                    break
                default:
                     rawCalib = [0.0, 17.0, 33.0, 50.0, 66.7, 84.0]
            }

            if (dbgEnable) { log.debug "Converting XY: $xy, bri: $bri, GamutType: $gamutType, Gamut: $gamut, Correction: $rawCalib" }
            List<Double> hs = xyToHSV(xy[0] as double, xy[1] as double, bri, gamut, rawCalib)
            events.hue = Math.round(hs[0]) as int
            events.saturation = Math.round(hs[1]) as int
            if (dbgEnable) { log.debug "Converted XY: $xy, bri: $bri, Gamut: $gamut to HS: $hs" }
        }
    }

    // Cap level to an integer value between 0 and 100
    if (events.level != null) {
        if (events.level < 1 && events.switch == 'off') {
            events.level = 0
        } else {
            events.level = valueBetween(events.level as int, 1, 100)
        }
    }

    // If an event has the colormode, and the associated color mode's attributes, then compute color name
    if (child.hasAttribute('colorName') && isHubVersion('2.3.2')) {
        switch (events.colorMode) {
            case 'CT':
                events.colorName = convertTemperatureToGenericColorName(events.colorTemperature)
                break

            case 'RGB':
                if (events.containsKey('hue') && events.containsKey('saturation')) {
                    Integer hue = events.hue
                    Integer sat = events.saturation
                    if (hue != null && sat != null) {
                        events.colorName = convertHueToGenericColorName(hue, sat)
                    }
                }
                break
        }
    }

    // Processes events for a child device and sends the appropriate events to the child device.
    // The method performs the following steps:
    // 1. Removes the 'switch' event from the events map and checks if it is 'on'. If so, sends a 'switch' event to the child device.
    // 2. Removes the 'level' and 'colorName' events from the events map and sends them to the child device if they are not null.
    // 3. Sends all remaining events in the events map to the child device.
    // 4. If the 'switch' event is 'off', sends a 'switch' event to the child device and turns off all child devices.
    def sw = events.remove('switch')
    if (sw == 'on') {
        sendChildEvent(child, [name: 'switch', value: sw])
    }
    def level = events.remove('level')
    def colorName = events.remove('colorName')
    if (level != null) { sendChildEvent(child, [name: 'level', value: level]) }
    if (colorName != null) { sendChildEvent(child, [name: 'colorName', value: colorName]) }

    // Send all events to the child device and if the device is being turned off, turn all child devices off too.
    events.each { key, value -> sendChildEvent(child, [name: key, value: value]) }

    if (sw == 'off') { 
        sendChildEvent(child, [name: 'switch', value: sw])
        // if the device is being turned off, then turn all child devices off too.
        child.getChildDevices().each { dev -> sendChildEvent(dev, [name: 'switch', value: 'off']) }
    }

    // child.getCapabilities().each { log.info "${it.name} = ${it.reference} :: ${it.toString()}" }

    // TODO: Get the current color of the device via a device refresh

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
    Float heLevel = convertToHueLevel(level as int)
    Map args = ['on': (heLevel > 0), 'bri': heLevel]
    if (duration != null) { args['transitiontime'] = (duration as int) * 10 }

    setDeviceState(child, args)
}

void componentStartLevelChange(child, direction) {
    Integer level = 0
    if (direction == 'up')        { level =  254 }
    else if (direction == 'down') { level = -254 }
    Map value = ['bri_inc': level, 'transitiontime': transitionTime() * 10]
    if (direction == 'up' && currentValue(child, 'switch') != 'on') {
        value['on'] = true
    }
    setDeviceState(child, value)

}

void componentStopLevelChange(def child) {
    setDeviceState(child, ['bri_inc': 0])
}

void componentSetColor(def child, Map colormap) {
    if (colormap?.hue == null)        { colormap.hue        = currentValue(child, 'hue') ?: 0 }
    if (colormap?.saturation == null) { colormap.saturation = currentValue(child, 'saturation') ?: 50 }
    if (colormap?.level == null)      { colormap.level      = currentValue(child, 'level') ?: 100 }

    Map args = ['on': colormap.level > 0,
                'hue': convertToHueHue(colormap.hue as int),
                'sat': convertToHueSaturation(colormap.saturation as int),
                'bri': convertToHueLevel(colormap.level as int)]

    setDeviceState(child, args)

}

void componentSetHue(def child, hue) {
    setDeviceState(child, ['on': true, 'hue': convertToHueHue(hue as int)])
}

void componentSetSaturation(def child, saturation) {
    setDeviceState(child, ['on': true, 'sat': convertToHueSaturation(saturation as int)])
}

void componentSetColorTemperature(def child, colortemperature, level = null, transitionTime = null) {
    Map values = [
        'on': (level?:100) > 0,
        'ct': convertToHueColortemp(colortemperature as int)]
    if (level)          { values['bri']            = convertToHueLevel(level as int) }
    if (transitionTime) { values['transitiontime'] = (transitionTime as int) * 10}

    setDeviceState(child, values)
}

Integer transitionTime() {
    2
}

//
// Utility Functions
//

boolean isHubVersion(String versionString) {
    if (!(versionString ==~ /^\d+(\.\d+){2,3}$/)) { return false }
    List minVer = versionString.split('\\.')
    List curVer = getLocation().getHub().firmwareVersionString.split('\\.')
    for (int i=0; i< minVer.size(); i++) {
        if (i >= curVer.size) { return false }
        int c=curVer[i] as int, m=minVer[i] as int
        if (c > m) { return true }
        if (c < m) { return false }
    }
    return true
}

void syncState(List devices) {

}

private Map parseDeviceHandler(Map data) {
    Map event = [state:[:], action:[:], config:[:]]

    if (!data.id_v1) {
        if (this[SETTING_DBG_ENABLE]) { log.debug "Received unparsable v2 only message; missing id_v1: ${data}" }
        return [:]
    }
    String idV1 = (data.id_v1.split('/') as List).last()

    String dataType = data.type
    switch (dataType) {
        case 'light':
            // log.info "Parsing event: ${data}"

            String nid = networkIdForLight(data.id)
            if (!getChildDevice(nid)) {
                nid = networkIdForLight(idV1)
            }
            if (!getChildDevice(nid)) { break }

            event.state << mapEventState(data)
            event.config << event.state.config
            event.state.remove('config')

            return [(nid): event]

        case 'grouped_light':
            //log.info "Parsing event (grouped_light): ${data}"

            String nid = networkIdForGroup(data.id)
            if (!getChildDevice(nid)) {
                nid = networkIdForGroup(idV1)
            }
            if (!parent.getChildDevice(nid)) { break }

            Map pevent = mapEventState(data)
            event.action << pevent
            event.config << event.action.config
            event.action.remove('config')

            return [(nid): event]

        case 'motion':
        case 'temperature':
        case 'light_level':
        case 'device_power':
            //log.info "Parsing event: ${data}"

            String nid = networkIdForSensor(data.id)
            if (!getChildDevice(nid)) {
                nid = networkIdForSensor(idV1)
            }
            if (!parent.getChildDevice(nid)) { break }

            event.state << mapEventState(data)
            event.config << event.state.config
            event.state.remove('config')

            return [(nid): event]

        case 'button':
            String nid = networkIdForSensor(data.id)
            def child = parent.getChildDevice(nid)
            if (!child) {
                nid = networkIdForSensor(idV1)
                child = parent.getChildDevice(nid)
            }
            if (!child) { break }

            Map props = [
                id: data.id,
                last_event: data.button.last_event
            ]
            child.setHueProperty(props)
            break

        case 'zigbee_connectivity':
            break

        default:
            if (this[SETTING_DBG_ENABLE]) {
                log.warn "Unhandled event data type: ${dataType}"
                log.debug "$data"
            }
    }
    return [:]
}

private Map mapEventState(data) {

    Map result = [config:[:]]
    if (data.on)      { result << data.on }
    if (data.dimming) { result << data.dimming }
    if (data.color)   {
        result << [colormode: 'xy']
        result << [xy: [data.color.xy.x, data.color.xy.y]]
    }
    if (data.color_temperature && data.color_temperature.mirek_valid == true) {
        result << [colormode: 'ct']
        result << data.color_temperature.find { k, v -> k == 'mirek' }
    }

    if (data.motion?.motion_valid)           { result << [presence:    data.motion.motion] }
    if (data.temperature?.temperature_valid) { result << [temperature: data.temperature.temperature * 100.0] }
    if (data.light?.light_level_valid)       { result << [lightlevel:  data.light.light_level] }

    // device health attributes
    if (data.power_state)                    { result.config << [battery:   data.power_state.battery_level] }
    if (data.enabled != null)                { result.config << [on:        data.enabled] }
    if (data.status != null)                 { result.config << [reachable: data.status == 'connected'] }
    return result
}

// -----------------
// Helper functions
// -----------------


// -------------------------
// Helper Functions
// -------------------------

/**
 * Clamp a value between min and max.
 */
BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
    return value.max(min).min(max)
}

/**
 * Convert gamut map to a list of BigDecimal points.
 */
List<List<BigDecimal>> parseGamut(Map<String, Map<String, Double>> gamut) {
    return gamut.collect { k, v -> [new BigDecimal(v.x.toString()), new BigDecimal(v.y.toString())] }
}

/**
 * Check if a given (x, y) point is inside the gamut triangle.
 */
boolean isInsideGamut(BigDecimal x, BigDecimal y, List<List<BigDecimal>> gamut) {
    BigDecimal Xr = gamut[0][0], Yr = gamut[0][1]
    BigDecimal Xg = gamut[1][0], Yg = gamut[1][1]
    BigDecimal Xb = gamut[2][0], Yb = gamut[2][1]
    BigDecimal detT = (Xg - Xr) * (Yb - Yr) - (Xb - Xr) * (Yg - Yr)
    BigDecimal alpha = ((x - Xr) * (Yb - Yr) - (y - Yr) * (Xb - Xr)) / detT
    BigDecimal beta = ((Xg - Xr) * (y - Yr) - (Yg - Yr) * (x - Xr)) / detT
    BigDecimal gamma = BigDecimal.ONE - alpha - beta
    return (alpha >= BigDecimal.ZERO && beta >= BigDecimal.ZERO && gamma >= BigDecimal.ZERO)
}

/**
 * Clamp an (x, y) point to the closest point inside the light's gamut triangle.
 */
List<BigDecimal> clampXYtoGamut(BigDecimal x, BigDecimal y, List<List<BigDecimal>> gamut) {
    if (isInsideGamut(x, y, gamut)) {
        return [x, y]
    }
    List<List<BigDecimal>> edges = [[gamut[0], gamut[1]], [gamut[1], gamut[2]], [gamut[2], gamut[0]]]
    List<BigDecimal> closestPoint = gamut[0]
    BigDecimal minDist = BigDecimal.valueOf(Double.MAX_VALUE)
    for (List<BigDecimal> edge : edges) {
        List<BigDecimal> A = edge[0], B = edge[1]
        BigDecimal Ax = A[0], Ay = A[1], Bx = B[0], By = B[1]
        BigDecimal t = ((x - Ax) * (Bx - Ax) + (y - Ay) * (By - Ay)) /
                      ((Bx - Ax).pow(2) + (By - Ay).pow(2))
        t = clamp(t, BigDecimal.ZERO, BigDecimal.ONE)
        BigDecimal Px = Ax + t * (Bx - Ax)
        BigDecimal Py = Ay + t * (By - Ay)
        BigDecimal dist = (x - Px).pow(2) + (y - Py).pow(2)
        if (dist < minDist) {
            minDist = dist
            closestPoint = [Px, Py]
        }
    }
    return closestPoint
}

/**
 * Convert RGB to Hue & Saturation.
 * Returns [hue (0–100), saturation (0–100)].
 */
List<BigDecimal> rgbToHS(BigDecimal r, BigDecimal g, BigDecimal b) {
    BigDecimal max = [r, g, b].max()
    BigDecimal min = [r, g, b].min()
    BigDecimal delta = max - min
    BigDecimal hue = BigDecimal.ZERO
    if (delta.compareTo(BigDecimal.ZERO) != 0) {
        if (max == r) {
            hue = ((g - b) / delta).remainder(BigDecimal.valueOf(6))
        } else if (max == g) {
            hue = ((b - r) / delta).add(BigDecimal.valueOf(2))
        } else {
            hue = ((r - g) / delta).add(BigDecimal.valueOf(4))
        }
        hue = hue.multiply(BigDecimal.valueOf(60))
        if (hue.compareTo(BigDecimal.ZERO) < 0) {
            hue = hue.add(BigDecimal.valueOf(360))
        }
    }
    // Scale hue from degrees (0–360) to 0–100.
    hue = hue.divide(BigDecimal.valueOf(360), 10, RoundingMode.HALF_UP)
             .multiply(BigDecimal.valueOf(100))
    BigDecimal saturation = (max.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO :
        delta.divide(max, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
    return [hue, saturation]
}

/**
 * Convert HSV (hue, saturation, brightness on a 0–100 scale) to RGB (0–255 scale).
 * Hue is first scaled from 0–100 to degrees.
 */
List<Integer> hsvToRGB(double hue, double sat, double bri) {
    double H = hue * 3.6   // Convert 0–100 to 0–360.
    double S = sat / 100.0
    double V = bri / 100.0
    double C = V * S
    double X = C * (1 - Math.abs(((H / 60.0) % 2) - 1))
    double m = V - C
    double r = 0, g = 0, b = 0
    if (H < 60) {
        r = C; g = X; b = 0
    } else if (H < 120) {
        r = X; g = C; b = 0
    } else if (H < 180) {
        r = 0; g = C; b = X
    } else if (H < 240) {
        r = 0; g = X; b = C
    } else if (H < 300) {
        r = X; g = 0; b = C
    } else {
        r = C; g = 0; b = X
    }
    int R = (int)Math.round((r + m) * 255)
    int G = (int)Math.round((g + m) * 255)
    int B = (int)Math.round((b + m) * 255)
    return [R, G, B]
}

/**
 * Inverse sRGB gamma correction.
 * Converts a gamma‐corrected channel (in [0,1]) back to linear light.
 */
double inverseGamma(double channel) {
    if (channel <= 0.04045) {
        return channel / 12.92
    } else {
        return Math.pow((channel + 0.055) / 1.055, 2.4)
    }
}

// -------------------------
// Hue Correction via Lagrange Interpolation
// -------------------------

/**
 * Applies Lagrange interpolation to compute the corrected hue.
 * Uses static intended calibration values:
 *   Red: 0, Yellow: 17, Green: 33.3, Cyan: 50, Blue: 66.6, Magenta: 84.
 *
 * @param rawHue   The raw hue value (0–100).
 * @param rawCalib List of raw hue calibration values (0–100) for the six points.
 * @return         Corrected hue.
 */
double applyHueCorrectionLagrange(double rawHue, List<Double> rawCalib) {
    List<Double> intendedCalib = [0.0, 17.0, 33.3, 50.0, 66.6, 84.0]
    int n = rawCalib.size()
    double result = 0.0
    for (int i = 0; i < n; i++) {
        double term = intendedCalib[i]
        for (int j = 0; j < n; j++) {
            if (i != j) {
                term *= (rawHue - rawCalib[j]) / (rawCalib[i] - rawCalib[j])
            }
        }
        result += term
    }
    return result
}

/**
 * Inverts the hue correction function.
 * Given a corrected hue (0–100), finds the raw hue (0–100) such that
 * applyHueCorrectionLagrange(rawHue, rawCalib) approximates the corrected hue.
 * Assumes the mapping is monotonic.
 */
double invertHueCorrection(double correctedHue, List<Double> rawCalib) {
    double low = 0.0, high = 100.0, mid = 0.0
    double tol = 0.01
    int iterations = 0, maxIter = 100
    while ((high - low) > tol && iterations < maxIter) {
        mid = (low + high) / 2.0
        double test = applyHueCorrectionLagrange(mid, rawCalib)
        if (test < correctedHue) {
            low = mid
        } else {
            high = mid
        }
        iterations++
    }
    return mid
}

// -------------------------
// Inverse Philips Conversion: Linear RGB -> XYZ
// -------------------------
/**
 * Converts linear RGB values (each in [0,1]) to XYZ using the inverse Philips matrix.
 * The inverse matrix here is an approximation.
 */
List<Double> linearRGBtoXYZ(double r_lin, double g_lin, double b_lin) {
    // Forward Philips matrix:
    //   r = 1.612*X - 0.203*Y - 0.302*Z
    //   g = -0.509*X + 1.412*Y + 0.066*Z
    //   b = 0.026*X - 0.072*Y + 0.962*Z
    // An approximate inverse is:
    double m00 = 0.6496, m01 = 0.1034, m02 = 0.1970
    double m10 = 0.2340, m11 = 0.7430, m12 = 0.0226
    double m20 = -0.00003, m21 = 0.0529, m22 = 1.0363
    double X = m00 * r_lin + m01 * g_lin + m02 * b_lin
    double Y = m10 * r_lin + m11 * g_lin + m12 * b_lin
    double Z = m20 * r_lin + m21 * g_lin + m22 * b_lin
    return [X, Y, Z]
}

// -------------------------
// Forward Conversion: XY+Bri -> Corrected HSV
// -------------------------
/**
 * Convert Philips Hue XY and brightness values to corrected HSV (0–100 scale).
 * Returns a list: [corrected hue, saturation, brightness].
 * Brightness is passed through.
 *
 * This function performs gamut clamping, Philips conversion (XYZ→RGB + gamma correction),
 * then computes raw HSV and applies hue correction via Lagrange interpolation.
 *
 * @param x        The x chromaticity coordinate.
 * @param y        The y chromaticity coordinate.
 * @param bri      The brightness value (0–100).
 * @param gamut    A map defining the light's gamut with keys "red", "green", and "blue".
 * @param rawCalib List<Double> of raw hue calibration values (0–100 scale) for six points.
 * @return         A list [corrected hue, saturation, brightness] (all on 0–100 scale).
 */
List<Double> xyToHSV(Double x, Double y, Double bri, Map<String, Map<String, Double>> gamut, List<Double> rawCalib) {
    // Convert inputs to BigDecimal.
    BigDecimal X = new BigDecimal(x.toString())
    BigDecimal Y = new BigDecimal(y.toString())
    BigDecimal brightnessPct = new BigDecimal(bri.toString())

    // Clamp XY to the gamut.
    List<List<BigDecimal>> gamutBD = parseGamut(gamut)
    List<BigDecimal> clampedXY = clampXYtoGamut(X, Y, gamutBD)
    BigDecimal clampedX = clampedXY[0]
    BigDecimal clampedY = clampedXY[1]

    // Normalize brightness from 0–100 to 0–1.
    BigDecimal briNorm = brightnessPct.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)

    // Compute XYZ (Y set by brightness).
    BigDecimal Yval = briNorm
    BigDecimal Xval = (Yval.divide(clampedY, 10, RoundingMode.HALF_UP)).multiply(clampedX)
    BigDecimal Zval = (Yval.divide(clampedY, 10, RoundingMode.HALF_UP))
                        .multiply(BigDecimal.ONE.subtract(clampedX).subtract(clampedY))

    // Convert XYZ to linear RGB using Philips coefficients.
    BigDecimal r = Xval.multiply(BigDecimal.valueOf(1.612))
                      .subtract(Yval.multiply(BigDecimal.valueOf(0.203)))
                      .subtract(Zval.multiply(BigDecimal.valueOf(0.302)))
    BigDecimal g = Xval.multiply(BigDecimal.valueOf(-0.509))
                      .add(Yval.multiply(BigDecimal.valueOf(1.412)))
                      .add(Zval.multiply(BigDecimal.valueOf(0.066)))
    BigDecimal bVal = Xval.multiply(BigDecimal.valueOf(0.026))
                         .subtract(Yval.multiply(BigDecimal.valueOf(0.072)))
                         .add(Zval.multiply(BigDecimal.valueOf(0.962)))
    // Clamp negatives.
    r = r.max(BigDecimal.ZERO)
    g = g.max(BigDecimal.ZERO)
    bVal = bVal.max(BigDecimal.ZERO)
    // Normalize if any channel > 1.
    BigDecimal maxRGB = [r, g, bVal].max()
    if (maxRGB.compareTo(BigDecimal.ONE) > 0) {
         r = r.divide(maxRGB, 10, RoundingMode.HALF_UP)
         g = g.divide(maxRGB, 10, RoundingMode.HALF_UP)
         bVal = bVal.divide(maxRGB, 10, RoundingMode.HALF_UP)
    }
    // Apply sRGB gamma correction.
    def gammaCorrect = { BigDecimal channel ->
        if (channel.compareTo(BigDecimal.valueOf(0.0031308)) > 0) {
            return BigDecimal.valueOf(1.055 * Math.pow(channel.doubleValue(), 1.0/2.4) - 0.055)
        } else {
            return channel.multiply(BigDecimal.valueOf(12.92))
        }
    }
    r = gammaCorrect(r)
    g = gammaCorrect(g)
    bVal = gammaCorrect(bVal)
    r = clamp(r, BigDecimal.ZERO, BigDecimal.ONE)
    g = clamp(g, BigDecimal.ZERO, BigDecimal.ONE)
    bVal = clamp(bVal, BigDecimal.ZERO, BigDecimal.ONE)
    
    // Convert RGB to raw hue and saturation.
    List<BigDecimal> hs = rgbToHS(r, g, bVal)
    double rawHue = hs[0].doubleValue() // on 0–100 scale.
    double saturation = hs[1].doubleValue()
    
    // Apply hue correction.
    double correctedHue = applyHueCorrectionLagrange(rawHue, rawCalib)
    correctedHue = Math.max(0, Math.min(100, correctedHue))
    
    return [correctedHue, saturation, brightnessPct.doubleValue()]
}

// -------------------------
// Reverse Conversion: Corrected HSV -> XY+Bri
// -------------------------
/**
 * Convert corrected HSV (0–100 scale) to XY and brightness.
 * Inverts the hue correction via binary search, converts raw HSV to RGB,
 * applies inverse gamma correction, inverts Philips conversion, and computes chromaticity.
 *
 * @param correctedHue Corrected hue (0–100).
 * @param sat          Saturation (0–100).
 * @param bri          Brightness (0–100).
 * @param rawCalib     List<Double> of raw hue calibration values (0–100) for six points.
 * @return             A list [x, y, brightness].
 */
List<Double> hsvToXY(double correctedHue, double sat, double bri, List<Double> rawCalib) {
    // 1. Invert hue correction to obtain raw hue.
    double rawHue = invertHueCorrection(correctedHue, rawCalib)
    
    // 2. Convert raw HSV to RGB.
    List<Integer> rgb = hsvToRGB(rawHue, sat, bri)  // RGB in 0–255.
    double R = rgb[0] / 255.0
    double G = rgb[1] / 255.0
    double B = rgb[2] / 255.0
    
    // 3. Inverse gamma correction.
    double r_lin = inverseGamma(R)
    double g_lin = inverseGamma(G)
    double b_lin = inverseGamma(B)
    
    // 4. Convert linear RGB to XYZ using inverse Philips matrix.
    List<Double> xyz = linearRGBtoXYZ(r_lin, g_lin, b_lin)
    double X = xyz[0], Y = xyz[1], Z = xyz[2]
    double sum = X + Y + Z
    if (sum == 0) sum = 1e-6
    double x = X / sum
    double y = Y / sum
    
    return [x, y, bri]
}


/**
* Ensures a value is between a minimum and maximum range.
*
* @param value The value to check.
* @param min The minimum allowable value.
* @param max The maximum allowable value.
* @return The value clamped between the min and max.
*/
static Number valueBetween(Number value, Number min, Number max) {
    return Math.max(min, Math.min(max, value))
}

/**
* Converts a hexadecimal string to an IP address.
*
* @param hex The hexadecimal string to convert.
* @return The IP address as a string.
*/
static String convertHexToIP(String hex) {
    if (hex == null || hex.length() != 8) {
        throw new IllegalArgumentException("Invalid hex string")
    }
    return [
        Integer.parseInt(hex.substring(0, 2), 16),
        Integer.parseInt(hex.substring(2, 4), 16),
        Integer.parseInt(hex.substring(4, 6), 16),
        Integer.parseInt(hex.substring(6, 8), 16)
    ].join('.')
}

/**
* Converts a hexadecimal string to an integer.
*
* @param hex The hexadecimal string to convert.
* @return The integer value.
*/
static Integer convertHexToInt(String hex) {
    if (hex == null) {
        throw new IllegalArgumentException("Invalid hex string")
    }
    return Integer.parseInt(hex, 16)
}

/**
* Converts a hue level value to a percentage.
*
* This function takes a hue level value and converts it to a percentage
* by dividing the value by 2.54 and rounding the result. The resulting
* value is then constrained to be between 1 and 100, except when the
* input value is 0, in which case the result is 0.
*
* @param value The hue level value to convert.
* @return The converted percentage value, constrained between 1 and 100,
*         or 0 if the input value is 0.
*/
static Integer convertFromHueLevel(Number value) {
    valueBetween(Math.round(value / 2.54), (value == 0 ? 0 : 1), 100)
}

/**
* Converts a given value to a Hue level.
*
* This function takes an integer value, multiplies it by 2.54, rounds the result,
* and ensures it is within the range of 0 to 254.
*
* @param value The integer value to be converted.
* @return The converted Hue level as an integer.
*/
static Integer convertToHueLevel(Integer value) {
    valueBetween(Math.round(value * 2.54), 0, 254)
}

/**
* Converts a hue value from the Hue system to a percentage.
*
* The Hue system uses a range of 0 to 65535 for hue values. This function
* converts that range to a percentage (0 to 100).
*
* @param value The hue value from the Hue system (0 to 65535).
* @return The corresponding percentage value (0 to 100).
*/
static Number convertFromHueHue(Number value) {
    Math.round(value / 655.35)
}

/**
* Converts a given value to a corresponding Hue hue value.
*
* The function performs the following conversions:
* - If the input value is 33, it returns 21845.
* - If the input value is 66, it returns 43690.
* - Otherwise, it scales the input value to a range between 0 and 65535.
*
* @param value The input value to be converted.
* @return The corresponding Hue hue value.
*/
static Number convertToHueHue(Number value) {
    value == 33 ? 21845 : value == 66 ? 43690 : valueBetween(Math.round(value * 655.35), 0, 65535)
}

/**
* Converts a given hue saturation value to a different scale.
*
* This function takes a hue saturation value and converts it by dividing it by 2.54
* and rounding the result to the nearest whole number.
*
* @param value The hue saturation value to be converted.
* @return The converted value as a Number.
*/
static Number convertFromHueSaturation(Number value) {
    Math.round(value / 2.54)
}

/**
* Converts a given value to a Hue-compatible saturation value.
*
* This function takes a numerical value, multiplies it by 2.54, rounds it to the nearest whole number,
* and then ensures the result is within the range of 0 to 254.
*
* @param value The numerical value to be converted.
* @return The converted Hue-compatible saturation value, constrained between 0 and 254.
*/
static Number convertToHueSaturation(Number value) {
    valueBetween(Math.round(value * 2.54), 0, 254)
}

/**
* Converts a Hue color temperature value to a standard color temperature value.
*
* @param value The Hue color temperature value to convert.
* @return The converted color temperature value, constrained between 2000 and 6500.
*/
static Number convertFromHueColorTemp(Number value) {
    // 4500 / 347 = 12.968 (but 12.96 scales better)
    valueBetween(Math.round(((500 - value) * 12.96) + 2000 ), 2000, 6500)
}

/**
* Converts a given color temperature value to the corresponding Hue color temperature value.
*
* @param value The color temperature value to convert (in Kelvin).
* @return The converted Hue color temperature value, constrained between 153 and 500.
*/
static Number convertToHueColortemp(Number value) {
    valueBetween(Math.round(500 - ((value - 2000) / 12.96)), 153, 500)
}

/**
* Converts a given light level to a Hue light level.
*
* This function takes a light level value and converts it to a Hue-compatible light level
* using a logarithmic scale. The conversion formula is:
* 
*     HueLightLevel = 10 ^ ((lightLevel - 1) / 10000.0)
*
* If the input light level is null, it defaults to 1.
*
* @param lightLevel The light level to be converted. If null, defaults to 1.
* @return The converted Hue light level as an integer.
*/
static Number convertToHueLightLevel(Number lightLevel) {
    Math.pow(10, (((lightLevel?:1)-1)/10000.0)) as int
}

/**
* Converts the given temperature to the Hue temperature scale.
*
* @param temperature The temperature value to be converted.
* @return The converted temperature value, scaled to one decimal place.
*/
static Number convertFromHueTemperature(Number temperature) {
    return (temperature?:0) / 100
}

/**
* Converts a Hue color mode value to a corresponding string representation.
*
* @param value The Hue color mode value to convert. Expected values are 'hs', 'ct', or 'xy'.
* @return A string representing the converted color mode. Returns 'RGB' for 'hs' and 'xy', 'CT' for 'ct', 
*         and an empty string for any other value.
*/
static String convertFromHueColorMode(String value) {
    if (value == 'hs') { return 'RGB'  }
    if (value == 'ct') { return 'CT' }
    if (value == 'xy') { return 'RGB' }
    return ''
}

