# Advanced Hue Bridge Integration for Hubitat

This application and associated drivers is to be considered *experimental code*, and not to be used for a production automation system at this time.

The functionality is very limited at this time.


## Features

 - [Advanced Hue Bridge Integration App](app/hue-bridge-integration.grooby)
   - This is the main component.  Like the native application, this is the app that you will use to link the Hue bridge to your HE system.  All drivers in this project are 100% dependent upon this app.
 - [Advanced Hue Bridge Child Device](device/advanced-hue-bridge.groovy)
   - The main app will install an instance of this device after the app successfully pairs the hue bridge.
 - [Advance Hue Group Child Device](device/advanced-hue-group.groovy)
   - All imported Hue Groups will use this device to manage the group.  This device depends on the parent app to operate. The initial version of this device will support all color light control capabilities, and per-group refresh.  

As this project is in early development stages, the only functionality presently implemented is the ability to turn on/off groups, and set the group's dimmer level.  These features will be expanded upon daily, so if you choose to play with the project, watch the project page for updates.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.  
