    /**
    * Advanced Philips Hue Bridge Integration application
    * Version 1.5.2
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
    @Field static final String PAGE_UNLINK = 'unlink'
    @Field static final String PAGE_FIND_LIGHTS = 'findLights'
    @Field static final String PAGE_ADD_LIGHTS = 'addLights'
    @Field static final String PAGE_FIND_GROUPS = 'findGroups'
    @Field static final String PAGE_ADD_GROUPS = 'addGroups'
    @Field static final String PAGE_FIND_SCENES = 'findScenes'
    @Field static final String PAGE_ADD_SCENES = 'addScenes'
    @Field static final String PAGE_FIND_SENSORS = 'findSensors'
    @Field static final String PAGE_ADD_SENSORS = 'addSensors'

    @Field static Map volatileAtomicStateByDeviceId = new ConcurrentHashMap()
    @Field static Map volatileAtomicQueuebyDeviceId = new ConcurrentHashMap()
    @Field static Map volatileAtomicRefreshQueue = new ConcurrentHashMap()

    preferences {
        page(name: PAGE_MAINPAGE)
        page(name: PAGE_BRIDGE_DISCOVERY, title: 'Device Discovery', refreshTimeout:PAGE_REFRESH_TIMEOUT)
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

    synchronized Map getVolatileAtomicState(def device) { 
        Integer deviceId = device.deviceId ?: device.device.deviceId
        Map result = volatileAtomicStateByDeviceId.get(deviceId)
        if (result == null) {
            result = new ConcurrentHashMap()
            volatileAtomicStateByDeviceId[deviceId] = result
        }
        return result
    }

    synchronized Map getVolatileAtomicQueue(def device) { 
        Integer deviceId = device.deviceId ?: device.device.deviceId
        Map result = volatileAtomicQueuebyDeviceId.get(deviceId)
        if (result == null) {
            result = new ConcurrentHashMap()
            volatileAtomicQueuebyDeviceId[deviceId] = result
        }
        return result
    }

    synchronized Map getRefreshQueue() { 
        Integer appId = app.getId()
        Map result = volatileAtomicRefreshQueue.get(appId)
        if (result == null) {
            result = new ConcurrentHashMap()
            volatileAtomicRefreshQueue[appId] = result
        }
        return result
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
        if (!bridgeHost) {
            return bridgeDiscovery()
        }

        if (params.nextPage==PAGE_BRIDGE_LINKING) {
            return bridgeLinking()
        }

        String title
        if (selectedDevice) {
            discoveredHubs()[selectedDevice]
            title=discoveredHubs()[selectedDevice]
        } else {
            title='Find Bridge'
        }

        Boolean uninstall = bridgeHost ? false : true

        return dynamicPage(name: PAGE_MAINPAGE, title: '', nextPage: null, uninstall: uninstall, install: true) {
                section (getFormat("title", "Advanced Hue Bridge")) {
                    paragraph getFormat("subtitle", "Manage your linked Hue Bridge")
                    paragraph getFormat("line")
                }

            if (selectedDevice == null) {
                section('Setup'){
                    paragraph 'To begin, select Find Bridge to start searching for your Hue Bride.'
                    href PAGE_BRIDGE_DISCOVERY, title:'Find Bridge', description:''//, params: [pbutton: i]
                }
            } else {
                section('Configure'){

                    href PAGE_FIND_LIGHTS, title:'Find Lights', description:''
                    href PAGE_FIND_GROUPS, title:'Find Groups', description:''
                    href PAGE_FIND_SCENES, title:'Find Scenes', description:''
                    href PAGE_FIND_SENSORS, title:'Find Sensors', description:''
                    href selectedDevice ? PAGE_BRIDGE_LINKING : PAGE_BRIDGE_DISCOVERY, title:title, description:'', state:selectedDevice? 'complete' : null //, params: [nextPage: PAGE_BRIDGE_LINKING]
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

        Boolean uninstall = bridgeHost ? false : true
        String nextPage = selectedDevice ? PAGE_BRIDGE_LINKING : null

        return dynamicPage(name:PAGE_BRIDGE_DISCOVERY, title:'Discovery Started!', nextPage:nextPage, refreshInterval:refreshInterval, uninstall:uninstall) {
            section('Please wait while we discover your Hue Bridge. Note that you must first configure your Hue Bridge and Lights using the Philips Hue application. Discovery can take five minutes or more, so sit back and relax, the page will reload automatically! Select your Hue Bridge below once discovered.') {
                input 'selectedDevice', 'enum', required:false, title:"Select Hue Bridge (${numFound} found)", multiple:false, options:options, submitOnChange: true
            }
        }
    }

    def unlink() {
        def hub = getHubForMac(selectedDevice)
        hub.remove("clientkey")
        hub.remove("username")
        state.hub = hub
        state.remove('username')
        state.remove('clientkey')
        selectedDevice = ''

        return dynamicPage(name:PAGE_UNLINK, title:'Unlink bridge', nextPage:null, uninstsall: false) {
            section('') {
                paragraph "You hub has been unlinked.  Use hub linking to re-link your hub."
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
            if (hub?.username && hub?.clientkey) { //if discovery worked
                if (logEnable) { log.debug "Hub linking completed for ${hub.name}" }
                return addDevice(hub)
            }
            if (hub?.networkAddress) {
                setBridgeHost(hub.networkAddress)
            }

            if (hub) {
                if((linkRefreshcount % 2) == 0 && (!state.username || !state.clientkey)) {
                    requestHubAccess(selectedDevice)
                }
            }

        } else {
            paragraphText = 'You haven\'t selected a Hue Bridge, please Press \'Done\' and select one before clicking next.'
        }

        def uninstall = bridgeHost ? false : true

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
            setBridgeHost("${device.networkAddress}:${device.deviceAddress}")
            state.username = "${device.username}"
            state.clientkey = "${device.clientkey}"
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

                def child = addChildDevice('hubitat', "Generic Component ${type}", "${dni}",
                [label: "${name}", isComponent: false, name: 'AdvancedHueBulb'])
                child.updateSetting('txtEnable', false)
                lights.remove(lightId)
                child.refresh()

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

        return dynamicPage(name:PAGE_ADD_LIGHTS, title:title, nextPage:null) {
            section() {
                paragraph sectionText
            }
        }

    }

    def findGroups(params){
        enumerateGroups()

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
        "https://${bridgeHost}/api/${state.username}"
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

    private enumerateGroups() {

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

    private enumerateLights() {

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

    private enumerateScenes() {

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
                        renameInstalledDevices(scenes, state.scenes)
                    }
                    parseScenes(data)
                }
            }

        }

    }

    private enumerateSensors() {
    
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

    private List enumerateResourcesV2() {
        
        def url = "${apiV2Url}/resource"

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

        String deviceNetworkId = child.device.deviceNetworkId
        // Establish the eventQueue for the device.
        Map eventQueue = getVolatileAtomicQueue(child)
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
            if (!newState.containsKey('hue') && currentValue(child, 'hue') != null) { newState.hue = convertHEHue(currentValue(child, 'hue')) }
            if (!newState.containsKey('sat') && currentValue(child, 'saturation') != null) { newState.sat = convertHESaturation(currentValue(child, 'saturation')) }
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
                    setHueProperty(device, [(result[3]): [(result[4]): value]])
                }
            }
        }
    }

    void queueDeviceRefresh(List deviceIds) {
        unschedule(dispatchRefresh)
        Map queue = volatileAtomicRefreshQueue
        deviceIds.each { id -> queue[(id)] = true }
        runIn(1, dispatchRefresh)
    }

    void dispatchRefresh() {
        List nids = volatileAtomicRefreshQueue.keySet() as ArrayList
        volatileAtomicRefreshQueue.clear()
        nids.each { nid ->
            def child = getChildDevice(nid)
            if (child) {
                getDeviceState(child)
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
                
                    stateForNetworkId(deviceNetworkId).putAll(data)
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
    public String networkIdForLight(id) {
        String type=bulbTypeForLight(id)
        "hueBulb${type}:${app.getId()}/${id}"
    }
    public networkIdForScene(sceneId) {
        String groupId = state.scenes[sceneId]?.group
        "hueScene:${app.getId()}/${groupId}/${sceneId}"
    }
    public networkIdForScene(groupId,sceneId) {
        "hueScene:${app.getId()}/${groupId}/${sceneId}"
    }
    public networkIdForSensor(sensorId) {
        "hueSensor:${app.getId()}/${sensorId}"
    }

    public stateForNetworkId(deviceNetworkId) {
        def (String type, String address) = deviceNetworkId.split(':',2)
        String hueId = address.split('/').last()
        switch (type) {
            case 'hueGroup':   return state.groups[hueId];
            case ~/hueBulb.*/: return state.lights[hueId];
            case 'hueScene':   return state.scenes[hueId];
            case 'hueSensor':  return state.sensors[hueId];
        }
        return null
    }

    public getChildDeviceById(Long id) {
        getChildDevices().find { child -> child.device.deviceId == id }
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
                if (dbgEnable) { log.debug "Parsing response: $data for $id" }
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
                if (dbgEnable) { log.debug "Parsing response: $data for $id" }
                setHueProperty(light, [state: data.state, action: data.action])
            }
        }
    }

    void parseScenes(json) {
        //json.each { id, value ->
        //    if (dbgEnable) { log.debug "${id}" }
        //}
    }

    void parseSensors(json) {
        json.each { id, data ->
            // Add code to update all installed groups state
            def sensor = getChildDevice(networkIdForSensor(id))
            if (sensor) {
                if (dbgEnable) { log.debug "Parsing response: $data for $id" } 
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

        if (devstate.containsKey('colormode')) {
            if (devstate.colormode == 'ct') { 
                devstate.remove('hue')
                devstate.remove('sat')
                devstate.remove('xy')
            } else if (devstate.colormode == 'hs') { 
                devstate.remove('ct')
                devstate.remove('mirek')
                devstate.remove('xy')
            } else if (devstate.color?.xy && devstate.mirek_valid == false) {
                devstate.colormode = 'xy'
            }
        }

        // transform hue attributes and values to device attributes and values
        def events = devstate.collectEntries {  key, value ->
            switch (key) {
                case 'on':          ['switch':           value ? 'on' : 'off']; break
                case 'colormode':   ['colorMode':        convertHBColorMode(value)]; break
                case 'bri':         ['level':            convertHBLevel(value)]; break
                case 'brightness':  ['level':            convertHBLevel((value * 2.55) as int)]; break
                case 'hue':         ['hue':              convertHBHue(value)]; break
                case 'sat':         ['saturation':       convertHBSaturation(value)]; break
                case 'ct':          ['colorTemperature': convertHBColortemp(value)]; break
                case 'mirek':       ['colorTemperature': convertHBColortemp(value)]; break
                case 'presence':    ['motion':           value ? 'active' : 'inactive']; break
                case 'lightlevel':  ['illuminance':      convertHELightLevel(value)]; break
                case 'temperature': ['temperature':      convertHETemperature(value)]; break
                default:           [key, value]
            }
        }.findAll { key, value -> child.hasAttribute("$key") }

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
        
        if (child.hasAttribute('colorName') && isHubVersion('2.3.2')) {
            String cm = events.colorMode ?: currentValue(child, 'colorMode')
            if (cm == 'CT') {
                events.colorName = convertTemperatureToGenericColorName(events.colorTemperature)
            } else if (cm == 'RGB') {
                Integer hue = events.hue ?: currentValue(child, 'hue')
                Integer sat = events.saturation ?: currentValue(child, 'saturation')
                if (hue != null && sat != null) {
                    events.colorName = convertHueToGenericColorName(hue, sat)
                }
            }
        }

        if (events.level && child.hasAttribute('level')) {
            String sw = events.switch ?: currentValue(child, 'switch')
            events.level = valueBetween(events.level, (sw == 'on' ? 1 : 0), 100)
        }
        
        events.each { key, value -> 
            sendChildEvent(child, [name: key, value: value]) 
            if (key == 'switch' && value == 'off') {
                child.getChildDevices().each { dev -> sendChildEvent(dev, [name: 'switch', value: 'off']) }
            }
        }

        // child.getCapabilities().each { log.info "${it.name} = ${it.reference} :: ${it.toString()}" }
        
        // Fast-fail here to abort additional processing if this is not a color capable device
        if (!child.hasCapability('ColorControl')) { return }

        // Fast-fail here to abort additional processing if this is not an XY color change
        String colorMode = events.colorMode?:currentValue(child, 'ColorMode')
        if (colorMode != 'RGB' || !devstate.xy) { return }

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
        Float heLevel = convertHELevel(level as int)
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

        Map args = ['on': colormap.level > 0,
                    'hue': convertHEHue(colormap.hue as int),
                    'sat': convertHESaturation(colormap.saturation as int),
                    'bri': convertHELevel(colormap.level as int)]

        setDeviceState(child, args)

    }

    void componentSetHue(def child, hue) {
        setDeviceState(child, ['on': true, 'hue': convertHEHue(hue as int)])
    }

    void componentSetSaturation(def child, saturation) {
        setDeviceState(child, ['on': true, 'sat': convertHESaturation(saturation as int)])
    }

    void componentSetColorTemperature(def child, colortemperature, level = null, transitionTime = null) {
        Map values = [
            'on': (level?:100) > 0,
            'ct': convertHEColortemp(colortemperature as int)]
        if (level)          { values['bri']            = convertHELevel(level as int) }
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

    Integer convertHBLevel(Number value) {
        valueBetween(Math.round(value / 2.54), 0, 100)
    }

    Integer convertHELevel(Integer value) {
        valueBetween(Math.round(value * 2.54), 1, 254)
    }

    Number convertHBHue(Number value) {
        Math.round(value / 655.35)
    }

    Number convertHEHue(Number value) {
        value == 33 ? 21845 : value == 66 ? 43690 : valueBetween(Math.round(value * 655.35), 0, 65535)
    }

    Number convertHBSaturation(Number value) {
        Math.round(value / 2.54)
    }

    Number convertHESaturation(Number value) {
        valueBetween(Math.round(value * 2.54), 0, 254)
    }

    Number convertHBColortemp(Number value) {
        // 4500 / 347 = 12.968 (but 12.96 scales better)
        valueBetween(Math.round(((500 - value) * 12.96) + 2000 ), 2000, 6500)
    }

    Number convertHEColortemp(Number value) {
        valueBetween(Math.round(500 - ((value - 2000) / 12.96)), 153, 500)
    }

    Number convertHELightLevel(Number lightLevel) {
        Math.pow(10, (((lightLevel?:1)-1)/10000.0)) as int
    }

    Number convertHETemperature(Number temperature) {
        Number tC = (temperature?:0) / 100
        (location.temperatureScale == 'C' ? tC : ((tC * 1.8) + 32)).setScale(1, RoundingMode.HALF_UP)
    }

    String convertHBColorMode(String value) {
        if (value == 'hs') { return 'RGB'  }
        if (value == 'ct') { return 'CT' }
        if (value == 'xy') { return 'RGB' }
        return ''
    }

    boolean isHubVersion(String versionString) {
        if (!(versionString ==~ /^\d+(\.\d+){2,3}$/)) { return false }
        List minVer = versionString.split('\\.')
        List curVer = getLocation().getHub().firmwareVersionString.split('\\.')
        if (curVer.size < minVer.size) { return false }
        for (int i=0; i< minVer.size(); i++) {
            int c=curVer[i] as int, m=minVer[i] as int
            if (c > m) { return true }
            if (c < m) { return false }
        }
        return true
    }

    void syncState(List devices) {
        
    }

