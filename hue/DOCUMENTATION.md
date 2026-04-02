# Advanced Hue Bridge Integration - Complete Documentation

## Overview

The Advanced Hue Bridge Integration is a comprehensive Hubitat package that provides full integration with Philips Hue Bridges and their associated devices. This package enables discovery, linking, and management of Hue lights, groups, scenes, and sensors within the Hubitat ecosystem.

**Version:** 1.10.24  
**Author:** Armand Welsh  
**Minimum Hubitat Version:** 2.2.9  
**License:** MIT License

## Package Structure

```
hue/
├── app/
│   └── hue-bridge-integration.groovy     # Main parent application
├── device/
│   ├── advanced-hue-bridge.groovy        # Bridge controller device
│   ├── advanced-hue-group.groovy         # Group/room/zone device
│   ├── advanced-hue-motion-sensor.groovy # Motion sensor device
│   ├── advanced-hue-light-sensor.groovy  # Light level sensor device
│   ├── advanced-hue-temperature-sensor.groovy # Temperature sensor device
│   ├── advanced-hue-dimmer-sensor.groovy # Dimmer switch device
│   ├── advanced-hue-tap-sensor.groovy    # Tap switch device
│   └── advanced-hue-runlesswires-sensor.groovy # RunLessWires switch device
├── libs/
│   └── HueFunctions.groovy               # Shared utility functions
├── packageManifest.json                  # Package metadata
├── README.md                             # Basic readme
└── LICENSE.md                            # License file
```

## Core Components

### 1. Parent Application (`hue-bridge-integration.groovy`)

**Purpose:** The main orchestrator that discovers Hue Bridges, manages device installation, and coordinates communication between Hubitat and Hue devices.

**Key Features:**
- **Bridge Discovery:** Uses SSDP (Simple Service Discovery Protocol) to find Hue Bridges on the network
- **Bridge Linking:** Establishes secure connections with Hue Bridges using the Hue API
- **Device Management:** Discovers and installs lights, groups, scenes, and sensors
- **State Synchronization:** Maintains real-time state synchronization between Hubitat and Hue
- **Event Handling:** Processes events from Hue Bridges and translates them to Hubitat events

**Main Functions:**
- `ssdpSubscribe()` - Subscribes to SSDP discovery events
- `verifyDevice()` - Verifies discovered Hue Bridges
- `enumerateLights()`, `enumerateGroups()`, `enumerateScenes()`, `enumerateSensors()` - Device discovery
- `setDeviceState()` - Sends state changes to Hue devices
- `getDeviceState()` - Retrieves current state from Hue devices

**Configuration Pages:**
- Bridge Discovery
- Device Addition (Lights, Groups, Scenes, Sensors)
- Bridge Linking/Unlinking
- Hub Refresh

### 2. Bridge Device (`advanced-hue-bridge.groovy`)

**Purpose:** Represents the Hue Bridge itself as a Hubitat device, providing bridge-level controls and monitoring.

**Capabilities:**
- `Switch` - Turn bridge connectivity on/off
- `Refresh` - Refresh bridge state
- `Initialize` - Initialize bridge connection

**Key Features:**
- **Connection Management:** Monitors bridge connectivity and automatically reconnects
- **Watchdog Timer:** Aggressive monitoring of connection state changes
- **Auto Refresh:** Configurable polling intervals for bridge state updates
- **Event Stream Management:** Handles real-time event streams from the bridge

**Settings:**
- Auto Refresh (enabled/disabled)
- Refresh Interval (1 minute to 3 hours)
- Connection Watchdog (enabled/disabled)
- ANY on vs ALL on behavior for groups

### 3. Group Device (`advanced-hue-group.groovy`)

**Purpose:** Manages Hue groups (rooms, zones, areas) as color-capable light devices in Hubitat.

**Capabilities:**
- `Switch` - Turn group on/off
- `ColorControl` - Control color and color temperature
- `ColorTemperature` - Set color temperature
- `Level` - Control brightness
- `Refresh` - Refresh group state

**Key Features:**
- **Scene Integration:** Can trigger scenes when turned on
- **Color Support:** Full color control with gamut mapping
- **State Tracking:** Tracks individual light states within the group
- **Auto Refresh:** Configurable polling for state updates

**Settings:**
- Default Scene (scene to activate when group is turned on)
- Scene Mode (trigger/switch behavior)
- Auto Refresh settings
- ANY on vs ALL on behavior

### 4. Sensor Devices

#### Motion Sensor (`advanced-hue-motion-sensor.groovy`)
**Capabilities:** `MotionSensor`, `Battery`, `Refresh`, `Sensor`
**Features:** Motion detection, light level reporting, battery monitoring, configurable sensitivity

#### Light Sensor (`advanced-hue-light-sensor.groovy`)
**Capabilities:** `IlluminanceMeasurement`, `Battery`, `Refresh`, `Sensor`
**Features:** Ambient light level measurement, battery monitoring

#### Temperature Sensor (`advanced-hue-temperature-sensor.groovy`)
**Capabilities:** `TemperatureMeasurement`, `Battery`, `Refresh`, `Sensor`
**Features:** Temperature measurement, battery monitoring

#### Dimmer Switch (`advanced-hue-dimmer-sensor.groovy`)
**Capabilities:** `Button`, `Battery`, `Refresh`, `Sensor`
**Features:** 4-button dimmer control, battery monitoring, button press events

#### Tap Switch (`advanced-hue-tap-sensor.groovy`)
**Capabilities:** `Button`, `Battery`, `Refresh`, `Sensor`
**Features:** 4-button tap control, battery monitoring, button press events

#### RunLessWires Switch (`advanced-hue-runlesswires-sensor.groovy`)
**Capabilities:** `Button`, `Battery`, `Refresh`, `Sensor`
**Features:** Friends of Hue compatible switch, battery monitoring

### 5. Utility Library (`HueFunctions.groovy`)

**Purpose:** Shared utility functions for color conversion, gamut mapping, and Hue API interactions.

**Key Functions:**
- `clamp()` - Clamp values between min/max
- `parseGamut()` - Parse color gamut definitions
- `isInsideGamut()` - Check if color is within gamut
- `clampXYtoGamut()` - Clamp XY coordinates to gamut
- `rgbToHS()` - Convert RGB to Hue/Saturation
- `hsToRGB()` - Convert Hue/Saturation to RGB
- `xyToRGB()` - Convert XY coordinates to RGB
- `rgbToXY()` - Convert RGB to XY coordinates

## Installation and Setup

### Prerequisites
- Hubitat Elevation hub (version 2.2.9 or higher)
- Philips Hue Bridge (any generation)
- Network connectivity between Hubitat and Hue Bridge

### Installation Steps

1. **Install the Package:**
   - Use Hubitat Package Manager or manual installation
   - Install the parent application first
   - Install required device handlers

2. **Discover Bridges:**
   - Open the parent application
   - Navigate to "Device Discovery"
   - Press the link button on your Hue Bridge
   - The app will discover and list available bridges

3. **Link Bridge:**
   - Select your bridge from the discovered list
   - Complete the linking process
   - Verify connection status

4. **Add Devices:**
   - Use the discovery pages to find lights, groups, scenes, and sensors
   - Add desired devices to Hubitat
   - Configure device settings as needed

## Configuration Options

### Bridge Settings
- **Auto Refresh:** Enable automatic state polling
- **Refresh Interval:** How often to poll for updates (1 min - 3 hours)
- **Connection Watchdog:** Aggressive connection monitoring
- **ANY on vs ALL on:** Group behavior when some lights are on

### Device Settings
- **Default Scenes:** Scenes to activate when groups are turned on
- **Scene Mode:** How scenes behave (trigger or switch)
- **Auto Refresh:** Device-level refresh settings
- **Sensitivity:** Motion sensor sensitivity (where applicable)

## API Integration

### Hue API Support
- **v1 API:** Legacy support for older bridges
- **v2 API:** Modern API with enhanced features
- **Event Stream:** Real-time state updates
- **SSDP Discovery:** Automatic bridge discovery

### Color Management
- **Gamut Mapping:** Automatic color gamut correction
- **Color Space Conversion:** RGB ↔ XY ↔ HSV conversions
- **Color Temperature:** Support for white temperature control
- **Brightness Control:** Level and dimming support

## Troubleshooting

### Common Issues

1. **Bridge Not Found:**
   - Ensure bridge is on the same network as Hubitat
   - Check firewall settings
   - Verify bridge is powered and connected
   - Discovery uses **SSDP**; mDNS is not used. The app normalizes bridge host addresses for **HTTPS (port 443)**, which the Hue **v2** API expects. If you recently upgraded, pull the latest package—historical builds had edge cases around host/port handling (see package release notes, e.g. 1.10.24).

2. **Linking Fails:**
   - Press the link button on the bridge within 30 seconds
   - Check network connectivity
   - Verify bridge IP address

3. **Devices Not Updating:**
   - Check auto-refresh settings
   - Verify event stream connectivity
   - Review log messages for errors
   - Ensure the Hue bridge firmware supports reliable **v2 event-stream** behavior; conservative **bridge-level** auto-refresh is recommended as a backstop (see README / release notes for 1.10.0+).

4. **Color Issues:**
   - Verify bulb supports color
   - Check gamut mapping settings
   - Review color conversion logs

5. **NullPointerException / “state not synced” after upgrade or re-pair:**
   - Community reports of NPEs tied to **v2 metadata** (e.g. `state.lights_v2`) not being populated yet—often right after migrating to newer code, re-pairing the bridge, or running a patched/old copy of the app out of sync with GitHub.
   - **First:** Open the **Advanced Hue Bridge Integration** app and run **hub / metadata refresh** from the flow that touches the linked bridge (use the normal **Hue hub** navigation in the app—not “Unlink” unless you intend to unlink). That forces Hue settings and v2 device data to reload.
   - **Then:** **Reboot Hubitat** if problems persist; startup is supposed to kick off metadata refresh when things are healthy.
   - **Hubitat Package Manager:** If no update appears but you expect fixes, run a **Repair** on this package to force a fresh download from the manifest URLs.
   - Avoid long-term **private forks** without merging upstream; the maintainer cannot support modified code bases.

6. **Battery not shown on Hue dimmers / accessories (built-in Hubitat Hue app):**
   - In the [community thread](https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420), users noted that devices added with **Hubitat’s built-in Hue Bridge Integration** sometimes **do not show a `battery` attribute**, even when changing the device driver (e.g. `hueBridgeButton`, Generic Zigbee Dimmer, generic **Device**).
   - **Advanced Hue** exposes **`Battery`** on supported **Hue** accessory types that use the corresponding drivers (e.g. **Hue Dimmer**, **Tap**, motion / light / temperature sensors). Not every third-party or rare model is enumerated or mapped to a driver—if the app does not offer a compatible type, the device is unsupported until added to the integration.
   - **Lutron Aurora** (and similar) came up specifically in [post 504](https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420/504): community members discussed whether battery could be monitored for alerts; the maintainer indicated interest in possible support, while another user relied on **on-device low-battery indication** (e.g. LED behavior) where Hubitat does not surface the value. Treat battery alerts on nonstandard accessories as **best-effort** and verify in Hubitat **Current States** after pairing with this integration.

7. **Running alongside Hubitat’s built-in Hue integration:**
   - Discussed in the [same release thread](https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420): using **both** integrations may be possible if **device network IDs / device identity** does not conflict, but you can **roughly double** Hue-related **network and parsing work** on the hub when both are active—especially if both use **v2** traffic.
   - Prefer **one** primary integration per bridge unless you have a specific reason (e.g. built-in cannot add a particular device type—on/off-only lights have been raised as an example; try Advanced Hue or alternatives such as **CoCo Hue** if this integration does not fit).

8. **Third-party “Hue bridge” emulation (e.g. Tasmota):**
   - This integration expects a **real Philips Hue bridge** with **Hue v2** and **HTTPS**. Firmware that only emulates **Hue v1 over HTTP** is unlikely to work without a different community integration that allows HTTP + v1 (as some users reported doing elsewhere). This is not a supported configuration for Advanced Hue.

### Debug Options
- Enable debug logging in device settings
- Check Hubitat logs for detailed error messages
- Use the refresh capability to force state updates

## Performance Considerations

### Network Impact
- Event streams reduce polling overhead
- Configurable refresh intervals allow optimization
- Batch operations minimize API calls

### Hubitat Resources
- Large installations may require increased refresh intervals
- Monitor hub CPU and memory usage
- Consider disabling auto-refresh for stable devices

## Version History

### Recent Updates (1.10.x)
- Fixed infinite :443 appending bug
- Improved hub discovery and linking
- Enhanced IP address change detection
- Added V2 API color conversion support
- Improved event stream reliability

### Key Features by Version
- **1.10.0:** Event stream-based updates
- **1.9.0:** RunLessWires switch support
- **1.8.x:** Enhanced group and scene handling
- **1.7.x:** Improved sensor support
- **1.6.x:** Advanced bridge management

## Support and Community

- **GitHub:** https://github.com/apwelsh/hubitat
- **Community Forum:** https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420
- **Documentation:** https://github.com/apwelsh/hubitat/tree/master/hue

## License

This software is licensed under the MIT License. See LICENSE.md for full details.

---

*This documentation covers the complete Advanced Hue Bridge Integration package. For specific implementation details, refer to the individual file documentation below.* 