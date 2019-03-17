/**
 * Roku App
 * This is a child device.  To use this device you simply need to install the device type handler.
 * The parent device will take care of add instances of the device handler for each application that
 * needs to be activated.  As this device is a generic push-button with a momentary switch, simillar to
 * the scene controllers,  in a future version this will be modified to be a generic momentary switch child
 * device instead.
 *
 */
metadata {
	definition (name: "Roku App", namespace: "apwelsh", author: "Armand Welsh") {
		capability "Momentary"
		capability "Switch"
		capability "Actuator"
	}
}

def on() {
	sendEvent (name: "switch", value:"on")
	def appId = device.deviceNetworkId.split("-")[1]
	parent.launchApp(appId)
	
	runInMillis(3000, 'off')
}

def off() {
	sendEvent (name: "switch", value:"off")	
}

def push() {
	on()
}

def parse(String description) {
    log.debug "parse(${description}) called"
}

