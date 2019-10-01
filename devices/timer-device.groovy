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

def updated() {
    installed()
}

def installed() {
    sendEvent(name: "numberOfButtons", value: 1)
}

def cancel() {
    unschedule()
    
    setStatus("canceled")
    runIn(1, stop)
}

def pause() {
    unschedule()
    setStatus("paused")
}

def setTimeRemaining(seconds) {
    sendEvent(name: "timeRemaining", value: seconds)
    def mins = seconds / 60
    def secs = seconds.intValue() % 60
    def remaining = String.format("%02d:%02d", mins.intValue(), secs.intValue())
    sendEvent(name: "display", value: remaining)
}

def start() {
    def refreshInterval = 1
    setStatus("running")
    schedule("0/${refreshInterval} * * * * ?", timerEvent)
}

def stop() {
    unschedule()
    setStatus("stopped")
    push()
}

def push() {
    sendEvent(name: "pushed", value: 1)
    runInMillis(250, release)
}

def release() {
    sendEvent(name: "pushed", value: 0)
}

def setStatus(status) {
    sendEvent(name: "sessionStatus", value: status)
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

