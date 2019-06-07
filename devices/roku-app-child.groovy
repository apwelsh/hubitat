/**
 * Roku App
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device.  To use this device you simply need to install the device type handler.
 * The parent device will take care of add instances of the device handler for each application that
 * needs to be activated.  As this device is a generic push-button with a momentary switch, simillar to
 * the scene controllers,  in a future version this will be modified to be a generic momentary switch child
 * device instead.
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
	input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
}

metadata {
	definition (name: "Roku App", namespace: "apwelsh", author: "Armand Welsh") {
		capability "Momentary"
		capability "Switch"
		capability "Actuator"
	}

}

def on() {
	sendEvent (name: "switch", value:"turning-on")
	def appId = parent.appIdForNetworkId(device.deviceNetworkId)
	parent.launchApp(appId)
}

def off() {
	if (device.currentValue("switch") == "off")
		return

	sendEvent (name: "switch", value:"turning-off")	
	parent.home()
}

def push() {
	on()
}

def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
}
