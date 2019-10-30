/**
 * Advanced Hue Bridge
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device handler for the Advance Hue Bridge Integration App.  This device manage the hub directly for
 * actions such as refresh.
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
import groovy.json.JsonSlurper

preferences {
    input name: "refreshUnits",    type: "enum",   title: "Refresh interval measured in Minutes, or Seconds", options:["Minutes","Seconds"], defaultValue: "Minutes", required: true
    input name: "refreshInterval", type: "number", title: "Refresh the status at least every n ${refreshUnits}.  0 disables auto-refresh, which is not recommended.", range: 0..60, defaultValue: 5, required: true
    input name: "autoManage",      type: "bool",   title: "Enable automatic management of child devices", defaultValue: true, required: true
    input name: "logEnable",       type: "bool",   title: "Enable debug logging", defaultValue: true, required: true
}

metadata {
    definition (
        name:      "AdvancedHueBridge", 
		namespace: "apwelsh", 
		author:    "Armand Welsh", 
		importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-bridge.groovy") {
		
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
	unschedule()
	if (deviceIp && refreshInterval > 0) {
        if (refreshUnits == "Seconds") {            
            schedule("${new Date().format("s")}/${refreshInterval} * * * * ?", refresh)
        } else {
    		schedule("${new Date().format("s")} 0/${refreshInterval} * * * ?", refresh)
        }
	}
}

/*
 * Device Capability Interface Functions
 */

def refresh() {
    if (logEnable) log.trace "Executing 'refresh'"
    parent.refreshHubStatus(device.deviceNetworkId)    
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



