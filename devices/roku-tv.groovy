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
    input name: "deviceIp",        type: "text",   title: "Device IP", required: true
    input name: "deviceMac",       type: "text",   title: "Device MAC Address", required: true, defaultValue: "UNKNOWN"
    input name: "refreshUnits",    type: "enum",   title: "Refresh interval measured in Minutes, or Seconds", options:["Minutes","Seconds"], defaultValue: "Minutes", required: true
    input name: "refreshInterval", type: "number", title: "Refresh the status at least every n ${refreshUnits}.  0 disables auto-refresh, which is not recommended.", range: 0..60, defaultValue: 5, required: true
    input name: "autoManage",      type: "bool",   title: "Enable automatic management of child devices", defaultValue: true, required: true
    if (autoManage) {
        input name: "manageApps",      type: "bool",   title: "Enable management of Roku installed Applications", defaultValue: true, required: true
        input name: "hdmiPorts",       type: "enum",   title: "Number of HDMI inputs", options:["0","1","2","3","4"], defaultValue: "3", required: true
        input name: "inputAV",         type: "bool",   title: "Enable AV Input", defaultValue: false, required: true
        input name: "inputTuner",      type: "bool",   title: "Enable Tuner Input", defaultValue: false, required: true
    }
    input name: "logEnable",       type: "bool",   title: "Enable debug logging", defaultValue: true, required: true
}

metadata {
    definition (name:      "Roku TV", 
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
				"VolumeUp",	 "VolumeDown", "VolumeMute",  "Power",         "PowerOff",
				"ChannelUp", "ChannelDown", "InputTuner", "InputAV1",      "InputHDMI1", "InputHDMI2", "InputHDMI3", "InputHDMI4"] ] ]
        
        command "reloadApps"
		
        attribute "application", "string"
//		attribute "current_app_icon_html", "string"
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
	unschedule()
	if (deviceIp && refreshInterval > 0) {
        if (refreshUnits == "Seconds") {
            schedule("${new Date().format("s")}/${refreshInterval} * * * * ?", refresh)
        } else {
    		schedule("${new Date().format("s m")}/${refreshInterval} * * * ?", refresh)
        }
		runIn(1,refresh)
	}
}

/**
 * Event Parsers
 **/
def parse(String description) {
    def msg = parseLanMessage(description)
    if (msg.status == 200) {
        if (msg.body) {
            def body = new XmlParser().parseText(msg.body)
            switch (body.name()) {
                case "device-info":
		    		cleanState()
                    parseMacAddress body
                    parsePowerState body
                    parseState body
                    break;
                case "apps":
                    parseInstalledApps body
                    break;
                case "active-app":
                    parseActiveApp body
                    break;
            }
        } else {
			// upon a successful RESTful response with no body, assume a POST and force a refresh
			runInMillis(1500, refresh)
		}
    }
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
	
private def parseInstalledApps(Node body) {
    
    if (!autoManage) 
		return
	
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
        def appName = node.value()[0]
        updateChildApp(netId, appName)
    }
}

private def purgeInstalledApps() {    
    if (manageApps) childDevices.each{ child ->
		deleteChildDevice(child.deviceNetworkId)
	}
}

private def parseActiveApp(Node body) {
    def app = body.app[0]?.value() 
    if (app != null) {
		def currentApp = "${app[0].replaceAll( ~ /\h/," ")}"  // Convert non-ascii spaces to spaces.
		sendEvent(name: "application", value: currentApp)

		childDevices.each { child ->
			def appName = "${child.name}"
			def value = (currentApp.equals(appName)) ? "on" : "off"
			child.sendEvent(name: "switch", value: value)
//			if (value == "on") 	sendEvent(name: "current_app_icon_html", value:"<img src=\"${iconPathForApp(child.deviceNetworkId)}\"/>")


        }
    }
}

private def parseState(Node body) {
    for (def node : body) {
        def key = node.name()
        if (key == null)
            continue
        
        if (isStateProperty(key)) {
            def value = node.value()
            if (value != null) {
                if (value[0] != this.state."${key}") {
                    this.state."${key}" = value[0]
                    if (logEnable) log.debug "set ${key} = ${value[0]}"
                }
            }
        }
    }
}

private def isStateProperty(String key) {
    switch (key) {
        case "serial-number":
        case "vendor-name":
        case "device-id":
        case "mode-name":
        case "screen-size":
        case "user-device-name":
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

private def parseMacAddress(Node body) {
	def type = body."network-type"[0]?.value()[0]
	def wifiMac = body."${type}-mac"[0]?.value()
    if (wifiMac != null) {
        def macAddress = wifiMac[0].replaceAll("[^A-f,a-f,0-9]","")
        if (!deviceMac || deviceMac != macAddress) {
            if (logEnable) log.debug "Update config [MAC Address = ${macAddress}]"
            device.updateSetting("deviceMac", [value: macAddress, type:"text"])

        }
    }
}

private def parsePowerState(Node body) {
    def powerMode = body."power-mode"[0]?.value()
    if (powerMode != null) {
        def mode = powerMode[0]
        switch (mode) {
            case "PowerOn":
                if (this.state!="on") {
                    sendEvent(name: "switch", value: "on")
                } 
                break;
            case "PowerOff":
            case "DisplayOff":
            case "Headless":
                if (this.state!="off") {
                    sendEvent(name: "switch", value: "off")
                }
                break;
        }
    }    
}

/*
 * Device Capability Interface Functions
 */

def on() {
    sendEvent(name: "switch", value: "turning-on")

    sendHubCommand(new hubitat.device.HubAction (
        "wake on lan ${deviceMac}",
        hubitat.device.Protocol.LAN,
        null,
        [:]
    ))

    keyPress('Power')
}

def off() {
    sendEvent(name: "switch", value: "turning-off")
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

def setVolume() {
    
}

def unmute() {
    keyPress('VolumeMute')
}

def mute() {
    keyPress('VolumeMute')
}

def poll() {
    if (logEnable) log.trace "Executing 'poll'"
    runInMillis(1500, refresh)
}

def refresh() {
    if (logEnable) log.trace "Executing 'refresh'"
    runInMillis(500,queryCurrentApp)
    runInMillis(500,queryDeviceState)
    if (autoManage) runInMillis(500, queryInstalledApps)
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
    runIn(1,queryInstalledApps)
}


/**
 * Roku API Section
 * The following functions are used to communicate with the Roku RESTful API
 **/

def queryDeviceState() {
    sendHubCommand(new hubitat.device.HubAction(
        method: "GET",
        path: "/query/device-info",
        headers: [ HOST: "${deviceIp}:8060" ]
    ))
}

def queryCurrentApp() {
    sendHubCommand(new hubitat.device.HubAction(
        method: "GET",
        path: "/query/active-app",
        headers: [ HOST: "${deviceIp}:8060" ]
    ))
    
}

def queryInstalledApps() {
    sendHubCommand(new hubitat.device.HubAction(
        method: "GET",
        path: "/query/apps",
        headers: [ HOST: "${deviceIp}:8060" ]
    ))
}


def keyPress(key) {
	if (!isValidKey(key)) {
		log.warning("Invalid key press: ${key}")
		return
	}
    if (logEnable) log.debug "Executing '${key}'"
    def result = new hubitat.device.HubAction(
        method: "POST",
        path: "/keypress/${key}",
        headers: [ HOST: "${deviceIp}:8060" ],
        body: "",

    )
}

private def isValidKey(key) {
	def keys = [
		"Home",      "Back",       "FindRemote",  "Select",
		"Up",        "Down",       "Left",        "Right",
		"Play",      "Rev",        "Fwd",         "InstantReplay",
		"Info",      "Search",     "Backspace",   "Enter",
		"VolumeUp",	 "VolumeDown", "VolumeMute",
		"Power",     "PowerOff",
		"ChannelUp", "ChannelDown", "InputTuner", "InputAV1",
		"InputHDMI1", "InputHDMI2", "InputHDMI3", "InputHDMI4"
		]
	
	return keys.contains(key)
}

def launchApp(appId) {
    if (logEnable) log.debug "Executing 'launchApp ${appId}'"
	if (appId =~ /(hdmi\d)|AV1|Tuner/) {
		this."input_$appId"()
	} else {
        def result = new hubitat.device.HubAction(
        method: "POST",
        path: "/launch/${appId}",
        headers: [ HOST: "${deviceIp}:8060" ],
        body: "",
    )
    sendHubCommand(result)
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


