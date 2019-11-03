# Advanced Hue Bridge Integration for Hubitat

This application and associated drivers is to be considered *experimental code*, and not to be used for a production automation system at this time.

The functionality is very limited at this time.


## Features

 - [Advanced Hue Bridge Integration App](app/hue-bridge-integration.grooby)
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
November 22, 2019
- The Hue Scenes can now be used to turn on / activate any registered scene.  Scenes are created as child devices of the hue hue group for which they belong.
- Added the ability to activate any scene on the Hue bridge from the group device, by providing the scene name or scene ID (for speed, the scene ID is recommended)
- Added ablity to control hue, saturation, colormap, and colortemp to the hue groups.
- Added a scene manager to the Hue Hub Integration application.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.  
