/**
 * Roku TV
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a parent device handler designed to manage and control a Roku TV or Player connected to the same network 
 * as the Hubitat hub.  This device handler requires the installation of a child device handler available from
 * the github repo.
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
preferences {
    def keys=[
        "Home",      "Back",       "FindRemote",  "Select",
        "Up",        "Down",       "Left",        "Right",
        "Play",      "Rev",        "Fwd",         "InstantReplay",
        "Info",      "Search",     "Backspace",   "Enter",
        "VolumeUp",  "VolumeDown", "VolumeMute",
        "Power",     "PowerOff",   "ChannelUp",   "ChannelDown"
        ]
    input name: "deviceIp",        type: "text",   title: "Device IP", required: true
    input name: "refreshUnits",    type: "enum",   title: "Refresh interval measured in Minutes, or Seconds", options:["Minutes","Seconds"], defaultValue: "Minutes", required: true
    input name: "refreshInterval", type: "number", title: "Refresh the status at least every n ${refreshUnits}.  0 disables auto-refresh, which is not recommended.", range: 0..60, defaultValue: 5, required: true
    input name: "appRefresh",      type: "bool",   title: "Refresh current application status seperate from TV status.", defaultValue: false, required: true
    if (appRefresh) {
        input name: "appInterval",     type: "number", title: "Refresh the current application at least every n seconds.", range: 1..120, defaultValue: 60, required: true        
    }
    input name: "autoManage",      type: "bool",   title: "Enable automatic management of child devices", defaultValue: true, required: true
    if (autoManage) {
        input name: "manageApps",      type: "bool",   title: "Enable management of Roku installed Applications", defaultValue: true, required: true
        input name: "hdmiPorts",       type: "enum",   title: "Number of HDMI inputs", options:["0","1","2","3","4"], defaultValue: "3", required: true
        input name: "inputAV",         type: "bool",   title: "Enable AV Input", defaultValue: false, required: true
        input name: "inputTuner",      type: "bool",   title: "Enable Tuner Input", defaultValue: false, required: true
        input name: "createChildKey",  type: "enum",   title: "Select a key to add a child switch for, and save changes to add the child button for the selected key", options:keys, required: false
    }
    input name: "logEnable",       type: "bool",   title: "Enable debug logging", defaultValue: true, required: true
}

metadata {
    definition (
        name:      "Roku TV", 
        namespace: "apwelsh", 
        author:    "Armand Welsh", 
        importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/devices/roku-tv.groovy") {
        
        capability "TV"
        capability "AudioVolume"
        capability "Switch"
        capability "Polling"
        capability "Refresh"

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
    asynchttpGet(parseDeviceState, [uri: "http://${deviceIp}:8060/query/device-info"])
}

def parseDeviceState(response, data) {
    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return
    
    def body = response.getXml()
    parsePowerState(body)
    parseState(body)
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
    asynchttpGet(parseCurrentApp, [uri: "http://${deviceIp}:8060/query/active-app"])
}

def parseCurrentApp(response, data) {
    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return
    
    def body = response.getXml()
    def app = body.app.text()
    if (app != null) {
        def currentApp = "${app.replaceAll( ~ /\h/," ")}"  // Convert non-ascii spaces to spaces.
        sendEvent(name: "application", value: currentApp, isStateChange: true)

        childDevices.each { child ->
            def appName = "${child.name}"
            def value = (currentApp.equals(appName)) ? "on" : "off"
            child.sendEvent(name: "switch", value: value)
//            if (value == "on")     sendEvent(name: "current_app_icon_html", value:"<img src=\"${iconPathForApp(child.deviceNetworkId)}\"/>")
        }
    }
}

private def purgeInstalledApps() {    
    if (manageApps) childDevices.each{ child ->
        deleteChildDevice(child.deviceNetworkId)
    }
}

def queryInstalledApps() {
    if (!autoManage) 
        return
    asynchttpGet(parseInstalledApps, [uri: "http://${deviceIp}:8060/query/apps"])
}

def parseInstalledApps(response, data) {

    def status = response.getStatus();
    if (status < 200 || status >= 300)
        return

    def body = response.getXml()
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
        
        body.app.each{ node ->
            nodeExists = nodeExists || networkIdForApp(node.attributes().id) == child.deviceNetworkId
        }
        
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

    if (manageApps) body.app.each{ node ->
        if (node.attributes().type != "appl") {
            return
        }

        def netId = networkIdForApp(node.attributes().id)
        def appName = node.text()
        updateChildApp(netId, appName)
    }
}

def keyPress(key) {
    if (!isValidKey(key)) {
        log.warning("Invalid key press: ${key}")
        return
    }
    if (logEnable) log.debug "Executing '${key}'"
    asynchttpPost(keyPressHandler, [uri: "http://${deviceIp}:8060/keypress/${key}"], [key: key])
}

def keyPressHandler(response, data) {
    def status = response.getStatus();
    if (200 <= status && status < 300)
        poll()
    else
        log.error "Failed to send key press event for ${data.key}"
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
    if (appId =~ /\d+/) {
        asynchttpPost(launchAppHandler, [uri: "http://${deviceIp}:8060/launch/${appId}"], [appId: appId])
    } else if (appId =~ /(hdmi\d)|AV1|Tuner/) {
        this."input_$appId"()
    } else {
        this.keyPress(appId)
    }
}

def launchAppHandler(response, data) {
    def status = response.getStatus()
    if (200 <= status && status < 300)
        queryCurrentApp()
    else
        log.error "Failed to launch appId: ${data.appId}"
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
    if(child != null) { //If child exists, do not create it
        return
    }

    if (appName != null) {
        createChildApp(netId, appName)
    } else {
        if (logEnable) log.error "Cannot create child: (${netId}) due to missing 'appName'"
    }
}

private void createChildApp(String netId, String appName) {
    try {
        def label = deviceLabel()
        addChildDevice("Roku App", "${netId}",
            [label: "${label}-${appName}", 
             isComponent: false, name: "${appName}"])
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
}

private def deviceLabel() {
    if (device.label == null)
        return device.name
    return device.label
}
