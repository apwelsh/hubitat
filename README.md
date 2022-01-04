
# Hubitat Drivers [![Donate](https://img.shields.io/badge/donate-PayPal-blue.svg?logo=paypal&style=plastic)](https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J)


Welcome to my Hubitat Github repo.  All drivers and applications in this repo are free for use.  If you like them, please share, and consider a modest donation.  If you have recommendations for updates, or observed bugs, please open an issue and communiate it.  If you have a bug-fix, or enhancement you think I might like, or others might like, please submit a pull request to merge your changes.


## Current Projects

 - [Roku TV and Media Players](roku/README.md)
   - An application and device driver for managing Roku TVs and Media Players.  This solution can be used to create rules that respond to Roku events, such as:
     - [X] Turn off theater lights, and dim the entry light with Plex loads
     - [X] Turn on living room lights when show is paused, and restore prior state with show is playing
     - [X] Change the TV LED light strips to Red when Netflix is, Green for Hulu, Blue for Amazon Prime, etc...
     - [X] Turn on the living room lights for 5 minutes when the TV is powered off, and the Mode is Night
   - Or use the Basic Rules to automate your TV, such as:
     - [X] If mode transitions to day, then hallway motion is detected, turn on the kitchen TV, and play a random episode of Dora the Explorer
     - [X] If Goodnight Scene is actived, then hallway motion is detected, then bedroom motion is detected, turn off living room TV and turn on Hulu on bedroom TV
 - [Timer Device](Timer.md)
   - This is a simple count-down timer tile.  Use this device when you want a timer that shows you the amount of time remaining and that is SharpTools.io HERO tile friendly.  The timer supports start/stop/pause/cancel events.  Cancel is basically the same as stop, except the sessionStatus will report canceled, rather than stopped.  The device is rather straight forward.  To use the timer in applications, the timer attribute sessionStatus will report the timer status as text, and the switch state will be set to on while the timer is running and off when it is stopped.  To detect that the timer ended, the timer implemented the PushableButton and will send a button 1 pushed event upon timer completion.
 - [Advanced Hue Hub Integration](hue/README.md)
   - This project's goal is to bridge the gap that currently exists between Hubitat and Hue Bridge.  The hubitat native support should be used whenever possible -- however, there are some things in the native hue integration that create an unpleasent user experience.  Hue Scenes are not supported in hubitat.  This *experimental*, and **in development** project's goal is to bring Hue scenes to hubitat as child devices of the hue groups.
   - This project has been expanded to also support real-time updates for hue device changes, and adds support for hue sensors, and dimmer controllers.  
- [Tesla Connect](tesla/README.md) **Project is dead**
   - This project's goal is to convert the SmartThings Tesla-Connect solution for Hubitat, and enhance it to provide features not available with the current SmartThings solution.  This *experimental*, and **in development** project's goal is to bring Tesla Presence based logic to Hubit and provide an alternative to Tesla HomeLink.  _** Update **_ Tesla Motors has changed how their service works, and this app no longer works.


## License

Portions of this repository are licensed under the MIT License - see the [MIT LICENSE](MIT-LICENSE.md)

Portions of this repository are licensed under the Apache 2.0 License - see the [Apache LICENSE](APACHE-LICENSE.md)
