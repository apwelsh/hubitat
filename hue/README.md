# Advanced Hue Bridge Integration for Hubitat

This application and associated drivers is to be considered *experimental code*, and not to be used for a production automation system at this time.

The functionality is very limited at this time.


## Features

 - [Advanced Hue Bridge Integration App](app/hue-bridge-integration.groovy)
   - This is the main component.  Like the native application, this is the app that you will use to link the Hue bridge to your HE system.  All drivers in this project are 100% dependent upon this app.
 - [Advanced Hue Bridge Child Device](device/advanced-hue-bridge.groovy)
   - The main app will install an instance of this device after the app successfully pairs the hue bridge.
 - [Advance Hue Group Child Device](device/advanced-hue-group.groovy)
   - All imported Hue Groups will use this device to manage the group.  This device depends on the parent app to operate. The initial version of this device supports all color light control capabilities, and the ability to activate any hue scene by scene ID or scene name (as identified in the hue bridge).
- [Advance Hue Scene Child Device](device/advanced-hue-scene.groovy)
   - All imported Hue Scenes will use this device to manage the scene.  This device depends on the parent app and the group device to operate. The initial version of this device supports the ability to activate a scene using the on button.  This is device does not maintain state, as Hue does not maintain state.

**Not yet implemented** 
Hue Lights (RGB, CT and Dimmable) are not yet available.
Refresh of device state is not yet implemented.

### Status Updates
October 22, 2019
- The Hue Scenes can now be used to turn on / activate any registered scene.  Scenes are created as child devices of the hue hue group for which they belong.
- Added the ability to activate any scene on the Hue bridge from the group device, by providing the scene name or scene ID (for speed, the scene ID is recommended)
- Added ablity to control hue, saturation, colormap, and colortemp to the hue groups.
- Added a scene manager to the Hue Hub Integration application.

Noverber 8, 2019
- Added auto-refresh with custom interval for Hue Groups, and for the Hub Bridge
- Added switch capability to hue hub device.  The hub switch can be used to turn on or off all lights on hue hub.
- Added colormode to hue group to provide full support of current and commanded color.
- Implemented some minor performance optimizations.

## Vision / Future Enhancements
The goal of this project is to more tightly integrate the hue system with Hubitat.  
- Hue Integrated Group Manager -- Hubitat has support for groups, but adding hue lights to groups results in slow automation, as the bridge controls the light individually.  With this project, I will enable the ability to create and manage custom groups on Hue that can include Hue and HE devices.  This will enable making changes to the hue group with a single API call to hue.
- Hue Integrated Scene Manager -- Hubitat has support for scenes, but adding hue lights to scenes results in slow and out of sync transitions.  With this project, I will enable the ability to create and manage scenes on Hue that can include Hue and HE devices.  This will make it possible to syncronize the hue devices with the HE devices as they transition.  To what extent I will be able to sync with HE and achieve the zigbee optiminizations of non-hue is not yet known.
- Add transition time to the HUE API calls when provided in the call to setLevel of any bulb or group.
- Add individual control of Hue Bulbs
- Add a way to detect when an API refresh is completed.  The standard Hue integration only supports refresh of all devices, which is slightly more costly than to refresh the state of a single device.  And since hue does not yet support push notifications on device state change, HE must rely on polling.  This poses inconsistency challenges when used with capture state, as the state could have changed between the last refresh, and the capture state event.  Rather than implement overly aggressive refresh, it is the intent to add a mechanism to trigger when a refresh has completed.  In this way, the Rule Engine could issue a per-device refresh, then wait for event, and finally capture state -- no more race conditions for capture state.


## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.  
