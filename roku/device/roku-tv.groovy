/**
 * Roku TV
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a parent device handler designed to manage and control a Roku TV or Player connected to the same network 
 * as the Hubitat hub.  This device handler requires the installation of a child device handler available from
 * the github repo.
 *-------------------------------------------------------------------------------------------------------------------
 * Copyright 2020 Armand Peter Welsh
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *-------------------------------------------------------------------------------------------------------------------
 **/
preferences {
    def keys=[
        "Home",      "Back",       "FindRemote",  "Select",
        "Up",        "Down",       "Left",        "Right",
        "Play",      "Rev",        "Fwd",         "InstantReplay",
        "Info",      "Search",     "Backspace",   "Enter",
        "VolumeUp",  "VolumeDown", "VolumeMute",
        "Power",     "PowerOff",   "ChannelUp",   "ChannelDown"
        ]
    def apps=[:]
    def installed=[:]
    if (deviceIp && (!autoManage || !manageApps)) {
        getInstalledApps().each { netId, appName ->
            if (getChildDevice(netId)) {
                installed[netId] = appName
            } else {
                apps[netId] = appName
            }
        }
    }


    input name: "deviceIp",        type: "text",   title: "Device IP", required: true
    if (deviceIp) {
        input name: "refreshUnits",    type: "enum",   title: "Refresh interval measured in Minutes, or Seconds", options:["Minutes","Seconds"], defaultValue: "Minutes", required: true
        input name: "refreshInterval", type: "number", title: "Refresh the status at least every n ${refreshUnits}.  0 disables auto-refresh, which is not recommended.", range: 0..60, defaultValue: 5, required: true
        input name: "appRefresh",      type: "bool",   title: "Refresh current application status seperate from TV status.", defaultValue: false, required: true
        if (appRefresh) {
            input name: "appInterval",     type: "number", title: "Refresh the current application at least every n seconds.", range: 1..120, defaultValue: 60, required: true        
        }
        input name: "autoManage",      type: "bool",   title: "Enable automatic management of child devices", defaultValue: true, required: true
        if (autoManage) {
            input name: "manageApps",      type: "bool",   title: "Auto-manage Roku Applications", defaultValue: true, required: true
            input name: "hdmiPorts",       type: "enum",   title: "Number of HDMI inputs", options:["0","1","2","3","4"], defaultValue: "3", required: true
            input name: "inputAV",         type: "bool",   title: "Enable AV Input", defaultValue: false, required: true
            input name: "inputTuner",      type: "bool",   title: "Enable Tuner Input", defaultValue: false, required: true
            input name: "createChildKey",  type: "enum",   title: "Select a key to add a child switch for, and save changes to add the child button for the selected key", options:keys, required: false
        }
        if (!autoManage || !manageApps) {
            input name: "createChildApp",     type: "enum",   title: "Add Roku App", options: apps, required: false
            input name: "deleteChildApp",     type: "enum",   title: "Remove Roku App", options: installed, required: false

        }
    }
    input name: "logEnable",       type: "bool",   title: "Enable debug logging", defaultValue: true, required: true
}

metadata {
    definition (
        name:      "Roku TV", 
        namespace: "apwelsh", 
        author:    "Armand Welsh", 
        importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/roku/device/roku-tv.groovy") {
        
        capability "TV"
        capability "AudioVolume"
        capability "Switch"
        capability "Polling"
        capability "Refresh"

        attribute "refresh", "string"

        command "home"
        command "keyPress", [[name:"Key Press Action", type: "ENUM", constraints: [
                "Home",      "Back",       "FindRemote",  "Select",        "Up",        "Down",       "Left",        "Right",
                "Play",      "Rev",        "Fwd",         "InstantReplay", "Info",      "Search",     "Backspace",   "Enter",
                "VolumeUp",     "VolumeDown", "VolumeMute",  "Power",         "PowerOff",
                "ChannelUp", "ChannelDown", "InputTuner", "InputAV1",      "InputHDMI1", "InputHDMI2", "InputHDMI3", "InputHDMI4"] ] ]
        
        command "reloadApps"
        
        attribute "application", "string"
//        attribute "current_app_icon_html", "string"
    }
}

/**
 * Hubitat DTH Lifecycle Functions
 **/
def installed() {
    sendEvent(name: "refresh", value: "idle")
    updated()
}

def updated() {
    if (logEnable) log.debug "Preferences updated"
    if (deviceIp) {
        def mac = getMACFromIP(deviceIp)
        if (state.deviceMac != mac) {
            log.debug "Updating Mac from IP: ${mac}"
            state.deviceMac = mac
        }
    }
    unschedule()
    if (deviceIp && refreshInterval > 0) {
        if (refreshUnits == "Seconds") {            
            schedule("0/${refreshInterval} * * * * ?", refresh)
        } else {
            schedule("${new Date().format("s")} 0/${refreshInterval} * * * ?", refresh)
        }
    }
    if (appRefresh && appInterval > 0) schedule("0/${appInterval} * * * * ?", queryCurrentApp)
    if (createChildKey) {
        def key=createChildKey
        def text=createChildKey.replaceAll( ~ /([A-Z])/, ' $1').trim()
        device.updateSetting("createChildKey", [value: "", type:"enum"])
        updateChildApp(networkIdForApp(key), text)
    }
    if (createChildApp) {
        def netId=createChildApp
        device.updateSetting("createChildApp", [value: "", type:"enum"])
        if (autoManage==false || manageApps==false) {
            def apps=getInstalledApps()
            def appName=apps[netId]
            if (appName && netId)
                updateChildApp(netId, appName)
        }
    }
    if (deleteChildApp) {
        def netId=deleteChildApp
        device.updateSetting("deleteChildApp", [value: "", type:"enum"])
        if (autoManage==false || manageApps==false) {
            deleteChildDevice(netId)
        }
    }

}

/**
 * Event Parsers
 **/
def parse(String description) {
}

def networkIdForApp(String appId) {
    return "${device.deviceNetworkId}-${appId}"    
}

def appIdForNetworkId(String netId) {
    return netId.replaceAll(~/.*\-/,"")
}

def iconPathForApp(String netId) {
    return "http://${deviceIp}:8060/query/icon/${appIdForNetworkId(netId)}"    
}
    
/*
 * Component Child Methods
 */

 void componentOn(child) {
    def appId = appIdForNetworkId(child.deviceNetworkId)
    launchApp(appId)
}

void componentOff(child) {
    if (child.currentValue("switch") == "off")
        return
    home()
}

void componentRefresh(cd){
    if (logEnable) log.info "received refresh request from ${cd.displayName} - ignored"
    //if ("${device.currentValue('refresh')}" == "idle") {
    //    queryCurrentApp()
    //}
}
    
/*
 * Device Capability Interface Functions
 */

def on() {
    if (device.currentValue('switch') == "off")
        sendWakeUp()
    keyPress('Power')
}

def off() {
    keyPress('PowerOff')
}

def home() {
    keyPress('Home')
}

def channelUp() {
    keyPress('ChannelUp')
}

def channelDown() {
    keyPress('ChannelDown')
}

def volumeUp() {
    keyPress('VolumeUp')
}

def volumeDown() {
    keyPress('VolumeDown')
}

def setVolume(level) {
    log.trace "set volume not supported by Roku"
}

def unmute() {
    keyPress('VolumeMute')
}

def mute() {
    keyPress('VolumeMute')
}

def poll() {
    if (logEnable)  log.trace "Executing 'poll'"
    if (appRefresh) queryCurrentApp
    refresh()
}

def refresh() {
    if (logEnable) log.trace "Executing 'refresh'"
    queryDeviceState()
    if (!appRefresh) queryCurrentApp()
    if (autoManage)  queryInstalledApps()
}

/**
 * Custom DTH Command interface functions
 **/

def input_AV1() {
    keyPress('InputAV1')
}

def input_Tuner() {
    keyPress('InputTuner')
}

def input_hdmi1() {
    keyPress('InputHDMI1')
}

def input_hdmi2() {
    keyPress('InputHDMI2')
}

def input_hdmi3() {
    keyPress('InputHDMI3')
}

def input_hdmi4() {
    keyPress('InputHDMI4')
}

def reloadApps() {
    sendEvent(name: "refresh", value: "reload-apps")
    purgeInstalledApps()
    queryInstalledApps()
}


/**
 * Roku API Section
 * The following functions are used to communicate with the Roku RESTful API
 **/

def sendWakeUp() {
    if (state."wake-on-lan" == true) {
        sendHubCommand(new hubitat.device.HubAction (
            "wake on lan ${state.deviceMac}",
            hubitat.device.Protocol.LAN,
            null,
            [:]
        ))
    }
}

def queryDeviceState() {
    sendEvent(name: "refresh", value: "device-info")
    httpGet("http://${deviceIp}:8060/query/device-info") { response -> 
        if (!response.isSuccess())
            return

        def body = response.data
        parsePowerState(body)
        parseState(body)
        sendEvent(name: "refresh", value: "idle")

    }
}

private def parseState(body) {
    
    if (body["supports-wake-on-wlan"] == "true" || !(body["network-type"] == "wifi")) 
        setState("wake-on-lan", true)
    
    if (body["supports-find-remote"] == "true" && body["find-remote-is-possible"] == "true")
        setState("supports-find-remote", true)
    
    ["serial-number", "vendor-name", "device-id", "model-name", "screen-size", "user-device-name"].each { nodeName ->
        setState(nodeName, "${body[nodeName]}")
    }
}

private def setState(key, value) {
    if (value != "" && value != state."${key}") {
        state."${key}" = value
        if (logEnabled) log.debug "Set ${key} = ${value}"
    }
}

private def isStateProperty(String key) {
    switch (key) {
        case "serial-number":
        case "vendor-name":
        case "device-id":
        case "model-name":
        case "screen-size":
        case "user-device-name":
        case "deviceMac":
        case "supports-find-remote":
        case "wake-on-lan":
            return true
    }
    return false
}

private def cleanState() {
    def keys = this.state.keySet()
    for (def key : keys) {
        if (!isStateProperty(key)) {
            if (logEnable) log.debug("removing ${key}")
            this.state.remove(key)
        }
    }
}

private def parsePowerState(body) {
    def powerMode = body."power-mode"?.text()
    if (powerMode != null) {
        def mode = powerMode
        switch (mode) {
            case "PowerOn":
                if (this.state!="on") {
                    sendEvent(name: "switch", value: "on")
                    if (appRefresh && appInterval > 0) {
                        queryCurrentApp()
                        schedule("0/${appInterval} * * * * ?", queryCurrentApp)
                    }
                } 
                break;
            case "PowerOff":
            case "DisplayOff":
            case "Headless":
                if (this.state!="off") {
                    sendEvent(name: "switch", value: "off")
                    unschedule(queryCurrentApp)
                }
                break;
        }
    }    
}


def queryCurrentApp() {
    httpGet("http://${deviceIp}:8060/query/active-app") { response -> 
        if (!response.isSuccess()) 
            return
        
        def body = response.data
        def app = body.app.text()
        if (app != null) {
            def currentApp = "${app.replaceAll( ~ /\h/," ")}"  // Convert non-ascii spaces to spaces.
            sendEvent(name: "application", value: currentApp, isStateChange: true)

            childDevices.each { child ->
                def appName = "${child.name}"
                def value = (currentApp.equals(appName)) ? "on" : "off"
                if ("${child.currentValue('switch')}" != "${value}") {
                    child.parse([[name: "switch", value: value, descriptionText: "${child.displayName} was turned ${value}"]])
                }
            }
        }
    }
}

private def purgeInstalledApps() {    
    if (manageApps) childDevices.each{ child ->
        deleteChildDevice(child.deviceNetworkId)
    }
}

def getInstalledApps() {
    def apps=[:]
    httpGet("http://${deviceIp}:8060/query/apps") { response ->
        
        if (!response.isSuccess())
        return

        def body = response.data
        
        body.app.each{ node ->
            if (node.attributes().type != "appl") {
                return
            }

            def netId = networkIdForApp(node.attributes().id)
            def appName = node.text()
            apps[netId] = appName
        }
    }
    return apps
}

def queryInstalledApps() {
    if (!autoManage) 
        return
    if ("${device.currentValue("refresh")}" == "idle") {
        sendEvent(name: "refresh", value: "find-apps")
    }
    
    def apps = getInstalledApps()

    if (apps) { 

        def hdmiCount = hdmiPorts as int
        
        childDevices.each{ child ->
            //log.debug "child: ${child.deviceNetworkId} (${child.name})"
            def nodeExists = false
            if (hdmiCount > 0 ) (1..hdmiCount).each { i -> 
                nodeExists = nodeExists || networkIdForApp("hdmi${i}") == child.deviceNetworkId
            }
            
            if (inputAV)
                nodeExists = nodeExists || networkIdForApp("AV1") == child.deviceNetworkId
    
            if (inputTuner)
                nodeExists = nodeExists || networkIdForApp("Tuner") == child.deviceNetworkId
            
            nodeExists = nodeExists || apps.containsKey(child.deviceNetworkId)
            
            if (!nodeExists) {
                if (appIdForNetworkId(child.deviceNetworkId) =~ /^(Tuner|AV1|hdmi\d)$/ || manageApps) {
                    if (logEnable) log.trace "Deleting child device: ${child.name} (${child.deviceNetworkId})"
                    deleteChildDevice(child.deviceNetworkId)
                }
            }
        }
        
        if (inputAV)    updateChildApp(networkIdForApp("AV1"), "AV")
        if (inputTuner) updateChildApp(networkIdForApp("Tuner"), "Antenna TV")
        if (hdmiCount > 0) (1..hdmiCount).each{ i -> 
            updateChildApp(networkIdForApp("hdmi${i}"), "HDMI ${i}")
        }

        if (manageApps) apps.each { netId, appName ->
            updateChildApp(netId, appName)
        }
    
        sendEvent(name: "refresh", value: "idle")

    }

}

def keyPress(key) {
    if (!isValidKey(key)) {
        log.warning("Invalid key press: ${key}")
        return
    }
    if (logEnable) log.debug "Executing '${key}'"
    httpPost("http://${deviceIp}:8060/keypress/${key}") { response -> 
        if (response.isSuccess())
            poll()
        else
            log.error "Failed to send key press event for ${key}"
    }
}

private def isValidKey(key) {
    def keys = [
        "Home",      "Back",       "Select",
        "Up",        "Down",       "Left",        "Right",
        "Play",      "Rev",        "Fwd",         "InstantReplay",
        "Info",      "Search",     "Backspace",   "Enter",
        "VolumeUp",  "VolumeDown", "VolumeMute",
        "Power",     "PowerOff",
        "ChannelUp", "ChannelDown","InputTuner", "InputAV1",
        "InputHDMI1","InputHDMI2", "InputHDMI3", "InputHDMI4" 
        ]
    if (state."supports-find-remote" == true)
        keys << "FindRemote"
    
    return keys.contains(key)
}

def launchApp(appId) {
    if (logEnable) log.debug "Executing 'launchApp ${appId}'"
    if (appId =~ /^\d+$/ ) {
        httpPost("http://${deviceIp}:8060/launch/${appId}") { response ->
            if (response.isSuccess()) 
                queryCurrentApp()
            else
                log.error "Failed to launch appId: ${data.appId}"
        }
    } else if (appId =~ /^(AV1|Tuner|hdmi\d)$/ ) {
        this."input_$appId"()
    } else {
        this.keyPress(appId)
    }
}

/**
 * Child Device Maintenance Section
 * These functions are used to manage the child devices bound to this device
 */

private def getChildDevice(String netId) {
    try {
        def result = null
        childDevices.each{ child ->
            if(child.deviceNetworkId == netId) {
                result = child
            }
        }
        return result
    } catch(e) {
        if (logEnable) log.error "Failed to find child with exception: ${e}";
    }
    return null
}

private void updateChildApp(String netId, String appName) {
    def child = getChildDevice(netId)
    if(child) { //If child exists, do not create it
        return
    }

    if (appName) {
        createChildApp(netId, appName)
    } else {
        if (logEnable) log.error "Cannot create child: (${netId}) due to missing 'appName'"
    }
}

private void createChildApp(String netId, String appName) {
    sendEvent(name: "refresh", value: "busy")
    try {
        def label = deviceLabel()
        def child = addChildDevice("hubitat", "Generic Component Switch", "${netId}",
            [label: "${label}-${appName}", 
             isComponent: false, name: "${appName}"])
        child.updateSetting("txtEnable", false)
        if (logEnable) log.debug "Created child device: ${appName} (${netId})"
    } catch(IllegalArgumentException e) {
        if (getChildDevice(netId)) {
            if (logEnabled) log.warn "Attempted to create duplicate child device for ${appName} (${netId}); Skipped"
        } else {
            if (logEnable) log.error "Failed to create child device with exception: ${e}"
        }
    } catch(Exception e) {
        if (logEnable) log.error "Failed to create child device with exception: ${e}"
    }
    sendEvent(name: "refresh", value: "idle")
}

private def deviceLabel() {
    if (device.label == null)
        return device.name
    return device.label
}

