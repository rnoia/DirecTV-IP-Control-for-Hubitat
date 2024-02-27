# DirecTV-IP-Control-for-Hubitat
Hubitat driver for DirecTV IP control of receivers

    *  Change History:
    *
    *   Version Date        Who             What
    *   ----    ----        ---             ----
    *   1.0     2024-02-26  Roger Noia      Initial Release
    *
        
This driver uses the DirecTV SHEF API. As implemented, it only uses HTTP Gets, but the API permits other methods for the adventurous
        
Getting the current state of the receiver requires polling the receiver so it's not possible to have real-time, event-based triggers of what happens on the receiver.  Considering the rather sluggish performance of my DVR, spamming the DVR with requests to have up-to-the-second visiblity of what is playing isn't realistic or recommended.  It would probably make the already slow-performing receiver unresponsive.  Once a minute, sure that should be ok.    

<h2>What can you do</h2>
This driver permits local, direct control of a DirecTV receiver using DirecTV's own API that runs on the receiver itself.
The following functions are available:
<ul>
	<li>Check the operating mode of the receiver</li>
	<li>Change the operating mode of the receiver (on or standby)</li>
	<li>Check the currently playing channel and program</li>
	<li>Change the channel</li>
	<li>Send "remote" buttons</li>
</ul>

<h2>What you can't do</h2>
<ul>
	<li>Anything involving the playlist as the DirecTV API no longer permits this</li>
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
<li>sendKey: key (by name, from the list below. only the keys listed work). Note: newer DirecTV remotes don't have all the buttons anymore but the keys still work via API. For example, the BLUE button marks a recording as "keep"</li>
	</ul>
 </ul>

<h3>General</h3>
<ul>
<li>As this driver simulates a switch, rule machine can trigger this device via simple Switch on/off comamnds which would then send the command to the receiver.</li>

<li>Anything the driver sees/cares about is saved to state attributes for usage in dashboards via the obviously named attribute: Channel Callsign, Channel numnber, Program Title, Episode Title, Is the current program a recording, Recorded program scheduler priority</li>

<li>Client names/locations, mac addresses are permanently saved to the state variable in the device page so the user can easily get access to the necessary mac address. Refresh the browser after issuing the "getLocations" functon. This will be used to issue commands to a specific receiver client </li>

 <li>The "initialize" function does: getMode (what is current state), clearState (attributues) and getLocations (get clients)</li>
 <li>The "refresh" function does: getMode, getTuned (what is currently being played), getLocations</li>
<li>I recommend putting a button on your dashboard then telling a button controller to run "getTuned" for spontaneous/on-demand updating of what is playing</li>
<li>The debugMode option in preferences is very chatty - every command will be displayed for troubleshooting purposes.	</li>

<li>The getMode command has an optional feature - fix the mode. If set to the default "nocheck" - it just tells you the mode. If you change the option or pass the specified parameter of "ShouldBeOn" of "ShouldBeOff" it will attempt to fix the receiver's mode i.e. - turn it on if it's off and vice versa</li>
</ul>

<h2>Notes from DirecTV API documentation:</h2>
2.7 Limitations (from DirecTV's documentation)
SHEF provides sufficient responsiveness to be acceptable to a person controlling the STB. Subsecond responses are desired, but millisecond response time is not required. SHEF is single-threaded and will not directly queue requests. This will provide some degree of serialization, but the execution of requests in a particular order is not guaranteed as http does not guarantee requests will arrive in any particular order.

<h2>Future Ideas</h2>
        - Supporting client state attributes
        - Hubiat Child devices?
        - Scheduled polling (the user can easily schedule recurring jobs in rule-machine and use the "getMode" or "getTuned" functions from rule machine in the meantime via "run custom command > actuator > device > getMode or getTuned") 

<h2>There's more information in the functions in the driver</h2>
