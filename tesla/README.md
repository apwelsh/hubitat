# Tesla Connect

This is my implementation of the **Tesla Connect App and Tesla Device** adapted and enhanced for Hubitat and to meet my needs.

NOTICE: This driver is a very early implementation  

## Getting Started

To use this software, you must download two files:
 - [tesla.groovy](device/tesla.groovy)
 - [tesla-connect.groovy](device/tesla-connect.groovy)

## Installation
Sign into your Hubitat device, and add the devices and apps included in this project.  To do so, from the menu select the **"Drivers Code"** menu option.

![](images/HubitatMenuDriversCode.png)

Next, click the **"(+) New Driver"** button

![](images/NewDriverButton.png)

### Tesla Device
Select the import button, and put in the URL to the [tesla.groovy](device/tesla.groovy), Click the import button, and the new driver is ready.  
Click **Save**. 


### Tesla Connect App
Next, navigate to **"Apps Code"**
and repeat the process for the [tesla-connect.groovy](/app/tesla-connect.groovy) app.

## Configuration

The configuration is quite simple, and tries to be as automatic as possible.  Once everything is installed, go to Apps, and select **"Add User Apps"** then select the **Tesla Connect** app.

Once the app is installed, open the Tesla Connect app (this normally happens automatically after install) and enter you Tesla Connect credentials.  Once signed in, you will receive a list of all your connected vehivles.  Select the vehicle(s) to install, and click next.  This will install the Tesla device for each selected vehicle.  Now you are ready to configure and manage each vehicle.

### Prerequisite
For smart home automation to work reliably, it is highly recommended that each installed vehicle be configured to join your wifi network, and to always be assigned the same IP Address (check your router documentation of how to configure DHCP reservations).  This is not required, but it will help to ensure the best results for consistency.

**Not yet implemented** 
Presence based on distance from home
Custom attribute for geofenced home range

### Status Updates
January 20, 2020
- Initial implementation with minor changes from the original SmartThings implementation to adapt this solution to my needs

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE.md](LICENSE.md) file for details.  Portions of this code are licensed from Trent Foley (https://github.com/trentfoley)

## Acknowledgments
This software would not be possible without the efforts and free sharing of information provided by the original author of the [SmartThings Tesla-Connect App](https://github.com/trentfoley/SmartThingsPublic/tree/master/smartapps/trentfoley/tesla-connect.src) and [SmartThings Tesa Device](https://github.com/trentfoley/SmartThingsPublic/tree/master/devicetypes/trentfoley/tesla.src) created by [Trent Foley](https://github.com/trentfoley).
