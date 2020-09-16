/**
 * AdvancedHueGroup
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device for the Advanced Hue Bridge Integeration app.  This device is used to manage hue zones and rooms
 * as color lights.  The key difference between this and the built-in hueGroup device is that this device supports the
 * refresh capability -- if enabled -- to allow for fast device refresh, and to act as the parent device for the
 * AdvancedHueScene device.
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
    input name: "autoRefresh", type: "bool", defaultValue: false, title: "Auto Refresh",        description: "Should this device support automatic refresh" 
    if (autoRefresh)
        input name: "refreshInterval", type: "number", defaultValue: 60, title: "Refresh Inteval", description: "Number of seconds to refresh the group state" 
    input name: "anyOn",       type: "bool", defaultValue: true,  title: "ANY on or ALL on",    description: "When ebabled, the group is considered on when any light is on"
    input name: "logEnable",   type: "bool", defaultValue: false, title: "Enable debug logging"

}

metadata {
    definition (name:      "AdvancedHueGroup", 
                namespace: "apwelsh", 
                author:    "Armand Welsh", 
                importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-group.groovy") {
        
        capability "Light"
        capability "Switch"
        capability "SwitchLevel"
        capability "Actuator"
        capability "ColorControl"
        capability "ColorMode"
        capability "ColorTemperature"
        capability "Refresh"
        
        command "activateScene", [[name: "Scene Identitier*", type: "STRING", description: "Enter a scene name or the scene number as defined by the Hue Bridge"]]
    }
}

def installed() {
    parent.getDeviceState(this)
}

def updated() {
    if (logEnable) log.debug "Preferences updated"
    parent.getDeviceState(this)
}

/** Switch Commands **/

def on() {
    setDeviceState(["on":true])
}

def off() {
    setDeviceState(["on": false])
}

/** ColorControl Commands **/

def setColor(colormap) {
    //colormap required (COLOR_MAP) - Color map settings [hue*:(0 to 100), saturation*:(0 to 100), level:(0 to 100)]
    def hue = Math.round((colormap.hue?:device.currentValue('hue')?:0) * 655.35)
    def saturation = Math.round((colormap.saturation?:device.currentValue('saturation')?:50) * 2.54)
    def level = Math.round((colormap.level?:device.currentValue('level')?:100 ) * 2.54)
    
    def args = ["hue":hue, 
                "sat":saturation,
                "bri":level]
    setDeviceState(args)
    
}
def setHue(hue) {
    //hue required (NUMBER) - Color Hue (0 to 100)
    
    def args = ["hue":Math.round(hue * 655.35)]
    setDeviceState(args)
}

def setSaturation(saturation) {
    //saturation required (NUMBER) - Color Saturation (0 to 100)
    def args = ["sat":Math.round(saturation * 2.54)]
    setDeviceState(args)
}

/** ColorTemperature Commands **/

def setColorTemperature(colortemperature) {
    //colortemperature required (NUMBER) - Color temperature in degrees Kelvin
    //are capable of 153 (6500K) to 500 (2000K).
    
    def ct = Math.round(500 - ((colortemperature - 2000) / (4500 / 347)))
    setDeviceState(["ct":ct, "colormode":"ct"])
}

/** SwitchLevel Commands **/

def setLevel(level, duration=null) {
    //level required (NUMBER) - Level to set (0 to 100) (Hue expects 0-254)
    //duration optional (NUMBER) - Transition duration in seconds
    
    def args = ["bri":Math.round(level * 2.54)]
    if (duration != null) {
        args["transitiontime"] = duration * 10
    }
    setDeviceState(args)
}

/** Refresh Commands **/
def refresh() {
    parent.getDeviceState(this)
}

def resetRefreshSchedule() {
    unschedule()
    if (autoRefresh) schedule("0/${refreshInterval} * * * * ?", refresh) // Move the schedule to avoid redundant refresh events    
}


def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
}

def setHueProperty(name, value) {
    switch (name) {
        case "scene":
        def nid = networkIdForScene(value)
        setSwitchState("on", getChildDevice(nid))
        refresh()
        break;
        case "any_on":
        if (anyOn) sendEvent(name: "switch", value: value ? "on" : "off")
        break;
        case "all_on":
        if (!anyOn) sendEvent(name: "switch", value: value ? "on" : "off")
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

def activateScene(scene) {
    def sceneId = parent.findScene(deviceIdNode(device.deviceNetworkId), scene)?.key
    if (sceneId) {
        if (logEnable) log.info "Activate Scene: ${sceneId}"
        setDeviceState(["scene": sceneId])
    }
}

/*
 * Component Child Methods
 */

void componentOn(child) {
    def node = deviceIdNode(child.deviceNetworkId)
    setDeviceState(["scene":node])
}

void componentOff(child) {
    setSwitchState("off", child)
}

void componentRefresh(child){
    if (logEnable) log.info "received refresh request from ${child.displayName} - ignored"
}

def setSwitchState(value, child) {
    if (value == "on") {
        child.runInMillis(400, off)
    } else {
        unschedule(child.off)
    }
    child.sendEvent(name: "switch", value: value)
}

def autoOff(child) {
    child.sendEvent(name: "switch", value: "off")
}
