/**
 * Timer Device
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a simple count-down timer I created for a friend.  I wanted to use the standard TimedSession capability.
 * Making it work with rules is more difficult than it should be though, since HE does not seem to support this yet.
 * As such, I implement PushableButton as well to be able to trigger an event when the timer has expired.  This is a
 * very simple timer based on a cron type schedule.  It is not perfectly accurate, but it is rather light-weight.
 * In the next version, I plan to improve the computation of the timer.  
 * To use the timer, first set the TimeRemaining attribute, then start the timer.
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
    input name: "idleText", type: "bool", title: "Show Idle message when timer is done", defaultValue: false
	input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
}

metadata {
    definition (name:      "Timer Device", 
                namespace: "apwelsh", 
                author:    "Armand Welsh", 
                importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/devices/timer-device.groovy") {
		
        capability "TimedSession"
        capability "Sensor"
        capability "PushableButton"
        
        attribute  "display", "string"
    }
}

/**
 ** Lifecycle Methods
 **/

def updated() {
    def timeRemaining = device.currentValue('timeRemaining')
    setTimeRemaining(timeRemaining?:0)
}

def installed() {
    sendEvent(name: "numberOfButtons", value: 1)
    setTimeRemaining(0)
}

/**
 ** TimedSession Methods
 **/

def cancel() {
    setStatus("canceled")
    setTimeRemaining(0)
}

def pause() {
    unschedule()
    setStatus("paused")
}

def setTimeRemaining(seconds) {
    sendEvent(name: "timeRemaining", value: seconds)
    def remaining
    if (seconds == 0) {
        unschedule()
        if (device.currentValue('sessionStatus') != "canceled")
            setStatus("stopped")
        runIn(1, resetDisplay)
        push()
    }
    def hours = (seconds / 3600) as int
    if (hours > 0)
        seconds = seconds.intValue() % 3600 // remove the hours component
    def mins = (seconds / 60) as int
    def secs = (seconds.intValue() % 60) as int
    if (hours > 0) {
        remaining = String.format("%d:%02d:%02d", hours, mins, secs)
    } else {
        remaining = String.format("%02d:%02d", mins, secs)
    }
    sendEvent(name: "display", value: remaining)
}

def start() {
    setStatus("running")
    def refreshInterval = 1
    schedule("0/${refreshInterval} * * * * ?", timerEvent, [mifire: true])
}

def stop() {
    setTimeRemaining(0)
}

/**
 ** PushableButton Method
 **/

def push() {
    sendEvent(name: "pushed", value: 1, isStateChange: true)
}

/**
 ** Support Methods
 **/

def setStatus(status) {
    sendEvent(name: "sessionStatus", value: status, isStateChange: true)
}


def resetDisplay() {
    sendEvent(name: "display", value: idleText ? "idle" : "--:--")
}

def timerEvent() {
    def timeRemaining = device.currentValue('timeRemaining')
    if (timeRemaining) {
        timeRemaining = timeRemaining - 1
        setTimeRemaining(timeRemaining)
    } else {
        stop()
    }
}


