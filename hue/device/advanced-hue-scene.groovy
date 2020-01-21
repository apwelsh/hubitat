/**
 * AdvancedHueScene
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device for the Advanced Hue Bridge Integeration app.  This device is used to manage hue scenes which 
 * are custome settings for hue groups.  As such, the scenes are implemented a switches.  At this time, the scenes may no
 * cutomized by this device, though plans are in the works to allow for this -- most likely via a hue scene controller.
 * This is a child device that will be created by the Advanced Hue Bridge Integration App.
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
    input name: "refreshEnabled", type: "bool", title: "Enable refresh", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false

}

metadata {
    definition (name:      "AdvancedHueScene", 
                namespace: "apwelsh", 
                author:    "Armand Welsh", 
                importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-sceme.groovy") {
        
        capability "Switch"
        capability "Actuator"
        capability "Refresh"
        
    }
}

/** Switch Commands **/

def on() {
    def node = deviceIdNode(device.deviceNetworkId)
    setDeviceState(this, ["scene":node])
}

def off() {
    setSwitchState("off")
}

/** Refresh Commands **/
def refresh() {

}

def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
}

def autoOff() {
    sendEvent(name: "switch", value: "off")
}

def deviceIdNode(deviceNetworkId) {
    parent.deviceIdNode(deviceNetworkId)
}

def setDeviceState(device, args) {
    parent.setDeviceState(args)
}

def setSwitchState(value) {
    if (value == "on") {
        runInMillis(200, autoOff)
    } else {
        unschedule(autoOff)
    }
    sendEvent(name: "switch", value: value)
}

