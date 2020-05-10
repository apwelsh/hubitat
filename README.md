# Hubitat Drivers

Welcome to my Hubitat Github repo.  All drivers and applications in this repo are free for use.  If you like them, please share.  If you have recommendations for updates, please open an issue, and report it.  If you have a bug-fix, or enhancement you think I might like, or others might like, please submit a pull request to merge your changes.


## Current Projects

 - [Roku TV and Media Players](roku/README.md)
   - A set of drivers for controlling a Roku TV or Media Player.
 - [Timer Device](Timer.md)
   - This is a simple count-down timer tile.  Use this device when you want a timer that shows you the amount of time remaining and that is SharpTools.io HERO tile friendly.  The timer supports start/stop/pause/cancel events.  Cancel is basically the same as stop, except the sessionStatus will report canceled, rather than stopped.  The device is rather straight forward.  To use the timer in applications, the timer attribute sessionStatus will report the timer status as text, and the switch state will be set to on while the timer is running and off when it is stopped.  To detect that the timer ended, the timer implemented the PushableButton and will send a button 1 pushed event upon timer completion.
 - [Advanced Hue Hub Integration](hue/README.md)
   - This project's goal is to bridge the gap that currently exists between Hubitat and Hue Bridge.  The hubitat native support should be used whenever possible -- however, there are some things in the native hue integration that create an unpleasent user experience.  Hue Scenes are not supported in hubitat.  This *experimental*, and **in development** project's goal is to bring Hue scenes to hubitat as child devices of the hue groups.  
- [Tesla Connect](tesla/README.md)
   - This project's goal is to convert the SmartThings Tesla-Connect solution for Hubitat, and enhance it to provide features not available with the current SmartThings solution.  This *experimental*, and **in development** project's goal is to bring Tesla Presence based logic to Hubit and provide an alternative to Tesla HomeLink.


## License

Portions of this repository are licensed under the MIT License - see the [MIT LICENSE](MIT-LICENSE.md)

Portions of this repository are licensed under the Apache 2.0 License - see the [Apache LICENSE](APACHE-LICENSE.md)
