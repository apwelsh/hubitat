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
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
}

metadata {
    definition (name:      "Roku App", 
                namespace: "apwelsh", 
                author:    "Armand Welsh", 
                importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/roku/device/roku-tv-app.groovy") {
        
        capability "Momentary"
        capability "Switch"
        capability "Actuator"
    }
}

def on() {
    def appId = parent.appIdForNetworkId(device.deviceNetworkId)
    parent.launchApp(appId)
}

def off() {
    if (device.currentValue("switch") == "off")
        return

    parent.home()
}

def push() {
    on()
}

def parse(String description) {
    if (logEnable) log.debug "parse(${description}) called"
}
