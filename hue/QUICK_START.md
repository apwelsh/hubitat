# Advanced Hue Bridge Integration - Quick Start Guide

## Overview

This quick start guide will help you get the Advanced Hue Bridge Integration package up and running in your Hubitat environment in just a few minutes.

## Prerequisites

Before you begin, ensure you have:

- **Hubitat Elevation Hub** (version 2.2.9 or higher)
- **Philips Hue Bridge** (any generation) connected to your network
- **Network connectivity** between Hubitat and Hue Bridge
- **Hubitat Package Manager** (recommended) or manual installation capability

## Installation

### Option 1: Package Manager (Recommended)

1. **Open Hubitat Package Manager**
   - Navigate to your Hubitat web interface
   - Go to **Apps** → **Hubitat Package Manager**

2. **Install the Package**
   - Search for "Advanced Hue Bridge Integration"
   - Click **Install**
   - Follow the installation prompts

3. **Verify Installation**
   - Check that the parent application appears in your Apps list
   - Verify all device handlers are installed

### Option 2: Manual Installation

1. **Install Parent Application**
   - Go to **Apps** → **Code**
   - Click **+ New App**
   - Copy and paste the contents of `hue-bridge-integration.groovy`
   - Click **Save**

2. **Install Device Handlers**
   - Go to **Devices** → **Code**
   - Install each device handler:
     - `advanced-hue-bridge.groovy`
     - `advanced-hue-group.groovy`
     - `advanced-hue-motion-sensor.groovy`
     - `advanced-hue-light-sensor.groovy`
     - `advanced-hue-temperature-sensor.groovy`
     - `advanced-hue-dimmer-sensor.groovy`
     - `advanced-hue-tap-sensor.groovy`
     - `advanced-hue-runlesswires-sensor.groovy`

3. **Install Utility Library**
   - Go to **Apps** → **Code**
   - Click **+ New Library**
   - Copy and paste the contents of `HueFunctions.groovy`
   - Click **Save**

## Initial Setup

### Step 1: Launch the Application

1. **Open the Parent App**
   - Go to **Apps** → **Advanced Hue Bridge Integration**
   - Click on the app to open it

2. **Access Main Dashboard**
   - You'll see the main dashboard with navigation options
   - Click **"Device Discovery"** to begin

### Step 2: Discover Your Hue Bridge

1. **Start Discovery**
   - On the Device Discovery page, click **"Start Discovery"**
   - The app will scan your network for Hue Bridges

2. **Press the Link Button**
   - When prompted, press the **link button** on your Hue Bridge
   - You have 30 seconds to complete this step
   - The button is located on the top of the bridge

3. **Verify Discovery**
   - Your bridge should appear in the discovered devices list
   - Note the bridge name and IP address

### Step 3: Link Your Bridge

1. **Select Your Bridge**
   - Click on your bridge in the discovered devices list
   - Click **"Link Bridge"**

2. **Complete Linking**
   - Follow the on-screen instructions
   - The app will establish a secure connection
   - You'll see a success message when complete

3. **Verify Connection**
   - Return to the main dashboard
   - Check that your bridge shows as "Connected"

## Adding Devices

### Step 1: Discover Devices

1. **Choose Device Type**
   - From the main dashboard, select the type of device you want to add:
     - **Lights** - Individual Hue bulbs
     - **Groups** - Rooms, zones, or areas
     - **Scenes** - Pre-configured lighting scenes
     - **Sensors** - Motion, light, temperature, or switch sensors

2. **Start Discovery**
   - Click the appropriate discovery button
   - The app will scan your bridge for available devices

3. **Review Device List**
   - You'll see a list of all available devices
   - Each device shows its name, type, and current status

### Step 2: Add Devices to Hubitat

1. **Select Devices**
   - Check the boxes next to the devices you want to add
   - You can select multiple devices at once

2. **Configure Settings**
   - For each device type, you may see configuration options:
     - **Groups:** Default scene, scene mode, refresh settings
     - **Sensors:** Sensitivity, logging options
     - **Lights:** Auto-refresh settings

3. **Install Devices**
   - Click **"Add Selected Devices"**
   - The app will create Hubitat child devices
   - You'll see a success message when complete

### Step 3: Verify Device Installation

1. **Check Device List**
   - Go to **Devices** in your Hubitat interface
   - Look for your newly added Hue devices
   - They should appear with appropriate icons and names

2. **Test Basic Functions**
   - Click on a device to open its control panel
   - Test basic functions like on/off, brightness, or color
   - Verify that changes are reflected on your Hue devices

## Configuration

### Bridge Settings

1. **Access Bridge Device**
   - Find your bridge device in the Devices list
   - Click on it to open the control panel

2. **Configure Settings**
   - **Auto Refresh:** Enable automatic state polling
   - **Refresh Interval:** Set how often to poll (1 min - 3 hours)
   - **Connection Watchdog:** Enable aggressive connection monitoring
   - **Logging:** Enable debug logging if needed

### Device Settings

1. **Group Configuration**
   - **Default Scene:** Set a scene to activate when group is turned on
   - **Scene Mode:** Choose between "trigger" or "switch" behavior
   - **Auto Refresh:** Enable device-level refresh

2. **Sensor Configuration**
   - **Motion Sensitivity:** Adjust motion detection sensitivity
   - **Logging:** Enable device-specific logging

## Basic Usage

### Controlling Lights

1. **Individual Light Control**
   - Open any light device
   - Use the on/off switch
   - Adjust brightness with the slider
   - Change color using the color picker

2. **Group Control**
   - Open a group device
   - Control all lights in the group simultaneously
   - Set colors and brightness for the entire group
   - Activate scenes

### Using Sensors

1. **Motion Sensors**
   - Monitor motion detection events
   - Check light level readings
   - Monitor battery levels

2. **Switch Sensors**
   - Use button presses to trigger automations
   - Monitor battery levels
   - Configure button behaviors

### Creating Automations

1. **Basic Automation**
   - Go to **Automation** → **Rules**
   - Create a new rule
   - Use Hue devices as triggers or actions

2. **Example: Motion-Activated Lighting**
   - **Trigger:** Motion sensor detects motion
   - **Condition:** Light level is below threshold
   - **Action:** Turn on lights in the room

## Troubleshooting

### Common Issues

#### Bridge Not Found
- **Check Network:** Ensure bridge and Hubitat are on the same network
- **Check Power:** Verify bridge is powered and connected
- **Check Firewall:** Ensure network allows SSDP discovery
- **Try Manual IP:** Enter bridge IP address manually

#### Linking Fails
- **Press Link Button:** Ensure you press the link button within 30 seconds
- **Check Bridge:** Verify bridge is not already linked to another system
- **Network Issues:** Check network connectivity
- **Try Again:** Unlink and relink the bridge

#### Devices Not Responding
- **Check Bridge Connection:** Verify bridge is connected
- **Refresh Devices:** Use the refresh command
- **Check Logs:** Enable debug logging and check for errors
- **Restart Bridge:** Power cycle the Hue Bridge

#### Performance Issues
- **Reduce Refresh Rate:** Increase refresh intervals
- **Disable Auto-Refresh:** Turn off unnecessary polling
- **Check Hub Resources:** Monitor Hubitat CPU and memory usage
- **Limit Device Count:** Consider reducing the number of devices

#### Errors after an update, or “NullPointerException” in logs
- Open the **Advanced Hue Bridge Integration** app and complete a **hub / metadata refresh** for the linked bridge (normal in-app hub flow—not the same as **Unlink**).
- **Reboot** the Hubitat hub if issues continue.
- In **Hubitat Package Manager**, run **Repair** on this package if you suspect stale code.
- See [DOCUMENTATION.md](DOCUMENTATION.md) troubleshooting for detail (matches guidance discussed in the [forum thread](https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420)).

#### No `battery` on Hue dimmers when using Hubitat’s built-in Hue app
- The **built-in** integration may not populate `battery` on some button/dimmer devices, regardless of driver choice.
- **Advanced Hue** reports battery on drivers where the accessory is supported (e.g. Hue Dimmer, Tap, sensors). Accessories must be added through **this** app and show the expected driver—unusual models may not be supported.
- Community discussion ([e.g. post 504 — Lutron Aurora](https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420/504)): some users rely on **physical low-battery signals** on the device if Hubitat never exposes the attribute.

#### Two Hue integrations on one hub
- Running **both** Hubitat’s built-in Hue integration and **Advanced Hue** against the same bridge can **duplicate** traffic and load; avoid unless necessary, and watch for device ID conflicts. Full notes: [DOCUMENTATION.md](DOCUMENTATION.md).

### Getting Help

1. **Check Logs**
   - Enable debug logging in device settings
   - Review Hubitat logs for error messages
   - Look for specific error codes

2. **Community Support**
   - Visit the [Hubitat Community Forum](https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420)
   - Search for existing solutions
   - Post questions with detailed information

3. **Documentation**
   - Review the full documentation in this package
   - Check the API reference for technical details
   - Read the troubleshooting sections

## Next Steps

### Advanced Configuration

1. **Scene Management**
   - Create and manage Hue scenes
   - Use scenes in automations
   - Configure scene triggers

2. **Color Management**
   - Understand color gamuts
   - Configure color accuracy
   - Set up color-based automations

3. **Performance Optimization**
   - Fine-tune refresh intervals
   - Optimize network usage
   - Monitor system resources

### Integration Examples

1. **Home Security**
   - Use motion sensors for security monitoring
   - Create lighting sequences for alerts
   - Integrate with other security devices

2. **Energy Management**
   - Use light sensors for daylight harvesting
   - Create energy-efficient lighting schedules
   - Monitor device usage patterns

3. **Comfort Automation**
   - Use temperature sensors for climate control
   - Create lighting based on time of day
   - Integrate with HVAC systems

## Support Resources

- **GitHub Repository:** https://github.com/apwelsh/hubitat
- **Community Forum:** https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420
- **Documentation:** See the full documentation in this package
- **API Reference:** Technical details for developers

---

*This quick start guide covers the essential steps to get your Advanced Hue Bridge Integration up and running. For detailed information, refer to the complete documentation and API reference.* 