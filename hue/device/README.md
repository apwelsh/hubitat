# Advanced Hue Bridge Integration - Device Handlers

## Overview

This directory contains all the device handlers for the Advanced Hue Bridge Integration package. Each device handler represents a specific type of Hue device and provides the interface between Hubitat and the Hue Bridge.

## Device Handler Files

### 1. Advanced Hue Bridge (`advanced-hue-bridge.groovy`)

**Purpose:** Represents the Hue Bridge itself as a Hubitat device, providing bridge-level controls and monitoring.

**Version:** 1.5.0  
**Size:** ~26KB, 662 lines

#### Capabilities
- `Switch` - Turn bridge connectivity on/off
- `Refresh` - Refresh bridge state
- `Initialize` - Initialize bridge connection

#### Key Features
- **Connection Management:** Monitors bridge connectivity and automatically reconnects
- **Watchdog Timer:** Aggressive monitoring of connection state changes
- **Auto Refresh:** Configurable polling intervals for bridge state updates
- **Event Stream Management:** Handles real-time event streams from the bridge

#### Settings
- **Auto Refresh:** Enable automatic state polling (default: true)
- **Refresh Interval:** How often to poll for updates (1 min - 3 hours, default: 10 min)
- **Connection Watchdog:** Aggressive connection monitoring (default: false)
- **ANY on vs ALL on:** Group behavior when some lights are on (default: true)
- **Logging:** Enable informational and debug logging

#### Commands
- `connect()` - Manually connect to the bridge
- `disconnect()` - Manually disconnect from the bridge
- `refresh()` - Force a refresh of bridge state

#### Attributes
- `networkStatus` - Current connection status (connected/disconnected)

#### Use Cases
- Bridge health monitoring
- Connection troubleshooting
- Manual connection control
- Bridge-level configuration

---

### 2. Advanced Hue Group (`advanced-hue-group.groovy`)

**Purpose:** Manages Hue groups (rooms, zones, areas) as color-capable light devices in Hubitat.

**Version:** 1.6.1  
**Size:** ~12KB, 341 lines

#### Capabilities
- `Switch` - Turn group on/off
- `ColorControl` - Control color and color temperature
- `ColorTemperature` - Set color temperature
- `Level` - Control brightness
- `Refresh` - Refresh group state

#### Key Features
- **Scene Integration:** Can trigger scenes when turned on
- **Color Support:** Full color control with gamut mapping
- **State Tracking:** Tracks individual light states within the group
- **Auto Refresh:** Configurable polling for state updates

#### Settings
- **Default Scene:** Scene to activate when group is turned on
- **Scene Mode:** How scenes behave (trigger/switch)
- **Scene Off Tracking:** Track scene state when enabled
- **Auto Refresh:** Enable automatic state polling
- **Refresh Interval:** How often to poll for updates
- **ANY on vs ALL on:** Group behavior when some lights are on

#### Commands
- `on()` - Turn group on
- `off()` - Turn group off
- `setLevel(level)` - Set brightness level
- `setColor(color)` - Set color
- `setColorTemperature(temperature)` - Set color temperature
- `refresh()` - Force refresh of group state

#### Attributes
- `switch` - On/off state
- `level` - Brightness level (0-100)
- `hue` - Color hue (0-100)
- `saturation` - Color saturation (0-100)
- `colorTemperature` - Color temperature in Kelvin
- `colorMode` - Current color mode (HS/CT/XY)

#### Use Cases
- Room lighting control
- Zone management
- Scene activation
- Color and brightness control

---

### 3. Advanced Hue Motion Sensor (`advanced-hue-motion-sensor.groovy`)

**Purpose:** Manages Hue motion sensors, providing motion detection and light level reporting.

**Version:** 1.0.7  
**Size:** ~4.6KB, 130 lines

#### Capabilities
- `MotionSensor` - Motion detection
- `Battery` - Battery level monitoring
- `Refresh` - Refresh sensor state
- `Sensor` - General sensor capabilities

#### Key Features
- **Motion Detection:** Real-time motion sensing
- **Light Level Reporting:** Ambient light level measurement
- **Battery Monitoring:** Battery level tracking
- **Configurable Sensitivity:** Adjustable motion sensitivity

#### Settings
- **Logging:** Enable informational and debug logging
- **Motion Sensitivity:** Adjust motion detection sensitivity (0-max)

#### Commands
- `refresh()` - Force refresh of sensor state

#### Attributes
- `motion` - Motion detection state (active/inactive)
- `battery` - Battery level (0-100)
- `illuminance` - Light level in lux
- `status` - Sensor status (enabled/disabled)
- `health` - Sensor health (reachable/unreachable)

#### Use Cases
- Security monitoring
- Occupancy detection
- Light level monitoring
- Automated lighting control

---

### 4. Advanced Hue Light Sensor (`advanced-hue-light-sensor.groovy`)

**Purpose:** Manages Hue light sensors for ambient light level measurement.

**Version:** 1.0.6  
**Size:** ~3.5KB, 99 lines

#### Capabilities
- `IlluminanceMeasurement` - Light level measurement
- `Battery` - Battery level monitoring
- `Refresh` - Refresh sensor state
- `Sensor` - General sensor capabilities

#### Key Features
- **Light Level Measurement:** Precise ambient light measurement
- **Battery Monitoring:** Battery level tracking
- **Real-time Updates:** Continuous light level monitoring

#### Settings
- **Logging:** Enable informational and debug logging

#### Commands
- `refresh()` - Force refresh of sensor state

#### Attributes
- `illuminance` - Light level in lux
- `battery` - Battery level (0-100)
- `status` - Sensor status (enabled/disabled)
- `health` - Sensor health (reachable/unreachable)

#### Use Cases
- Daylight harvesting
- Automated lighting control
- Energy management
- Environmental monitoring

---

### 5. Advanced Hue Temperature Sensor (`advanced-hue-temperature-sensor.groovy`)

**Purpose:** Manages Hue temperature sensors for environmental temperature monitoring.

**Version:** 1.0.7  
**Size:** ~3.5KB, 99 lines

#### Capabilities
- `TemperatureMeasurement` - Temperature measurement
- `Battery` - Battery level monitoring
- `Refresh` - Refresh sensor state
- `Sensor` - General sensor capabilities

#### Key Features
- **Temperature Measurement:** Precise temperature monitoring
- **Battery Monitoring:** Battery level tracking
- **Real-time Updates:** Continuous temperature monitoring

#### Settings
- **Logging:** Enable informational and debug logging

#### Commands
- `refresh()` - Force refresh of sensor state

#### Attributes
- `temperature` - Temperature in degrees Celsius
- `battery` - Battery level (0-100)
- `status` - Sensor status (enabled/disabled)
- `health` - Sensor health (reachable/unreachable)

#### Use Cases
- Environmental monitoring
- HVAC control
- Energy management
- Climate control automation

---

### 6. Advanced Hue Dimmer Sensor (`advanced-hue-dimmer-sensor.groovy`)

**Purpose:** Manages Hue dimmer switches, providing button control and battery monitoring.

**Version:** 1.0.3  
**Size:** ~5.0KB, 150 lines

#### Capabilities
- `Button` - Button press detection
- `Battery` - Battery level monitoring
- `Refresh` - Refresh sensor state
- `Sensor` - General sensor capabilities

#### Key Features
- **4-Button Control:** Supports all 4 dimmer buttons
- **Battery Monitoring:** Battery level tracking
- **Button Press Events:** Individual button press detection
- **Hold and Release Events:** Support for button hold/release

#### Settings
- **Logging:** Enable informational and debug logging

#### Commands
- `refresh()` - Force refresh of sensor state

#### Attributes
- `button1`, `button2`, `button3`, `button4` - Button states
- `battery` - Battery level (0-100)
- `status` - Sensor status (enabled/disabled)
- `health` - Sensor health (reachable/unreachable)

#### Use Cases
- Lighting control
- Scene activation
- Dimming control
- Custom automation triggers

---

### 7. Advanced Hue Tap Sensor (`advanced-hue-tap-sensor.groovy`)

**Purpose:** Manages Hue tap switches, providing button control and battery monitoring.

**Version:** 1.0.3  
**Size:** ~4.4KB, 128 lines

#### Capabilities
- `Button` - Button press detection
- `Battery` - Battery level monitoring
- `Refresh` - Refresh sensor state
- `Sensor` - General sensor capabilities

#### Key Features
- **4-Button Control:** Supports all 4 tap buttons
- **Battery Monitoring:** Battery level tracking
- **Button Press Events:** Individual button press detection
- **Battery-Free Operation:** No battery required (kinetic energy)

#### Settings
- **Logging:** Enable informational and debug logging

#### Commands
- `refresh()` - Force refresh of sensor state

#### Attributes
- `button1`, `button2`, `button3`, `button4` - Button states
- `battery` - Battery level (0-100, typically 100 for kinetic devices)
- `status` - Sensor status (enabled/disabled)
- `health` - Sensor health (reachable/unreachable)

#### Use Cases
- Lighting control
- Scene activation
- Custom automation triggers
- Battery-free control

---

### 8. Advanced Hue RunLessWires Sensor (`advanced-hue-runlesswires-sensor.groovy`)

**Purpose:** Manages RunLessWires Friends of Hue compatible switches.

**Version:** 1.0.0  
**Size:** ~9.7KB, 258 lines

#### Capabilities
- `Button` - Button press detection
- `Battery` - Battery level monitoring
- `Refresh` - Refresh sensor state
- `Sensor` - General sensor capabilities

#### Key Features
- **Friends of Hue Compatible:** Works with RunLessWires switches
- **Battery Monitoring:** Battery level tracking
- **Button Press Events:** Individual button press detection
- **Advanced Button Support:** Multiple button configurations

#### Settings
- **Logging:** Enable informational and debug logging

#### Commands
- `refresh()` - Force refresh of sensor state

#### Attributes
- `button1`, `button2`, `button3`, `button4` - Button states
- `battery` - Battery level (0-100)
- `status` - Sensor status (enabled/disabled)
- `health` - Sensor health (reachable/unreachable)

#### Use Cases
- Third-party switch integration
- Custom lighting control
- Scene activation
- Automation triggers

## Common Features Across All Devices

### State Management
All device handlers implement consistent state management:
- Device state tracking
- Event processing
- State synchronization with Hue Bridge
- Error handling and recovery

### Logging
Standardized logging across all devices:
- Informational logging for normal operations
- Debug logging for troubleshooting
- Error logging for issues
- Configurable log levels

### Refresh Capability
All devices support the `Refresh` capability:
- Manual refresh commands
- Automatic refresh scheduling
- State synchronization
- Error recovery

### Health Monitoring
Device health tracking:
- Connection status
- Device reachability
- Battery levels (where applicable)
- Error states

### Event Handling
Consistent event processing:
- Button press events
- State change events
- Error events
- Battery level events

## Installation and Configuration

### Prerequisites
- Advanced Hue Bridge Integration parent application installed
- Hue Bridge linked and configured
- Devices discovered and added through parent application

### Configuration Steps
1. **Install Device Handlers:** Install all required device handlers
2. **Discover Devices:** Use parent application to discover devices
3. **Add Devices:** Add desired devices to Hubitat
4. **Configure Settings:** Adjust device-specific settings
5. **Test Functionality:** Verify all features work correctly

### Settings Configuration
- **Logging:** Enable appropriate logging levels
- **Auto Refresh:** Configure refresh intervals
- **Device-Specific Settings:** Adjust sensitivity, scenes, etc.
- **Performance Settings:** Optimize for your environment

## Troubleshooting

### Common Issues
1. **Devices Not Responding**
   - Check bridge connectivity
   - Verify device is reachable
   - Review log messages
   - Test manual refresh

2. **Battery Issues**
   - Check battery levels
   - Replace batteries if needed
   - Verify device is within range

3. **Button Press Issues**
   - Check button configuration
   - Verify event handling
   - Review automation rules

4. **Sensor Accuracy**
   - Check sensor placement
   - Verify sensitivity settings
   - Review environmental factors

### Debug Options
- Enable debug logging
- Monitor device events
- Check bridge connectivity
- Review automation rules

## Performance Considerations

### Resource Usage
- Monitor hub CPU and memory usage
- Adjust refresh intervals as needed
- Disable unnecessary features
- Optimize automation rules

### Network Impact
- Minimize polling frequency
- Use event streams when available
- Batch operations where possible
- Monitor network traffic

### Battery Life
- Adjust sensor sensitivity
- Optimize refresh intervals
- Monitor battery levels
- Replace batteries proactively

---

*This documentation covers all device handlers in the Advanced Hue Bridge Integration package. For specific implementation details, refer to the individual device handler files and their inline documentation.* 