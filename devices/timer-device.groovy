/**
 * Timer Device
 * v1.10
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a simple count-down timer I created for a friend.  I wanted to use the standard TimedSession capability.
 * Making it work with rules is more difficult than it should be though, since HE does not seem to support this yet.
 * As such, I implement the PushableButton as well to be able to trigger an event when the timer has expired.  This is a
 * very simple timer based on a cron type schedule.  It is now an accurate timer, that is rather light-weight.
 * To use the timer, first set the TimeRemaining attribute, then start the timer.  When the timer has expired, it
 * will issue a button push.  To use this in ruless to trigger timer completion, create a rule on Button 1 Pushed.
 * 
 * I have updated the driver to allow upates of every 1 second, every 5 seconds, or dynamic (every 10 seconds at most)
 * This update includes improved timer scheduler logic
 *-------------------------------------------------------------------------------------------------------------------
 * Copyright 2020 Armand Peter Welsh
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 * documentation files (the 'Software'), to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *-------------------------------------------------------------------------------------------------------------------
 **/
preferences {
    input name: 'idleText',     
          type: 'bool', title: 'Idle Message',   
          description: 'Show Idle message when timer is done', 
          defaultValue: false
    input name: 'useDefault',
          type: 'bool', title: 'Use default timer time',
          description: 'Enable this switch to define a default timer time to use.'
          defaultValue: false
    if (useDefault) {
        input name: 'defaultTime',
            type: 'number', title: 'Default Timer',
            description: 'The number of seconds to set the timer to, if the timeRemaining value is zero (0).'
            defaultValue: null
    }
    input name: 'cancelWhenOff',
          type: 'bool', title: cancelWhenOff ? 'Timer will be canceled when off' : 'Timer will be stopped when off'
          description: 'Toggle to change the behavior when the timer is turned off'
          defaultValue: false
    input name: 'logEnable',    
          type: 'bool', title: 'Logging',        
          description: 'Enable debug logging', 
          defaultValue: false
    input name: 'updateMode',
          type: 'enum', title: 'Update Mode',
          options: ['aggressive': 'Aggressive - Every seconds', 'moderate': 'Moderate - Every 5 seconds', 'dynamic': 'Dynamic - More aggressive as time nears completion'],
          description: 'If enabled, the time remaining will be updated every single second.',
          defaultValue: 'dynamic'
}

metadata {
    definition (name:      'Timer Device', 
                namespace: 'apwelsh', 
                author:    'Armand Welsh', 
                importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/devices/timer-device.groovy') {
        
        capability 'TimedSession'
        capability 'Sensor'
        capability 'PushableButton'
        capability 'Switch'
        
        attribute  'display', 'string'
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
    sendEvent(name: 'numberOfButtons', value: 1)
    setTimeRemaining(0)
}

/**
 ** TimedSession Methods
 **/

def cancel() {
    if (logEnable) log.info 'Canceling timer'
    setStatus('canceled')
    setTimeRemaining(0)
}

def pause() {
    if (state.alerttime) {
        setTimeRemaining(((state.alerttime - now()) / 1000) as int)
        unschedule()
        state.remove('refreshInterval')
        state.remove('alerttime')
        setStatus('paused')
        if (logEnable) log.info 'Timer paused'
    }
}

def scheduleTimerEvent(secondsRemaining) {
    def refreshInterval = 1

    if (secondsRemaining > 60 && updateMode == 'dynamic') {
        if ((secondsRemaining as int) % 10 == 0) refreshInterval = 10
        else return
    }
    else if (secondsRemaining > 10 && updateMode != 'aggressive') {
        if ((secondsRemaining as int) % 5 == 0)  refreshInterval = 5
        else return
    }
    
    if (((state.refreshInterval?:0) as int) != refreshInterval) {
        def t = refreshInterval == 1 ? '*' : new Date().getSeconds() % refreshInterval
        unschedule(timerEvent)
        schedule("${t}/${refreshInterval} * * * * ?", timerEvent, [misfire: 'ignore', overwrite: false])
        state.refreshInterval = refreshInterval
        if (logEnable) log.info "Changed timer update frequency to every ${refreshInterval} second(s)"
    }

}

def setTimeRemaining(seconds) {

    if (seconds == 0) {
        timerDone()
    }

    if (state.alerttime) {
        scheduleTimerEvent(seconds as int)
    }
    
    int days  = (seconds / 86400) as int
    int hours = ((seconds.intValue() % 86400) / 3600) as int
    int mins  = ((seconds.intValue() % 3600) / 60) as int
    int secs  = (seconds.intValue() % 60) as int
    if (days > 0) {
        remaining = String.format('%d %s %d:%02d:%02d', days, days == 1 ? 'day' : 'days', hours, mins, secs)
    } else if (hours > 0) {
        remaining = String.format('%d:%02d:%02d', hours, mins, secs)
    } else {
        remaining = String.format('%02d:%02d', mins, secs)
    }
    
    sendEvent(name: 'timeRemaining', value: seconds)
    sendEvent(name: 'display', value: remaining)

}


def on() {
    start()
}

def start() {
    if (logEnable) log.info 'Timer started'
    unschedule()
    long timeRemaining = (device.currentValue('timeRemaining') ?: 0 as long)
    if (timeRemaining == 0 && useDefault) {
        timeRemaining = defaultTime
        if (logEnable) log.info "Using default time of ${timeRemaining} seconds"
        setTimeRemaining(timeRemaining)
    }

    setStatus('running')
    
    runIn(timeRemaining, timerDone,[overwrite:true, misfire: 'ignore'])
    state.alerttime = now() + (timeRemaining * 1000)

    def refreshInterval = 1
    state.refreshInterval = refreshInterval
    schedule('* * * * * ?', timerEvent, [misfire: 'ignore', overwrite: true])
}

def off() {
    if (cancelWhenOff == true) {
        cancel()
    } else {
        stop()
    }
}

def stop() {
    unschedule()
    setTimeRemaining(0)
    if (logEnable) log.info 'Timer stopped'
}

/**
 ** PushableButton Method
 **/

def push() {
    sendEvent(name: 'pushed', value: 1, isStateChange: true)
}

/**
 ** Support Methods
 **/

def setStatus(status) {
    sendEvent(name: 'sessionStatus', value: status, isStateChange: true)
    switch (status) {
        case 'running':
        case 'paused':
            sendEvent(name: 'switch', value: 'on')
            break;
        default:
            sendEvent(name: 'switch', value: 'off')
    }
}

def resetDisplay() {
    sendEvent(name: 'display', value: idleText ? 'idle' : '--:--')
}

def timerDone() {
    if (device.currentValue('switch') == 'on') {
        unschedule()
        state.remove('alerttime')
        state.remove('refreshInterval')
        if (device.latestValue('sessionStatus') != 'canceled') {
            sendEvent(name: 'timeRemaining', value: 0)
            setStatus('stopped')
        }
        runIn(1, resetDisplay)
        if (device.latestValue('sessionStatus') != 'canceled') {
            push()
        }   
    }
}

def timerEvent() {
    if (state.alerttime) {
        setTimeRemaining(((state.alerttime - now())/1000) as int)
    } else {
        stop()
    }
}


