# DirecTV-IP-Control-for-Hubitat
Hubitat driver for DirecTV IP control of receivers

    *  Change History:
    *
    *   Version Date        Who             What
    *   ----    ----        ---             ----
    *   1.0     2024-02-26  Roger Noia      Initial Release
    *
        
This driver uses the DirecTV SHEF API. As implemented, it only uses HTTP Gets, but the API permits other methods for the adventurous. This driver was created so I could use Hubitat to replace my now defunct Logitech Harmony remote and not use an IR-based remote. As I know nothing about Groovy but had a good knowledge of Perl and Javascript, I used this as an exercise to learn how to do what I needed.  I then took the extra step to document to the best of my ability so other non-developers won't have to go through what I did. (*note: I was able to functionally replicate most of what this driver does using Rule Machine itself)
        
<h2>What can you do</h2>
This driver permits local, direct control of a DirecTV receiver using DirecTV's own API that runs on the receiver itself. You can get on-demand access to information on the receiver
The following functions are available:
<ul>
	<li>Check the operating mode of the receiver</li>
	<li>Change the operating mode of the receiver (on or standby)</li>
	<li>Check the currently playing channel and program</li>
	<li>Change the channel</li>
	<li>Send "remote" buttons: here is what DirecTV provdies for: "power", "poweron", "poweroff", "format", "pause", "rew", "replay", "stop", "advance", "ffwd", "record", "play", "guide", "active", "list", "exit", "back", "menu", "info", "up", "down", "left", "right", "select", "red", "green", "yellow", "blue", "chanup", "chandown", "prev", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "dash", "enter"</li>
</ul>

<h2>What you can't do</h2>
<ul>
	<li>Anything involving the playlist as the DirecTV API no longer permits this</li>
	<li>Getting the current state of the receiver requires polling the receiver so it's not possible to have real-time, event-based triggers of what happens on the receiver.  Considering the rather sluggish performance of my DVR, spamming the DVR with requests to have up-to-the-second visiblity of what is playing isn't realistic or recommended.  It would probably make the already slow-performing receiver unresponsive.  Once a minute, sure that should be ok.    
</li>
</ul>
				
<h2>Limitations</h2>
<ol>
<li>If the receiver is "off" (meaning in standby) issuing some commands like getTuned throws a 500 server error on the DirecTV receiver itself.</li>
        
  <li> If the DirecTV receiver is shut off via remote or manual button press, this driver won't know about it until the getMode function checks. </li>

  <li>The DirecTV API responds with a "success" code upon receiving a command, but this doesn't mean it actually did what you asked - only that it received the command.  For commands like "tuneTo", there is a noticeable 1-2 second delay between it saying "success" and the channel actually changing, so any further actions need to be paused before another command is done. Commands like "getTuned" have a very fast response - milliseconds or even instantaneous</li>
	
 <li>The DirecTV receiver must (obviously) be connected to your home network, but Internet access isn't required - everything runs locally on the receiver</li>
 
 <li>The DirecTV receiver must have a static/fixed IP address</li>

<li> Not all program options, response data are currently utilized in this driver (such as program recorded times, duration). Refer to DirecTV SHEF IP control API to see what's available.
        https://blog.solidsignal.com/docs/DTV-MD-0359-DIRECTV_SHEF_Command_Set-V1.3.C.pdf </li>
        
<li>There are a few API commands that have been deprecated by DirecTV - the legacy "playlist" and "play" commands that would be used to play a recording for example so it's unlikely they will come back</li>

<li>The "getTuned" command used only shows what is _currently_ being watched (i.e. on the connected screen) so it is assumed that any useful commands (or any command involving a channel or what is being played) will not work against the headless Genie 2 Server (model HS17). I don't have this DVR, so don't know what happens</li>

<li>The DirecTV receiver needs to have external device access enabled (see below)</li>

<li>DirecTV may (does) charge for the "whole home dvr" service which this depends on - if you don't have whole home DVR service, it is unlikely your DVR will respond to your commands</li>
</ol>

<h2>Compatibility</h2>
        This was tested against:
<ul>
	<li>DirecTV Satellite HR-54 DVR</li>
	<li>C61 (R2 Genie Mini)</li>
</ul>

It's assumed to work with any other Genie Satellite receiver DVR that connects directly to a screen. 
        
It is unknown if this would work with a DirecTV stream box as there is no DVR server. It should work with non-DVR, legacy regular receivers (with their own tuners)

<h2>Setup</h2>
To use this driver, the DirecTV DVR and clients need to have special permissions set in the configuration UI, per the API documentation.

Once you have the device setup with the driver, specify the IP address on the device preferences page and you should be all set. Do an "initialize" and verify the state attributes update and show, at a minimum, the receiver is off. Turn the receiver on (press on) and then press "getTuned" to get the current program playing. The state attributes should update in real-time on the device page.

2.4 SHEF Opt In
In order to use SHEF in your network, it must be enabled in the set-top box by navigating to the “External Device” settings screen (Menu-&gt;System Setup-&gt;Whole-Home-&gt;External Device) screen. Once on this screen select “Allow” for “External Access” as shown in Figure 2-2 (in the API doc). Also turn on the other options as desired. 



<h2>Usage</h2>

<h3>Rule Machine</h3>
This driver allows commands to be issued against it from rule machine's ability to target actuators and switches.
Just issue the command shown on the device page (which are the functions defined in the driver) via: 
Run custom command &gt; Actuator &gt;  Function name (getTuned) 

<ul>
 <li>with no parameters for the following commands:  getTuned, getMode, getLocations, on, off, powerToggle, clearState</li>
<li>The following commands require a seconary parameter be passed:
	<ul>	
		<li>tuneTo:  Channel number. Optionally use a Major.Minor channel number (such as 229.1). The driver will catch your intent and send the correct channel to the receiver. In the receiver's guide, the channel number will appear as "229-1" just replace the dash with a period when using this driver</li>
<li>sendKey: key (by name, from this:"power", "poweron", "poweroff", "format", "pause", "rew", "replay", "stop", "advance", "ffwd", "record", "play", "guide", "active", "list", "exit", "back", "menu", "info", "up", "down", "left", "right", "select", "red", "green", "yellow", "blue", "chanup", "chandown", "prev", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "dash", "enter"
	only the keys listed work). Note: newer DirecTV remotes don't have all the buttons anymore but the keys still work via API. For example, the BLUE button marks a recording as "keep"</li>
	</ul>
 </ul>

<h3>General</h3>
<ul>
<li>As this driver simulates a switch, rule machine can trigger this device via simple Switch on/off commands which would then send the command to the receiver.</li>

<li>Anything the driver sees/cares about is saved to state attributes for usage in dashboards via the obviously named attribute: Channel Callsign, Channel numnber, Program Title, Episode Title, Is the current program a recording, Recorded program scheduler priority</li>

<li>Client names/locations, mac addresses are permanently saved to the state variable in the device page so the user can easily get access to the necessary mac address. Refresh the browser after issuing the "getLocations" functon. This will be used to issue commands to a specific receiver client </li>

 <li>The "initialize" function does: getMode (what is current state), clearState (attributues) and getLocations (get clients)</li>
 <li>The "refresh" function does: getMode, getTuned (what is currently being played), getLocations</li>
<li>I recommend putting a button on your dashboard then telling a button controller to run "getTuned" for spontaneous/on-demand updating of what is playing. See my personal dashboard on the screens at the end</li>
<li>The debugMode option in preferences is very chatty - every command will be displayed for troubleshooting purposes.	</li>

<li>The getMode command has an optional feature - fix the mode. If set to the default "nocheck" - it just tells you the mode. If you change the option or pass the specified parameter of "ShouldBeOn" of "ShouldBeOff" it will attempt to fix the receiver's mode i.e. - turn it on if it's off and vice versa</li>
</ul>

<h2>Hidden functionality</h2>
The driver code has an optional section for "favorite" buttons. This will make up to 9 stand-alone remote key buttons for your convenience. You can comment out the section or change or remove all the buttons in the Favorite Buttons array. Refer to the main screenshot to see favorite buttons.

<h2>Notes from DirecTV API documentation:</h2>
2.7 Limitations (from DirecTV's documentation)
SHEF provides sufficient responsiveness to be acceptable to a person controlling the STB. Subsecond responses are desired, but millisecond response time is not required. SHEF is single-threaded and will not directly queue requests. This will provide some degree of serialization, but the execution of requests in a particular order is not guaranteed as http does not guarantee requests will arrive in any particular order.

<h2>Future Ideas</h2>
        - Supporting client state attributes
        - Hubiat Child devices?
        - Scheduled polling (the user can easily schedule recurring jobs in rule-machine and use the "getMode" or "getTuned" functions from rule machine in the meantime via "run custom command > actuator > device > getMode or getTuned") 

<h2>There's more information in the functions in the driver</h2>

<h2>Screens</h2>

<h3>Device Page with the receiver off - notice the favorite remote buttons directly in the commands - optional within the code.</h3>
<img width="1406" alt="directv commands" src="https://github.com/rnoia/DirecTV-IP-Control-for-Hubitat/assets/65049274/31ea9ad1-9631-48f6-8af1-0875383e2f21">

<h3>State attributes - accessible from dashboards</h3>
<h4>State attributes with receiver off</h4>
<img width="258" alt="direct state attributes" src="https://github.com/rnoia/DirecTV-IP-Control-for-Hubitat/assets/65049274/0a8a948d-000e-4deb-aa57-d07a9fa071f5">

<h4>State attributes watching a non-recorded program live</h4>
<img width="268" alt="direct state attributes - on1" src="https://github.com/rnoia/DirecTV-IP-Control-for-Hubitat/assets/65049274/b428f6f5-b2e4-4eb1-86aa-58fc1211e8e2">

<h4>State attributes watrching a recorded program (either live while recording or from the recording)</h4>
<img width="358" alt="direct state attributes - recording" src="https://github.com/rnoia/DirecTV-IP-Control-for-Hubitat/assets/65049274/4d678d09-69c5-450c-a0e2-d2d7861d4c83">

<h4>State variables showing the clients and receivers on your network - this persists. The number under the red is the receiver's MAC address and is needed to issue commands to that receiver</h4>
<img width="287" alt="Directv state variables" src="https://github.com/rnoia/DirecTV-IP-Control-for-Hubitat/assets/65049274/d0d36f90-1405-412b-a3db-abea0853fb9e">

<h4>My dashboard showing state attributes for on-demand viewing of program and channel info</h4>

<img width="1671" alt="dashboard" src="https://github.com/rnoia/DirecTV-IP-Control-for-Hubitat/assets/65049274/59c4cd53-7d1c-41c6-8993-1ce5dc1e4a48">
