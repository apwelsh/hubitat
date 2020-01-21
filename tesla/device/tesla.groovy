/** 
 *  Tesla
 *
 *  Copyright 2020 Armand Welsh
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Tesla", namespace: "trentfoley", author: "Trent Foley") {
		capability "Actuator"
		capability "Battery"
		capability "Lock"
		capability "Motion Sensor"
		capability "Presence Sensor"
		capability "Refresh"
		capability "Temperature Measurement"
		capability "Thermostat Mode"
		capability "Thermostat Setpoint"

		attribute "state", "string"
		attribute "vin", "string"
		attribute "odometer", "number"
		attribute "batteryRange", "number"
		attribute "chargingState", "string"

		attribute "latitude", "number"
		attribute "longitude", "number"
		attribute "method", "string"
		attribute "heading", "number"
		attribute "lastUpdateTime", "string"
		attribute “distanceAway”, “number”

		command "wake"
		command "setThermostatSetpoint"
		command "startCharge"
		command "stopCharge"
		command "openFrontTrunk"
		command "openRearTrunk"
	}

}

def initialize() {
	log.debug "Executing 'initialize'"
    
    sendEvent(name: "supportedThermostatModes", value: ["auto", "off"])
    
    runEvery15Minutes(refresh)
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

private processData(data) {
	  if(data) {
        log.debug "processData: ${data}"
        
        sendEvent(name: "state", value: data.state)
        sendEvent(name: "motion", value: data.motion)
        sendEvent(name: "speed", value: data.speed, unit: "mph")
        sendEvent(name: "vin", value: data.vin)
        sendEvent(name: "thermostatMode", value: data.thermostatMode)
        
        if (data.chargeState) {
            sendEvent(name: "battery", value: data.chargeState.battery)
            sendEvent(name: "batteryRange", value: data.chargeState.batteryRange)
            sendEvent(name: "chargingState", value: data.chargeState.chargingState)
        }
        
        if (data.driveState) {
            sendEvent(name: "latitude", value: data.driveState.latitude)
            sendEvent(name: "longitude", value: data.driveState.longitude)
            sendEvent(name: "method", value: data.driveState.method)
            sendEvent(name: "heading", value: data.driveState.heading)
            sendEvent(name: "lastUpdateTime", value: data.driveState.lastUpdateTime)
            def dist = distance(location.latitude, location.longitude, data.driveState.latitude, data.driveState.longitude)
            log.debug "distance: ${dist}"
            sendEvent(name: "distanceAway", value: dist)
        }
        
        if (data.vehicleState) {
            sendEvent(name: "presence", value: data.vehicleState.presence)
            sendEvent(name: "lock", value: data.vehicleState.lock)
            sendEvent(name: "odometer", value: data.vehicleState.odometer)
        }
        
        if (data.climateState) {
        	sendEvent(name: "temperature", value: data.climateState.temperature)
            sendEvent(name: "thermostatSetpoint", value: data.climateState.thermostatSetpoint)
        }
    } else {
    	  log.error "No data found for ${device.deviceNetworkId}"
    }
}

def refresh() {
	  log.debug "Executing 'refresh'"
    log.debug "Home Location (lat/lon) ${location.latitude}/${location.longitude}"
    def data = parent.refresh(this)
	  processData(data)
}

def wake() {
    log.debug "Executing 'wake'"
    def data = parent.wake(this)
    processData(data)
    runIn(30, refresh)
}

def lock() {
    log.debug "Executing 'lock'"
    def result = parent.lock(this)
    if (result) { refresh() }
}

def unlock() {
    log.debug "Executing 'unlock'"
    def result = parent.unlock(this)
    if (result) { refresh() }
}

def auto() {
	log.debug "Executing 'auto'"
	def result = parent.climateAuto(this)
    if (result) { refresh() }
}

def off() {
	log.debug "Executing 'off'"
	def result = parent.climateOff(this)
    if (result) { refresh() }
}

def heat() {
	log.debug "Executing 'heat'"
	// Not supported
}

def emergencyHeat() {
	log.debug "Executing 'emergencyHeat'"
	// Not supported
}

def cool() {
	log.debug "Executing 'cool'"
	// Not supported
}

def setThermostatMode(mode) {
	log.debug "Executing 'setThermostatMode'"
	switch (mode) {
    	case "auto":
        	auto()
            break
        case "off":
        	off()
            break
        default:
        	log.error "setThermostatMode: Only thermostat modes Auto and Off are supported"
    }
}

def setThermostatSetpoint(setpoint) {
	log.debug "Executing 'setThermostatSetpoint'"
	def result = parent.setThermostatSetpoint(this, setpoint)
    if (result) { refresh() }
}

def startCharge() {
	log.debug "Executing 'startCharge'"
    def result = parent.startCharge(this)
    if (result) { refresh() }
}

def stopCharge() {
	log.debug "Executing 'stopCharge'"
    def result = parent.stopCharge(this)
    if (result) { refresh() }
}

def openFrontTrunk() {
	log.debug "Executing 'openFrontTrunk'"
    def result = parent.openTrunk(this, "front")
    // if (result) { refresh() }
}

def openRearTrunk() {
	log.debug "Executing 'openRearTrunk'"
    def result = parent.openTrunk(this, "rear")
    // if (result) { refresh() }
}

def distance(lat1, lon1, lat2, lon2) {
    // compute horizontal distance bteween two points at sea-level, in meters

    final int R = 6371; // Radius of the earth (equator 6378, at poles 6357, median radius 6371)

    def latDistance = Math.toRadians(lat2 - lat1);
    def lonDistance = Math.toRadians(lon2 - lon1);
    def a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    def c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    def distance = R * c * 1000; // convert to meters

    return distance
}

