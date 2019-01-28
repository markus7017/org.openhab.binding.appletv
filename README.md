# Apple-TVBinding (org.openhab.binding.appletv)

This openHAB 2 Binding implements control for the Apple-TV devices. This includes sending remote keys (control the Apple-TV from openHAB). An upcoming version will also process the status updates to provide information about the media being played.

Author: markus7017
Check put https://community.openhab.org/t/binding-for-apple-tv/65397 for more information, questions and contributing ideas. Any comment is welcome!


## Supported Devices, Platforms

Apple-TV 3, latest firmware - fully supported, development environment
Apple-TV 4 - no information, should work
Apple-TV 2 - no information, won't expect to work

The binding requires a proper installation of Python 3. Open a terminal and run "python3 --version". You need Python 3.5.3 or newer. Currently macOS (10.12) and Linux (Raspberry) are supported.
Check https://realpython.com/installing-python/ for more information.

## Supported Things

device - represents an Apple-TV

# Discovery

Auto discovery is planned for an upcoming release. PyATV already supplies the "scan" and "pair" commands. For now you need to pair your device first (using atvremote cli) and create the thing manually in Paper UI.

## Binding Configuration

There are no textual configuration files.

## Thing Configuration

(update needed)

## Channels

(update needed)

## Full Example

(update needed)

## Notes

It integrates the PyATV Python library, which implements the protocol layer. The binding includes also platform specific stuff (jpy Java/Phyton bridge). All modules are included in the binding package and the binding tries to auto-select them. An upcoming version will allow to overwrite this auto-detection in case something went wrong or you have specific installation requirements.

Thanks postlund for his great work in contributing the PyATV library (https://github.com/postlund/pyatv) and the jpy team (https://github.com/bcdev/jpy).
