/**
 * AdvancedHueGroup
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device for the Advanced Hue Bridge Integeration app.  This device is used to manage hue zones and rooms
 * as color lights.  The key difference between this and the built-in hueGroup device is that this device supports the
 * refresh capability -- if enabled -- to allow for fast device refresh, and to act as the parent device for the
 * AdvancedHueScene device.
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
    input name: "refreshEnabled", type: "bool", title: "Enable refresh", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false

}

metadata {
    definition (name:      "AdvancedHueGroup", 
                namespace: "apwelsh", 
                author:    "Armand Welsh", 
                importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-group.groovy") {
        
        capability "Light"
        capability "SwitchLevel"
        capability "Actuator"
        capability "ColorControl"
        capability "ColorMode"
        capability "ColorTemperature"
        capability "Refresh"
        
    }
}

/** Switch Commands **/

def on() {
    parent.setDeviceState(this, ["on":true])
}

def off() {
    parent.setDeviceState(this, ["on": false])
}

/** ColorControl Commands **/

def setColor(colormap) {
    //colormap required (COLOR_MAP) - Color map settings [hue*:(0 to 100), saturation*:(0 to 100), level:(0 to 100)]
}
def setHue(hue) {
    //hue required (NUMBER) - Color Hue (0 to 100)
}

def setSaturation(saturation) {
    //saturation required (NUMBER) - Color Saturation (0 to 100)
}

/** ColorTemperature Commands **/

def setColorTemperature(colortemperature) {
    //colortemperature required (NUMBER) - Color temperature in degrees Kelvin
}

/** SwitchLevel Commands **/

def setLevel(level, duration=null) {
    //level required (NUMBER) - Level to set (0 to 100) (Hue expects 0-254)
    //duration optional (NUMBER) - Transition duration in seconds
    
    def args = ["bri":Math.round(level * 2.54)]
    if (duration != null) {
        args["transitiontime"] = duration * 10
    }
    parent.setDeviceState(this, args)
}

/** Refresh Commands **/
def refresh() {

}

def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
}

def setHueProperty(name, value) {
    log.info "setHutProperty(${name}) = ${value}"
    switch (name) {
        case "on":
        sendEvent(name: "switch", value: value == true ? "on" : "off")
        break;
        case "bri":
        sendEvent(name: "level", value: Math.round(value / 2.54))
        break;
    }
        
}
