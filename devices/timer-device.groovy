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
    input name: "idleText",     
          type: "bool", title: "Idle Message",   
          description: "Show Idle message when timer is done", 
          defaultValue: false
	input name: "logEnable",    
          type: "bool", title: "Logging",        
          description: "Enable debug logging", 
          defaultValue: false
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
        attribute  "switch",  "string"
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

private def shouldUpdate() {
    def seconds = (state.seconds?:0) as int
    if (seconds <= 10) // if 10 seconds remaining
        return true // every 1 second
    if (seconds <= 30) // if 30 seconds remaining
        if (seconds % 5 == 0) // every 5 seconds
            return true
    if (seconds <= 60) // if 60 seconds remaining
        if (seconds % 15 == 0) // every 15 seconds
            return true
    if (seconds <= 300) // if 5 minutes remaining
        if (seconds % 30 == 0) // every 30 seconds
            return true
    if (seconds <= 1200) // if 20 minutes remaining
        if (seconds % 60 == 0) // every 1 minute
            return true
    if (seconds <= 1800) // if 30 minutes remaining
        if (seconds % 120 == 0) // every 2 minutes
            return true
    if (seconds <= 3600) // if 1 hour remaining
        if (seconds % 300 == 0) // every 5 minutes
            return true
    if (seconds <= 14400) // if 4 hours remaining
        if (seconds % 600 == 0) // every 10 minutes
            return true
    if (seconds % 1800 == 0) // every 30 minutes
        return true
    
    return false
}

def setTimeRemaining(seconds) {
    if (seconds == 0)
        state.remove("seconds")
    else
        state.seconds = seconds
    
    if (!shouldUpdate())
        return
    
    if (seconds == 0) {
        unschedule()
        if (device.currentValue('sessionStatus') != "canceled") {
            sendEvent(name: "timeRemaining", value: seconds)
            setStatus("stopped")
        }
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
    
    sendEvent(name: "timeRemaining", value: seconds)
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
    switch (status) {
        case "running":
        case "paused":
            sendEvent(name: "switch", value: "on")
            break;
        default:
            sendEvent(name: "switch", value: "off")
    }
}


def resetDisplay() {
    sendEvent(name: "display", value: idleText ? "idle" : "--:--")
}

def timerEvent() {
    def timeRemaining = state.seconds
    if (timeRemaining) {
        timeRemaining = timeRemaining - 1
        setTimeRemaining(timeRemaining)
    } else {
        stop()
    }
}
