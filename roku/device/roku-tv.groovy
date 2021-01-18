/**
 * Roku TV
 * Version 2.5.1
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
import java.net.URLEncoder

@Field static final String  REFRESH_UNIT_SECONDS = 'Seconds'
@Field static final String  REFRESH_UNIT_MINUTES = 'Minutes'

@Field static final String  DEFAULT_REFRESH_UNITS    = 'Minutes'
@Field static final Integer DEFAULT_REFRESH_INTERVAL = 5
@Field static final Boolean DEFAULT_APP_REFRESH      = false
@Field static final String  DEFAULT_APP_UNITS        = 'Seconds'
@Field static final Integer DEFAULT_APP_INTERVAL     = 1
@Field static final Boolean DEFAULT_AUTO_MANAGE      = true
@Field static final Boolean DEFAULT_MANAGE_APPS      = true
@Field static final Integer DEFAULT_HDMI_PORTS       = 3
@Field static final Boolean DEFAULT_INPUT_AV         = false
@Field static final Boolean DEFAULT_INPUT_TUNER      = false
@Field static final Boolean DEFAULT_LOG_ENABLE       = false
@Field static final Integer DEFAULT_TIMEOUT          = 10

@Field static final Integer LIMIT_REFRESH_INTERVAL_MAX = 59
@Field static final Integer LIMIT_REFRESH_INTERVAL_MAN = 1
@Field static final Integer LIMIT_APP_INTERVAL_MAX     = 59
@Field static final Integer LIMIT_APP_INTERVAL_MIN     = 1
@Field static final Integer LIMIT_TIMEOUT_MIN          = 1

@Field static final String  SETTING_REFRESH_UNITS   = 'refreshUnits'
@Field static final String SETTING_REFRESH_INTERVAL = 'refreshInterval'
@Field static final String SETTING_APP_REFRESH      = 'appRefresh'
@Field static final String  SETTING_APP_UNITS       = 'appUnits'
@Field static final String SETTING_APP_INTERVAL     = 'appInterval'
@Field static final String SETTING_AUTO_MANAGE      = 'autoManage'
@Field static final String SETTING_MANAGE_APPS      = 'manageApps'
@Field static final String SETTING_HDMI_PORTS       = 'hdmiPorts'
@Field static final String SETTING_INPUT_AV         = 'inputAV'
@Field static final String SETTING_INPUT_TUNER      = 'inputTuner'
@Field static final String SETTING_LOG_ENABLE       = 'logEnable'
@Field static final String SETTING_TIMEOUT          = 'timeout'

metadata {
    definition (
        name:      'Roku TV',
        namespace: 'apwelsh',
        author:    'Armand Welsh',
        importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/roku/device/roku-tv.groovy') {
        
        capability 'TV'
        capability 'AudioVolume'
        capability 'Switch'
        capability 'Polling'
        capability 'Refresh'

        attribute 'refresh', 'string'

        command 'home'
        command 'keyPress', [[name:'Key Press Action', type: 'ENUM', constraints: [
                'Home',      'Back',       'FindRemote',  'Select',        'Up',        'Down',       'Left',        'Right',
                'Play',      'Rev',        'Fwd',         'InstantReplay', 'Info',      'Search',     'Backspace',   'Enter',
                'VolumeUp',  'VolumeDown', 'VolumeMute',  'Power',         'PowerOn',   'PowerOff',
                'ChannelUp', 'ChannelDown', 'InputTuner', 'InputAV1',      'InputHDMI1', 'InputHDMI2', 'InputHDMI3', 'InputHDMI4'] ] ]
        
        command 'reloadApps'

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
        if (deviceIp && (!autoManage || !manageApps)) {
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
        input name: 'deviceIp',        type: 'text',   title: 'Device IP', required: true
    }
    input name: 'usePowerOn',      type: 'bool', title: 'Use Power On or Power Toggle for On', required: true, defaultValue: state.isTV ?: false, description: 'Recommend Power On, however, if Power On does not work, disable to use Power toggle'
    input name: 'usePowerOff',     type: 'bool', title: 'Use Power Off or Power Toggle for Off', required: true, defaultValue: state.isTV ?: false, description: 'Recommend Power Off, however, if Power Off does not work, disable to use Power toggle'

    input name: 'timeout',         type: 'number', title: 'Communcation timeout', required: true, defaultValue: DEFAULT_TIMEOUT, range: 1..60, description: 'The maximum number of seconds to wait for a roku instruction to complete.  Defaults to 10 seconds.  For fast networks, set to a lower time for better hub performance when your device is offline.'
    if (deviceIp) {
        input name: 'refreshUnits',    type: 'enum',   title: 'Refresh interval measured in Minutes, or Seconds', options:[REFRESH_UNIT_MINUTES,REFRESH_UNIT_SECONDS], defaultValue: DEFAULT_REFRESH_UNITS, required: true
        input name: 'refreshInterval', type: 'number', title: "Refresh the status at least every n ${refreshUnits}.  0 disables auto-refresh, which is not recommended.", range: 0..60, defaultValue: DEFAULT_REFRESH_INTERVAL, required: true
        input name: 'appRefresh',      type: 'bool',   title: 'Refresh current application status seperate from TV status.', defaultValue: DEFAULT_APP_REFRESH, required: true
        if (appRefresh) {
            input name: 'appUnits',    type: 'enum',   title: 'Refresh interval measured in Minutes, or Seconds', options:[REFRESH_UNIT_MINUTES,REFRESH_UNIT_SECONDS], defaultValue: DEFAULT_APP_UNITS, required: true
            input name: 'appInterval', type: 'number', title: 'Refresh the current application at least every n seconds.', range: 1..59, defaultValue: DEFAULT_APP_INTERVAL, required: true        
        }
        input name: 'autoManage',      type: 'bool',   title: 'Enable automatic management of child devices', defaultValue: DEFAULT_AUTO_MANAGE, required: true
        if (autoManage?:true == true) {
            input name: 'manageApps',      type: 'bool',   title: 'Auto-manage Roku Applications', defaultValue: DEFAULT_MANAGE_APPS, required: true
            input name: 'hdmiPorts',       type: 'enum',   title: 'Number of HDMI inputs', options:['0','1','2','3','4'], defaultValue: DEFAULT_HDMI_PORTS, required: true
            input name: 'inputAV',         type: 'bool',   title: 'Enable AV Input', defaultValue: DEFAULT_INPUT_AV, required: true
            input name: 'inputTuner',      type: 'bool',   title: 'Enable Tuner Input', defaultValue: DEFAULT_INPUT_TUNER, required: true
        }
        input name: 'createChildKey',  type: 'enum',   title: 'Select a key to add a child switch for, and save changes to add the child button for the selected key', options:keys, required: false
        input name: 'deleteChildKey',  type: 'enum',   title: 'Remove Roku Remote Control Key', options: installedKeys, required: false
        if ((autoManage?:true == false || manageApps?:true == false) && !parent) {
            input name: 'createChildApp',     type: 'enum',   title: 'Add Roku App', options: apps, required: false
            input name: 'deleteChildApp',     type: 'enum',   title: 'Remove Roku App', options: installed, required: false

        }
    }
    input name: 'logEnable',       type: 'bool',   title: 'Enable debug logging', defaultValue: DEFAULT_LOG_ENABLE, required: true
}

/**
 * Hubitat DTH Lifecycle Functions
 **/
def installed() {
    sendEvent(name: 'refresh', value: 'idle')
    updated()
}


def updated() {

    // Default unset values
    if (settings.refreshUnits    == null) device.updateSetting(SETTING_REFRESH_UNITS,    DEFAULT_REFRESH_UNITS)
    if (settings.refreshInterval == null) device.updateSetting(SETTING_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)
    if (settings.appRefresh      == null) device.updateSetting(SETTING_APP_REFRESH,      DEFAULT_APP_REFRESH)
    if (settings.appUnits        == null) device.updateSetting(SETTING_APP_UNITS,        DEFAULT_APP_UNITS)
    if (settings.appInterval     == null) device.updateSetting(SETTING_APP_INTERVAL,     DEFAULT_APP_INTERVAL)
    if (settings.autoManage      == null) device.updateSetting(SETTING_AUTO_MANAGE,      DEFAULT_AUTO_MANAGE)
    if (settings.manageApps      == null) device.updateSetting(SETTING_MANAGE_APPS,      DEFAULT_MANAGE_APPS)
    if (settings.hdmiPorts       == null) device.updateSetting(SETTING_HDMI_PORTS,       DEFAULT_HDMI_PORTS)
    if (settings.inputAV         == null) device.updateSetting(SETTING_INPUT_AV,         DEFAULT_INPUT_AV)
    if (settings.inputTuner      == null) device.updateSetting(SETTING_INPUT_TUNER,      DEFAULT_INPUT_TUNER)
    if (settings.logEnable       == null) device.updateSetting(SETTING_LOG_ENABLE,       DEFAULT_LOG_ENABLE)
    if (settings.timeout         == null) device.updateSetting(SETTING_TIMEOUT,          DEFAULT_TIMEOUT)

    // Override out-of-bounds values
    if (settings.refreshInterval  > 59  ) device.updateSetting(SETTING_REFRESH_INTERVAL, LIMIT_REFRESH_INTERVAL_MAX)
    if (settings.refreshInterval  < 1   ) device.updateSetting(SETTING_REFRESH_INTERVAL, LIMIT_REFRESH_INTERVAL_MAN)
    if (settings.appInterval      > 59  ) device.updateSetting(SETTING_APP_INTERVAL,     LIMIT_APP_INTERVAL_MAX)
    if (settings.appInterval      < 1   ) device.updateSetting(SETTING_APP_INTERVAL,     LIMIT_APP_INTERVAL_MIN)
    if (settings.timeout          < 1   ) device.updateSetting(SETTING_TIMEOUT,          LIMIT_TIMEOUT_MIN)

    if (logEnable) log.debug 'Preferences updated'
    if (deviceIp) {
        String mac = getMACFromIP(deviceIp)
        if (state.deviceMac != mac) {
            if (logEnable) log.debug "Updating Mac from IP: ${mac}"
            state.deviceMac = mac
        }
    }
    unschedule()
    if (deviceIp && refreshInterval > 0) {
        if (refreshUnits == REFRESH_UNIT_SECONDS) {            
            schedule("0/${refreshInterval} * * * * ?", refresh)
        } else {
            schedule("${new Date().format('s')} 0/${refreshInterval} * * * ?", refresh)
        }
    }
    if (appRefresh && appInterval > 0) {
        if (appUnits == REFRESH_UNIT_SECONDS) {            
            schedule("0/${appInterval} * * * * ?", refresh)
        } else {
            schedule("${new Date().format('s')} 0/${appInterval} * * * ?", refresh)
        }
    }
    if (createChildKey) {
        String key=createChildKey
        String text=createChildKey.replaceAll( ~ /([A-Z])/, ' $1').trim()
        device.updateSetting('createChildKey', [value: '', type:'enum'])
        updateChildApp(networkIdForApp(key), text)
    }
    if (deleteChildKey) {
        String netId=deleteChildKey
        device.updateSetting('deleteChildKey', [value: '', type:'enum'])
        deleteChildAppDevice(netId)
    }

    if (createChildApp) {
        String netId=createChildApp
        device.updateSetting('createChildApp', [value: '', type:'enum'])
        if (autoManage==false || manageApps==false) {
            Map apps=getInstalledApps()
            String appName=apps[netId]
            if (appName && netId)
                updateChildApp(netId, appName)
        }
    }
    if (deleteChildApp) {
        String netId=deleteChildApp
        device.updateSetting('deleteChildApp', [value: '', type:'enum'])
        if (autoManage==false || manageApps==false) {
            deleteChildAppDevice(netId)
        }
    }

}

String networkIdForApp(String appId) {
    return "${device.deviceNetworkId}-${appId}"    
}

String appIdForNetworkId(String netId) {
    return netId.replaceAll(~/.*\-/,"")
}

String iconPathForApp(String netId) {
    return "http://${deviceIp}:8060/query/icon/${appIdForNetworkId(netId)}"    
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
    if (logEnable) { log.info "received refresh request from ${child.displayName} - ignored" }
}
    
/*
 * Device Capability Interface Functions
 */

void on() {
    
    Boolean isOff = device.currentValue('switch') == 'off'
    
    sendEvent(name: 'switch', value: 'on')
    if (isOff) { sendWakeUp() }
    if (usePowerOn) {
        keyPress('PowerOn')
    } else {
        queryDeviceState()
        if (device.currentValue('switch') == 'off') {
            keyPress('Power')
        }
    }
}

void off() {

    Boolean isOn = device.currentValue('switch') == 'on'

    sendEvent(name: 'switch', value: 'off')
    if (usePowerOff) {
        keyPress('PowerOff')
    } else {
        queryDeviceState()
        if (device.currentValue('switch') == 'on') {
            keyPress('Power')
        }
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
    if (logEnable)  { log.trace 'Executing \'poll\'' }
    if (appRefresh) { queryCurrentApp }
    refresh()
}

void refresh() {
    if (logEnable) { log.trace 'Executing \'refresh\'' }
    queryDeviceState()
    if (!appRefresh) { queryCurrentApp() }
    if (autoManage)  { queryInstalledApps() }
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
    sendEvent(name: 'refresh', value: 'reload-apps')
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
        httpPost([uri:"http://${deviceIp}:8060/search/browse",
                  query: args, 
                  timeout: timeout]) { response -> 
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

void queryDeviceState() {
    sendEvent(name: 'refresh', value: 'device-info')
    try {
        httpGet([uri:"http://${deviceIp}:8060/query/device-info", timeout: timeout]) { response -> 
            if (!response.isSuccess()) { return }

            def body = response.data
            parsePowerState(body)
            parseState(body)
        }
    } catch (ex) {
        logExceptionWithPowerWarning(ex)

    }
    sendEvent(name: 'refresh', value: 'idle')
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
}

private void setState(String key, def value) {
    if (value && value != state[(key)]) {
        state[(key)] = value
        if (logEnabled) { log.debug "Set ${key} = ${value}" }
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
            return true
    }
    return false
}

private void cleanState() {
    this.state.retainAll { key, value -> isStateProperty(key) }
    // def keys = this.state.keySet()
    // for (def key : keys) {
    //     if (!isStateProperty(key)) {
    //         if (logEnable) log.debug("removing ${key}")
    //         this.state.remove(key)
    //     }
    // }
}

private def parsePowerState(body) {
    def powerMode = body.'power-mode'?.text()
    if (powerMode != null) {
        def mode = powerMode
        switch (mode) {
            case 'PowerOn':
                if (this.state!='on') {
                    sendEvent(name: 'switch', value: 'on')
                    if (appRefresh && appInterval > 0) {
                        queryCurrentApp()
                        schedule("0/${appInterval} * * * * ?", queryCurrentApp)
                    }
                } 
                break;
            case 'PowerOff':
            case 'DisplayOff':
            case 'Headless':
            case 'Ready':
                if (this.state!='off') {
                    sendEvent(name: 'switch', value: 'off')
                    unschedule(queryCurrentApp)
                }
                break;
        }
    }    
}


def queryCurrentApp() {
    try {
        httpGet([uri:"http://${deviceIp}:8060/query/active-app", timeout: timeout]) { response -> 
            if (!response.isSuccess()) 
            return

            def body = response.data
            def app = body.app.text()
            if (app != null) {
                def currentApp = "${app.replaceAll( ~ /\h/,' ')}"  // Convert non-ascii spaces to spaces.
                sendEvent(name: 'application', value: currentApp, isStateChange: true)

                childDevices.each { child ->
                    def appName = "${child.name}"
                    def value = (currentApp.equals(appName)) ? 'on' : 'off'
                    if ("${child.currentValue('switch')}" != "${value}") {
                        child.parse([[name: 'switch', value: value, descriptionText: "${child.displayName} was turned ${value}"]])
                    }
                }
            }
        }
    } catch (ex) {
        logExceptionWithPowerWarning(ex)
    }
}

private def purgeInstalledApps() {    
    if (manageApps) childDevices.each{ child ->
        deleteChildDevice(child.deviceNetworkId)
    }
}

def getInstalledApps() {
    def apps=[:]
    try {
        httpGet([uri:"http://${deviceIp}:8060/query/apps",timeout: timeout]) { response ->

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
        logExceptionWithPowerWarning(ex)
    }
    return apps
}

def queryInstalledApps() {
    if (!autoManage) 
        return
    if ("${device.currentValue('refresh')}" == 'idle') {
        sendEvent(name: 'refresh', value: 'find-apps')
    }
    
    def apps = getInstalledApps()

    if (apps) { 

        def hdmiCount = hdmiPorts as int
        
        childDevices.each{ child ->
            
            def nodeExists = false
            
            if (hdmiCount > 0 ) (1..hdmiCount).each { i -> 
                nodeExists = nodeExists || networkIdForApp("hdmi${i}") == child.deviceNetworkId
            }
            
            if (inputAV)
                nodeExists = nodeExists || networkIdForApp('AV1') == child.deviceNetworkId
    
            if (inputTuner)
                nodeExists = nodeExists || networkIdForApp('Tuner') == child.deviceNetworkId

            if (!appIdForNetworkId(child.deviceNetworkId) ==~ /^(Tuner|AV1|hdmi\d)$/) {
                nodeExists = nodeExist || isValidKey(appIdForNetworkId(child.deviceNetworkId))            
            }

            nodeExists = nodeExists || apps.containsKey(child.deviceNetworkId)
            
            if (!nodeExists) {
                if (appIdForNetworkId(child.deviceNetworkId) ==~ /^(Tuner|AV1|hdmi\d)$/ || manageApps) {
                    if (logEnable) log.trace "Deleting child device: ${child.name} (${child.deviceNetworkId})"
                    deleteChildDevice(child.deviceNetworkId)
                }
            }
        }
        
        if (inputAV)    updateChildApp(networkIdForApp('AV1'), 'AV')
        if (inputTuner) updateChildApp(networkIdForApp('Tuner'), 'Antenna TV')
        if (hdmiCount > 0) (1..hdmiCount).each{ i -> 
            updateChildApp(networkIdForApp("hdmi${i}"), "HDMI ${i}")
        }

        if (manageApps) apps.each { netId, appName ->
            updateChildApp(netId, appName)
        }
    
        sendEvent(name: 'refresh', value: 'idle')

    }

}

def keyPress(key) {
    if (!isValidKey(key)) {
        log.warn "Invalid key press: ${key}"
        return
    }
    if (logEnable) log.debug "Executing '${key}'"
    try {
        httpPost([uri:"http://${deviceIp}:8060/keypress/${key}", timeout: timeout]) { response -> 
            if (response.isSuccess()) poll()
            else log.error "Failed to send key press event for ${key}"
        }
    } catch (ex) {
        logExceptionWithPowerWarning(ex)
    }
}

private def isValidKey(key) {
    def keys = [
        'Home',      'Back',       'Select',
        'Up',        'Down',       'Left',        'Right',
        'Play',      'Rev',        'Fwd',         'InstantReplay',
        'Info',      'Search',     'Backspace',   'Enter',
        'VolumeUp',  'VolumeDown', 'VolumeMute',
        'Power',     'PoweOn',     'PowerOff',
        'ChannelUp', 'ChannelDown','InputTuner', 'InputAV1',
        'InputHDMI1','InputHDMI2', 'InputHDMI3', 'InputHDMI4' 
        ]
    if (state.'supports-find-remote' == true)
        keys << 'FindRemote'
    
    return keys.contains(key)
}

def launchApp(appId) {
    if (logEnable) log.debug "Executing 'launchApp ${appId}'"
    if (appId ==~ /^\d+$/ ) {
        try {
            httpPost([uri:"http://${deviceIp}:8060/launch/${appId}", timeout: timeout]) { response ->
                if (response.isSuccess()) {
                    def netId = networkIdForApp(appId)
                    def child = getChildDevice(netId)
                    log.info "Launch app: ${appId} with Network Id: ${netId}"
                    child.sendEvent(name: 'switch', value: 'on')
                    queryCurrentApp()
                } else {
                    log.error "Failed to launch appId: ${data.appId}"
                }
            }
        } catch (ex) {
            logExceptionWithPowerWarning(ex)
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
        if (logEnable) log.error "Cannot create child: (${netId}) due to missing 'appName'"
    }
}

void createChildAppDevice(String netId, String appName) {
    sendEvent(name: 'refresh', value: 'busy')
    try {
        def label = deviceLabel()
        def child = addChildDevice('hubitat', 'Generic Component Switch', "${netId}",
            [label: "${label}-${appName}", 
             isComponent: parent ? true : false, name: "${appName}"])
        child.updateSetting('txtEnable', false)
        if (appIdForNetworkId(netId) ==~ /^\d+$/ ) {
            child.updateDataValue('iconPath', iconPathForApp(netId))
        }
        if (logEnable) log.debug "Created child device: ${appName} (${netId})"
    } catch(IllegalArgumentException e) {
        if (getChildDevice(netId)) {
            if (logEnabled) log.warn "Attempted to create duplicate child device for ${appName} (${netId}); Skipped"
        } else {
            if (logEnable) log.error "Failed to create child device with exception: ${e}"
        }
    } catch(Exception e) {
        if (logEnable) log.error "Failed to create child device with exception: ${e}"
    }
    sendEvent(name: 'refresh', value: 'idle')
}

void deleteChildAppDevice(String netId) {
    sendEvent(name: 'refresh', value: 'busy')
    try {
        def appName = getChildDevice(netId)?.name ?: ""
        deleteChildDevice(netId)
        if (logEnable) log.debug "Removed child device: ${appName} (${netId})"
    } catch(Exception e) {
        if (logEnable) log.error "Failed to remove child device with exception: ${e}"
    }
    sendEvent(name: 'refresh', value: 'idle')
}

private def deviceLabel() {
    if (device.label == null)
        return device.name
    return device.label
}

private void logExceptionWithPowerWarning(ex) {
    if (logEnable) {
        log.error ex
        log.warn 'The device appears to be powered off.  Please make sure Fast-Start is enabled on your Roku.'
    }
}