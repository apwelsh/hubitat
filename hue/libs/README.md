# Advanced Hue Bridge Integration - Utility Library

## File: `HueFunctions.groovy`

### Overview

The HueFunctions library provides a comprehensive set of utility functions for color conversion, gamut mapping, and Hue API interactions. This library is shared across all components of the Advanced Hue Bridge Integration package to ensure consistent color handling and mathematical operations.

**Version:** 1.0.0  
**Size:** ~19KB, 537 lines  
**Dependencies:** Hubitat ColorUtils, Java Math libraries

### Purpose

The library serves as a centralized repository for:
- Color space conversions (RGB, HSV, XY, CIE)
- Gamut mapping and color correction
- Mathematical utilities for color calculations
- Hue-specific color transformations
- Precision arithmetic operations

### Key Features

#### 1. Color Space Conversions
- **RGB ↔ HSV:** Convert between RGB and Hue/Saturation/Value
- **RGB ↔ XY:** Convert between RGB and CIE 1931 XY coordinates
- **HSV ↔ XY:** Convert between HSV and XY coordinates
- **Color Temperature:** Convert between Kelvin and RGB/XY

#### 2. Gamut Management
- **Gamut Parsing:** Parse Hue device gamut definitions
- **Gamut Validation:** Check if colors are within device gamut
- **Gamut Clamping:** Clamp colors to valid gamut boundaries
- **Multi-Gamut Support:** Support for different color gamuts

#### 3. Mathematical Utilities
- **Precision Arithmetic:** High-precision decimal calculations
- **Value Clamping:** Clamp values to valid ranges
- **Rounding Control:** Configurable rounding modes
- **Error Handling:** Robust error handling for edge cases

### Function Reference

#### Core Utility Functions

##### `clamp(BigDecimal value, BigDecimal min, BigDecimal max)`
Clamps a value between minimum and maximum bounds.
```groovy
// Example: Clamp brightness between 0 and 100
BigDecimal brightness = clamp(level, 0, 100)
```

##### `parseGamut(Map<String, Map<String, Double>> gamut)`
Parses a gamut definition into a list of XY coordinate points.
```groovy
// Example: Parse Hue bulb gamut
List<List<BigDecimal>> gamut = parseGamut(deviceGamut)
```

#### Gamut Management Functions

##### `isInsideGamut(BigDecimal x, BigDecimal y, List<List<BigDecimal>> gamut)`
Checks if a given XY point is inside the gamut triangle.
```groovy
// Example: Check if color is within device gamut
boolean isValid = isInsideGamut(x, y, deviceGamut)
```

##### `clampXYtoGamut(BigDecimal x, BigDecimal y, List<List<BigDecimal>> gamut)`
Clamps an XY point to the closest valid point within the gamut.
```groovy
// Example: Clamp color to valid gamut
List<BigDecimal> validColor = clampXYtoGamut(x, y, deviceGamut)
```

#### Color Conversion Functions

##### `rgbToHS(BigDecimal r, BigDecimal g, BigDecimal b)`
Converts RGB values to Hue and Saturation (0-100 scale).
```groovy
// Example: Convert RGB to HS
List<BigDecimal> hs = rgbToHS(255, 128, 64)
BigDecimal hue = hs[0]        // 0-100
BigDecimal saturation = hs[1] // 0-100
```

##### `hsToRGB(BigDecimal h, BigDecimal s, BigDecimal v)`
Converts Hue, Saturation, and Value to RGB values.
```groovy
// Example: Convert HS to RGB
List<BigDecimal> rgb = hsToRGB(30, 75, 100)
BigDecimal red = rgb[0]   // 0-255
BigDecimal green = rgb[1] // 0-255
BigDecimal blue = rgb[2]  // 0-255
```

##### `xyToRGB(BigDecimal x, BigDecimal y, BigDecimal brightness)`
Converts CIE 1931 XY coordinates to RGB values.
```groovy
// Example: Convert XY to RGB
List<BigDecimal> rgb = xyToRGB(0.5, 0.4, 100)
```

##### `rgbToXY(BigDecimal r, BigDecimal g, BigDecimal b)`
Converts RGB values to CIE 1931 XY coordinates.
```groovy
// Example: Convert RGB to XY
List<BigDecimal> xy = rgbToXY(255, 128, 64)
BigDecimal x = xy[0] // 0-1
BigDecimal y = xy[1] // 0-1
```

#### Advanced Color Functions

##### `xyToHSV(BigDecimal x, BigDecimal y, BigDecimal brightness)`
Converts XY coordinates to HSV color space.
```groovy
// Example: Convert XY to HSV
List<BigDecimal> hsv = xyToHSV(0.5, 0.4, 100)
BigDecimal hue = hsv[0]        // 0-100
BigDecimal saturation = hsv[1] // 0-100
BigDecimal value = hsv[2]      // 0-100
```

##### `hsvToXY(BigDecimal h, BigDecimal s, BigDecimal v)`
Converts HSV values to XY coordinates.
```groovy
// Example: Convert HSV to XY
List<BigDecimal> xy = hsvToXY(30, 75, 100)
```

##### `kelvinToRGB(BigDecimal kelvin)`
Converts color temperature in Kelvin to RGB values.
```groovy
// Example: Convert 2700K to RGB
List<BigDecimal> rgb = kelvinToRGB(2700)
```

##### `rgbToKelvin(BigDecimal r, BigDecimal g, BigDecimal b)`
Converts RGB values to approximate color temperature.
```groovy
// Example: Convert RGB to Kelvin
BigDecimal kelvin = rgbToKelvin(255, 200, 150)
```

### Color Gamut Support

#### Supported Gamuts
The library supports multiple color gamuts used by different Hue devices:

1. **Gamut A (Original):** First generation Hue bulbs
2. **Gamut B (Extended):** Second generation Hue bulbs
3. **Gamut C (Wide):** Third generation and newer Hue bulbs
4. **Custom Gamuts:** Device-specific gamut definitions

#### Gamut Definitions
```groovy
// Example gamut definitions
Map gamutA = [
    red: [x: 0.704, y: 0.296],
    green: [x: 0.2151, y: 0.7106],
    blue: [x: 0.138, y: 0.08]
]

Map gamutB = [
    red: [x: 0.675, y: 0.322],
    green: [x: 0.409, y: 0.518],
    blue: [x: 0.167, y: 0.04]
]

Map gamutC = [
    red: [x: 0.692, y: 0.308],
    green: [x: 0.17, y: 0.7],
    blue: [x: 0.153, y: 0.048]
]
```

### Precision and Accuracy

#### Decimal Precision
The library uses `BigDecimal` for all calculations to ensure high precision:
- **Rounding Mode:** `RoundingMode.HALF_UP` for consistent results
- **Scale:** 10 decimal places for internal calculations
- **Output Scale:** Appropriate scale for each color space

#### Error Handling
- **Input Validation:** Validates input ranges and types
- **Edge Case Handling:** Handles edge cases gracefully
- **Fallback Values:** Provides sensible defaults for invalid inputs
- **Error Logging:** Logs errors for debugging

### Performance Considerations

#### Optimization Features
- **Cached Calculations:** Reuses calculated values where possible
- **Efficient Algorithms:** Uses optimized mathematical algorithms
- **Memory Management:** Minimizes object creation
- **Batch Processing:** Supports batch color conversions

#### Resource Usage
- **CPU:** Optimized for minimal CPU usage
- **Memory:** Efficient memory usage with BigDecimal
- **Network:** No network calls (pure mathematical operations)

### Integration with Hue API

#### Hue Color Format
The library handles Hue's specific color format requirements:
- **XY Coordinates:** Primary color format for Hue API
- **Brightness:** Separate brightness control
- **Color Temperature:** White temperature control
- **Transition Times:** Smooth color transitions

#### API Compatibility
- **v1 API:** Full support for legacy Hue API
- **v2 API:** Enhanced support for newer API features
- **Event Stream:** Compatible with real-time updates
- **Batch Operations:** Supports bulk color operations

### Usage Examples

#### Basic Color Conversion
```groovy
// Convert RGB to Hue-compatible XY
List<BigDecimal> rgb = [255, 128, 64]
List<BigDecimal> xy = rgbToXY(rgb[0], rgb[1], rgb[2])
BigDecimal brightness = 100

// Send to Hue device
Map colorData = [
    xy: [xy[0], xy[1]],
    bri: brightness
]
```

#### Gamut-Aware Color Setting
```groovy
// Get device gamut
List<List<BigDecimal>> gamut = parseGamut(deviceGamut)

// Convert desired color to XY
List<BigDecimal> desiredXY = rgbToXY(255, 0, 0)

// Check if color is within gamut
if (!isInsideGamut(desiredXY[0], desiredXY[1], gamut)) {
    // Clamp to valid gamut
    desiredXY = clampXYtoGamut(desiredXY[0], desiredXY[1], gamut)
}

// Use clamped color
Map colorData = [xy: [desiredXY[0], desiredXY[1]]]
```

#### Color Temperature Control
```groovy
// Convert Kelvin to RGB
List<BigDecimal> rgb = kelvinToRGB(2700)

// Convert to XY for Hue
List<BigDecimal> xy = rgbToXY(rgb[0], rgb[1], rgb[2])

// Set warm white color
Map colorData = [xy: [xy[0], xy[1]]]
```

### Testing and Validation

#### Test Cases
The library includes comprehensive test coverage for:
- **Color Conversions:** All conversion functions
- **Gamut Operations:** Gamut validation and clamping
- **Edge Cases:** Boundary conditions and invalid inputs
- **Precision:** Accuracy of mathematical operations

#### Validation Methods
- **Cross-Validation:** Verify conversions in both directions
- **Known Values:** Test against known color values
- **Gamut Boundaries:** Test gamut edge cases
- **Performance:** Measure calculation speed

### Error Handling

#### Common Errors
1. **Invalid Input Ranges:** Values outside expected ranges
2. **Null Values:** Missing or null input parameters
3. **Invalid Gamut:** Malformed gamut definitions
4. **Precision Errors:** Mathematical precision issues

#### Error Recovery
- **Input Validation:** Validate inputs before processing
- **Default Values:** Provide sensible defaults
- **Error Logging:** Log errors for debugging
- **Graceful Degradation:** Continue operation when possible

### Future Enhancements

#### Planned Features
- **Additional Color Spaces:** Support for LAB, LUV, etc.
- **Advanced Gamut Mapping:** Improved gamut mapping algorithms
- **Color Profiles:** Device-specific color profiles
- **Batch Processing:** Enhanced batch color operations

#### Performance Improvements
- **Algorithm Optimization:** Faster mathematical algorithms
- **Memory Optimization:** Reduced memory footprint
- **Caching:** Intelligent result caching
- **Parallel Processing:** Multi-threaded operations

---

*This documentation covers the complete HueFunctions utility library. For specific implementation details, refer to the source code comments and inline documentation.* 