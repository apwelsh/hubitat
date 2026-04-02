# Advanced Hue Bridge Integration - API Reference

## Overview

This document provides a comprehensive API reference for the Advanced Hue Bridge Integration package, including all public methods, commands, attributes, and configuration options available to developers and users.

## Table of Contents

1. [Parent Application API](#parent-application-api)
2. [Device Handler APIs](#device-handler-apis)
3. [Utility Library API](#utility-library-api)
4. [Configuration Options](#configuration-options)
5. [Event Reference](#event-reference)
6. [Error Codes](#error-codes)

---

## Parent Application API

### File: `hue-bridge-integration.groovy`

#### Public Methods

##### Bridge Discovery
```groovy
def ssdpSubscribe()
```
Subscribes to SSDP discovery events to find Hue Bridges on the network.

**Returns:** `void`

---

```groovy
def ssdpHandler(evt)
```
Handles SSDP discovery events and processes discovered devices.

**Parameters:**
- `evt` - SSDP discovery event

**Returns:** `void`

---

```groovy
def verifyDevice(device)
```
Verifies a discovered device is a valid Hue Bridge.

**Parameters:**
- `device` - Discovered device information

**Returns:** `boolean` - True if valid Hue Bridge

---

```groovy
def linkBridge(device)
```
Initiates the linking process with a Hue Bridge.

**Parameters:**
- `device` - Bridge device information

**Returns:** `boolean` - True if linking successful

---

##### Device Management
```groovy
def enumerateLights()
```
Discovers all available lights on linked bridges.

**Returns:** `Map` - Map of light devices

---

```groovy
def enumerateGroups()
```
Discovers all available groups on linked bridges.

**Returns:** `Map` - Map of group devices

---

```groovy
def enumerateScenes()
```
Discovers all available scenes on linked bridges.

**Returns:** `Map` - Map of scene devices

---

```groovy
def enumerateSensors()
```
Discovers all available sensors on linked bridges.

**Returns:** `Map` - Map of sensor devices

---

```groovy
def addLights(selectedLights)
```
Adds selected lights as Hubitat child devices.

**Parameters:**
- `selectedLights` - List of light device IDs

**Returns:** `void`

---

```groovy
def addGroups(selectedGroups)
```
Adds selected groups as Hubitat child devices.

**Parameters:**
- `selectedGroups` - List of group device IDs

**Returns:** `void`

---

```groovy
def addScenes(selectedScenes)
```
Adds selected scenes as Hubitat child devices.

**Parameters:**
- `selectedScenes` - List of scene device IDs

**Returns:** `void`

---

```groovy
def addSensors(selectedSensors)
```
Adds selected sensors as Hubitat child devices.

**Parameters:**
- `selectedSensors` - List of sensor device IDs

**Returns:** `void`

---

##### State Management
```groovy
def setDeviceState(device, state)
```
Sets the state of a Hue device.

**Parameters:**
- `device` - Hubitat device object
- `state` - Map of state properties

**Returns:** `boolean` - True if successful

---

```groovy
def getDeviceState(device)
```
Retrieves the current state of a Hue device.

**Parameters:**
- `device` - Hubitat device object

**Returns:** `Map` - Current device state

---

```groovy
def setHueProperty(device, properties)
```
Sets specific properties on a Hue device.

**Parameters:**
- `device` - Hubitat device object
- `properties` - Map of properties to set

**Returns:** `boolean` - True if successful

---

##### Utility Methods
```groovy
def getAtomicState(device)
```
Gets thread-safe state for a device.

**Parameters:**
- `device` - Hubitat device object

**Returns:** `Map` - Device state

---

```groovy
def getAtomicQueue(device)
```
Gets thread-safe command queue for a device.

**Parameters:**
- `device` - Hubitat device object

**Returns:** `Map` - Command queue

---

```groovy
def getDeviceForChild(child)
```
Gets the parent device for a child device.

**Parameters:**
- `child` - Child device object

**Returns:** `Object` - Parent device

---

```groovy
def getChildDeviceForMac(mac)
```
Gets child device by MAC address.

**Parameters:**
- `mac` - MAC address string

**Returns:** `Object` - Child device or null

---

#### Configuration Pages

##### Main Page
- **Name:** `mainPage`
- **Purpose:** Main dashboard and navigation
- **Features:** Bridge status, device counts, navigation links

##### Bridge Discovery
- **Name:** `bridgeDiscovery`
- **Purpose:** Discover and list Hue Bridges
- **Features:** SSDP discovery, bridge listing, manual entry

##### Bridge Linking
- **Name:** `bridgeLinking`
- **Purpose:** Link discovered bridges
- **Features:** Link button guidance, authentication, status

##### Device Discovery
- **Name:** `findLights`, `findGroups`, `findScenes`, `findSensors`
- **Purpose:** Discover specific device types
- **Features:** Device listing, selection, refresh

##### Device Addition
- **Name:** `addLights`, `addGroups`, `addScenes`, `addSensors`
- **Purpose:** Add devices to Hubitat
- **Features:** Device selection, configuration, installation

---

## Device Handler APIs

### Advanced Hue Bridge

#### Commands
```groovy
def connect()
```
Manually connect to the Hue Bridge.

---

```groovy
def disconnect()
```
Manually disconnect from the Hue Bridge.

---

```groovy
def refresh()
```
Force a refresh of bridge state.

---

#### Attributes
- `networkStatus` (String) - Connection status: "connected", "disconnected"
- `switch` (String) - Bridge state: "on", "off"

#### Settings
- `autoRefresh` (Boolean) - Enable automatic refresh
- `refreshInterval` (Integer) - Refresh interval in seconds
- `watchdog` (Boolean) - Enable connection watchdog
- `anyOn` (Boolean) - ANY on vs ALL on behavior
- `logEnable` (Boolean) - Enable informational logging
- `debug` (Boolean) - Enable debug logging

---

### Advanced Hue Group

#### Commands
```groovy
def on()
```
Turn group on.

---

```groovy
def off()
```
Turn group off.

---

```groovy
def setLevel(level)
```
Set brightness level.

**Parameters:**
- `level` (Integer) - Brightness level (0-100)

---

```groovy
def setColor(color)
```
Set color.

**Parameters:**
- `color` (Map) - Color map with hue, saturation, level

---

```groovy
def setColorTemperature(temperature)
```
Set color temperature.

**Parameters:**
- `temperature` (Integer) - Color temperature in Kelvin

---

```groovy
def refresh()
```
Force refresh of group state.

---

#### Attributes
- `switch` (String) - On/off state: "on", "off"
- `level` (Integer) - Brightness level (0-100)
- `hue` (Integer) - Color hue (0-100)
- `saturation` (Integer) - Color saturation (0-100)
- `colorTemperature` (Integer) - Color temperature in Kelvin
- `colorMode` (String) - Color mode: "HS", "CT", "XY"

#### Settings
- `defaultScene` (String) - Default scene name
- `sceneMode` (String) - Scene mode: "trigger", "switch"
- `sceneOff` (Boolean) - Track scene state
- `autoRefresh` (Boolean) - Enable automatic refresh
- `refreshInterval` (Integer) - Refresh interval in seconds
- `anyOn` (Boolean) - ANY on vs ALL on behavior
- `logEnable` (Boolean) - Enable logging

---

### Sensor Devices

#### Common Commands
```groovy
def refresh()
```
Force refresh of sensor state.

#### Common Attributes
- `battery` (Integer) - Battery level (0-100)
- `status` (String) - Sensor status: "enabled", "disabled"
- `health` (String) - Sensor health: "reachable", "unreachable"

#### Motion Sensor
- `motion` (String) - Motion state: "active", "inactive"
- `illuminance` (Integer) - Light level in lux

#### Light Sensor
- `illuminance` (Integer) - Light level in lux

#### Temperature Sensor
- `temperature` (Integer) - Temperature in degrees Celsius

#### Button Devices (Dimmer, Tap, RunLessWires)
- `button1`, `button2`, `button3`, `button4` (String) - Button states

---

## Utility Library API

### File: `HueFunctions.groovy`

#### Core Utility Functions
```groovy
BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max)
```
Clamps a value between minimum and maximum bounds.

**Parameters:**
- `value` - Value to clamp
- `min` - Minimum bound
- `max` - Maximum bound

**Returns:** `BigDecimal` - Clamped value

---

#### Gamut Management
```groovy
List<List<BigDecimal>> parseGamut(Map<String, Map<String, Double>> gamut)
```
Parses a gamut definition into XY coordinate points.

**Parameters:**
- `gamut` - Gamut definition map

**Returns:** `List<List<BigDecimal>>` - List of XY points

---

```groovy
boolean isInsideGamut(BigDecimal x, BigDecimal y, List<List<BigDecimal>> gamut)
```
Checks if a point is inside the gamut triangle.

**Parameters:**
- `x` - X coordinate
- `y` - Y coordinate
- `gamut` - Gamut definition

**Returns:** `boolean` - True if inside gamut

---

```groovy
List<BigDecimal> clampXYtoGamut(BigDecimal x, BigDecimal y, List<List<BigDecimal>> gamut)
```
Clamps XY coordinates to valid gamut.

**Parameters:**
- `x` - X coordinate
- `y` - Y coordinate
- `gamut` - Gamut definition

**Returns:** `List<BigDecimal>` - Clamped XY coordinates

---

#### Color Conversion Functions
```groovy
List<BigDecimal> rgbToHS(BigDecimal r, BigDecimal g, BigDecimal b)
```
Converts RGB to Hue/Saturation.

**Parameters:**
- `r` - Red component (0-255)
- `g` - Green component (0-255)
- `b` - Blue component (0-255)

**Returns:** `List<BigDecimal>` - [hue, saturation] (0-100)

---

```groovy
List<BigDecimal> hsToRGB(BigDecimal h, BigDecimal s, BigDecimal v)
```
Converts Hue/Saturation/Value to RGB.

**Parameters:**
- `h` - Hue (0-100)
- `s` - Saturation (0-100)
- `v` - Value (0-100)

**Returns:** `List<BigDecimal>` - [red, green, blue] (0-255)

---

```groovy
List<BigDecimal> rgbToXY(BigDecimal r, BigDecimal g, BigDecimal b)
```
Converts RGB to CIE 1931 XY coordinates.

**Parameters:**
- `r` - Red component (0-255)
- `g` - Green component (0-255)
- `b` - Blue component (0-255)

**Returns:** `List<BigDecimal>` - [x, y] (0-1)

---

```groovy
List<BigDecimal> xyToRGB(BigDecimal x, BigDecimal y, BigDecimal brightness)
```
Converts XY coordinates to RGB.

**Parameters:**
- `x` - X coordinate (0-1)
- `y` - Y coordinate (0-1)
- `brightness` - Brightness (0-100)

**Returns:** `List<BigDecimal>` - [red, green, blue] (0-255)

---

```groovy
List<BigDecimal> xyToHSV(BigDecimal x, BigDecimal y, BigDecimal brightness)
```
Converts XY coordinates to HSV.

**Parameters:**
- `x` - X coordinate (0-1)
- `y` - Y coordinate (0-1)
- `brightness` - Brightness (0-100)

**Returns:** `List<BigDecimal>` - [hue, saturation, value] (0-100)

---

```groovy
List<BigDecimal> hsvToXY(BigDecimal h, BigDecimal s, BigDecimal v)
```
Converts HSV to XY coordinates.

**Parameters:**
- `h` - Hue (0-100)
- `s` - Saturation (0-100)
- `v` - Value (0-100)

**Returns:** `List<BigDecimal>` - [x, y] (0-1)

---

```groovy
List<BigDecimal> kelvinToRGB(BigDecimal kelvin)
```
Converts color temperature to RGB.

**Parameters:**
- `kelvin` - Color temperature in Kelvin

**Returns:** `List<BigDecimal>` - [red, green, blue] (0-255)

---

```groovy
BigDecimal rgbToKelvin(BigDecimal r, BigDecimal g, BigDecimal b)
```
Converts RGB to approximate color temperature.

**Parameters:**
- `r` - Red component (0-255)
- `g` - Green component (0-255)
- `b` - Blue component (0-255)

**Returns:** `BigDecimal` - Color temperature in Kelvin

---

## Configuration Options

### Global Settings

#### Discovery Settings
- `PAGE_REFRESH_TIMEOUT` (Integer) - Page refresh timeout in seconds
- `DEVICE_REFRESH_DISCOVER_INTERVAL` (Integer) - Device discovery interval
- `DEVICE_REFRESH_MAX_COUNT` (Integer) - Maximum refresh count

#### Network Settings
- **SSDP Timeout:** How long to wait for bridge discovery
- **Discovery Retries:** Number of discovery attempts
- **Network Interface:** Specific network interface for discovery

#### Performance Settings
- **Auto Refresh:** Enable automatic state polling
- **Refresh Intervals:** How often to poll for updates
- **Event Stream:** Enable real-time event processing
- **Batch Operations:** Group multiple operations together

### Device-Specific Settings

#### Bridge Settings
- **Auto Refresh:** Enable automatic state polling (default: true)
- **Refresh Interval:** How often to poll for updates (1 min - 3 hours, default: 10 min)
- **Connection Watchdog:** Aggressive connection monitoring (default: false)
- **ANY on vs ALL on:** Group behavior when some lights are on (default: true)

#### Group Settings
- **Default Scene:** Scene to activate when group is turned on
- **Scene Mode:** How scenes behave (trigger/switch)
- **Scene Off Tracking:** Track scene state when enabled
- **Auto Refresh:** Enable automatic state polling
- **Refresh Interval:** How often to poll for updates

#### Sensor Settings
- **Motion Sensitivity:** Adjust motion detection sensitivity (0-max)
- **Logging:** Enable informational and debug logging
- **Auto Refresh:** Enable automatic state polling

---

## Event Reference

### SSDP Events
```groovy
// SSDP discovery event
ssdpTerm.urn:schemas-upnp-org:device:Basic:1
```

### Device Events
```groovy
// Device state change event
deviceStateChange
```

### Bridge Events
```groovy
// Bridge connection event
bridgeConnected
bridgeDisconnected
```

### Sensor Events
```groovy
// Motion detection event
motion.active
motion.inactive

// Button press event
button.pressed
button.held
button.released

// Battery level event
battery.level
```

### Color Events
```groovy
// Color change event
color.changed
hue.changed
saturation.changed
colorTemperature.changed
```

---

## Error Codes

### Network Errors
- **1001:** Bridge not found
- **1002:** Connection timeout
- **1003:** Authentication failed
- **1004:** Network unreachable

### Device Errors
- **2001:** Device not found
- **2002:** Device unreachable
- **2003:** Invalid device ID
- **2004:** Device not supported

### API Errors
- **3001:** Invalid API key
- **3002:** Rate limit exceeded
- **3003:** Invalid request format
- **3004:** Server error

### Configuration Errors
- **4001:** Invalid settings
- **4002:** Missing required parameters
- **4003:** Configuration conflict
- **4004:** Permission denied

### Color Errors
- **5001:** Invalid color format
- **5002:** Color out of gamut
- **5003:** Unsupported color space
- **5004:** Color conversion failed

---

## Usage Examples

### Basic Bridge Discovery
```groovy
// Subscribe to SSDP events
ssdpSubscribe()

// Handle discovered devices
def ssdpHandler(evt) {
    def device = evt.device
    if (verifyDevice(device)) {
        // Device is a valid Hue Bridge
        linkBridge(device)
    }
}
```

### Device State Management
```groovy
// Set device state
def state = [
    on: true,
    bri: 100,
    hue: 50000,
    sat: 100
]
setDeviceState(device, state)

// Get device state
def currentState = getDeviceState(device)
```

### Color Conversion
```groovy
// Convert RGB to Hue XY
def rgb = [255, 128, 64]
def xy = rgbToXY(rgb[0], rgb[1], rgb[2])

// Check gamut and clamp if needed
if (!isInsideGamut(xy[0], xy[1], deviceGamut)) {
    xy = clampXYtoGamut(xy[0], xy[1], deviceGamut)
}
```

### Event Handling
```groovy
// Subscribe to device events
subscribe(device, "switch", switchHandler)
subscribe(device, "level", levelHandler)
subscribe(device, "color", colorHandler)

// Handle events
def switchHandler(evt) {
    log.info "Switch changed to: ${evt.value}"
}

def levelHandler(evt) {
    log.info "Level changed to: ${evt.value}"
}

def colorHandler(evt) {
    log.info "Color changed to: ${evt.value}"
}
```

---

*This API reference provides comprehensive documentation for all public interfaces in the Advanced Hue Bridge Integration package. For implementation details, refer to the source code and inline documentation.* 