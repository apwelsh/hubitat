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
	input "deviceIp", "text", title: "Device IP", required: true
	input "deviceMac", "text", title: "Device MAC Address", required: true, defaultValue: "UNKNOWN"
}

metadata {
	definition (name: "Roku TV", namespace: "apwelsh", author: "Armand Welsh") {
		capability "TV"
		capability "AudioVolume"
		capability "Switch"
		capability "Polling"
		capability "Refresh"

		command "hdmi1"
		command "hdmi2"
		command "hdmi3"
		command "hdmi4"
		
		command "reloadApps"
  }
}

def installed() {
	updated()
}

def updated() {
	log.debug "updated"
	poll()
	runEvery5Minutes(poll)
	runEvery1Minute(queryCurrentApp)
}

def parse(String description) {
	def msg = parseLanMessage(description)
	
	if (msg.body) {
		def body = new XmlParser().parseText(msg.body)
		switch (body.name()) {
			case "device-info":
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
	}
}

def reloadApps() {
	parseInstalledApps new XmlParser().parseText("<app/>")
	refresh()
}

def parseInstalledApps(Node body) {
	
	childDevices.each{ child ->
		//log.debug "child: ${child.deviceNetworkId} (${child.name})"
		def nodeExists = false
		body.app.each{ node ->
			if ("${device.deviceNetworkId}-${node.attributes().id}" == child.deviceNetworkId)
			nodeExists = true
		}
		if (!nodeExists) {
			log.debug "Deleting missing app: ${child.name}"
			deleteChildDevice(child.deviceNetworkId)
		}
	}
	
	body.app.each{ node ->
		if (node.attributes().type != "appl") {
			return
		}
		def appId = node.attributes().id
		def appName = node.value()[0]
		
		// log.debug "Detected installed channel app: (${appId}) ${appName}"
		
		updateChild(appId, appName)
	}
}

def parseActiveApp(Node body) {
	def app = body.app[0]?.value()
	if (app != null) {
		def currentApp = app[0]
		if (currentApp != this.state."active-app") {
			log.debug "set active-app = ${currentApp}"
			this.state."active-app" = currentApp
		}
	}
}

def parseState(Node body) {
	for (def node : body) {
		def key = node.name()
		if (key == null)
			continue
		
		switch (key) {
			case "serial-number":
			case "vendor-name":
			case "device-id":
			case "mode-name":
			case "screen-size":
			case "user-device-name":
				def value = node.value()
				if (value != null) {
					if (value[0] != this.state."${key}") {
						this.state."${key}" = value[0]
						log.debug "set ${key} = ${value[0]}"
					}
				}
		}
	}
}

def parseMacAddress(Node body) {
	def wifiMac = body."wifi-mac"[0]?.value()
	if (wifiMac != null) {
		def macAddress = wifiMac[0].replaceAll("[^A-f,a-f,0-9]","")
		if (!deviceMac || deviceMac != macAddress) {
			log.debug "Update config [MAC Address = ${macAddress}]"
			device.updateSetting("deviceMac", [value: macAddress, type:"text"])
		}
	}
}

def parsePowerState(Node body) {
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
			case "Headless":
				if (this.state!="off") {
					sendEvent(name: "switch", value: "off")
				}
				break;
		}
	}	
}

def on() {
	sendEvent(name: "switch", value: "on")

	sendHubCommand(new hubitat.device.HubAction (
		"wake on lan ${deviceMac}",
		hubitat.device.Protocol.LAN,
		null,
		[:]
	))

	keypress('Power')
}

def off() {
	sendEvent(name: "switch", value: "off")
	keypress('PowerOff')
}

def channelUp() {
	keypress('ChannelUp')
}

def channelDown() {
	keypress('ChannelDown')
}

def volumeUp() {
	keypress('VolumeUp')
}

def volumeDown() {
	keypress('VolumeDown')
}

def setVolume() {
	
}

def unmute() {
	keypress('VolumeMute')
}

def mute() {
	keypress('VolumeMute')
}

def hdmi1() {
	keypress('InputHDMI1')
}

def hdmi2() {
	keypress('InputHDMI2')
}

def hdmi3() {
	keypress('InputHDMI3')
}

def hdmi4() {
	keypress('InputHDMI4')
}

def poll() {
	log.debug "Executing 'poll'"
	refresh()
}

def refresh() {
	log.debug "Executing 'refresh'"
	queryDeviceState()
	queryInstalledApps()
	//queryCurrentApp()
}

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

def keypress(key) {
	log.debug "Executing '${key}'"
	def result = new hubitat.device.HubAction(
		method: "POST",
		path: "/keypress/${key}",
		headers: [ HOST: "${deviceIp}:8060" ],
	)
}

def launchApp(appId) {
	log.debug "Executing 'launchApp ${appId}'"
	def result = new hubitat.device.HubAction(
		method: "POST",
		path: "/launch/${appId}",
		headers: [ HOST: "${deviceIp}:8060" ],
	)
	sendHubCommand(result)
	runInMillis(3000, 'queryCurrentApp')
	runInMillis(5000, 'queryCurrentApp')
	runInMillis(10000, 'queryCurrentApp')
}

def updateChild(String appId, String appName) {
    
	//Try to find existing child device
    def child = getChild(appId)

    //If child exists, do not create it
	if(child != null) {
		return
	}

	if (appName != null) {
		if (logEnable) log.debug "child with appId = ${appId}, appName = ${appName}  does not exist."
		createChildDevice(appId, appName)
		child = getChild(appId)
	} else {
		log.error "Cannot create child device for ${appId} due to missing 'activityName'"
	}
}

private def getChild(String appId) {
	
	def netId = "${device.deviceNetworkId}-${appId}"
    try {
		def result = null
        childDevices.each{ child ->
            if(child.deviceNetworkId == netId) {
                result = child
            }
        }
		return result
    } catch(e) {
        log.error "Failed to find child without exception: ${e}";
    }
	return null
}

private void createChildDevice(String appId, String appName) {
    try {
		def netId = "${device.deviceNetworkId}-${appId}"
        addChildDevice("Roku App", "${netId}",
            [label: "${device.displayName}-${appName}", 
             isComponent: false, name: "${appName}"])
        log.trace "Created child device with network id: ${netId}"
    } catch(e) {
        log.error "Failed to create child device with error = ${e}"
    }
}

