[![Donate](https://img.shields.io/badge/donate-PayPal-green.svg)](https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J)

# Roku Drivers

This is my implementation of the **Roku TV + App Control** Device handlers for Hubitat.

NOTICE: This driver has been been through several updates.  

WARNING:  Upgrading to this new version may break some of your automations, as some functionality has changed.  **Upgrade may require reconfiguration**

## Getting Started

To use this software, you must download just this one file:
 - [roku-tv.groovy](device/roku-tv.groovy) - The primary driver for controlling, and querying Roku devices.
   - Convert any application into a simple on/off child device
   - Convert any remote control button press into a momentary on child device
   - Configure simple or custom polling configurations for device state (on, off, play, pause, current application selected on tv, etc)
 
 _** Note ** that there used to be two files, but I have converted the device driver to use the built-in `Generic Component Switch` driver._
 
 _Optional (highly recommended)_
 - [roku-connect.groovy](app/roku-connect.groovy) - A streamlined management tool to make managing your Roku devices easier.
   - Discovers active Roku devices using the SSDP device discovery protocol, no more managing IP addresses manually.
   - Rename your Roku devices for easier navigation in Hubitat
   - Add, Remove, and Rename installed Roku applications
   - Add, Remove, and Rename Roku TV input devices

## Installation
I recommend using Hubitat Package Manager to install Roku Connect and the Roku TV device driver.

The following steps are used to install the necessary code manually:

Sign into your Hubitat device, and add the Roku TV device handler.  To do so, from the menu select the **"Drivers Code"** menu option.

![](../images/HubitatMenuDriversCode.png)

Next, click the **"(+) New Driver"** button

![](../images/NewDriverButton.png)

### Roku TV Driver
Select the import button, and put in the URL to the [roku-tv.groovy](device/roku-tv.groovy) driver. Click the import button, and the new driver is ready.  
Click **Save**. 
![save button](../images/NewDriverExample.png)

### Roku Connect App
from the menu, select the **"App Code"** menu option.

Next, click the **"(+) New App"** button

Select the import button, and put in the URL to the [roku-connect.groovy](app/roku-connect.groovy) app.  Click the import button, and the new app is ready.
Click **Save**.


## Configuration

The configuration is quite simple, and tries to be as automatic as possible.  Once the device hander and app are installed, you will need to add the new Roku TV devices that you want to automate, and configure the IP Address.

### Prerequisite
For smart home automation to work reliably, all devices on your network that will be access from the Hubitat hub should be configured with a static address.  This also true for the Roku TV devices.  I cannot provide details on how to do this, as each network is unique, and different routers have different solutions.  If your devices receive their network address via the router using DHCP (Dynamica Host Configuration Protocol) -- this is most typical -- then you may need to configure your router to *reserve* the IP address assigned to the Roku device.  This way, every time the Roku device is powered on, it will always have the same IP address, and the Hubitat Elevation hub, and this device handler, will know how to find it.

### Adding the Roku Connect App
If you choose to use the Roku Connect App, you will not need to add the Roku TV devices individually.  Instead, you can skip the **Adding the Roku Device** section.

To manage your Roku device from Roku Connect, navigate to **"Apps"** in the Hubitat menu, and select **"(+) Add User App"**.
In the pop-up, select Roku Connect, the click the **done** button.

From the list of installed applicaiton, locate **"Roku Connect"** and run it.

From here you can **"Discover New Devices"**, and manage instsalled device.  

If you cannot setup DHCP reservations, and static IP addresses are not an option, be sure to turn on **Auto detect IP changes of Roku devices**.  This feature will periodically ping the SSDP connected devices, even when the app is not in discovery mode, so locate and detect IP address changes for intalled Roku devices.

I believe the Roku Connect application is rather simple, needs little explanation.  This is my preferred solution for managing your Roku devices.


### Adding the Roku Device
If you would rather not use the Roku Connect application, you can manually install the Roku TV devices. 
Any device installed using this manual method cannot be managed by the Roku Connect application, until the installed device is removed from hubitat first.

To add your Roku device, navigate the **Devices** in the Hubitat menu, and select **Add Virtual Device**

![](../images/AddVirtualDeviceButton.png)

Give your device a Friendly, but unique device name, and device network Id.
I like to prefix my Device Network Id with a discriptive prefix to help ensure uniqueness, and to isolate my virtual devices by type. Be sure to select the new **Roky TV** devicea as the type, and then Save the device information.

![](../images/RokuTVDeviceInfo.png)

Enter the IP Address of your Roku TV device in the Prefrences section, the MAC address is not required, it will attempt to fill-in when the Roku device is queried.

![](../images/RokuTVPreferences.png)

Click the **Save Prefrences** button.  

### Finalizing the configuration

The next step is a little quirky, because the Roku TV device handler is going to auto-configure this device as much as possible.  If you do not see the device **Current States**, then I recommend issuing a browser refresh. This can be achieved by pressing the `F5` key on Windows, and some Linux systems, or `CMD+R` on MacOS.

## Using the new device

![](../images/RokuTVCurrentState.png)

Once the device looks something like the above image, your TV is configured and ready to go.

At this point, you should see the installed child devices for the apps, which should look something like this:

![](../images/InstalledAppsList.png)

Note:  The MAC address is used to institute a wake-on-lan event to wakeup Roku devices that entered into a deep sleep.

### How to use

All the button on the Roku TV device implement the Hubitat standards for control of the associated commands.  The Roku API does not appear to provide a direct mechanism to set some of the parameters available.  

**Features** 

| Command | Description |
| - | - |
| Volumne Up | Increments the volume by 1 step |
| Volume Down | Decrements the volume by 1 step |
| Set Volume | _not supported by Roku API_ at this time |
| Channel Up | Change channel up |
| Channel Down | Change channel down |
| Mute | Toggle the audio Mute state on/off |
| Unmute | _not support by Roku API_ same behavior as Mute |
| On | Turn the TV on |
| Off | Turn the TV off |
| Poll | Issues a Refresh |
| Refresh | Forced refresh of TV state |
| Reload Apps | Deletes and reloads all child devices |

_At present, the `Set Volume` button is ignored_.  
The Roku API does not report mute state, so mute and unmute is just a toggle, both calling the mute button event.

Although Hubitat has what it needs to see this as a TV type device, the current Alexa skill app does not support the TV type, so it will appear as a standard switch.

Note: The TV Device does not keep an active link with the Roku, and there is not facility within the Roku API for this.  As such, this handler will only issue a poll/refresh once every five (5) minutes.  And the current application is polled once every minute, as this is a much smaller request.

### Roku App (Child Device)

The Roku App child device handler is just a child switch.  The switch allows the buttons to appear in Alexa as devices.  The status of the switch will automatically change to on or off based on the currently running application.  If the switch is commanded on or off, then the switch status will change to turning-on or turning-off until the actual state is confirmed.

**Not yet implemented** 
Roke Integration App to find and manage roku device installatios
Roke Integration Child App to manage each roku devices features.
Implement a refresh stack to limit TV  refresh status to one-at-a-time.  Parallel refreshes seem to severely slow down the Hub.

### Status Updates
January 21, 2020
- Remove roku-tv child app, and replace with built-in Generic Component Switch device
- Add new attribute "refresh" to track the current refresh status.  If refresh is pending, some refresh operations are suppressed.
January 20, 2020
- Relocated device drivers to now location for better management of code
- Created a temporary old device with new URL.  This new device should act as a bridge to migrate existing installations to using the new devices.

## Support the Author
Please consider donating. This app took a lot of work to make.
Any donations received will be used to help fund new projects

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J)                  

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.  Portions of this code are licensed from Eric Boehs [LICENSE.md](https://raw.githubusercontent.com/ericboehs/smartthings-roku-tv/master/LICENSE)

## Acknowledgments
This software would not be possible without the efforts and free sharing of information provided by the original author of the [TCL Smartthings Roku TV](https://github.com/ericboehs/smartthings-roku-tv) created by [Eric Boehs](https://github.com/ericboehs), upon which I was inspired learn about the ECP protocol, and Hubitat development.

Additional thanks go to Roku for freely publishing the [External Control API](https://developer.roku.com/docs/developer-program/debugging/external-control-api.md) documentation.
