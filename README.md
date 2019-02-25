# Apple-TVBinding (org.openhab.binding.appletv)

This openHAB 2 Binding implements control for the Apple-TV devices. This includes sending remote keys (control the Apple-TV from openHAB). An upcoming version will also process the status updates to provide information about the media being played.

Author: markus7017
Check put https://community.openhab.org/t/binding-for-apple-tv/65397 for more information, questions and contributing ideas. Any comment is welcome!

<hr>Release: alpha2, check master branch for stable release<hr>
<p>
Known issues:<br>
* On macOS Python 3.6 is used, need to change to Python 3.5<br>
* Channel remoteKey and keySequence needs to be renamed to command and commandSequence (keys are just one control element that can be send)<br>
* channel keySequence does not work yet when sending more than one command<br>
* Any other channel is currently not implemented<br>
* Support for Synology NAS (amd64) is not verified <br>
<p>
Please note:<br>
This is an alpha release, it has bugs, requires manual install etc. Questions, feedback and contributions are welcome (e.g. improving this documentation).<p>
<hr>

## Supported Devices, Platforms

Devices
* Apple-TV 3, latest firmware - fully supported, development environment
* Apple-TV 4 - no information, should work
* Apple-TV 2 - no information, won't expect to work

Platforms
* macOS - dev environment is Mojave, but should also work with Sierra and High Sierra
* Raspberyy with OpenHabian - default test environment
* Synology NAS is work in progress
* others currently not supported - contact author

## Supported Things

device - represents an Apple-TV device

# Discovery

Auto discovery is planned for an upcoming release. PyATV already supplies the "scan" and "pair" commands. For now you need to pair your device first (using atvremote cli) and create the thing manually in Paper UI.
* make sure all required packages have been installed
* run "atvremote scan" - this will show you the ip address of your Apple TV device
* run "atvremote pair -r openHAB" -  this will initialte the pairing process
* On your Appe TV screen go to Settings->General->Remotes
* you should see the openHAB remote - if not you need to restart the Apple TV
* select the "openHAB" entry - the Apple TV requests a pairing code
* enter "1234" - the process should be completed without an error
* you could terminate the "atvremote pair" command with Ctrl-C
* you should see the login id, which will be required for the thing configuration 

Proceed with Thing Configuration below

## Binding installation

For now the bindinng is not available on the Eclipse Smart Home Market Place nor part of the openHAB distribution so you need to install it manually.<p>

As described the binding integrates the Phyton-based PyATV project so you need to install Python 3.5 and the required modules:<p>
* Platform software packages:<br>
sudo apt-get update<br>
sudo apt-get install python3.5 python3-pip libpython3.5 python3-jpy<br>
sudo apt-get install avhi-utils<p>
Python 3.5 for macOS can be found here: https://www.python.org/downloads/mac-osx/<p>

* Python modules:<br>
sudo pip3.5 install pyatv zeroconf sh<p>
On macOS use Homebrew to install the additional Python modules.<p>
Make sure those modules go into the Python 3.5 folders if you have multiple versions installed (by using the pip3.5 command).<p>

* Verification
You should verify the installation before installing/configuring the binding:<br>
atvremote --address <ip address>  --login_id <login id from pairing>  top_menu<p>
should work without error messages and move the focus on the Apple-TV to the top menu.<p>

* The binding itself<br>
Copy the binding jar to openHAB's addons folder, add the thing in Paper UI (see below) and restart openHAB.<p>

## Binding Configuration

There are no textual configuration files.

## Thing Configuration

Before adding the thing make sure that all pre-requisites are met and all modules have been installed (see above).

You could use Paper UI to add a thing manually
* Go to Configuration->Things and click on '+'
* Select Apple TV Binding
* fill in the device's ip address and login id as discovered through the pairing process
* Once you save the configuration the thing should become online

## Channels

<table>
<tr><td>Thing</td><td>Description</td></tr>
<tr><td>remoteKey</td><td>A single command to be send to the Apple-TV - see below for a list of valid commands</td></tr>
<tr><td>keysSequence</td><td>A sequence of commands to be send to the Apple-TV</td></tr>
</table>
<p>

For now the following control commands can be used:<p>
Remote control commands:
 - down - Press key down
 - left - Press key left
 - menu - Press key menu
 - next - Press key next
 - pause - Press key play
 - play - Press key play
 - previous - Press key previous
 - right - Press key right
 - select - Press key select
 - set_position - Seek in the current playing media
 - set_repeat - Change repeat mode
 - set_shuffle - Change shuffle mode to on or off
 - stop - Press key stop
 - top_menu - Go to main menu (long press menu)
 - up - Press key up


## Full Example

* items:<p>
String Atv_Remote "ATV [%s]" {channel="appletv:device:34fc39d8:control#remoteKey"}<p>

* sitemape:<p>
Switch item=Atv_Remote mappings=[up = "^" ]<br>
Switch item=Atv_Remote mappings=[left = "<", select = "Sel", right = ">" ]<br>
Switch item=Atv_Remote mappings=[menu = "Menu", down = "  v   ", play = "Play" ]<br>
Switch item=Atv_Remote mappings=[previous='Prev', pause='Pause', next='Next']<p>

## Notes

It integrates the PyATV Python library, which implements the protocol layer. The binding includes also platform specific stuff (jpy Java/Phyton bridge). All modules are included in the binding package and the binding tries to auto-select them. An upcoming version will allow to overwrite this auto-detection in case something went wrong or you have specific installation requirements.

Thanks postlund for his great work in contributing the PyATV library (https://github.com/postlund/pyatv) and the jpy team (https://github.com/bcdev/jpy).
