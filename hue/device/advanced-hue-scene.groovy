/**
 * AdvancedHueScene
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device for the Advanced Hue Bridge Integeration app.  This device is used to manage hue scenes which 
 * are custome settings for hue groups.  As such, the scenes are implemented a switches.  At this time, the scenes may no
 * cutomized by this device, though plans are in the works to allow for this -- most likely via a hue scene controller.
 * This is a child device that will be created by the Advanced Hue Bridge Integration App.
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

