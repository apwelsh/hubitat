/**
 * iopool Connect
 * Version 1.0.0
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is an integration app for Hubitat designed to locate, and install any/all attached iopool managed devices.
 * The iopool application works with the iopool EcO and the pHin pool monitor.  iopool support the pHin mointor for 
 * users that bought the pHin pool monitor, and were stuck with a monitor and not support.  I do not have a pHin to test 
 * with.  If you have a pHin, the data should be available via the iopool public API for use by this app.  Send me a note 
 * if you would like to use a pHin device.  Huibitat does not have a bluetooth receiver.  All communications in this application
 * use the iopool public API in the cloud.  To use this app, you will need the iopool app, and possibly the gateway too.
 * To use this application, follow the instruction on GitHub to obtain an API key, and enter it into this application.
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

definition(
    name:        'iopool Connect',
    namespace:   'apwelsh',
    author:      'Armand Welsh (apwelsh)',
    description: 'iopool Connect Integration',
    category:    'Convenience',
    //importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/iopool/app/iopool-connect.groovy',
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: ''
)

preferences {
    page(name: 'mainPage')
    page(name: 'addSelectedDevices')
}

/*
 * Life Cycle Functions
 */

def installed() {
    initialize()
}

def uninstalled() {
    unschedule()
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updated() {
    unschedule()
    initialize()
}

def initialize() {
    scheduleRefresh()
}

/*
 * Application Screens
 */

def mainPage() {

    if (!state && !apiKey) {
        return dynamicPage(name: 'mainPage', title: 'iopool Connect', uninstall: true, install: true) {
            section('Login') {
                input name: 'apiKey', type: 'string', title: 'Public API Key', description: 'Once the iopool mobile app is configure, request an API key from the company, and enter it here.'
            }
            section {
                paragraph 'Hit Done to to install the iopool Connect Integration.\nRe-open to setup.'
            }
        }
    }

    Map pools = findPools()
    Map availablePools = pools.collectEntries { it }  // clone the list to create a copy
    List installedPools = childDevices
    installedPools.each { child -> availablePools.remove(child.device.deviceNetworkId) }

    if (selectedPools) {
        selectedPools?.each { dni ->
            log.debug "${dni} selected from [${pools}]"
            if (pools[dni]) {
                log.debug "installing child device"
                installedPools << addChildDevice('apwelsh', 'EcO Water Quality Sensor', "${dni}", [name: pools[dni]])
                availablePools.remove(dni)
            }
        }
        selectedPools = selectedPools?.collectMany { dni -> return availablePools[dni] ? it : [] } ?: []
        app.updateSetting('selectedPools', selectedPools )
    }

    return dynamicPage(name: 'mainPage', title: '', uninstall: (!installedPools), install: true) {

        section(getFormat('title', 'iopool Connect')) {
            paragraph getFormat('line')
        }

        if (availablePools) {
            section('Available Monitors') {
                input 'selectedPools', 'enum', title: '<B>Select Pool/Spa monitors to install</B>', required: false, multiple: true, options: availablePools, submitOnChange: true
            }
        }

        if (installedPools) {
            section('Installed Devices') {
                installedPools.sort({ a, b -> a.label <=> b.label }).each { child ->
                    def desc = child.label != child.name ? child.name : ''
                    Boolean ena = pools[child.device.deviceNetworkId]
                    paragraph """<button type="button" class="btn btn-default btn-lg btn-block hrefElem ${ena ? 'btn-state-complete' : '' } mdl-button--raised mdl-shadow--2dp" style="text-align:left;width:100%" onclick="window.location.href='/device/edit/${child.device.id}'">
                                    <span style="text-align:left;white-space:pre-wrap">${child.label ?: child.name} (${child.name})</span>
                                </button>"""

                }
            }
        }

        section('Options') {
            input name: 'refreshInterval', type: 'number', defaultValue: 5, title: 'Refresh interval (minutes)'
            input name: 'logEnable', type: 'bool', defaultValue: true, title: 'Enable logging'
        }

        section {
            paragraph getFormat('line')
            paragraph "<div style='color:#1A77C9;text-align:center'>iopool Connect<br><a href='https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J' target='_blank'><img src='https://img.shields.io/badge/donate-PayPal-blue.svg?logo=paypal&style=plastic' border='0' alt='Donate'></a><br><br>Please consider donating. This app took a lot of work to make.<br>If you find it valuable, I'd certainly appreciate it!</div>"
        }
    }
}

private String getFormat(String type, String myText='') {
    if (type == 'line')  return '<hr style="background-color:#1A77C9; height: 1px; border: 0;">'
    if (type == 'title') return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

private String deviceLabel(device) {
    return device?.label ?: device?.name
}

private Map findPools() {
    return httpGet([uri:'https://api.iopool.com/v1/pools', timeout: 20, headers: ['x-api-key': apiKey]]) { response ->
        if (!response.success) {
            log.error "(${response.status}) ${response.statusLine}"
            return [:]
        }
        List data = response.data
        refreshChildren(data)
        return data?.collectEntries { sensor ->
            if (!sensor.id) { return }
            [(sensor.id): sensor.title]
        }
    }
}

private void scheduleRefresh() {
    int interval = Math.max(Math.min(refreshInterval?:5,1440),1) * 60
    runIn(interval, 'queryPools', [overwrite: true, misfire: 'ignore'])
}

private void queryPools() {
    try {
        httpGet([uri:'https://api.iopool.com/v1/pools', timeout: 20, headers: ['x-api-key': apiKey]]) { response ->
            if (!resonse.success) {
                log.error "(${response.status}) ${response.statusLine}"
                return
            }
            refreshChildren(resonse.data)
        }
    } finally  {
        scheduleRefresh()
    }
}

private void refreshChildren(List data) {
    try {
        data?.each { poolData ->
            String nid = poolData.id
            def child = getChildDevice(nid)
            child?.parseMessage(poolData)
        }
    } finally {
        scheduleRefresh()
    }
}
