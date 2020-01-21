/**
 * Advanced Hue Bridge
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device handler for the Advance Hue Bridge Integration App.  This device manage the hub directly for
 * actions such as refresh.
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
import groovy.json.JsonSlurper

preferences {
    input name: "autoRefresh", type: "bool", defaultValue: false, title: "Auto Refresh",        description: "Should this device support automatic refresh" 
    if (autoRefresh)
        input name: "refreshInterval", type: "number", defaultValue: 60, title: "Refresh Inteval", description: "Number of seconds to refresh the group state" 
    input name: "anyOn",       type: "bool", defaultValue: true,  title: "ANY on or ALL on",    description: "When ebabled, the group is considered on when any light is on"
    input name: "logEnable",   type: "bool", defaultValue: false, title: "Enable debug logging"

}

metadata {
    definition (
        name:      "AdvancedHueBridge", 
		namespace: "apwelsh", 
		author:    "Armand Welsh", 
		importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-bridge.groovy") {
		
        capability "Switch"
        capability "Refresh"

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
	refresh()
}

/*
 * Device Capability Interface Functions
 */

/** Switch Commands **/

def on() {
    setDeviceState(["on":true])
}

def off() {
    setDeviceState(["on": false])
}


def refresh() {
    if (logEnable) log.trace "Executing 'refresh'"
    parent.getDeviceState(this)
    parent.getHubStatus()
}

def resetRefreshSchedule() {
    unschedule()
    if (autoRefresh) schedule("0/${refreshInterval} * * * * ?", refresh) // Move the schedule to avoid redundant refresh events    
}

/**
 * Event Parsers
 **/
def parse(data) {
    if (data.groups) {
        parseGroups(data.groups)
    }
}

def parseGroups(groups) {
    if (!groups) {
        return
    }
    groups.each { idx, group ->
        log.debug "Found group: ${group.name} on ${device.label}"
    }
//    log.debug "${groups}"
}

private def cleanState() {
	this.state.clear
}


def setHueProperty(name, value) {
    // if (logEnable) log.info "setHueProperty(${name}) = ${value}"
    switch (name) {
        case "on":
        sendEvent(name: "switch", value: value == true ? "on" : "off")
        break;
        case "any_on":
        sendEvent(name: "switch", value: value ? "on" : "off")
        break;
    }
}

def deviceIdNode(deviceNodeId) {
     parent.deviceIdNode(deviceNodeId)
}

def setDeviceState(args) {
    parent.setDeviceState(this, args)
}

def networkIdForScene(sceneId) {
    parent.networkIdForScene(deviceIdNode(device.deviceNetworkId), sceneId)
}



