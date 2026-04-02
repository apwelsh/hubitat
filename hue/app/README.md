# Advanced Hue Bridge Integration - Parent Application

## File: `hue-bridge-integration.groovy`

### Overview

This is the main parent application for the Advanced Hue Bridge Integration package. It serves as the central orchestrator for discovering, linking, and managing Philips Hue Bridges and their associated devices within the Hubitat ecosystem.

**Version:** 1.6.8  
**Size:** ~102KB, 2,970 lines  
**Dependencies:** HueFunctions library, Hubitat helper libraries

### Purpose

The parent application provides:
- Automatic discovery of Hue Bridges on the network
- Secure linking and authentication with Hue Bridges
- Device discovery and installation (lights, groups, scenes, sensors)
- Real-time state synchronization between Hubitat and Hue
- Event processing and translation
- User interface for configuration and management

### Key Features

#### 1. Bridge Discovery
- **SSDP Protocol:** Uses Simple Service Discovery Protocol to find Hue Bridges
- **Network Scanning:** Automatically scans the local network for Hue devices
- **Bridge Verification:** Validates discovered bridges and their capabilities
- **Multiple Bridge Support:** Can manage multiple Hue Bridges simultaneously

#### 2. Bridge Linking
- **Secure Authentication:** Establishes secure connections using Hue API
- **Link Button Support:** Guides users through the physical link button process
- **Connection Monitoring:** Continuously monitors bridge connectivity
- **Auto-Reconnection:** Automatically reconnects when connections are lost

#### 3. Device Management
- **Light Discovery:** Finds and lists all available Hue lights
- **Group Management:** Discovers rooms, zones, and areas
- **Scene Support:** Identifies and manages Hue scenes
- **Sensor Integration:** Supports motion, light, temperature, and switch sensors
- **Device Installation:** Creates Hubitat child devices for each Hue device

#### 4. State Synchronization
- **Bidirectional Sync:** Maintains state consistency between Hubitat and Hue
- **Event Stream Processing:** Handles real-time updates from Hue Bridges
- **Polling Fallback:** Uses polling when event streams are unavailable
- **Conflict Resolution:** Handles state conflicts between platforms

### Architecture

#### State Management
```groovy
@Field static Map atomicStateByDeviceId = new ConcurrentHashMap()
@Field static Map atomicQueueByDeviceId = new ConcurrentHashMap()
@Field static Map refreshQueue = new ConcurrentHashMap()
```

The application uses thread-safe maps to manage:
- Device-specific state information
- Command queues for each device
- Refresh scheduling and coordination

#### Page Structure
The application provides multiple configuration pages:
- `PAGE_MAINPAGE` - Main dashboard
- `PAGE_BRIDGE_DISCOVERY` - Bridge discovery interface
- `PAGE_BRIDGE_LINKING` - Bridge linking process
- `PAGE_ADD_DEVICE` - Device addition interface
- `PAGE_FIND_LIGHTS/GROUPS/SCENES/SENSORS` - Device discovery pages
- `PAGE_ADD_LIGHTS/GROUPS/SCENES/SENSORS` - Device installation pages

### Core Functions

#### Bridge Discovery Functions
```groovy
def ssdpSubscribe()
def ssdpHandler(evt)
def verifyDevice(device)
def linkBridge(device)
```

#### Device Management Functions
```groovy
def enumerateLights()
def enumerateGroups()
def enumerateScenes()
def enumerateSensors()
def addLights(selectedLights)
def addGroups(selectedGroups)
def addScenes(selectedScenes)
def addSensors(selectedSensors)
```

#### State Management Functions
```groovy
def setDeviceState(device, state)
def getDeviceState(device)
def setHueProperty(device, properties)
def parseDeviceHandler(data)
```

#### Utility Functions
```groovy
def getAtomicState(device)
def getAtomicQueue(device)
def getDeviceForChild(child)
def getChildDeviceForMac(mac)
```

### Configuration Options

#### Discovery Settings
- **SSDP Timeout:** How long to wait for bridge discovery
- **Discovery Retries:** Number of discovery attempts
- **Network Interface:** Specific network interface for discovery

#### Linking Settings
- **Link Timeout:** Time window for pressing the link button
- **Auto-Link:** Automatically attempt linking when bridges are found
- **Link Verification:** Verify successful linking

#### Device Settings
- **Auto-Refresh:** Enable automatic state polling
- **Refresh Intervals:** How often to poll for updates
- **Event Stream:** Enable real-time event processing
- **Batch Operations:** Group multiple operations together

### Event Handling

#### SSDP Events
The application subscribes to SSDP discovery events to find Hue Bridges:
```groovy
def ssdpSubscribe() {
    subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:Basic:1", ssdpHandler)
}
```

#### Device Events
Processes events from child devices and translates them to Hue API calls:
```groovy
def deviceHandler(evt) {
    def device = evt.device
    def state = evt.value
    setDeviceState(device, state)
}
```

#### Bridge Events
Handles events from Hue Bridges and updates child devices:
```groovy
def bridgeEventHandler(evt) {
    def data = evt.data
    def device = getDeviceForEvent(evt)
    updateDeviceState(device, data)
}
```

### Error Handling

#### Network Errors
- **Connection Timeouts:** Retry with exponential backoff
- **Bridge Unreachable:** Mark as offline and attempt reconnection
- **API Errors:** Log errors and notify user

#### Device Errors
- **Invalid Device IDs:** Skip and continue with valid devices
- **State Conflicts:** Resolve conflicts based on timestamp
- **Missing Devices:** Remove orphaned device references

#### User Interface Errors
- **Invalid Input:** Validate user input and provide feedback
- **Configuration Errors:** Guide users through correct setup
- **Permission Errors:** Explain required permissions

### Performance Considerations

#### Memory Management
- **State Cleanup:** Periodically clean up unused state data
- **Queue Management:** Limit queue sizes to prevent memory issues
- **Device Limits:** Support for large numbers of devices

#### Network Optimization
- **Batch Requests:** Group multiple API calls together
- **Connection Pooling:** Reuse HTTP connections
- **Rate Limiting:** Respect Hue API rate limits

#### CPU Usage
- **Event Processing:** Efficient event handling and filtering
- **Polling Optimization:** Smart polling based on device activity
- **Background Processing:** Move heavy operations to background

### Troubleshooting

#### Common Issues

1. **Bridge Not Found**
   - Check network connectivity
   - Verify bridge is powered and connected
   - Check firewall settings
   - Try manual IP address entry

2. **Linking Fails**
   - Ensure link button is pressed within timeout
   - Check bridge firmware version
   - Verify network connectivity
   - Try unlinking and relinking

3. **Devices Not Updating**
   - Check auto-refresh settings
   - Verify event stream connectivity
   - Review log messages for errors
   - Test manual refresh

4. **Performance Issues**
   - Reduce refresh intervals
   - Disable auto-refresh for stable devices
   - Check hub resource usage
   - Monitor network traffic

#### Debug Options
- **Logging Levels:** Enable debug logging for detailed information
- **Event Tracing:** Track event flow through the system
- **State Inspection:** Examine device state data
- **Network Monitoring:** Monitor API calls and responses

### Integration Points

#### Hubitat Integration
- **Child Device Creation:** Creates and manages child devices
- **Event Publishing:** Publishes events to Hubitat event system
- **State Management:** Manages device state in Hubitat
- **User Interface:** Provides configuration interface

#### Hue API Integration
- **REST API Calls:** HTTP requests to Hue Bridge API
- **Event Stream Processing:** Real-time event handling
- **Authentication:** Secure API key management
- **Error Handling:** API error processing and recovery

#### External Libraries
- **HueFunctions:** Color conversion and utility functions
- **Hubitat Helpers:** Platform-specific helper functions
- **Groovy Libraries:** Standard Groovy functionality

### Security Considerations

#### API Key Management
- **Secure Storage:** API keys stored securely in Hubitat
- **Key Rotation:** Support for API key updates
- **Access Control:** Limited access to bridge configuration

#### Network Security
- **Local Network Only:** All communication on local network
- **No External Calls:** No data sent to external services
- **Encrypted Communication:** HTTPS for API communication

#### Data Privacy
- **Local Processing:** All data processed locally
- **No Data Collection:** No user data collected or transmitted
- **Minimal Logging:** Limited logging of sensitive information

### Future Enhancements

#### Planned Features
- **Multi-Bridge Load Balancing:** Distribute load across multiple bridges
- **Advanced Scene Management:** Enhanced scene creation and editing
- **Device Templates:** Pre-configured device templates
- **Advanced Scheduling:** Sophisticated scheduling capabilities

#### Performance Improvements
- **Event Stream Optimization:** Improved real-time event handling
- **Memory Optimization:** Reduced memory footprint
- **Network Efficiency:** Optimized API communication
- **Background Processing:** Enhanced background task management

---

*This documentation provides a comprehensive overview of the parent application. For specific implementation details, refer to the source code comments and inline documentation.* 