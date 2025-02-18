library (
    author: "Armand Welsh",
    category: "utilities",
    description: "Library of functions for interfacing with Hue",
    name: "HueFunctions",
    namespace: "apwelsh",
    documentationLink: ""
)

import hubitat.helper.ColorUtils
import java.math.RoundingMode

import java.math.RoundingMode

// -------------------------
// Helper Functions
// -------------------------

/**
 * Clamp a value between min and max.
 */
BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
    return value.max(min).min(max)
}

/**
 * Convert gamut map to a list of BigDecimal points.
 */
List<List<BigDecimal>> parseGamut(Map<String, Map<String, Double>> gamut) {
    return gamut.collect { k, v -> [new BigDecimal(v.x.toString()), new BigDecimal(v.y.toString())] }
}

/**
 * Check if a given (x, y) point is inside the gamut triangle.
 */
boolean isInsideGamut(BigDecimal x, BigDecimal y, List<List<BigDecimal>> gamut) {
    BigDecimal Xr = gamut[0][0], Yr = gamut[0][1]
    BigDecimal Xg = gamut[1][0], Yg = gamut[1][1]
    BigDecimal Xb = gamut[2][0], Yb = gamut[2][1]
    BigDecimal detT = (Xg - Xr) * (Yb - Yr) - (Xb - Xr) * (Yg - Yr)
    BigDecimal alpha = ((x - Xr) * (Yb - Yr) - (y - Yr) * (Xb - Xr)) / detT
    BigDecimal beta = ((Xg - Xr) * (y - Yr) - (Yg - Yr) * (x - Xr)) / detT
    BigDecimal gamma = BigDecimal.ONE - alpha - beta
    return (alpha >= BigDecimal.ZERO && beta >= BigDecimal.ZERO && gamma >= BigDecimal.ZERO)
}

/**
 * Clamp an (x, y) point to the closest point inside the light's gamut triangle.
 */
List<BigDecimal> clampXYtoGamut(BigDecimal x, BigDecimal y, List<List<BigDecimal>> gamut) {
    if (isInsideGamut(x, y, gamut)) {
        return [x, y]
    }
    List<List<BigDecimal>> edges = [[gamut[0], gamut[1]], [gamut[1], gamut[2]], [gamut[2], gamut[0]]]
    List<BigDecimal> closestPoint = gamut[0]
    BigDecimal minDist = BigDecimal.valueOf(Double.MAX_VALUE)
    for (List<BigDecimal> edge : edges) {
        List<BigDecimal> A = edge[0], B = edge[1]
        BigDecimal Ax = A[0], Ay = A[1], Bx = B[0], By = B[1]
        BigDecimal t = ((x - Ax) * (Bx - Ax) + (y - Ay) * (By - Ay)) /
                      ((Bx - Ax).pow(2) + (By - Ay).pow(2))
        t = clamp(t, BigDecimal.ZERO, BigDecimal.ONE)
        BigDecimal Px = Ax + t * (Bx - Ax)
        BigDecimal Py = Ay + t * (By - Ay)
        BigDecimal dist = (x - Px).pow(2) + (y - Py).pow(2)
        if (dist < minDist) {
            minDist = dist
            closestPoint = [Px, Py]
        }
    }
    return closestPoint
}

/**
 * Convert RGB to Hue & Saturation.
 * Returns [hue (0–100), saturation (0–100)].
 */
List<BigDecimal> rgbToHS(BigDecimal r, BigDecimal g, BigDecimal b) {
    BigDecimal max = [r, g, b].max()
    BigDecimal min = [r, g, b].min()
    BigDecimal delta = max - min
    BigDecimal hue = BigDecimal.ZERO
    if (delta.compareTo(BigDecimal.ZERO) != 0) {
        if (max == r) {
            hue = ((g - b) / delta).remainder(BigDecimal.valueOf(6))
        } else if (max == g) {
            hue = ((b - r) / delta).add(BigDecimal.valueOf(2))
        } else {
            hue = ((r - g) / delta).add(BigDecimal.valueOf(4))
        }
        hue = hue.multiply(BigDecimal.valueOf(60))
        if (hue.compareTo(BigDecimal.ZERO) < 0) {
            hue = hue.add(BigDecimal.valueOf(360))
        }
    }
    // Scale hue from degrees (0–360) to 0–100.
    hue = hue.divide(BigDecimal.valueOf(360), 10, RoundingMode.HALF_UP)
             .multiply(BigDecimal.valueOf(100))
    BigDecimal saturation = (max.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO :
        delta.divide(max, 10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
    return [hue, saturation]
}

/**
 * Convert HSV (hue, saturation, brightness on a 0–100 scale) to RGB (0–255 scale).
 * Hue is first scaled from 0–100 to degrees.
 */
List<Integer> hsvToRGB(double hue, double sat, double bri) {
    double H = hue * 3.6   // Convert 0–100 to 0–360.
    double S = sat / 100.0
    double V = bri / 100.0
    double C = V * S
    double X = C * (1 - Math.abs(((H / 60.0) % 2) - 1))
    double m = V - C
    double r = 0, g = 0, b = 0
    if (H < 60) {
        r = C; g = X; b = 0
    } else if (H < 120) {
        r = X; g = C; b = 0
    } else if (H < 180) {
        r = 0; g = C; b = X
    } else if (H < 240) {
        r = 0; g = X; b = C
    } else if (H < 300) {
        r = X; g = 0; b = C
    } else {
        r = C; g = 0; b = X
    }
    int R = (int)Math.round((r + m) * 255)
    int G = (int)Math.round((g + m) * 255)
    int B = (int)Math.round((b + m) * 255)
    return [R, G, B]
}

/**
 * Inverse sRGB gamma correction.
 * Converts a gamma‐corrected channel (in [0,1]) back to linear light.
 */
double inverseGamma(double channel) {
    if (channel <= 0.04045) {
        return channel / 12.92
    } else {
        return Math.pow((channel + 0.055) / 1.055, 2.4)
    }
}

// -------------------------
// Hue Correction via Lagrange Interpolation
// -------------------------

/**
 * Applies Lagrange interpolation to compute the corrected hue.
 * Uses static intended calibration values:
 *   Red: 0, Yellow: 17, Green: 33.3, Cyan: 50, Blue: 66.6, Magenta: 84.
 *
 * @param rawHue   The raw hue value (0–100).
 * @param rawCalib List of raw hue calibration values (0–100) for the six points.
 * @return         Corrected hue.
 */
double applyHueCorrectionLagrange(double rawHue, List<Double> rawCalib) {
    List<Double> intendedCalib = [0.0, 17.0, 33.3, 50.0, 66.6, 84.0]
    int n = rawCalib.size()
    double result = 0.0
    for (int i = 0; i < n; i++) {
        double term = intendedCalib[i]
        for (int j = 0; j < n; j++) {
            if (i != j) {
                term *= (rawHue - rawCalib[j]) / (rawCalib[i] - rawCalib[j])
            }
        }
        result += term
    }
    return result
}

/**
 * Inverts the hue correction function.
 * Given a corrected hue (0–100), finds the raw hue (0–100) such that
 * applyHueCorrectionLagrange(rawHue, rawCalib) approximates the corrected hue.
 * Assumes the mapping is monotonic.
 */
double invertHueCorrection(double correctedHue, List<Double> rawCalib) {
    double low = 0.0, high = 100.0, mid = 0.0
    double tol = 0.01
    int iterations = 0, maxIter = 100
    while ((high - low) > tol && iterations < maxIter) {
        mid = (low + high) / 2.0
        double test = applyHueCorrectionLagrange(mid, rawCalib)
        if (test < correctedHue) {
            low = mid
        } else {
            high = mid
        }
        iterations++
    }
    return mid
}

// -------------------------
// Inverse Philips Conversion: Linear RGB -> XYZ
// -------------------------
/**
 * Converts linear RGB values (each in [0,1]) to XYZ using the inverse Philips matrix.
 * The inverse matrix here is an approximation.
 */
List<Double> linearRGBtoXYZ(double r_lin, double g_lin, double b_lin) {
    // Forward Philips matrix:
    //   r = 1.612*X - 0.203*Y - 0.302*Z
    //   g = -0.509*X + 1.412*Y + 0.066*Z
    //   b = 0.026*X - 0.072*Y + 0.962*Z
    // An approximate inverse is:
    double m00 = 0.6496, m01 = 0.1034, m02 = 0.1970
    double m10 = 0.2340, m11 = 0.7430, m12 = 0.0226
    double m20 = -0.00003, m21 = 0.0529, m22 = 1.0363
    double X = m00 * r_lin + m01 * g_lin + m02 * b_lin
    double Y = m10 * r_lin + m11 * g_lin + m12 * b_lin
    double Z = m20 * r_lin + m21 * g_lin + m22 * b_lin
    return [X, Y, Z]
}

// -------------------------
// Forward Conversion: XY+Bri -> Corrected HSV
// -------------------------
/**
 * Convert Philips Hue XY and brightness values to corrected HSV (0–100 scale).
 * Returns a list: [corrected hue, saturation, brightness].
 * Brightness is passed through.
 *
 * This function performs gamut clamping, Philips conversion (XYZ→RGB + gamma correction),
 * then computes raw HSV and applies hue correction via Lagrange interpolation.
 *
 * @param x        The x chromaticity coordinate.
 * @param y        The y chromaticity coordinate.
 * @param bri      The brightness value (0–100).
 * @param gamut    A map defining the light's gamut with keys "red", "green", and "blue".
 * @param rawCalib List<Double> of raw hue calibration values (0–100 scale) for six points.
 * @return         A list [corrected hue, saturation, brightness] (all on 0–100 scale).
 */
List<Double> xyToHSV(Double x, Double y, Double bri, Map<String, Map<String, Double>> gamut, List<Double> rawCalib) {
    // Convert inputs to BigDecimal.
    BigDecimal X = new BigDecimal(x.toString())
    BigDecimal Y = new BigDecimal(y.toString())
    BigDecimal brightnessPct = new BigDecimal(bri.toString())

    // Clamp XY to the gamut.
    List<List<BigDecimal>> gamutBD = parseGamut(gamut)
    List<BigDecimal> clampedXY = clampXYtoGamut(X, Y, gamutBD)
    BigDecimal clampedX = clampedXY[0]
    BigDecimal clampedY = clampedXY[1]

    // Normalize brightness from 0–100 to 0–1.
    BigDecimal briNorm = brightnessPct.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)

    // Compute XYZ (Y set by brightness).
    BigDecimal Yval = briNorm
    BigDecimal Xval = (Yval.divide(clampedY, 10, RoundingMode.HALF_UP)).multiply(clampedX)
    BigDecimal Zval = (Yval.divide(clampedY, 10, RoundingMode.HALF_UP))
                        .multiply(BigDecimal.ONE.subtract(clampedX).subtract(clampedY))

    // Convert XYZ to linear RGB using Philips coefficients.
    BigDecimal r = Xval.multiply(BigDecimal.valueOf(1.612))
                      .subtract(Yval.multiply(BigDecimal.valueOf(0.203)))
                      .subtract(Zval.multiply(BigDecimal.valueOf(0.302)))
    BigDecimal g = Xval.multiply(BigDecimal.valueOf(-0.509))
                      .add(Yval.multiply(BigDecimal.valueOf(1.412)))
                      .add(Zval.multiply(BigDecimal.valueOf(0.066)))
    BigDecimal bVal = Xval.multiply(BigDecimal.valueOf(0.026))
                         .subtract(Yval.multiply(BigDecimal.valueOf(0.072)))
                         .add(Zval.multiply(BigDecimal.valueOf(0.962)))
    // Clamp negatives.
    r = r.max(BigDecimal.ZERO)
    g = g.max(BigDecimal.ZERO)
    bVal = bVal.max(BigDecimal.ZERO)
    // Normalize if any channel > 1.
    BigDecimal maxRGB = [r, g, bVal].max()
    if (maxRGB.compareTo(BigDecimal.ONE) > 0) {
         r = r.divide(maxRGB, 10, RoundingMode.HALF_UP)
         g = g.divide(maxRGB, 10, RoundingMode.HALF_UP)
         bVal = bVal.divide(maxRGB, 10, RoundingMode.HALF_UP)
    }
    // Apply sRGB gamma correction.
    def gammaCorrect = { BigDecimal channel ->
        if (channel.compareTo(BigDecimal.valueOf(0.0031308)) > 0) {
            return BigDecimal.valueOf(1.055 * Math.pow(channel.doubleValue(), 1.0/2.4) - 0.055)
        } else {
            return channel.multiply(BigDecimal.valueOf(12.92))
        }
    }
    r = gammaCorrect(r)
    g = gammaCorrect(g)
    bVal = gammaCorrect(bVal)
    r = clamp(r, BigDecimal.ZERO, BigDecimal.ONE)
    g = clamp(g, BigDecimal.ZERO, BigDecimal.ONE)
    bVal = clamp(bVal, BigDecimal.ZERO, BigDecimal.ONE)
    
    // Convert RGB to raw hue and saturation.
    List<BigDecimal> hs = rgbToHS(r, g, bVal)
    double rawHue = hs[0].doubleValue() // on 0–100 scale.
    double saturation = hs[1].doubleValue()
    
    // Apply hue correction.
    double correctedHue = applyHueCorrectionLagrange(rawHue, rawCalib)
    correctedHue = Math.max(0, Math.min(100, correctedHue))
    
    return [correctedHue, saturation, brightnessPct.doubleValue()]
}

// -------------------------
// Reverse Conversion: Corrected HSV -> XY+Bri
// -------------------------
/**
 * Convert corrected HSV (0–100 scale) to XY and brightness.
 * Inverts the hue correction via binary search, converts raw HSV to RGB,
 * applies inverse gamma correction, inverts Philips conversion, and computes chromaticity.
 *
 * @param correctedHue Corrected hue (0–100).
 * @param sat          Saturation (0–100).
 * @param bri          Brightness (0–100).
 * @param rawCalib     List<Double> of raw hue calibration values (0–100) for six points.
 * @return             A list [x, y, brightness].
 */
List<Double> hsvToXY(double correctedHue, double sat, double bri, List<Double> rawCalib) {
    // 1. Invert hue correction to obtain raw hue.
    double rawHue = invertHueCorrection(correctedHue, rawCalib)
    
    // 2. Convert raw HSV to RGB.
    List<Integer> rgb = hsvToRGB(rawHue, sat, bri)  // RGB in 0–255.
    double R = rgb[0] / 255.0
    double G = rgb[1] / 255.0
    double B = rgb[2] / 255.0
    
    // 3. Inverse gamma correction.
    double r_lin = inverseGamma(R)
    double g_lin = inverseGamma(G)
    double b_lin = inverseGamma(B)
    
    // 4. Convert linear RGB to XYZ using inverse Philips matrix.
    List<Double> xyz = linearRGBtoXYZ(r_lin, g_lin, b_lin)
    double X = xyz[0], Y = xyz[1], Z = xyz[2]
    double sum = X + Y + Z
    if (sum == 0) sum = 1e-6
    double x = X / sum
    double y = Y / sum
    
    return [x, y, bri]
}


/**
* Ensures a value is between a minimum and maximum range.
*
* @param value The value to check.
* @param min The minimum allowable value.
* @param max The maximum allowable value.
* @return The value clamped between the min and max.
*/
static Number valueBetween(Number value, Number min, Number max) {
    return Math.max(min, Math.min(max, value))
}

/**
* Converts a hexadecimal string to an IP address.
*
* @param hex The hexadecimal string to convert.
* @return The IP address as a string.
*/
static String convertHexToIP(String hex) {
    if (hex == null || hex.length() != 8) {
        throw new IllegalArgumentException("Invalid hex string")
    }
    return [
        Integer.parseInt(hex.substring(0, 2), 16),
        Integer.parseInt(hex.substring(2, 4), 16),
        Integer.parseInt(hex.substring(4, 6), 16),
        Integer.parseInt(hex.substring(6, 8), 16)
    ].join('.')
}

/**
* Converts a hexadecimal string to an integer.
*
* @param hex The hexadecimal string to convert.
* @return The integer value.
*/
static Integer convertHexToInt(String hex) {
    if (hex == null) {
        throw new IllegalArgumentException("Invalid hex string")
    }
    return Integer.parseInt(hex, 16)
}

/**
* Converts a hue level value to a percentage.
*
* This function takes a hue level value and converts it to a percentage
* by dividing the value by 2.54 and rounding the result. The resulting
* value is then constrained to be between 1 and 100, except when the
* input value is 0, in which case the result is 0.
*
* @param value The hue level value to convert.
* @return The converted percentage value, constrained between 1 and 100,
*         or 0 if the input value is 0.
*/
static Integer convertFromHueLevel(Number value) {
    valueBetween(Math.round(value / 2.54), (value == 0 ? 0 : 1), 100)
}

/**
* Converts a given value to a Hue level.
*
* This function takes an integer value, multiplies it by 2.54, rounds the result,
* and ensures it is within the range of 0 to 254.
*
* @param value The integer value to be converted.
* @return The converted Hue level as an integer.
*/
static Integer convertToHueLevel(Integer value) {
    valueBetween(Math.round(value * 2.54), 0, 254)
}

/**
* Converts a hue value from the Hue system to a percentage.
*
* The Hue system uses a range of 0 to 65535 for hue values. This function
* converts that range to a percentage (0 to 100).
*
* @param value The hue value from the Hue system (0 to 65535).
* @return The corresponding percentage value (0 to 100).
*/
static Number convertFromHueHue(Number value) {
    Math.round(value / 655.35)
}

/**
* Converts a given value to a corresponding Hue hue value.
*
* The function performs the following conversions:
* - If the input value is 33, it returns 21845.
* - If the input value is 66, it returns 43690.
* - Otherwise, it scales the input value to a range between 0 and 65535.
*
* @param value The input value to be converted.
* @return The corresponding Hue hue value.
*/
static Number convertToHueHue(Number value) {
    value == 33 ? 21845 : value == 66 ? 43690 : valueBetween(Math.round(value * 655.35), 0, 65535)
}

/**
* Converts a given hue saturation value to a different scale.
*
* This function takes a hue saturation value and converts it by dividing it by 2.54
* and rounding the result to the nearest whole number.
*
* @param value The hue saturation value to be converted.
* @return The converted value as a Number.
*/
static Number convertFromHueSaturation(Number value) {
    Math.round(value / 2.54)
}

/**
* Converts a given value to a Hue-compatible saturation value.
*
* This function takes a numerical value, multiplies it by 2.54, rounds it to the nearest whole number,
* and then ensures the result is within the range of 0 to 254.
*
* @param value The numerical value to be converted.
* @return The converted Hue-compatible saturation value, constrained between 0 and 254.
*/
static Number convertToHueSaturation(Number value) {
    valueBetween(Math.round(value * 2.54), 0, 254)
}

/**
* Converts a Hue color temperature value to a standard color temperature value.
*
* @param value The Hue color temperature value to convert.
* @return The converted color temperature value, constrained between 2000 and 6500.
*/
static Number convertFromHueColorTemp(Number value) {
    // 4500 / 347 = 12.968 (but 12.96 scales better)
    valueBetween(Math.round(((500 - value) * 12.96) + 2000 ), 2000, 6500)
}

/**
* Converts a given color temperature value to the corresponding Hue color temperature value.
*
* @param value The color temperature value to convert (in Kelvin).
* @return The converted Hue color temperature value, constrained between 153 and 500.
*/
static Number convertToHueColortemp(Number value) {
    valueBetween(Math.round(500 - ((value - 2000) / 12.96)), 153, 500)
}

/**
* Converts a given light level to a Hue light level.
*
* This function takes a light level value and converts it to a Hue-compatible light level
* using a logarithmic scale. The conversion formula is:
* 
*     HueLightLevel = 10 ^ ((lightLevel - 1) / 10000.0)
*
* If the input light level is null, it defaults to 1.
*
* @param lightLevel The light level to be converted. If null, defaults to 1.
* @return The converted Hue light level as an integer.
*/
static Number convertToHueLightLevel(Number lightLevel) {
    Math.pow(10, (((lightLevel?:1)-1)/10000.0)) as int
}

/**
* Converts the given temperature to the Hue temperature scale.
*
* @param temperature The temperature value to be converted.
* @return The converted temperature value, scaled to one decimal place.
*/
static Number convertFromHueTemperature(Number temperature) {
    return (temperature?:0) / 100
}

/**
* Converts a Hue color mode value to a corresponding string representation.
*
* @param value The Hue color mode value to convert. Expected values are 'hs', 'ct', or 'xy'.
* @return A string representing the converted color mode. Returns 'RGB' for 'hs' and 'xy', 'CT' for 'ct', 
*         and an empty string for any other value.
*/
static String convertFromHueColorMode(String value) {
    if (value == 'hs') { return 'RGB'  }
    if (value == 'ct') { return 'CT' }
    if (value == 'xy') { return 'RGB' }
    return ''
}

