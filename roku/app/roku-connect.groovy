/**
 * Roku Connect
 * Version 1.2.1
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is an integration app for Hubitat designed to locate, and install any/all attached Roku devices.
 * The app uses SSDP auto-discovery (as supported by the Roku ECP protocol).  This integration app is not
 * required to use the Roku TV device, but if the app is installed, and used to manage the Roku TV devices,
 * then auto-discovery will keep the IP address up-to-date as the SSDP listener discovers that the Roku device's
 * IP Address has been changed.
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

definition(
    name:        'Roku Connect',
    namespace:   'apwelsh',
    author:      'Armand Welsh (apwelsh)',
    description: 'Roku Device Integration',
    category:    'Convenience',
    //importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/roku/app/roku-connect.groovy',
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: ''
)

preferences {
    page(name: 'mainPage')
    page(name: 'deviceDiscovery', title: 'Device Discovery', refreshTimeout:10)
    page(name: 'addSelectedDevices')
    page(name: 'configureDevice')
    page(name: 'changeName')
    page(name: 'manageApp')
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
    if (autoDiscovery) { runEvery5Minutes('ssdpDiscover') }
}

/*
 * Application Screens
 */

def mainPage() {

    if (!autoDiscovery) { unschedule('ssdpDiscover') }

    if (!state) {
        return dynamicPage(name: 'mainPage', title: 'Roku Connect', uninstall: true, install: true) {
            section {
                paragraph 'Hit Done to to install the Roku Connect Integration.\nRe-open to setup.'
            }
        }
    } else {
        app.removeSetting('deviceNetworkId')
        app.removeSetting('selectedDevice')
        app.removeSetting('selectedApps')
        return dynamicPage(name: 'mainPage', title: '', uninstall: true, install: true) {
            section (getFormat("title", "Roku Connect")) {
                paragraph getFormat("line")
            }
            section(){
                href 'deviceDiscovery', title:'Discover New Devices', description:''
            }
            section('Installed Devices'){
                getChildDevices().sort({ a, b -> a['label'] <=> b['label'] }).each {
                    def desc = it.label != it.name ? it.name : ''
                    href 'configureDevice', title:"${deviceLabel(it)}", description:desc, params: [netId: it.deviceNetworkId]
                }
            }
            section('Options') {
                input name: 'autoDiscovery', type: 'bool', defaultValue: true, title: 'Auto detect IP changes of Roku devices'
                input name: 'logEnable',     type: 'bool', defaultValue: true, title: 'Enable logging'
            }
            section() {
                paragraph getFormat("line")
                paragraph "<div style='color:#1A77C9;text-align:center'>Roku Connect<br><a href='https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Please consider donating. This app took a lot of work to make.<br>If you find it valuable, I'd certainly appreciate it!</div>"
            }      
        }
    }
}

def getFormat(type, myText=""){            // Borrowed from @dcmeglio HPM code 
	if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def deviceDiscovery() {
    if (logEnable)  { log.debug 'Searching for Hub additions and updates' }
    def refreshInterval = 30

    // Make sure we initiate a new search for Roku devices.
    ssdpSubscribe()
    ssdpDiscover()
    runEvery1Minute('ssdpDiscover')

    def installed = getChildDevices().collect { it.deviceNetworkId }
    def discovered = getDiscovered()

    def options = [:]
    if (discovered) {
        discovered.each {key, value ->
            if (installed.find { it == value.mac })  { return }
            options["${key}"] = "${value.name}"
        }
    }

    def numFound = options.size() ?: 0

    def uninstall = false
    def nextPage = selectedDevices ? 'addSelectedDevices' : null

    return dynamicPage(name:'deviceDiscovery', title:'Discovery Started!', nextPage:nextPage, refreshInterval:refreshInterval, uninstall:uninstall) {
        section('Please wait while we discover your Roku devices.  Discovery can take a few minutes, so sit back and relax. The page will reload automatically! Select your Roku below once discovered.') {
            input 'selectedDevices', 'enum', required:false, title:"Select Roku Devices to install (${numFound} found)", multiple:true, options:options, submitOnChange: true
        }
    }
}

def addSelectedDevices() {
    if (!selectedDevices) { return deviceDiscovery() }

    String subject = selectedDevices.size == 1 ? 'device' : 'devices'
    String title = ''
    String sectionText = ''

    def devices = selectedDevices.collect { it }  // clone the list, so as to not accidentally modify it
    Integer deviceCount = devices.size()

    selectedDevices.each { rokuId ->
        String name = state.discovered[rokuId].name
        String dni = state.discovered[rokuId].mac
        try {

            def child = addChildDevice('apwelsh', 'Roku TV', dni, [name: name, label: name])
            child.updateSetting('deviceIp', state.discovered[rokuId].networkAddress)
            child.updated()
            devices.remove(rokuId)

        } catch (ex) {
            if (ex.message =~ 'A device with the same device network ID exists.*') {
                sectionText = "\nA device with the same device network ID (${dni}) already exists; cannot add Group [${name}]"
            } else {
                sectionText += "\nFailed to add group [${name}]; see logs for details"
                if (logEnable)  { log.error "${ex}" }
            }
        }
    }

    if (!devices) { app.removeSetting('selectedDevices') }

    if (!sectionText) {
        title = "Adding ${deviceCount} Roku ${subject} to Hubitat"
        sectionText = "Added Roku ${subject}"
    } else {
        title = "Failed to add Roku ${subject}"
    }

    return dynamicPage(name:'addSelectedDevices', title:title, nextPage:null) {
        section() {
            paragraph sectionText
        }
    }

}

def configureDevice(params) {
    app.removeSetting('applicationNetworkId')

    def networkId = params?.netId ?: settings['deviceNetworkId']
    if (!networkId)  { return mainPage() }

    def child = getChildDevice(networkId)
    if (!child)
        return mainPage()

    app.updateSetting('deviceNetworkId', networkId)

    def rokuApps = child.getInstalledApps()
    def installedApps = child.getChildDevices().collect { it.deviceNetworkId}.findAll { it =~ /^.*\-\d+$/ }
    def selectedApps = settings["${networkId}_selectedApps"] ?: []

    // check the input for selected Apps to see if defined, and if not, pre-load the values
    // if (!settings["${networkId}_selectedApps"]) {
    //     installedApps.each { selectedApps << it }
    // }

    // Remove unselected apps as children
    installedApps?.findAll { !selectedApps.contains(it) }.each { appId ->
        if (logEnable)  { log.info "Removing child application device ${appId} (${rokuApps[appId]})" }
        child.deleteChildAppDevice(appId)
    }

    // Add selected apps as children
    selectedApps.findAll { !installedApps.contains(it) }.each { appId ->
        def appName = rokuApps[appId]
        if (logEnable)  { log.info "Installing child application device ${appId} (${appName})" }
        child.createChildAppDevice(appId, appName)
    }

    app.updateSetting("${networkId}_selectedApps", selectedApps)

    String label = (deviceLabel(child)?:'').trim()
    String newLabel = (settings["${networkId}_label"] ?: '').trim()
    if (newLabel != '' && label != newLabel) {
        renameChildDevice(this, networkId, newLabel)
        child.getChildDevices().findAll { deviceLabel(it)?.startsWith(label) }.each {
            renameChildDevice(child, it.deviceNetworkId, deviceLabel(it).replace(label, newLabel))
        }

        label = newLabel
    }

    app.removeSetting("${networkId}_label")

    return dynamicPage(name:'configureDevice', title:'Configure device', nextPage:null) {
        section() {
            paragraph 'Use this section to configure your Roku device settings'
            input "${networkId}_label", 'text', title: 'Device name', defaultValue:label, submitOnChange: true
        }

        section('Add / Remove Applications') {
            //href 'appDiscovery', title:'Add/Remove Roku Apps', description:'', params: params
            input "${networkId}_selectedApps", 'enum', title: 'Select Apps to use as switch devices, unlselect Apps to remove the switch device', required: flase, multiple: true, options: rokuApps, submitOnChange: true
        }

        section('Manage installed apps') {
            child.getChildDevices()?.sort({ a, b -> a['label'] <=> b['label'] }).findAll { it.deviceNetworkId =~ /^.*\-\d+$/ }.each {
                def desc = it.label != it.name ? it.name : ''
                href 'manageApp', title:"<img src='${child.iconPathForApp(it.deviceNetworkId)}' style='width:auto; height:1em'/> ${desc}", description:'', params: [netId: networkId, appId: it.deviceNetworkId]

            }
        }
    }
}

def manageApp(params) {
    def networkId = params?.netId ?: settings['deviceNetworkId']
    def appId     = params?.appId ?: settings['applicationNetworkId']
    if (!networkId || !appId) {
        app.removeSetting('applicationNetworkId')
        return configureDevice()
    }

    def child = getChildDevice(networkId)

    if (!child)
        return mainPage()

    // track state to backup to.
    app.updateSetting('deviceNetworkId', networkId)

    def device = child.getChildDevice(appId)
    String label = (deviceLabel(device) ?: '').trim()
    String newLabel = (settings["${appId}_label"] ?: '').trim()

    if (newLabel != '' && label != newLabel) {
        renameChildDevice(child, appId, newLabel)
        label = newLabel
    }
    app.removeSetting("${appId}_label")

    return dynamicPage(name: 'manageApp', title:"Manage Installed Apps for ${deviceLabel(child)}", nextPage:null) {
        section() {
            paragraph "Use this section to set the ${device.name} application name for ${deviceLabel(child)}"
            input "${appId}_label", 'text', title: 'Application name', defaultValue:label, submitOnChange: true
            paragraph "<img src='${child.iconPathForApp(appId)}'/>"

        }
    }

}

/*
 * SSDP Device Discover
 */

void ssdpSubscribe() {
    subscribe(location, 'ssdpTerm.roku:ecp', ssdpHandler)
}

void ssdpUnsubscribe() {
    unsubscribe(ssdpHandler)
}

void ssdpDiscover() {
    sendHubCommand(new hubitat.device.HubAction('lan discovery roku:ecp', hubitat.device.Protocol.LAN))
}

def ssdpHandler(event) {

    def parsedEvent = parseLanMessage(event.description)

    def roku = parsedEvent?.ssdpUSN.replaceAll(~/.*\:/,'')
    if (parsedEvent.networkAddress) {
        parsedEvent << ['roku':roku,
                        'networkAddress': convertHexToIP(parsedEvent.networkAddress),
                        'deviceAddress': convertHexToInt(parsedEvent.deviceAddress)]

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

    if (logEnabe)  { log.info "Verifying ${event.networkAddress}" }

    // Using the httpGet method, and arrow function, perform the validation check w/o the need for a callback function.
    httpGet([uri:"http://${event.networkAddress}:${event.deviceAddress}${ssdpPath}",timeout:5]) { response ->

        if (!response.isSuccess()) { return }

        def data = response.data
        if (data) {
            def device = data.device
            String model = device.modelName
            String ssdpUSN = "${event.ssdpUSN.toString()}"

            if (logEnable)  { log.debug "Identified model: ${model}" }
            def hubId = "${event.mac}"[-6..-1]
            String name = "${device.friendlyName} (${hubId})"

            event << [url: "${data.URLBase}",
                      name: name,
                      serialNumber: "${device.serialNumber}"]

            def discovered = getDiscovered()
            discovered << ["${ssdpUSN}": event]
            if (logEnable)  { log.debug "Discovered new Roku: ${name}" }
            cleanupOrphans(hubId)
        }

    }}

private updateDevice(event) {
    def ssdpUSN = event.ssdpUSN.toString()
    def discovered = getDiscovered()
    def roku = discovered["${ssdpUSN}"]

    if (roku.networkAddress != event.networkAddress || roku.deviceAddress != event.deviceAddress) {
        def oldPort = roku.deviceAddress
        def oldAddress = roku.networkAddress
        roku << ['networkAddress': event.networkAddress,
                 'deviceAddress':  event.deviceAddress]

        if (logEnable)  { log.debug "Detected roku address update: ${roku.name} from ${oldAddress}:${oldPort} to ${event.networkAddress}:${event.deviceAddress}" }
    }

    def child = getChildDevice(roku.mac)
    if (child) {
        child.updateIpAddress(roku.networkAddress)
    } else {
        cleanupOrphans(roku.mac)
    }
}

private cleanupOrphans(hubId) {
    if (getChildDevice(hubId))  { return }

    def orphans = settings.collect { key, value -> key }.findAll { it =~ /^${hubId}((\-\w+)?_\w+)?$/ }
    orphans.each { key ->
        app.removeSetting("${key}")
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
        if (logEnable)  { log.info "Renaming ${child.label} to ${name}" }
        child.label = name
    }
}

/*
 * Device Helpers
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

private String deviceLabel(device) {
    device?.label ?: device?.name
}
