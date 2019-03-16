/**
 *  Roku App
 *
 */
metadata {
	definition (name: "Roku App", namespace: "apwelsh", author: "Armand Welsh") {
		capability "Momentary"

	}

}

def push() {
	def appId = device.deviceNetworkId.split("-")[1]
	parent.launchApp(appId)
}

def parse(String description) {
    log.debug "parse(${description}) called"
}

