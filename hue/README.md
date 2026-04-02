# Advanced Hue Bridge Integration for Hubitat [![Donate](https://img.shields.io/badge/donate-PayPal-blue.svg?logo=paypal&style=plastic)](https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J)

The Advanced Hue Bridge Integration for Hubitat adds Hue **scenes**, **groups**, **lights**, and **sensors** with strong support for Hue’s **event stream** (push updates). It reduces Hubitat-side complexity by using **Hubitat Generic Component** drivers for bulbs and scene switches where possible, and picks an appropriate generic type per light (dimmer, CT, RGBW, RGB) so devices match real capabilities in dashboards and automations.

Current releases target **Hue Bridge API v2** for state synchronization; a **recent bridge firmware** is recommended so the event stream stays reliable. The integration also uses **SSDP** for discovery and can **follow the bridge when its IP changes** (unlink/relink or hub reboot helps apply new addressing).

Hue Play / **Hue Sync** style systems are not supported (no hardware to validate).

For deeper detail, see **[DOCUMENTATION.md](DOCUMENTATION.md)** (architecture and setup), **[QUICK_START.md](QUICK_START.md)** (step-by-step install), and **[API_REFERENCE.md](API_REFERENCE.md)** (methods, attributes, configuration).

**Package:** version in [packageManifest.json](packageManifest.json) (app/driver versions listed there). **Minimum Hubitat:** 2.2.9. **Community:** [Hubitat forum release thread](https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420).

---

## Repository layout

| Path | Purpose |
|------|---------|
| [app/hue-bridge-integration.groovy](app/hue-bridge-integration.groovy) | Parent app: discovery, pairing, device creation, Hue HTTP + event stream |
| [device/](device/) | Custom drivers (bridge, group, sensors) |
| [libs/HueFunctions.groovy](libs/HueFunctions.groovy) | Shared color / gamut utilities (library; required for manual installs) |
| [packageManifest.json](packageManifest.json) | Hubitat Package Manager manifest |
| [rest/hueTest.rest](rest/hueTest.rest) | Example REST calls for development/testing |

---

## Installation (summary)

1. Install via **Hubitat Package Manager** using this repo’s manifest, **or** manually paste the **parent app** and **each driver** you need (see [packageManifest.json](packageManifest.json)). The **[HueFunctions](libs/HueFunctions.groovy)** file is a **standalone Hubitat library** with the same color/gamut math as used in code; the shipping app **does not import it** (logic is inlined in the app), so you **do not** need to install the library for normal use.
2. Open the app, run **device discovery**, press the **link button** on the bridge when prompted, then add lights, groups, scenes, and sensors from the app UI.

Full steps: [QUICK_START.md](QUICK_START.md).

---

## Features

### [Advanced Hue Bridge Integration App](app/hue-bridge-integration.groovy)

- Main orchestrator: links the Hue bridge, enumerates resources, creates Hubitat devices, sends commands, and processes **SSE/event-stream** updates for near real-time state.
- Adds **lights** (Generic Component drivers), **groups**, **scenes**, and **supported sensors**.

### [Advanced Hue Bridge Device](device/advanced-hue-bridge.groovy)

- Created automatically after a successful bridge link.
- **Capabilities:** `Switch`, `Refresh`, `Initialize`; **commands:** `connect`, `disconnect`; **attribute:** `networkStatus`.
- Schedules / coordinates hub-level refresh; **Switch** can reflect “any light on” vs “all lights on” (preference).
- **Preferences:** auto refresh interval, **connection watchdog**, informational/debug logging.

### [Advanced Hue Group Device](device/advanced-hue-group.groovy)

- One device per imported Hue group/room/zone.
- **Capabilities** include `Light`, `Switch`, `SwitchLevel`, `ChangeLevel`, `ColorControl`, `ColorMode`, `ColorTemperature`, `Refresh`, `Initialize`, `Actuator`.
- Scene activation by **scene id** or **name**; optional **default scene** when the group turns on.
- Works with the app for state sync and refresh.

### Hue lights → Generic Hubitat drivers

Lights are added as **child devices of the app** using namespace `hubitat` **Generic Component** drivers (`isComponent: false`), so they appear as normal devices in Hubitat:

- **Generic Component Dimmer** — dimmable only  
- **Generic Component CT** — white color temperature  
- **Generic Component RGBW** — full color + white  
- **Generic Component RGB** — other color-capable lights  

### Hue scenes → Generic Component Switch

- Scenes appear as **Generic Component Switch** devices.
- **Momentary (trigger):** turn on activates the scene, then auto-off (~400 ms).  
- **Switch mode:** stays on until group/light state changes or another scene is activated; turning off can turn off the parent Hue group (see app behavior in [DOCUMENTATION.md](DOCUMENTATION.md)).

### [Advanced Hue Motion Sensor](device/advanced-hue-motion-sensor.groovy)

- **Capabilities:** `MotionSensor`, `Battery`, `Refresh`, `Sensor`.
- Motion, battery, refresh; **sensitivity** can be adjusted via Hue (app supports maintaining sensor config where applicable).

### [Advanced Hue Light Sensor](device/advanced-hue-light-sensor.groovy)

- **Capabilities:** `IlluminanceMeasurement`, `Battery`, `Refresh`, `Sensor`.

### [Advanced Hue Temperature Sensor](device/advanced-hue-temperature-sensor.groovy)

- **Capabilities:** `TemperatureMeasurement`, `Battery`, `Refresh`, `Sensor`.

### [Advanced Hue Dimmer Switch](device/advanced-hue-dimmer-sensor.groovy)

- **Capabilities:** `Battery`, `PushableButton`, `HoldableButton`, `ReleasableButton`, `Refresh`, `Initialize`.
- Hue dimmer-style multi-button events; attributes `status`, `health`.

### [Advanced Hue Tap switch](device/advanced-hue-tap-sensor.groovy)

- **Capabilities:** `Battery`, `PushableButton`, `Refresh`, `Initialize`.
- Tap button events; `status`, `health`.

### [Advanced Hue RunLessWires (Friends of Hue) switch](device/advanced-hue-runlesswires-sensor.groovy)

- **Capabilities:** `PushableButton`, `HoldableButton`, `Refresh`, `Initialize` (no battery reporting on this hardware path).
- Per-button option to enable **long press**; `status`, `health`.
- Contributed by Curtis Edge (@pocketgeek).

### [HueFunctions library](libs/HueFunctions.groovy)

- Standalone **gamut** and **XY / RGB / HSV** utilities (see [libs/README.md](libs/README.md)). Useful as a reference or for custom Groovy that wants the same conversions; the released integration **embeds equivalent logic** in the parent app rather than loading this library at runtime.

---

## Operational notes

- **Event stream:** Prefer bridge firmware that supports stable v2 **events** so sensors and lights update without aggressive polling. Optional **bridge auto-refresh** can remain conservative to limit hub load ([DOCUMENTATION.md](DOCUMENTATION.md) discusses watchdog and intervals).
- **Relink / IP changes:** If the bridge gets a new IP, discovery and relink logic can update binding; a hub reboot may be needed in some cases.
- **Logging:** Toggle informational/debug logging on the bridge device and sensor drivers when diagnosing issues.
- **Forum-backed issues:** Extra troubleshooting (metadata refresh / NPEs after upgrades, **`battery` missing on built-in Hue** for some dimmers, running **two** Hue integrations, **non-Philips Hue emulation**) is consolidated in [DOCUMENTATION.md — Troubleshooting](DOCUMENTATION.md#troubleshooting), including context from the [Hubitat release thread](https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420) (e.g. [post 504 on battery / Lutron Aurora](https://community.hubitat.com/t/release-advanced-hue-bridge-integration/51420/504)).

---

## Vision / future enhancements

The goal is tighter Hue + Hubitat integration:

- **Hue-integrated group manager** — custom Hue groups driven with single bridge calls even when mixing strategies with Hubitat groups.  
- **Hue-integrated scene manager** — scenes defined on Hue for smoother coordinated transitions; scope of sync with non-Hue Zigbee devices is TBD.

---

## Support the author

Please consider donating; this app took significant effort to build and maintain. Donations support further Hue-related development.

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/donate?hosted_button_id=XZXSPZWAABU8J)

---

## License

This project is licensed under the MIT License — see [LICENSE.md](LICENSE.md).
