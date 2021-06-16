/**
 * Roku TV
 * Version 2.7.11
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a parent device handler designed to manage and control a Roku TV or Player connected to the same network 
 * as the Hubitat hub.  This device handler requires the installation of a child device handler available from
 * the github repo.
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

import groovy.transform.Field
import java.util.concurrent.ConcurrentHashMap

@Field static final String  REFRESH_UNIT_SECONDS = 'Seconds'
@Field static final String  REFRESH_UNIT_MINUTES = 'Minutes'

@Field static final String  DEFAULT_REFRESH_UNITS    = 'Minutes'
@Field static final Integer DEFAULT_REFRESH_INTERVAL = 5
@Field static final Boolean DEFAULT_APP_REFRESH      = false
@Field static final String  DEFAULT_APP_UNITS        = 'Seconds'
@Field static final Integer DEFAULT_APP_INTERVAL     = 5
@Field static final String  DEFAULT_MEDIA_UNITS      = 'Seconds'
@Field static final Integer DEFAULT_MEDIA_INTERVAL   = 2
@Field static final String  DEFAULT_INV_UNITS        = 'Minutes'
@Field static final Integer DEFAULT_INV_INTERVAL     = 30
@Field static final Boolean DEFAULT_AUTO_MANAGE      = false
@Field static final Boolean DEFAULT_MANAGE_APPS      = true
@Field static final Integer DEFAULT_HDMI_PORTS       = 3
@Field static final Boolean DEFAULT_INPUT_AV         = false
@Field static final Boolean DEFAULT_INPUT_TUNER      = false
@Field static final Boolean DEFAULT_LOG_ENABLE       = false
@Field static final Boolean DEFAULT_DBG_ENABLE       = false
@Field static final Integer DEFAULT_TIMEOUT          = 10
@Field static final Boolean DEFAULT_EXPERIMENTAL     = false

@Field static final Integer LIMIT_REFRESH_INTERVAL_MAX = 240
@Field static final Integer LIMIT_REFRESH_INTERVAL_MIN = 0
@Field static final Integer LIMIT_TIMEOUT_MIN          = 1

@Field static final String SETTING_DEVICE_IP        = 'deviceIp'
@Field static final String SETTING_REFRESH_UNITS    = 'refreshUnits'
@Field static final String SETTING_REFRESH_INTERVAL = 'refreshInterval'
@Field static final String SETTING_APP_REFRESH      = 'appRefresh'
@Field static final String SETTING_APP_UNITS        = 'appUnits'
@Field static final String SETTING_APP_INTERVAL     = 'appInterval'
@Field static final String SETTING_MEDIA_UNITS      = 'mediaPlayerUnits'
@Field static final String SETTING_MEDIA_INTERVAL   = 'mediaPlayerInterval'
@Field static final String SETTING_INV_UNITS        = 'appInventoryUnits'
@Field static final String SETTING_INV_INTERVAL     = 'appInventoryInterval'
@Field static final String SETTING_AUTO_MANAGE      = 'autoManage'
@Field static final String SETTING_MANAGE_APPS      = 'manageApps'
@Field static final String SETTING_HDMI_PORTS       = 'hdmiPorts'
@Field static final String SETTING_INPUT_AV         = 'inputAV'
@Field static final String SETTING_INPUT_TUNER      = 'inputTuner'
@Field static final String SETTING_LOG_ENABLE       = 'logEnable'
@Field static final String SETTING_DBG_ENABLE       = 'dbgEnable'
@Field static final String SETTING_TIMEOUT          = 'timeout'
@Field static final String SETTING_USE_POWER_ON     = 'usePowerOn'
@Field static final String SETTING_USE_POWER_OFF    = 'usePowerOff'
@Field static final String SETTING_CREATE_KEY       = 'createChildKey'
@Field static final String SETTING_DELETE_KEY       = 'deleteChildKey'
@Field static final String SETTING_CREATE_APP       = 'createChildApp'
@Field static final String SETTING_DELETE_APP       = 'deleteChildApp'
@Field static final String SETTING_EXPERIMENTAL     = 'experimental'

@Field static final String MEDIA_STATE_PLAYING      = 'playing'
@Field static final String MEDIA_STATE_PAUSED       = 'paused'
@Field static final String MEDIA_STATE_STOPPED      = 'stopped'

@Field static Map volatileAtomicStateByDeviceId = new ConcurrentHashMap()

metadata {
    definition (
        name:      'Roku TV',
        namespace: 'apwelsh',
        author:    'Armand Welsh',
        importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/roku/device/roku-tv.groovy') {
        
        capability 'TV'
        capability 'AudioVolume'
        capability 'MediaTransport'
        capability 'Switch'
        capability 'Polling'
        capability 'Refresh'
        capability 'MediaInputSource'
        capability 'Configuration'
        capability 'Initialize'

        command 'home'
        command 'keyPress', [[name:'Key Press Action', type: 'ENUM', constraints: [
                'Home',      'Back',       'FindRemote',  'Select',        'Up',        'Down',       'Left',        'Right',
                'Play',      'Rev',        'Fwd',         'InstantReplay', 'Info',      'Search',     'Backspace',   'Enter',
                'VolumeUp',  'VolumeDown', 'VolumeMute',  'Power',         'PowerOn',   'PowerOff',
                'ChannelUp', 'ChannelDown', 'InputTuner', 'InputAV1',      'InputHDMI1', 'InputHDMI2', 'InputHDMI3', 'InputHDMI4'] ] ]
        
        command 'reloadApps'

        command 'queryDeviceInfo'
        command 'queryMediaPlayer'
        command 'queryActiveApp'
        command 'queryInstalledApps'

        command 'search', [[name: 'Keywords*',        type: 'STRING', description: 'Search keywords (REQUIRED)'],
                           [name: 'Type*',            type: 'ENUM',   constraints: ['movie', 'tv-show', 'person', 'channel', 'game']],
                           [name: 'Provider ID*',     type: 'NUMBER', description: 'Limit to Channel'],
                           [name: 'Show Unavailable', type: 'ENUM',   constraints: ['false', 'true']],
                           [name: 'TMS ID',           type: 'STRING']
                          ]
        
        attribute 'application', 'string'
//        attribute 'current_app_icon_html', 'string'
    }
}

preferences {
    List allKeys=[
        'Home',      'Back',       'FindRemote',  'Select',
        'Up',        'Down',       'Left',        'Right',
        'Play',      'Rev',        'Fwd',         'InstantReplay',
        'Info',      'Search',     'Backspace',   'Enter',
        'VolumeUp',  'VolumeDown', 'VolumeMute',  'Power',
        'PowerOn',   'PowerOff',   'ChannelUp',   'ChannelDown'
        ]
    List keys=[]
    Map installedKeys=[:]
    try {
        allKeys.each { key ->
            String netId = networkIdForApp(key)
            if (getChildDevice(netId)) {
                installedKeys[netId] = key
            } else {
                keys.add(key)
            }
        }
    } catch (ex) {}

    Map apps=[:]
    Map installed=[:]
    try {
        if (this[SETTING_DEVICE_IP] && (!this[SETTING_AUTO_MANAGE] || !this[SETTING_MANAGE_APPS])) {
            getInstalledApps().each { netId, appName ->
                if (getChildDevice(netId)) {
                    installed[netId] = appName
                } else {
                    apps[netId] = appName
                }
            }
        }
    } catch (ex) {}

    if (!parent) {
        input name: SETTING_DEVICE_IP, type: 'text', title: 'Device IP', required: true
    }
    if (this[SETTING_DEVICE_IP]) {
        input name: SETTING_USE_POWER_ON,      type: 'bool', title: 'Use Power On or Power Toggle for On', required: true, defaultValue: state.isTV ?: false, description: 'Recommend Power On, however, if Power On does not work, disable to use Power toggle'
        input name: SETTING_USE_POWER_OFF,     type: 'bool', title: 'Use Power Off or Power Toggle for Off', required: true, defaultValue: state.isTV ?: false, description: 'Recommend Power Off, however, if Power Off does not work, disable to use Power toggle'
        input name: SETTING_TIMEOUT,           type: 'number', title: 'Communcation timeout', required: true, defaultValue: DEFAULT_TIMEOUT, range: 1..60, description: 'The maximum number of seconds to wait for a roku instruction to complete.  Defaults to 10 seconds.  For fast networks, set to a lower time for better hub performance when your device is offline.'

        input name: SETTING_APP_REFRESH,      type: 'bool',   title: "Using ${this[SETTING_APP_REFRESH] ? 'advanced' : 'simple'} refresh controls", defaultValue: DEFAULT_APP_REFRESH, required: true
        input name: SETTING_REFRESH_UNITS,    type: 'enum',   title: "${this[SETTING_APP_REFRESH] ? 'Device Info' : 'Full'} refresh interval measured in Minutes, or Seconds", options:[REFRESH_UNIT_MINUTES,REFRESH_UNIT_SECONDS], defaultValue: DEFAULT_REFRESH_UNITS, required: true
        input name: SETTING_REFRESH_INTERVAL, type: 'number', title: "${this[SETTING_APP_REFRESH] ? 'Device Info' : 'Full'} refresh at least every n ${this[SETTING_REFRESH_UNITS]}.  0 disables auto-refresh, which is not recommended.", range: 0..240, defaultValue: DEFAULT_REFRESH_INTERVAL, required: true
        if (this[SETTING_APP_REFRESH]) {
            input name: SETTING_APP_UNITS,      type: 'enum',   title: 'Active App refresh interval measured in Minutes, or Seconds', options:[REFRESH_UNIT_MINUTES,REFRESH_UNIT_SECONDS], defaultValue: DEFAULT_APP_UNITS, required: true
            input name: SETTING_APP_INTERVAL,   type: 'number', title: "Active App refresh at least every n ${this[SETTING_APP_UNITS]}. 0 disables refresh", range: 0..240, defaultValue: DEFAULT_APP_INTERVAL, required: true        
            input name: SETTING_MEDIA_UNITS,    type: 'enum',   title: 'Media Player refresh interval measured in Minutes, or Seconds', options:[REFRESH_UNIT_MINUTES,REFRESH_UNIT_SECONDS], defaultValue: DEFAULT_MEDIA_UNITS, required: true
            input name: SETTING_MEDIA_INTERVAL, type: 'number', title: "Media Player refresh at least every n ${this[SETTING_MEDIA_UNITS]}. 0 disables refresh", range: 0..240, defaultValue: DEFAULT_MEDIA_INTERVAL, required: true        
            input name: SETTING_INV_UNITS,      type: 'enum',   title: 'Find installed apps interval measured in Minutes, or Seconds', options:[REFRESH_UNIT_MINUTES,REFRESH_UNIT_SECONDS], defaultValue: DEFAULT_INV_UNITS, required: true
            input name: SETTING_INV_INTERVAL,   type: 'number', title: "Find installed apps at least every n ${this[SETTING_INV_UNITS]}. 0 disables refresh.", range: 0..240, defaultValue: DEFAULT_INV_INTERVAL, required: true        
        }
        input name: SETTING_AUTO_MANAGE,      type: 'bool',   title: 'Enable automatic management of child devices', defaultValue: DEFAULT_AUTO_MANAGE, required: true
        if (this[SETTING_AUTO_MANAGE]?:true == true) {
            input name: SETTING_MANAGE_APPS,      type: 'bool',   title: 'Auto-manage Roku Applications', defaultValue: DEFAULT_MANAGE_APPS, required: true
            input name: SETTING_HDMI_PORTS,       type: 'enum',   title: 'Number of HDMI inputs', options:['0','1','2','3','4'], defaultValue: DEFAULT_HDMI_PORTS, required: true
            input name: SETTING_INPUT_AV,         type: 'bool',   title: 'Enable AV Input', defaultValue: DEFAULT_INPUT_AV, required: true
            input name: SETTING_INPUT_TUNER,      type: 'bool',   title: 'Enable Tuner Input', defaultValue: DEFAULT_INPUT_TUNER, required: true
        }
        input name: SETTING_CREATE_KEY,  type: 'enum',   title: 'Select a key to add a child switch for, and save changes to add the child button for the selected key', options:keys, required: false
        input name: SETTING_DELETE_KEY,  type: 'enum',   title: 'Remove Roku Remote Control Key', options: installedKeys, required: false
        if ((this[SETTING_AUTO_MANAGE]?:true == false || this[SETTING_MANAGE_APPS]?:true == false) && !parent) {
            input name: SETTING_CREATE_APP,     type: 'enum',   title: 'Add Roku App', options: apps, required: false
            input name: SETTING_DELETE_APP,     type: 'enum',   title: 'Remove Roku App', options: installed, required: false

        }
    }
    input name: SETTING_LOG_ENABLE,       type: 'bool',   title: 'Enable informational logging', defaultValue: DEFAULT_LOG_ENABLE, required: true
    input name: SETTING_DBG_ENABLE,       type: 'bool',   title: 'Enable verbose debug logging', defaultValue: DEFAULT_DBG_ENABLE, required: true
    input name: SETTING_EXPERIMENTAL,     type: 'bool',   title: 'Enable experimental features', defaultValue: DEFAULT_EXPERIMENTAL, required: true
}

Map getVolatileAtomicState() {
    volatileAtomicStateByDeviceId.get(device.deviceId)
}

/**
 * Hubitat DTH Lifecycle Functions
 **/
def installed() {
    if (!volatileAtomicStateByDeviceId[device.deviceId]) {
        volatileAtomicStateByDeviceId[device.deviceId] = new ConcurrentHashMap()
    }
    updated()
}

def updateSetting(key, value) {
    device.updateSetting(key, value)
    this[key] = value
}

def initialize() {
    if (!volatileAtomicStateByDeviceId[device.deviceId]) {
        volatileAtomicStateByDeviceId[device.deviceId] = new ConcurrentHashMap()
    }

    // If the cached power state is not set, assume it is off, then initialize the system.
    if (!volatileAtomicState.power) {
        volatileAtomicState.power = 'Off'
    }
    refresh()
}
void updateIpAddress(String ipAddress) {
    device.updateSetting(SETTING_DEVICE_IP, ipAddress)
    this[SETTING_DEVICE_IP] = ipAddress

    String uri = apiPath()
    updateDataValue('query/device-info', "${uri}/query/device-info")
    updateDataValue('query/media-player', "${uri}/query/media-player")
    updateDataValue('query/active-app', "${uri}/query/active-app")
    updateDataValue('query/apps', "${uri}/query/apps")
    String mac = getMACFromIP(this[SETTING_DEVICE_IP])
    if (state.deviceMac != mac) {
        if (this[SETTING_LOG_ENABLE]) log.debug "Updating Mac from IP: ${mac}"
        state.deviceMac = mac
    }

}

def updated() {
    if (!this[SETTING_DEVICE_IP]) {
        return
    }

    refresh()

    // Default unset values
    if (this[SETTING_REFRESH_UNITS]    == null) updateSetting(SETTING_REFRESH_UNITS,    DEFAULT_REFRESH_UNITS)
    if (this[SETTING_REFRESH_INTERVAL] == null) updateSetting(SETTING_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)
    if (this[SETTING_APP_REFRESH]      == null) updateSetting(SETTING_APP_REFRESH,      DEFAULT_APP_REFRESH)
    if (this[SETTING_APP_UNITS]        == null) updateSetting(SETTING_APP_UNITS,        DEFAULT_APP_UNITS)
    if (this[SETTING_APP_INTERVAL]     == null) updateSetting(SETTING_APP_INTERVAL,     DEFAULT_APP_INTERVAL)
    if (this[SETTING_MEDIA_UNITS]      == null) updateSetting(SETTING_MEDIA_UNITS,      DEFAULT_MEDIA_UNITS)
    if (this[SETTING_MEDIA_INTERVAL]   == null) updateSetting(SETTING_MEDIA_INTERVAL,   DEFAULT_MEDIA_INTERVAL)
    if (this[SETTING_INV_UNITS]        == null) updateSetting(SETTING_INV_UNITS,        DEFAULT_INV_UNITS)
    if (this[SETTING_INV_INTERVAL]     == null) updateSetting(SETTING_INV_INTERVAL,     DEFAULT_INV_INTERVAL)
    if (this[SETTING_AUTO_MANAGE]      == null) updateSetting(SETTING_AUTO_MANAGE,      DEFAULT_AUTO_MANAGE)
    if (this[SETTING_MANAGE_APPS]      == null) updateSetting(SETTING_MANAGE_APPS,      DEFAULT_MANAGE_APPS)
    if (this[SETTING_HDMI_PORTS]       == null) updateSetting(SETTING_HDMI_PORTS,       DEFAULT_HDMI_PORTS)
    if (this[SETTING_INPUT_AV]         == null) updateSetting(SETTING_INPUT_AV,         DEFAULT_INPUT_AV)
    if (this[SETTING_INPUT_TUNER]      == null) updateSetting(SETTING_INPUT_TUNER,      DEFAULT_INPUT_TUNER)
    if (this[SETTING_LOG_ENABLE]       == null) updateSetting(SETTING_LOG_ENABLE,       DEFAULT_LOG_ENABLE)
    if (this[SETTING_DBG_ENABLE]       == null) updateSetting(SETTING_DBG_ENABLE,       DEFAULT_DBG_ENABLE)
    if (this[SETTING_EXPERIMENTAL]     == null) updateSetting(SETTING_EXPERIMENTAL,     DEFAULT_EXPERIMENTAL)
    if (this[SETTING_TIMEOUT]          == null) updateSetting(SETTING_TIMEOUT,          DEFAULT_TIMEOUT)
    if (this[SETTING_USE_POWER_ON]     == null) updateSetting(SETTING_USE_POWER_ON,     state.isTV ?: false)
    if (this[SETTING_USE_POWER_OFF]    == null) updateSetting(SETTING_USE_POWER_OFF,    state.isTV ?: false)
   
    if (!this[SETTING_APP_REFRESH]) {
        [SETTING_APP_UNITS, SETTING_MEDIA_UNITS, SETTING_INV_UNITS].each { key -> updateSetting(key, DEFAULT_REFRESH_UNITS) }
        [SETTING_APP_INTERVAL, SETTING_MEDIA_INTERVAL, SETTING_INV_INTERVAL].each { key -> updateSetting(key, DEFAULT_REFRESH_INTERVAL) }
    }

    // Override out-of-bounds values
    [SETTING_REFRESH_INTERVAL, SETTING_APP_INTERVAL, SETTING_MEDIA_INTERVAL, SETTING_INV_INTERVAL].each { key ->
        Integer value = this[key]
        if (value  > LIMIT_REFRESH_INTERVAL_MAX) updateSetting(key, LIMIT_REFRESH_INTERVAL_MAX)
        if (value  < LIMIT_REFRESH_INTERVAL_MIN) updateSetting(key, LIMIT_REFRESH_INTERVAL_MIN)
    }
    if (this[SETTING_TIMEOUT] < 1) updateSetting(SETTING_TIMEOUT, LIMIT_TIMEOUT_MIN)

    updateIpAddress(this[SETTING_DEVICE_IP])
    getInstalledApps()
    scheduleRefresh()

    if (this[SETTING_CREATE_KEY]) {
        String key=this[SETTING_CREATE_KEY]
        String text=this[SETTING_CREATE_KEY].replaceAll( ~ /([A-Z])/, ' $1').trim()
        updateSetting(SETTING_CREATE_KEY, [value: '', type:'enum'])
        updateChildApp(networkIdForApp(key), text)
    }
    if (this[SETTING_DELETE_KEY]) {
        String netId=this[SETTING_DELETE_KEY]
        updateSetting(SETTING_DELETE_KEY, [value: '', type:'enum'])
        deleteChildAppDevice(netId)
    }

    if (this[SETTING_CREATE_APP]) {
        String netId=this[SETTING_CREATE_APP]
        updateSetting(SETTING_CREATE_APP, [value: '', type:'enum'])
        if (this[SETTING_AUTO_MANAGE]==false || this[SETTING_MANAGE_APPS]==false) {
            Map apps=getInstalledApps()
            String appName=apps[netId]
            if (appName && netId)
                updateChildApp(netId, appName)
        }
    }
    if (this[SETTING_DELETE_APP]) {
        String netId=this[SETTING_DELETE_APP]
        updateSetting(SETTING_DELETE_APP, [value: '', type:'enum'])
        if (this[SETTING_AUTO_MANAGE]==false || this[SETTING_MANAGE_APPS]==false) {
            deleteChildAppDevice(netId)
        }
    }

}

// override the unschedule method
void unscheduleQuery(handlerMethod = null) {
    if (handlerMethod == null) {
        volatileAtomicState.findAll { it.key ==~ /^(query[A-Z].*|pingDevice)/ }.each {
            volatileAtomicState[it.value] = false
            unschedule(it.key)
        }
    } else {
        volatileAtomicState[handlerMethod] = false
        unschedule(handlerMethod)
    }
}

// override the runIn scheduler method
void runQueryIn(Long delayInSeconds, String handlerMethod, Map options = null) {
    volatileAtomicState[handlerMethod] = true
    runIn(delayInSeconds, handlerMethod, options)
}

void scheduleRefresh() {
    unscheduleQuery()
    if (isPowerOff()) {
        // if the TV is powered off, then use Ping to detect power on
        schedulePingDevice()
    } else if (volatileAtomicState.power) {
        // if the TV is has any power state, then the network should be up, 
        // so startup the normal schedulers
        scheduleQueryActiveApp()
        scheduleQueryMediaPlayer()
        scheduleQueryDeviceInfo()
        scheduleQueryInstalledApps()
    } else {
        // if the TV does not have a power state, perform a ping test to determine how to proceed
        pingDevice()
    }
}

void scheduleQueryDeviceInfo() {

    if (volatileAtomicState.queryDeviceInfo) {
        return
    }

    if (volatileAtomicState.configured && !state.isTV) {
        return
    }

    if (isPowerOff()) {
        schedulePingDevice()
        return
    }

    Long delay = (this[SETTING_REFRESH_INTERVAL] ?: 0) * (this[SETTING_REFRESH_UNITS] == REFRESH_UNIT_MINUTES ? 60 : 1)

    if (this[SETTING_DEVICE_IP] && delay > 0) {
        runQueryIn(delay, 'queryDeviceInfo')
    }
}

void scheduleQueryActiveApp() {

    if (volatileAtomicState.queryActiveApp) {
        return
    }

    if (isPowerOff()) {
        schedulePingDevice()
        return
    }

    Long delay = (this[SETTING_APP_INTERVAL] ?: 0) * (this[SETTING_APP_UNITS] == REFRESH_UNIT_MINUTES ? 60 : 1)

    if (this[SETTING_DEVICE_IP] && delay > 0) {
        runQueryIn(delay, 'queryActiveApp')
    }
}

void scheduleQueryMediaPlayer() {

    if (volatileAtomicState.queryMediaPlayer) {
        return
    }

    if (isPowerOff()) {
        schedulePingDevice()
        return
    }

    Long delay = (this[SETTING_MEDIA_INTERVAL] ?: 0) * (this[SETTING_MEDIA_UNITS] == REFRESH_UNIT_MINUTES ? 60 : 1)

    if (viewingHomeScreen()) {
        return
    }

    if (this[SETTING_DEVICE_IP] && delay > 0) {
        runQueryIn(delay, 'queryMediaPlayer')
    }
}

void scheduleQueryInstalledApps() {

    if (volatileAtomicState.queryInstalledApps) {
        return
    }

    if (isPowerOff()) {
        schedulePingDevice()
        return
    }

    Long delay = (this[SETTING_INV_INTERVAL] ?: 0) * (this[SETTING_INV_UNITS] == REFRESH_UNIT_MINUTES ? 60 : 1)

    if (this[SETTING_DEVICE_IP] && delay > 0) {
        runQueryIn(delay, 'queryInstalledApps')
    }
}

void schedulePingDevice() {

    if (!isPowerOff()) {
        if (volatileAtomicState.pingDevice) { unscheduleQuery('pingDevice') }
        return
    }

    if (volatileAtomicState.pingDevice) {
        return
    }

    Long delay1 = (this[SETTING_REFRESH_INTERVAL] ?: 0) * (this[SETTING_REFRESH_UNITS] == REFRESH_UNIT_MINUTES ? 60 : 1)
    Long delay2 = (this[SETTING_APP_INTERVAL]     ?: 0) * (this[SETTING_APP_UNITS]     == REFRESH_UNIT_MINUTES ? 60 : 1)
    Long delay3 = (this[SETTING_MEDIA_INTERVAL]   ?: 0) * (this[SETTING_MEDIA_UNITS]   == REFRESH_UNIT_MINUTES ? 60 : 1)
    Long delay4 = (this[SETTING_INV_INTERVAL]     ?: 0) * (this[SETTING_INV_UNITS]     == REFRESH_UNIT_MINUTES ? 60 : 1)

    // determine the shortest of all the refreshes scheduled, but not more frequent than 10 seconds.
    Long delay = Math.max([delay1, delay2, delay3, delay4].findAll { it > 0 }.min { it }, 20)

    if (this[SETTING_DEVICE_IP] && delay > 0) {
        runQueryIn(delay, 'pingDevice')

        // Remove all other scheduled, because with the TV powered off, they will fail
        if (volatileAtomicState.queryDeviceInfo)    { unscheduleQuery('queryDeviceInfo')    }
        if (volatileAtomicState.queryActiveApp)     { unscheduleQuery('queryActiveApp')     }
        if (volatileAtomicState.queryMediaPlayer)   { unscheduleQuery('queryMediaPlayer')   }
        if (volatileAtomicState.queryInstalledApps) { unscheduleQuery('queryInstalledApps') }
    }
}

String networkIdForApp(String appId) {
    "${device.deviceNetworkId}-${appId}"
}

String appIdForNetworkId(String netId) {
    netId.replaceAll(~/.*\-/,"")
}

String iconPathForApp(String netId) {
    apiPath("query/icon/${appIdForNetworkId(netId)}")
}

void sendEvent(Map properties) {
    
    // Overrides driver.sendEvent() to populate cached values
    if (currentValue(properties.name) != properties.value) {
        device.sendEvent(properties)
        volatileAtomicState[properties.name] = properties.value
    }
}

Object currentValue(String attributeName) {
    def result = volatileAtomicState[attributeName]
    if (result != null) {
        return result
    }
    result = device.currentValue(attributeName)
    if (result != null) {
        volatileAtomicState[attributeName] = result
    }
    return result
}

/*
 * Component Child Methods
 */

 void componentOn(child) {
    String appId = appIdForNetworkId(child.deviceNetworkId)

    if (appId ==~ /^(AV1|Tuner|hdmi\d)$/ ) {
        this."input_$appId"()
    } else if (isValidKey(appId)) {
        // Key presses are actually button events, and do not keep state, this implements a momentary state of on while the event is being sent.
        child.sendEvent(name: 'switch', value: 'on')
        this.keyPress(appId)
        child.sendEvent(name: 'switch', value: 'off')
    } else {
        launchApp(appId)
    }
}

void componentOff(child) {
    if (child.currentValue('switch') == 'off') { return }

    String appId = appIdForNetworkId(child.deviceNetworkId)
    if (appId ==~ /^(AV1|Tuner|hdmi\d|\d+)$/)
        home()
    else if (isValidKey(appId)) {
        child.sendEvent(name: 'switch', value: 'off')
    } else {
        home()
    }

}

void componentRefresh(child){
    if (this[SETTING_LOG_ENABLE]) { log.info "received refresh request from ${child.displayName} - ignored" }
}
    
/*
 * Device Capability Interface Functions
 */

void configure() {
    volatileAtomicState.configured = false
    queryDeviceInfo()
}

void on() {
    
    // Do NOT use isPowerOff() here, it is experimental switch bound, and won't wakeup if experimental switch is off
    if (currentValue('power') == 'Off') { sendWakeUp() }
    if (this[SETTING_USE_POWER_ON]) {
        keyPress('PowerOn')
    } else if (!state.isTV) {
        keyPress('Power')
    } else {
        queryDeviceInfo()
        if (currentValue('switch') == 'off') {
            keyPress('Power')
        }
    }
}

void off() {

    Boolean isOn = currentValue('switch') == 'on'

    if (this[SETTING_USE_POWER_OFF]) {
        keyPress('PowerOff')
    } else if (!state.isTV) {
        keyPress('Power')
    } else {
        queryDeviceInfo()
        if (currentValue('switch') == 'on') {
            keyPress('Power')
        }
    }


}

void play() {
    if (currentValue('transportStatus') != MEDIA_STATE_PLAYING) {
        keyPress('Play')
    }
}

void pause() {
    if (currentValue('transportStatus') != MEDIA_STATE_PAUSED) {
        keyPress('Play')
    }
}

void stop() {
    if (currentValue('transportStatus') != MEDIA_STATE_STOPPED) {
        keyPress('Back')
    }
}

void home() {
    keyPress('Home')
}

void channelUp() {
    keyPress('ChannelUp')
}

void channelDown() {
    keyPress('ChannelDown')
}

void volumeUp() {
    keyPress('VolumeUp')
}

void volumeDown() {
    keyPress('VolumeDown')
}

void setVolume(Integer level) {
    log.trace 'set volume not supported by Roku'
}

void unmute() {
    keyPress('VolumeMute')
}

void mute() {
    keyPress('VolumeMute')
}

void poll() {
    if (this[SETTING_DBG_ENABLE])  { log.trace 'Executing \'poll\'' }
    if (this[SETTING_APP_REFRESH]) { queryActiveApp() }
    refresh()
}

void refresh() {
    if (volatileAtomicState._refresh) { return }
    try {
        volatileAtomicState._refresh = true
        if (this[SETTING_DBG_ENABLE]) { log.trace 'Enter refresh'}
        if (isPowerOff()) {
            pingDevice()
            return
        }        

        if (this[SETTING_DEVICE_IP]) {
            if (this[SETTING_DBG_ENABLE]) { log.trace 'Executing \'refresh\'' }
            queryActiveApp()

            queryDeviceInfo()
            if (!this[SETTING_APP_REFRESH]) { queryActiveApp() }
            if (this[SETTING_AUTO_MANAGE])  { queryInstalledApps() }
        }
    } finally {
        volatileAtomicState._refresh = false
    }
}

/**
 * Custom DTH Command interface functions
 **/

void input_AV1() {
    keyPress('InputAV1')
}

void input_Tuner() {
    keyPress('InputTuner')
}

void input_hdmi1() {
    keyPress('InputHDMI1')
}

void input_hdmi2() {
    keyPress('InputHDMI2')
}

void input_hdmi3() {
    keyPress('InputHDMI3')
}

void input_hdmi4() {
    keyPress('InputHDMI4')
}

void reloadApps() {
    purgeInstalledApps()
    queryInstalledApps()
}

void search(String keywords, String type, Number providerId, String showUnavailable=false, String tmsId=null) {

    Map args = ['provider-id': providerId, 
                'type':        type, 
                'keyword':     keywords, 
                'launch':      'true', 
                'match-any':   'true']

    if (tmsId && tmsId != false) { args['tms_id'] = tmsId }
    if (showUnavailable != null) { args['show-unavailable'] = (showUnavailble == 'true') ? 'true' : 'false' }
    
    queryContent(args)
}

/**
 * Roku API Section
 * The following functions are used to communicate with the Roku RESTful API
 **/

void queryContent(Map args) {
    try {
        httpPost([uri:apiPath('search/browse'),
                  query: args, 
                  timeout: this[SETTING_TIMEOUT]]) { response -> 
            if (!response.isSuccess()) { return }
        }
    } catch (ex) {
        logExceptionWithPowerWarning(ex)

    }
}

void sendWakeUp() {
    if (state.'wake-on-lan' == true) {
        sendHubCommand(new hubitat.device.HubAction (
            "wake on lan ${state.deviceMac}",
            hubitat.device.Protocol.LAN,
            null,
            [:]
        ))
    }
}

void pingDevice() {
    if (volatileAtomicState._pingDevice) { return }
    try {
        volatileAtomicState._pingDevice = true
        if (!this[SETTING_DEVICE_IP]) {
            return
        }


        def start = now()
        def result = hubitat.helper.NetworkUtils.ping(this[SETTING_DEVICE_IP], 1)
        log.trace "Ping completed in: ${now() - start}ms"

        if (result.packetLoss == 100 || result.packetsTransmitted == 0) {
            if (currentValue('power') != 'Off') {
                sendEvent(name: 'switch', value: 'off')
                sendEvent(name: 'power', value: 'Off')
                setCurrentApplication('Roku')
                sendEvent(name: 'mediaInputSource', value: 'Home')
                sendEvent(name: 'transportStatus', value: MEDIA_STATE_STOPPED)
            }
            unscheduleQuery('pingDevice')
            schedulePingDevice()
        } else {
            volatileAtomicState.power = 'Waking'
            unscheduleQuery('pingDevice')
            refresh()
        }
        if (SETTING_DBG_ENABLE) { log.trace "Ping/refresh completed in: ${now() - start}ms"}
    } finally {
        volatileAtomicState._pingDevice = false
    }
}

void queryDeviceInfo() {
    if (volatileAtomicState._queryDeviceInfo) { return }
    try {
        volatileAtomicState._queryDeviceInfo = true
        if (isPowerOff()) { 
            pingDevice()
            return 
        }
        if (this[SETTING_DBG_ENABLE]) { log.trace "queryDeviceInfo: enter " }
        
        // must unschedule the query first, because the scheduler may have one triggering at the same time.
        unscheduleQuery('queryDeviceInfo')
        
        if (!volatileAtomicState.configured || state.isTV) {
            try {
                httpGet([uri:apiPath('query/device-info'), timeout: this[SETTING_TIMEOUT]]) { response -> 
                    if (!response.isSuccess()) { return }

                    def body = response.data
                    parsePowerState(body)
                    if (!volatileAtomicState.configured) {
                        parseState(body)
                    }
                }
            } catch (ex) {
                logExceptionWithPowerWarning("queryDeviceInfo", ex)

            }
        }
        scheduleQueryDeviceInfo()
    } finally {
        if (this[SETTING_DBG_ENABLE]) { log.trace "queryDeviceInfo: exit " }
        volatileAtomicState._queryDeviceInfo = false
    }
}

void queryMediaPlayer() {
    if (volatileAtomicState._queryMediaPlayer) { return }
    try {
        volatileAtomicState._queryMediaPlayer = true
        if (isPowerOff()) { 
            pingDevice()
            return 
        }
        if (this[SETTING_DBG_ENABLE]) { log.trace "queryMediaPlayer: enter " }

        // must unschedule the query first, because the scheduler may have one triggering at the same time.
        unscheduleQuery('queryMediaPlayer')
        try {
            httpGet([uri:apiPath('query/media-player'), timeout: this[SETTING_TIMEOUT]]) { response -> 
                if (!response.isSuccess()) { return }

                def body = response.data
                parseMediaPlayer(body)
            }
        } catch (ex) {
            logExceptionWithPowerWarning("queryMediaPlayer", ex)

        }
        scheduleQueryMediaPlayer()
    } finally {
        if (this[SETTING_DBG_ENABLE]) { log.trace "queryMediaPlayer: exit " }
        volatileAtomicState._queryMediaPlayer = false
    }
}

String translateAppToInput(appName) {
    
    // Translate mediaInputSource to Generic App Names
    
    if (appName == 'Roku')          return 'Home'
    if (appName == 'tvinput.dtv')   return 'InputTuner'
    if (appName == 'tvinput.cvbs')  return 'InputAV1'
    if (appName == 'tvinput.hdmi1') return 'InputHDMI1'
    if (appName == 'tvinput.hdmi2') return 'InputHDMI2'
    if (appName == 'tvinput.hdmi3') return 'InputHDMI3'
    if (appName == 'tvinput.hdmi4') return 'InputHDMI4'

    return appName
}

void setCurrentApplication(currentApp) {
    def previousApp = currentValue('application')
    sendEvent(name: 'application', value: currentApp)
    
    // only perform updated if the application is different from last check
    if (currentApp != previousApp) {

        if (currentApp == 'Roku') {
            unscheduleQuery('queryMediaPlayer')
            scheduleQueryDeviceInfo()
        } else  {
            log.info "turning on, because app is not roku"
            sendEvent(name: 'switch', value: 'on')
            
            if (this[SETTING_APP_REFRESH]) { // if an app is active, TV is on.  Don't check for power state.
                unscheduleQuery('queryDeviceInfo') 
            }
            // Check the Media Player ONLY when an Application is active (Assumes any app other than Roku)
            // TODO: compare current app to installed apps to determine if app is active.
            scheduleQueryMediaPlayer()    

        }
        
        // update the child devices with the current state
        childDevices?.each { child ->
            def appName = "${child.name}"
            def value = (currentApp == appName) ? 'on' : 'off'
            if ("${child.currentValue('switch')}" != "${value}") {
                child.parse([[name: 'switch', value: value, descriptionText: "${child.displayName} was turned ${value}"]])
            }
        }
    }
}

void queryActiveApp() {
    if (volatileAtomicState._queryActiveApp) { return }
    try {
        volatileAtomicState._queryActiveApp = true
        if (isPowerOff()) { 
            pingDevice()
            return 
        }
        if (this[SETTING_DBG_ENABLE]) { log.trace "queryActiveApp: enter " }
        
        // must unschedule the query first, because the scheduler may have one triggering at the same time.
        unscheduleQuery('queryActiveApp')

        try {
            httpGet([uri:apiPath('query/active-app'), timeout: this[SETTING_TIMEOUT]]) { response -> 
                if (!response.isSuccess()) 
                return
                def body = response.data
                def appType = body.app.@type
                def appId = body.app.@id
                def app = body.app.text()
                def mediaApp = appType == 'tvin' ? appId : app
                sendEvent(name: 'mediaInputSource', value: translateAppToInput(mediaApp))
                setCurrentApplication(app)
            }
        } catch (ex) {
            logExceptionWithPowerWarning("queryActiveApp", ex)
        }
        scheduleQueryActiveApp()
    } finally {
        if (this[SETTING_DBG_ENABLE]) { log.trace "queryActiveApp: exit " }
        volatileAtomicState._queryActiveApp = false
    }
}

def queryInstalledApps() {
    if (volatileAtomicState._queryInstalledApps) { return }
    try {
        volatileAtomicState._queryInstalledApps = true
        if (isPowerOff()) { 
            pingDevice()
            return 
        }
        if (this[SETTING_DBG_ENABLE]) { log.trace "queryInstalledApps: enter " }

        if (!this[SETTING_AUTO_MANAGE]) 
            return
        
        // must unschedule the query first, because the scheduler may have one triggering at the same time.
        unscheduleQuery('queryInstalledApps')

        def apps = getInstalledApps()

        if (apps) { 

            def hdmiCount = this[SETTING_HDMI_PORTS] as int
            
            childDevices.each{ child ->
                
                def nodeExists = false
                
                if (hdmiCount > 0 ) (1..hdmiCount).each { i -> 
                    nodeExists = nodeExists || networkIdForApp("hdmi${i}") == child.deviceNetworkId
                }
                
                if (this[SETTING_INPUT_AV])
                    nodeExists = nodeExists || networkIdForApp('AV1') == child.deviceNetworkId
        
                if (this[SETTING_INPUT_TUNER])
                    nodeExists = nodeExists || networkIdForApp('Tuner') == child.deviceNetworkId

                if (!appIdForNetworkId(child.deviceNetworkId) ==~ /^(Tuner|AV1|hdmi\d)$/) {
                    nodeExists = nodeExist || isValidKey(appIdForNetworkId(child.deviceNetworkId))            
                }

                nodeExists = nodeExists || apps.containsKey(child.deviceNetworkId)
                
                if (!nodeExists) {
                    if (appIdForNetworkId(child.deviceNetworkId) ==~ /^(Tuner|AV1|hdmi\d)$/ || this[SETTING_MANAGE_APPS]) {
                        if (this[SETTING_LOG_ENABLE]) log.trace "Deleting child device: ${child.name} (${child.deviceNetworkId})"
                        deleteChildDevice(child.deviceNetworkId)
                    }
                }
            }
            
            if (this[SETTING_INPUT_AV])    updateChildApp(networkIdForApp('AV1'), 'AV')
            if (this[SETTING_INPUT_TUNER]) updateChildApp(networkIdForApp('Tuner'), 'Antenna TV')
            if (hdmiCount > 0) (1..hdmiCount).each{ i -> 
                updateChildApp(networkIdForApp("hdmi${i}"), "HDMI ${i}")
            }

            if (this[SETTING_MANAGE_APPS]) apps.each { netId, appName ->
                updateChildApp(netId, appName)
            }
        
        }
        scheduleQueryInstalledApps()
    } finally {
        if (this[SETTING_DBG_ENABLE]) { log.trace "queryInstalledApps: exit " }
        volatileAtomicState._queryInstalledApps = false
    }
}

private void parseState(body) {
    
    if (body['supports-wake-on-wlan'] == 'true' || !(body['network-type'] == 'wifi')) { 
        setState('wake-on-lan', true) 
    }
    
    if (body['supports-find-remote'] == 'true' && body['find-remote-is-possible'] == 'true') { 
        setState('supports-find-remote', true) 
    }
    
    ['serial-number', 'vendor-name', 'device-id', 'model-name', 'screen-size', 'user-device-name'].each { nodeName ->
        setState(nodeName, "${body[nodeName]}")
    }
    if (body['is-tv'] == 'true' ) {
        setState('isTV', true)
    } else {
        setState('isTV', false)
    }
    volatileAtomicState.configured = true
}

private void parseMediaPlayer(body) {
    switch (body.@state) {
        case 'play':
            if (currentValue('transportStatus') != MEDIA_STATE_PLAYING) {
               sendEvent(name: 'transportStatus', value: MEDIA_STATE_PLAYING)
            }
            break;
        case 'pause':
            if (currentValue('transportStatus') != MEDIA_STATE_PAUSED) {
                sendEvent(name: 'transportStatus', value: MEDIA_STATE_PAUSED)
            }
            break;
        default:
            if (currentValue('transportStatus') != MEDIA_STATE_STOPPED) {
                sendEvent(name: 'transportStatus', value: MEDIA_STATE_STOPPED)
            }
            break;
    }
}

private void setState(String key, def value) {
    if (value && value != state[(key)]) {
        state[(key)] = value
        if (this[SETTING_LOG_ENABLE]) { log.debug "Set ${key} = ${value}" }
    }
}

private Boolean isStateProperty(String key) {
    switch (key) {
        case 'serial-number':
        case 'vendor-name':
        case 'device-id':
        case 'model-name':
        case 'screen-size':
        case 'user-device-name':
        case 'deviceMac':
        case 'supports-find-remote':
        case 'wake-on-lan':
        case 'isTV':
            return true
    }
    return false
}

private void cleanState() {
    this.state.retainAll { key, value -> isStateProperty(key) }
}

private def parsePowerState(body) {
    def powerMode = body.'power-mode'?.text()
    if (powerMode != null) {
        def mode = powerMode
        sendEvent(name: 'power', value: mode)
        switch (mode) {
            case 'PowerOn':
                if (currentValue('switch') != 'on') {
                    sendEvent(name: 'switch', value: 'on')
                    queryActiveApp()
                }
                break;
            case 'PowerOff':
            case 'DisplayOff':
            case 'Headless':
            case 'Ready':
                if (currentValue('switch') != 'off') {
                    sendEvent(name: 'switch', value: 'off')
                    sendEvent(name: 'transportStatus', value: MEDIA_STATE_STOPPED)
                    sendEvent(name: 'mediaInputSource', value: 'Home')
                    setCurrentApplication('Roku')
                    scheduleQueryActiveApp()
                    unscheduleQuery('queryMediaPlayer')
                    unscheduleQuery('queryInstalledApps')
                }
                break;
        }
    }    
}


private def purgeInstalledApps() {    
    if (this[SETTING_MANAGE_APPS]) childDevices.each{ child ->
        deleteChildDevice(child.deviceNetworkId)
    }
}

def getInstalledApps() {
    if (isPowerOff()) { return }

    def apps=[:]
    try {
        httpGet([uri:apiPath('query/apps'),timeout: this[SETTING_TIMEOUT]]) { response ->

            if (!response.isSuccess())
            return

            def body = response.data

            body.app.each{ node ->
                if (node.attributes().type != 'appl') {
                    return
                }

                def netId = networkIdForApp(node.attributes().id)
                def appName = node.text()
                apps[netId] = appName
            }
        }
    } catch (ex) {
        logExceptionWithPowerWarning("getInstalledApps", ex)
        return
    }

    List inputs = ['Home']
    if (state.isTV) {
        def hdmiCount = this[SETTING_HDMI_PORTS] as int
        inputs += ["InputAV1", "InputTuner"]
        inputs += ((1..hdmiCount).collect { "InputHDMI${it}" })
    }
    inputs += apps.values()
    sendEvent(name: 'supportedInputs', value: inputs)

    return apps
}

def setInputSource(source) {

    if (source ==~ /^(Home)|Input(AV1|Tuner|HDMI\d)$/ ) {
        keyPress(source)
    } else {
        def nid = (getInstalledApps().find { it.value == source })?.key
        if (nid) {
            launchApp(appIdForNetworkId(nid))
        } else {
            return
        }
    }
    queryActiveApp()
}

def keyPress(key) {
    if (!isValidKey(key)) {
        log.warn "Invalid key press: ${key}"
        return
    }
    if (this[SETTING_LOG_ENABLE]) log.debug "Executing '${key}'"
    try {
        httpPost([uri:apiPath("keypress/${key}"), timeout: this[SETTING_TIMEOUT]]) { response -> 
            if (response.isSuccess()) poll()
            else log.error "Failed to send key press event for ${key}"
        }
    } catch (ex) {
        logExceptionWithPowerWarning("keyPress", ex)
    }
}

private def isValidKey(key) {
    def keys = [
        'Home',      'Back',       'Select',
        'Up',        'Down',       'Left',        'Right',
        'Play',      'Rev',        'Fwd',         'InstantReplay',
        'Info',      'Search',     'Backspace',   'Enter',
        'VolumeUp',  'VolumeDown', 'VolumeMute',
        'Power',     'PowerOn',    'PowerOff',
        'ChannelUp', 'ChannelDown','InputTuner', 'InputAV1',
        'InputHDMI1','InputHDMI2', 'InputHDMI3', 'InputHDMI4' 
        ]
    if (state.'supports-find-remote' == true)
        keys << 'FindRemote'
    
    return keys.contains(key)
}

def launchApp(appId) {
    if (this[SETTING_DBG_ENABLE]) log.debug "Executing 'launchApp ${appId}'"
    if (appId ==~ /^\d+$/ ) {
        try {
            httpPost([uri:apiPath("launch/${appId}"), timeout: this[SETTING_TIMEOUT]]) { response ->
                if (response.isSuccess()) {
                    def netId = networkIdForApp(appId)
                    def child = getChildDevice(netId)
                    log.info "Launch app: ${appId} with Network Id: ${netId}"
                    child.sendEvent(name: 'switch', value: 'on')
                    queryActiveApp()
                } else {
                    log.error "Failed to launch appId: ${data.appId}"
                }
            }
        } catch (ex) {
            logExceptionWithPowerWarning("launchApp", ex)
        }
    } else if (appId ==~ /^(AV1|Tuner|hdmi\d)$/ ) {
        this."input_$appId"()
    } else {
        this.keyPress(appId)
    }
}

/**
 * Child Device Maintenance Section
 * These functions are used to manage the child devices bound to this device
 */

private void updateChildApp(String netId, String appName) {
    def child = getChildDevice(netId)
    if(child) { //If child exists, do not create it
        return
    }

    if (appName) {
        createChildAppDevice(netId, appName)
    } else {
        if (this[SETTING_LOG_ENABLE]) log.error "Cannot create child: (${netId}) due to missing 'appName'"
    }
}

void createChildAppDevice(String netId, String appName) {
    try {
        def label = deviceLabel()
        def child = addChildDevice('hubitat', 'Generic Component Switch', "${netId}",
            [label: "${label}-${appName}", 
             isComponent: parent ? true : false, name: "${appName}"])
        child.updateSetting('txtEnable', false)
        if (appIdForNetworkId(netId) ==~ /^\d+$/ ) {
            child.updateDataValue('iconPath', iconPathForApp(netId))
        }
        if (this[SETTING_LOG_ENABLE]) log.debug "Created child device: ${appName} (${netId})"
    } catch(IllegalArgumentException e) {
        if (getChildDevice(netId)) {
            if (this[SETTING_LOG_ENABLE]) log.warn "Attempted to create duplicate child device for ${appName} (${netId}); Skipped"
        } else {
            if (this[SETTING_LOG_ENABLE]) log.error "Failed to create child device with exception: ${e}"
        }
    } catch(Exception e) {
        if (this[SETTING_LOG_ENABLE]) log.error "Failed to create child device with exception: ${e}"
    }
}

void deleteChildAppDevice(String netId) {
    try {
        def appName = getChildDevice(netId)?.name ?: ""
        deleteChildDevice(netId)
        if (this[SETTING_LOG_ENABLE]) log.debug "Removed child device: ${appName} (${netId})"
    } catch(Exception e) {
        if (this[SETTING_LOG_ENABLE]) log.error "Failed to remove child device with exception: ${e}"
    }
}

private def deviceLabel() {
    if (device.label == null)
        return device.name
    return device.label
}

private void logExceptionWithPowerWarning(String method, ex) {
    def result = hubitat.helper.NetworkUtils.ping(this[SETTING_DEVICE_IP], 1)
    if (result.packetLoss == 100 || result.packetsTransmitted == 0) {
        if (this[SETTING_LOG_ENABLE]) {
            if ( currentValue('switch') == 'on') {
                log.info "Lost connection to Roku device; Powering off."
            } else if ( currentValue('power') != 'Off') {
                log.info "Lost connection to Roku device; Switching to ping mode."
            }
        }
        sendEvent(name: 'switch', value: 'off')
        sendEvent(name: 'power', value: 'Off')
        setCurrentApplication('Roku')
        sendEvent(name: 'mediaInputSource', value: 'Home')
        sendEvent(name: 'transportStatus', value: MEDIA_STATE_STOPPED)
        schedulePingDevice()
    } else {
        if (this[SETTING_LOG_ENABLE]) {
            log.error "Exception thrown in call to ${method}: ${ex}"
            log.warn 'The device appears to be powered off.  Please make sure Fast-Start is enabled on your Roku.'
        }
    }
}

private String apiPath(String queryPath) {
    String suffix = queryPath ? "/${queryPath}" : ''
    "http://${this[SETTING_DEVICE_IP]}:8060${suffix}"
}

private Boolean isPowerOff() {
    currentValue('power') == 'Off' && this[SETTING_EXPERIMENTAL]
}

private Boolean viewingHomeScreen() {
    currentValue('application') == 'Roku'
}