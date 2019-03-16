# hubitat
This is my first fully implemented device handler.  It was adapted from the one created by Eric Boehns (https://github.com/ericboehs/smartthings-roku-tv) along with API documentation published by Roku (https://sdkdocs.roku.com/display/sdkdoc/External+Control+API)

With this version, there is a child device used to create launcher buttons for the installed Apps.  The each Roku Application will get its own Roku App button.  Pressing the button will launch the app on the target Roku device.

The parent device Roku-TV is published as a TV, and AudioVolume capable device in hopes that future integrations will see the Roku as a TV device, for actions such as, "Alexa, turn on the tv" even if the TV is named something else.

I will update this site over time to improve this document.  For now, installation is rather simple:

1) install Roku App device code
2) install Roku TV device code
3) Add virtual device "Roku TV"
4) Once created, fill-in the IP Address, and save the settings.
5) refresh the browser window, and the MAC address should be filled in for you now.

Note:  The MAC address is used to institute a wake-on-lan event to wakeup Roku devices that entered into a deep sleep.
