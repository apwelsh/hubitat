# Advanced Hue Bridge Integration for Hubitat

The Advanced Hue Bridge Integration for Hubitat brings support the Hue scenes, groups, and lights.  It minimizes hubitat complexity by leveraging the built-in Hubitat component drivers whereever possible.  Additionally, this system will pick the best fit generic component driver for each light, to ensure the devices are correctly represented in dashboards.

This application and associated drivers are now ready for general use.  A beta branch will be created for those that wish to use the newest features as they are being added and tested.

Hue Play systems, like the Hue Sync are not yet supported, as I do not own one to test with.

The functionality is mostly complete at this time.  I believe this system already provides more features and better performance than the built-in Hue integration, but that is an opion, not a measured fact.

This integration fully supports hue push notifaction events, so that sensors can be leveraged in near-realtime, and lights are updated in near-realtime with no need to require a scheduled refresh of the Hue system.


## Features

 - [Advanced Hue Bridge Integration App](app/hue-bridge-integration.groovy)
   - This is the main component.  Like the native application, this is the app that you will use to link the Hue bridge to your HE system.  All drivers in this project are 100% dependent upon this app.
   - Used to add Lights, Groups, and Scenes
 - [Advanced Hue Bridge Device](device/advanced-hue-bridge.groovy)
   - The main app will install an instance of this device after the app successfully pairs the hue bridge.
   - Used to schedule the hub refresh event.
   - Supports `Switch` capability to turn on/off all Hue lights.
   - Can be configured to automatically turn on if Any light is on, or if All ligts are on.
 - [Advanced Hue Group Device](device/advanced-hue-group.groovy)
   - All imported Hue Groups will use this device to manage the group.  This device depends on the parent app to operate. The initial version of this device supports all color light control capabilities, and the ability to activate any hue scene by scene ID or scene name (as identified in the hue bridge).
   - Can define a default scene to activate when the group is turned on.
 - Hue Lights Device `Generic Hubitat Device`
   - All imported Hue Ligts will use one of the matching Hubitat component drivers.  Hue lights are installed as individual lights, now as child devices or another device.
   - Use `Generic Component Dimmer` for dimmable lights
   - Use `Generic Component CT` for color temperature adjustable lights
   - Use `Generic Component RGBW` for extended color lights
   - Use `Generic Component RGB` for all other lights
 - Hue Scenes Device `Generic Hubitat Device`
   - One of the key features of this integration, is the ability to use and manage Hue scenes.   This device is the a switch devices used to active a hue scene.
   - Use `Generic Component Switch` for switching on hue scenes.
   - Can be configured as a momentary (trigger) switch, or as a toggle switch.
     - Trigger (momentary) mode.  When turned on, scene will be activated and will automatically turn off the switch after 400 milliseconds.
     - Switch mode.  Turning on a scene will result in the scene switch staying on, until *any* attribute of the group or bublbs within the group are changed, or until another scene is activated.  Turning off the active scene will turn off the parent Hue group.
 - [Advanced Hue Motion Sensor Device](device/advanced-hue-motion-sensor.groovy)
   - Support for Motion Sensor
   - Support for Battery level
   - Support for Refresh
 - [Advanced Hue Light Sensor Device](device/advanced-hue-light-sensor.groovy)
   - Support for IlluminanceMeasurement
   - Support for Battery level
   - Support for Refresh
 - [Advanced Hue Temperature Sensor Device](device/advanced-hue-temperature-sensor.groovy)
   - Support for TemperatureMeasurement
   - Support for Battery level
   - Support for Refresh
 
   

## Vision / Future Enhancements
The goal of this project is to more tightly integrate the hue system with Hubitat.  
- Hue Integrated Group Manager -- Hubitat has support for groups, but adding hue lights to groups results in slow automation, as the bridge controls the light individually.  With this project, I will enable the ability to create and manage custom groups on Hue that can include Hue and HE devices.  This will enable making changes to the hue group with a single API call to hue.
- Hue Integrated Scene Manager -- Hubitat has support for scenes, but adding hue lights to scenes results in slow and out of sync transitions.  With this project, I will enable the ability to create and manage scenes on Hue that can include Hue and HE devices.  This will make it possible to syncronize the hue devices with the HE devices as they transition.  To what extent I will be able to sync with HE and achieve the zigbee optiminizations of non-hue is not yet known.


## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.  
