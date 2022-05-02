/**
 * Advanced Hue RunLessWires Sensor
 * Version 1.0.0
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device handler for the Advance Hue Bridge Integration App. Although this can work in poll mode,
 * it is highly recommended to use the event stream based push notifications.
 * Author: Curtis Edge (@pocketgeek)
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


@Field static final Boolean DEFAULT_LOG_ENABLE       = true
@Field static final Boolean DEFAULT_DBG_ENABLE       = false

@Field static final String SETTING_LOG_ENABLE        = 'logEnable'
@Field static final String SETTING_DBG_ENABLE        = 'debug'

@Field static final Boolean BUTTON_1_HOLDABLE        = true
@Field static final Boolean BUTTON_2_HOLDABLE        = true
@Field static final Boolean BUTTON_3_HOLDABLE        = true
@Field static final Boolean BUTTON_4_HOLDABLE        = true

@Field static final String SETTING_BUTTON_1_HOLDABLE = 'Button 1 long press enable'
@Field static final String SETTING_BUTTON_2_HOLDABLE = 'Button 2 long press enable'
@Field static final String SETTING_BUTTON_3_HOLDABLE = 'Button 3 long press enable'
@Field static final String SETTING_BUTTON_4_HOLDABLE = 'Button 4 long press enable'

def heldButtonPressDelay = 2000

metadata {
    definition (
        name:      'AdvancedHueRunLessWiresSensor',
        namespace: 'apwelsh',
        author:    'Armand Welsh',
        importUrl: '') {

        capability 'PushableButton'
        capability 'HoldableButton'
        capability 'Refresh'
        capability 'Initialize'
 
        attribute  'status', 'string'  // expect enabled/disabled
        attribute  'health', 'string'  // reachable/unreachable
   }
}

preferences {

    input name: SETTING_LOG_ENABLE,
          type: 'bool',
          defaultValue: DEFAULT_LOG_ENABLE,
          title: 'Enable informational logging'

    input name: SETTING_DBG_ENABLE,
          type: 'bool',
          defaultValue: DEFAULT_DBG_ENABLE,
          title: 'Enable debug logging'

    input name: SETTING_BUTTON_1_HOLDABLE,
          type: 'bool',
          defaultValue: BUTTON_1_HOLDABLE,
          title: SETTING_BUTTON_1_HOLDABLE

    input name: SETTING_BUTTON_2_HOLDABLE,
          type: 'bool',
          defaultValue: BUTTON_2_HOLDABLE,
          title: SETTING_BUTTON_2_HOLDABLE

    input name: SETTING_BUTTON_3_HOLDABLE,
          type: 'bool',
          defaultValue: BUTTON_3_HOLDABLE,
          title: SETTING_BUTTON_3_HOLDABLE

    input name: SETTING_BUTTON_4_HOLDABLE,
          type: 'bool',
          defaultValue: BUTTON_4_HOLDABLE,
          title: SETTING_BUTTON_4_HOLDABLE

}

void updateSetting(String name, Object value) {
    device.updateSetting(name, value)
    this[name] = value
}

/**
 * Hubitat DTH Lifecycle Functions
 **/
def installed() {
    updated()
    initialize()
    refresh()

    mapButtons()

}

def initialize() {
    String id = parent.deviceIdNode(device.deviceNetworkId)
    Long buttons = parent.state.sensors[id]?.capabilities?.inputs?.size()?:0
    parent.sendChildEvent(this, [name: 'numberOfButtons', value: buttons])
}

def updated() {
    if (this[SETTING_LOG_ENABLE] == null) { updateSetting(SETTING_LOG_ENABLE,       DEFAULT_LOG_ENABLE) }
    if (this[SETTING_DBG_ENABLE] == null) { updateSetting(SETTING_DBG_ENABLE,       DEFAULT_DBG_ENABLE) }
    if (this[SETTING_BUTTON_1_HOLDABLE] == null) { updateSetting(SETTING_BUTTON_1_HOLDABLE,       BUTTON_1_HOLDABLE) }
    if (this[SETTING_BUTTON_2_HOLDABLE] == null) { updateSetting(SETTING_BUTTON_2_HOLDABLE,       BUTTON_2_HOLDABLE) }
    if (this[SETTING_BUTTON_3_HOLDABLE] == null) { updateSetting(SETTING_BUTTON_3_HOLDABLE,       BUTTON_3_HOLDABLE) }
    if (this[SETTING_BUTTON_4_HOLDABLE] == null) { updateSetting(SETTING_BUTTON_4_HOLDABLE,       BUTTON_4_HOLDABLE) }
    if (this[SETTING_LOG_ENABLE]) { log.debug 'Preferences updated' }
}

void mapButtons() {
    String id = parent.deviceIdNode(device.deviceNetworkId)

    state.buttonMap = parent.enumerateResourcesV2().findAll { resource ->
        resource.type == 'button' && resource.id_v1 == "/sensors/${id}"
    }.collectEntries { button -> 
        [(button.id): button.metadata.control_id]
    }
}

/*
 * Device Capability Interface Functions
 */


void refresh() {
    if (this[SETTING_DBG_ENABLE]) { log.debug "Sensor (${this}) refreshing" }
    parent.getDeviceState(this)
}


void setHueProperty(Map args) {
    if (args.last_event && args.id) {
        Number btn = state.buttonMap?.(args.id) ?: 0
        switch(args.last_event) {
            // For whatever reason the API for 'Friends of HUE' Switches changed somewhere around December 2021.
            // Home Assistent issue on it:  https://github.com/home-assistant/core/issues/61671
            // Now the only messages that are sent for these are initial_press and short_release.
            // And better yet, you always get two 'short_release' messages.  One about 1 second after the 'initial_press' message, even if you
            // are still holding the button, and one after you actually let go of the button.  This made it necessary to just ignore the first 'short_release' message after 
            // the 'initial_press' message.  In my teseting the initial short_release message usually comes about 1 second later, necessitating
            // the hold timer being 2 seconds.
            case 'initial_press':
            state.initial_pressTime = new Date().getTime() // Get time of initial_press
            state.short_releaseNumber = 0 // Reset short_release message counter
            switch(btn) {  // If holdable is false for any button return button press event without waiting for hold counter
                case 1:
                if (this[SETTING_BUTTON_1_HOLDABLE] == false) {
                    push(btn)
                }
                break;
                case 2:
                if (this[SETTING_BUTTON_2_HOLDABLE] == false) {
                    push(btn)
                }
                break; 
                case 3:
                if (this[SETTING_BUTTON_3_HOLDABLE] == false) {
                    push(btn)
                }
                break;
                case 4:
                if (this[SETTING_BUTTON_4_HOLDABLE] == false) {
                    push(btn)
                }
                break;
            }
            case 'short_release': 
            state.short_releaseNumber++ // Increment short_release counter.  
            if (state.short_releaseNumber != 2) {
                break;
            }
            state.buttonHoldTime = new Date().getTime() - state.initial_pressTime // Get the difference between initial_press and *actual* short_release time.
            switch(btn) {
                case 1:
                if (this[SETTING_BUTTON_1_HOLDABLE] == true) {
                    if (state.buttonHoldTime > 2000) {
                        hold(btn)
                    }
                    else {
                        push(btn)
                    }
                    break;
                }
                break;
                case 2:
                if (this[SETTING_BUTTON_2_HOLDABLE] == true) {
                    if (state.buttonHoldTime > 2000) {
                        hold(btn)
                    }
                    else {
                        push(btn)
                    }
                    break;
                }
                break;
                case 3:
                if (this[SETTING_BUTTON_3_HOLDABLE] == true) {
                    if (state.buttonHoldTime > 2000) {
                        hold(btn)
                    }
                    else {
                        push(btn)
                    }
                    break;
                }
                break;
                case 4:
                if (this[SETTING_BUTTON_4_HOLDABLE] == true) {
                    if (state.buttonHoldTime > 2000) {
                        hold(btn)
                    }
                    else {
                        push(btn)
                    }
                    break;
                }
                break;
            }
        }
    }
}

void push(Number buttonNumber) {
    state.buttonPressTime = 0
    sendEvent([name: 'pushed',   value: buttonNumber, isStateChange:true])
}

void hold(Number buttonNumber) {
    sendEvent([name: 'held',     value: buttonNumber, isStateChange: true])
}

void release(Number buttonNumber) {
    sendEvent([name: 'released', value: buttonNumber, isStateChange: true])
}