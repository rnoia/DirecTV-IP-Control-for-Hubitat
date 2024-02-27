/*
    *  DirecTV Receiver IP-Control For Hubitat Driver
    *
    *  Copyright 2024 Roger Noia
    *  This driver lives in github at: https://github.com/rnoia/DirecTV-IP-Control-for-Hubitat/blob/main/README.md
    *
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*
    *  Change History:
    *
    *   Version Date        Who             What
    *   ----    ----        ---             ----
    *   1.0     2024-02-26  Roger Noia      Initial Release
    *
        
 Refer to DirecTV SHEF IP control API to see what's available.
        https://blog.solidsignal.com/docs/DTV-MD-0359-DIRECTV_SHEF_Command_Set-V1.3.C.pdf 
        

        */


import groovy.json.JsonSlurper
def DirecTVfunction=""
def res=""
def url=""

if (debugMode){
    log.debug "Debug mode is on"
}

metadata {
    definition (name: "DirecTV Receiver IP-based Control", namespace: "rnoia", author: "community.hubitat@rnoia.com") {
        capability "Switch"
        capability "Actuator"
        capability "Initialize"
        capability "Refresh"
                
        attribute "channelNumber", "number" // major = channel number
        attribute "channelSubchannel", "number" // minor = sub-channel number
        attribute "channelCallsign", "string"        
        attribute "programTitle", "string"
        attribute "programEpisodeTitle", "string"
        attribute "mode", "string"
        attribute "isARecording", "string"
        attribute "programRecordedDate", "number"
        attribute "programRecordingPriority", "string"

        if (debugMode) log.debug "show advanced buttons: ${showAdvanced}"

        // all available button names supported by the API
        def buttonArray = ["power", "poweron", "poweroff", "format", "pause", "rew", "replay", "stop", "advance", "ffwd", "record", "play", "guide", "active", "list", "exit", "back", "menu", "info", "up", "down", "left", "right", "select", "red", "green", "yellow", "blue", "chanup", "chandown", "prev", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "dash", "enter"] as String []
        arraySize= buttonArray.size()

        // the above list with descriptive names as the key=value pair
        def ButtonMap = ["power":"Power toggle",    "poweron":"Power ON",   "poweroff":"Power OFF",     "format":"video format",    "pause":"Pause",     "rew":"Rewind",     "replay":"Replay"  , "stop":"Stop", "advance":"Advance forward (15 sec skip)",   "ffwd":"Fast Forward",   "record":"Record",  "play":"Play",  "guide":"Guide",    "active":"Active",  "list":"DVR Recording List",  "exit":"Exit",     "back":"Back",   "menu":"Menu",  "info":"Info",  "up":"Directional UP", "down":"Directional DOWN", "left":"Directional LEFT", "right":"Directional RIGHT",  "select":"Select", "red":"Red (delete)", "green":"Green", "yellow":"Yellow", "blue":"Blue (bookmark)", "chanup":"Channel UP", "chandown":"Channel DOWN", "prev":"Previous", "0":"0", "1":"1", "2":"2", "3":"3", "4":"4", "5":"5", "6":"6", "7":"7", "8":"8", "9":"9", "dash":"Dash button", "enter":"Enter"]


        // this displays your favorite buttons - to choose what shows up on the device page, change the "FavoritebuttonArray" to include only what you want - up to 9. When/if Hubitat permits reading state variables before the device is loaded, this can be changed to use actually dynamic favorites. (Currently, the state variables are not read when this first loads)
        def FavoriteButtonArray = ["replay",  "advance",  "guide",  "list",  "back", "menu", "info",  "dash"] as String []
        FavoriteArraySize= FavoriteButtonArray.size()


        // Optional, this is for listing *all* the buttons on the device page, but this makes a mess of the device page so it is disabled
        // ButtonMap.each{
        //     button -> 
        //         def buttonName="Button-" + button.key
        //         def paramName="\"Button ${button.value}\""
        //         def paramDescr="\"${button.value}\""
        //         command "${buttonName}", [[name:"${paramName}", description:"${paramDescr}"]]
        //         log.debug "trying to create button ${button.key} with value ${button.value}"
        //     }

        // this section populates the favorite buttons list. as you can't pass parameters via the button name, 9 pre-defined buttons and their respective functions are in this driver.
        for (i=0; i<FavoriteArraySize; i++){
            def buttonName="sendFavoriteKey_" + i
            def passButton = FavoriteButtonArray[i]
            def paramDescr="ButtonMap{${passButton}}"
            def paramName="${ButtonMap[passButton]}"
            command "${buttonName}",  [[name:"${paramName}", type:"ENUM", "constraints": ["${passButton}"], description:"Send favorite:${paramDescr}"]]

        }

        command "sendKey", [[name:"Send Key",  "type":"ENUM","description":"Key","constraints":["power", "poweron", "poweroff", "format", "pause", "rew", "replay", "stop", "advance", "ffwd", "record", "play", "guide", "active", "list", "exit", "back", "menu", "info", "up", "down", "left", "right", "select", "red", "green", "yellow", "blue", "chanup", "chandown", "prev", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "dash", "enter"],  description:"Sends a simulated remote control key from specified list"]]

        command "tuneTo", [[name:"Tune to Channel", type:"NUMBER", description:"Change the tuner to the specified channel, use ###.# for a channel with a sub-channel"]] 
        // change the tuner to the specified channel, if channel has a dot, need to convert to minor
        
        command "powerToggle", [[name:"Toggle the power", description:"Toggles the operating mode of the receiver"]] 
        // pseudo command to press the "power" remote button instead of specific on or off - uses key "power" instead of "poweron" or "poweroff"
        
        command "getTuned" , [[name:"Identify current channel and title", description:"Identifies current channel number, callsign, program title and episode title"]]
        // uses DirecTV function "getTuned" to get information about the current program - channel, start, duration, title, episode title, etc
        // command "getTunedClient" , [[name:"Identify current channel and title for specified client", type:"STRING", description:"Identifies current channel number, callsign, program title and episode title"]]// uses DirecTV function "getTuned" to get information about the current program - channel, start, duration, title, episode title, etc

        command "getMode", [[name:"Check and fix operating mode","type":"ENUM","description":"Key","constraints":["nocheck", "ShouldBeON", "ShouldBeOFF"], description:"Identifies the current operational mode of the receiver - on or off (standby), if a ShouldBe... option is specified, will attempt to correct the wrong state"]]

        command "getLocations", [[name:"Identify child client receivers", description:"Identifies other child receivers on the network or being served by this server/DVR, and saves their ID to the state variables below and temporarily to the right, but is not saved as an attribute"]]
        // identifies clients on the receiver, potentially to query those clients for what they are watching, but no function to do that at a cient level is currently implemented here

        command "clearState", [[name:"Clear state attributes", description:"Clear state attributes"]]     

        command "tempfunc"
        command "on"
        command "off"

    }
            
    preferences
    {
    input(name: "deviceIP", type: "string", title:"Device IP Address", description: "Enter IP Address of your DirecTV Receiver", required: true, displayDuringSetup: true)
    input(name: "devicePort", type: "string", title:"Device Port", description: "Enter Port of your receiver (defaults to 8080)", defaultValue: "8080", required: false, displayDuringSetup: true)
    input(name: "debugMode", type:"bool", title:"Debug mode logging", defaultValue:false )  
    }    

}


        def initialize(){
            if (debugMode) log.debug "Initializing..."
            if (debugMode) log.debug "Getting the current mode..."
            getMode()

            if (debugMode) log.debug "Clearing state attributes..."
            clearState()

            if (debugMode) log.debug "Getting receiver locations..."
            getLocations()
        }

        def refresh(){
            if (debugMode) log.debug "Refreshing..."

            if (debugMode) log.debug "Getting the current mode..."
            getMode()

            if (debugMode) log.debug "Querying receiver for program information via getTuned..."
            getTuned()

            if (debugMode) log.debug "Getting receiver locations..."
            getLocations()
        }


        def tempfunc(){
            //used for development functions like clearing states
            state.remove("myFavoriteButtons")
        }


        def getTunedClient(client){
            if (debugMode) log.debug "Querying receiver for program getTunedClient..."
            getTuned(client)
        }

        def getTuned(client){
            /*
            typical response:
            A program with episode title (episodetitle is present): {callsign=foodHD, date=20220403, duration=7200, episodeTitle=The Super 16 Sudden Death, isOffAir=false, isPclocked=3, isPpv=false, isRecording=false, isVod=false, major=231, minor=65535, offset=11557, programId=255704639, rating=TV-G, startTime=1708282800, stationId=3900976, status={code=200, commandResult=0, msg=OK., query=/tv/getTuned}, title=Tournament of Champions}

            DirecTV channel number is key "major". If there is a key named "minor" other than value 65535, it means there is a child channel like 200-1.  65535 is used for "no minor channel"

            note: "isRecording" does not mean it is a recorded program, but that it is current being recorded (which is false in this case). If the program being watched is a recording, the element "recType" would be present in the response as shown here:
            
            {broadcastStartTime=1708282800, callsign=COOKHD, contentStartTime=28488, date=20181006, duration=1890, episodeTitle=Tuscany, expiration=0, expiryDate=0, expiryTime=0, isOffAir=false, isPartial=false, isPclocked=3, isPpv=false, isRecording=false, isViewed=true, isVod=false, keepUntilFull=true, major=232, minor=65535, offset=33, priority=56 of 91, programId=96970959, rating=TV-G, recType=5, startTime=1708282800, stationId=9867492, status={code=200, commandResult=0, msg=OK., query=/tv/getTuned}, title=Bizarre Foods: Delicious Destinations, uniqueId=111810}

            note: elements that appear only in a recorded program: expiration, expiryDate, expiryTime, isPartial, isViewed, priority, recType
            note: element "priority 56 of 91" means this program is in the receiver's recording manager (scheduler) for automatic recording
            the API command is /tv/getTuned?[clientAddr=string] where clientAddr is optional / only used to specify which client, otherwise defaults to 0 which is the server
            */

            if (debugMode) log.debug "GETTUNED: Querying receiver for program getTuned..."

            def programTitle=""
            def programEpisode=""
            def channelNum=""
            def channelNumMinor=""
            def channelCallsign=""
            def killswitch=""
            def programRecordingPriority=""
            def programRecordedDate=""

            sendEvent(name: "programEpisodeTitle" , value:" ", descriptionText: "the title of the episode")

            if (client){
                if  (debugMode) log.debug "GETTUNED:client specified on gettuned: ${client}"
            }

            // getTuned will return 500 server error if the receiver is off, so it needs to be checked first...
            if (device.currentValue("switch") == "off"){
                if (debugMode) log.debug "GETTUNED: getTuned cannot be used against an off receiver"
                //sendEvent(name: "ERROR" , value:"getTuned command cannot be used when the receiver is off", descriptionText: "uh oh") // this puts a covenient temporary message into a state attribute if you uncomment this
                killswitch="kill"
                clearState()
                return
            }

            if (killswitch != "kill"){
                if (!(client)){
                //DirecTVfunction="tv/getTuned"            
                sendDirecTVCommand("getTuned", "none", "none")
                }
                else
                {
                sendDirecTVCommand("getTuned", "clientAddr", client) // this issues the getTuned command against a specific client receiver. unlike the primary receiver, this data is not stored in state attributes as I didn't think it was that useful for my particular needs.To make it work, state-attributes would need to be hard-coded into the driver, but since they can't be defined dynamically, so you'd have to pre-define some amount of state variables for each client receiver
                }
                
                if (debugMode) log.debug "GETTUNED:  response after sending: ${res}"

                if(res.recType) {
                    // program is a recording
                    if (debugMode) log.debug "GETTUNED: program is a recording"
                    sendEvent(name: "isARecording" , value:"true", descriptionText: descriptionText)

                    if (res.date){
                        sendEvent(name: "programRecordedDate" , value:res.date, descriptionText: "the date the program was recorded", isStateChange:true)
                        if (debugMode) log.debug "GETTUNED: recording recorded date is ${res.date}" 
                        }
                    
                    if (res.priority){
                        //this means the program has a scheduler for automated recordings, and its position
                        if (debugMode) log.debug "GETTUNED: Recording has a priority of ${res.priority}"
                        sendEvent(name: "programRecordingPriority" , value:res.priority, descriptionText: "the scheduler priority of the program",  isStateChange:true)
                        }  
                    }
                    else{
                    if (debugMode) log.debug "GETTUNED: program is a NOT recording"
                    sendEvent(name: "isARecording" , value:"false", descriptionText: "this is not a recording",  isStateChange:true)
                    }
                        
                if(res.episodeTitle){
                    //program has an episode name, likely a series, but it isn't always consistent
                    sendEvent(name: "programEpisodeTitle" , value:res.episodeTitle, descriptionText: "the title of the episode - ${programEpisode}",  isStateChange:true)
                    }
                else{
                    //clear the state attribute but no delete it so dashboard buttons work and can be updated later if an episode title appears
                    sendEvent(name: "programEpisodeTitle" , value:" ", descriptionText: "the title of the episode - none for this",  isStateChange:true)
                    }
            if (res.minor != 65535){
                // channel has a minor number, change channel displayed number to ###.#.  65535 is the directv minor channel number for "doesn't have a minor channel"
                if (debugMode) log.debug "GETTUNED: channel has a sub-channel minor number: ${res.minor}"
                channelNum=res.major + "." + res.minor
                }
            else{
                channelNum=res.major
                }

            programTitle= res.title
            channelCallsign=res.callsign

            sendEvent(name: "programTitle" , value:res.title, descriptionText: "program title",  isStateChange:true)
            sendEvent(name: "channelNumber" , value:channelNum, descriptionText: "channel number",  isStateChange:true)
            sendEvent(name: "channelCallsign" , value:res.callsign, descriptionText: "callsign",  isStateChange:true)
            } // end killswitch check   
            else{
                clearState()
                if (debugMode){
                    log.debug "getTuned: this function cannot be used against an off receiver"
                    sendEvent(name: "ERROR" , value:"getTuned command cannot be used when the receiver is off", descriptionText: "uh oh") // this puts a covenient temporary message into a state attribute
                    }
            } 
        } // end getTuned


        def sendDirecTVCommand(function,secondary,tertiary) {
            if (debugMode) log.debug "sendDirecTVCommand: function= ${function}, secondary=${secondary}, tertiary=${tertiary}"
            if(function == "getMode"){
                DirecTVfunction="info/mode"
            }
            else if (function == "getTuned"){
                if (secondary == "clientAddr"){
                    DirecTVfunction="tv/getTuned?clientAddr=" + tertiary
                    }
                    else{
                        DirecTVfunction="tv/getTuned"
                    }
                }
            else if (function == "sendKey"){
                DirecTVfunction="remote/processKey?key=" + secondary
            }
            else if (function == "getLocations"){
                DirecTVfunction="info/getLocations"
            }
            else if (function == "tuneTo"){
                if (debugMode) log.debug "sendDirecTVCommand  function is ${function}"

                if (tertiary != "none"){
                    // minor channel detected
                    if (debugMode) log.debug "sendDirecTVCommand: tertiary is not none, assuming minor channel"
                    DirecTVfunction="tv/tune?major=" + secondary + "&minor=" + tertiary // major is used to refer to the primary channel number vs secondary dot channels like 2.1
                    }
                else{
                    // channel with no minor number
                    channelNum=secondary
                    DirecTVfunction="tv/tune?major=" + secondary // major is used to refer to the primary channel number vs secondary dot channels like 2.1
                }
            }
            
            //def descriptionText = "${device.displayName} get current program"
                
            url = "http://$deviceIP:$devicePort/" + DirecTVfunction
            def postParams = [
                uri: url,
                requestContentType: 'application/json',
                contentType: 'application/json',
                headers: ['CustomHeader':'CustomHeaderValue'],
                body : ["name": "value"],
                textParser : "false"         
            ]
            
            if (debugMode) log.debug "sendDirecTVCommand: trying ${url}"
            httpGet(postParams) { resp ->
                if (resp.success) {
                    res = resp.data
                }
                else{
                    log.debug "senddirectvcommand: crap"
                    return "error"
                    }
            }
        } //end sendDirecTVCommand


        def getMode(command){
            // this function is used to query the DirecTV receiver for it's current state. 
            // mode=0 means the receiver is "on", meaning the power light is on. Yes, you read it correct, 0 = on.
            // mode=1 means the receiver is "off" in standby mode.
            // the API command is /info/mode
            // Sample mode response: ({mode=1, status={code=200, commandResult=0, msg=OK., query=/info/mode}})
            if (debugMode) log.debug "GETMODE: argument passed: ${command}"
            sendDirecTVCommand("getMode", "none", "none")

            if(res.mode ==0 ) {
                // receiver is on
                if (debugMode){
                    log.debug "GETMODE: directv is on"
                    sendEvent(name: "message", value: "DirecTV is on",  isStateChange:true)
                } 
                if (command == "ShouldBeOFF"){
                    //this is used to change the mode of the receiver if it's not in right state and is sent via the device UI page as the dropdown option. You can also send a command to this function with this parameter from another rule machine function or buttonb controller
                    if (debugMode){
                        log.debug "GETMODE: directv is on but should be off"
                        sendEvent(name: "message", value: "DirecTV is on, but should be off",  isStateChange:true)
                    } 
                    sendKey("poweroff")
                    sendEvent(name: "switch", value: "off",  isStateChange:true)
                    clearState()
                }
                else{
                    if (debugMode){
                        log.debug "GETMODE: directv is on"
                        sendEvent(name: "message", value: "DirecTV is on",  isStateChange:true)
                    } 
                    sendEvent(name: "switch" , value:"on", isStateChange:true)
                    pauseExecution(1000) // necessary to give received time to process between commands, the DirecTV receiver will give a response of "ok" to queries but that doesn't mean the function has been run - only that it received the response. For my receiver, 1 second (1000ms) is fine, but I've seen a delay of up to 2 seconds when there's lots of programs being recorded or otherwise busy
                    getTuned()
                }
            }
                else{
                    //receiver is off
                    if (debugMode){
                    log.debug "GETMODE: directv is off"
                    sendEvent(name: "message", value: "DirecTV is off",  isStateChange:true)
                    } 

                    if (command == "ShouldBeON"){
                        //this is used to change the mode of the receiver if it's not in right state
                        if (debugMode){
                        log.debug "GETMODE: directv is off but should be on - sending on"
                        sendEvent(name: "message", value: "DirecGETMODE: directv is off but should be on - sending on",  isStateChange:true)
                        } 


                        sendKey("poweron")
                        sendEvent(name: "switch", value: "on",  isStateChange:true)
                        
                        if (debugMode){
                        log.debug "GETMODE: now sending getTuned to get current program"
                        sendEvent(name: "message", value: "GETMODE: now sending getTuned to get current program",  isStateChange:true)
                        } 
                        getTuned()
                    }
                    else{
                        if (debugMode){
                        log.debug "GETMODE: directv is off"
                        sendEvent(name: "message", value: "GETMODE: directv is off",  isStateChange:true)
                        } 
                        sendEvent(name: "switch" , value:"off",  isStateChange:true)
                    }
                }
            if (res.mode==1 || command != "ShouldBeON"){
                clearState();
            }
        }

        def clearState(){
                if (debugMode) log.debug "clearState: Clearing state attributes..."
                // if the attributes are deleted, the dashboard attributes get deleted and have to be rebuilt just because the page was viewed, its easier to just make the value empty and retain the attribute

                sendEvent(name: "programTitle" , value:" ", descriptionText: "program title",  isStateChange:true)
                sendEvent(name: "channelNumber" , value:" ", descriptionText: "channel number",  isStateChange:true)
                sendEvent(name: "channelCallsign" , value:" ", descriptionText: "callsign",  isStateChange:true)
                sendEvent(name: "programRecordingPriority" , value:" ", descriptionText: "recording priority",  isStateChange:true)
                sendEvent(name: "programRecordedDate" , value:" ", descriptionText: "recording date",  isStateChange:true)
                sendEvent(name: "isARecording" , value:" ", descriptionText: "is a recordding",  isStateChange:true)
                sendEvent(name: "programEpisodeTitle" , value:" ", descriptionText: "episode title",  isStateChange:true)
            }

        def getLocations(){
            /*
            this function is used to query the DirecTV receiver to identify other clients it serves. note: this driver *does* currently support addressing child receivers for get tuned, but *not* for retaining the state, nor can you issue commands like "tune to" to a child yet 
            
            commands such as getTuned, tuneTo can have "&clientAddr=CLIENTMAC" appended to the query path, where CLIENTMAC is the hex mac address of the child client deevice, WITHOUT colons, this driver writes the mac address of the discovered clients to the state variable on the device page (refresh the page after using getLocations) - then copy the ID/mac address into tne input field for "get tuned" to see the results temporarily. The client ID is the mac address with colons removed automatically by the receiver. 
            
            If the client ID is "0" that's always the server - the API will not show the mac address of the server
            
            note: an undocumented attribute of "tunerBond:boolean" was discovered in writing this, presumably it means the client in question is using one of the live tuners to watch or record something, but there's no API to show tuner usage other than what the receiver is currently watching - despite having like 5 tuners available for recording, you can only see what is actively being watched on the receiver - it assumes a receiver DVR like the HR54, not a headless unit that doesn't connect to a screen
            */

            sendDirecTVCommand("getLocations", "none", "none")
            if (debugMode) log.debug "getLocations: response: ${res}"

            def mylength= res.locations.size() // number of locations in array

                for (i=0; i<mylength; i++)
                {
                    def mylocationname= res.locations[i].locationName
                    def myclientAddr = res.locations[i].clientAddr
                    def myclientNameDescriptive=""
                    def mytunerBond = res.locations[i].tunerBond
                    if (mytunerBond == true){mytunerBond = "TUNING"}else{mytunerBond=""}

                    if (myclientAddr == "0"){myclientNameDescriptive = "0 - Server ${mytunerBond}"} else {myclientNameDescriptive = "client ${mylocationname} ${mytunerBond}"}
                    
                    if (debugMode) log.debug "getLocations: Location client ${i} is named ${mylocationname} with mac address of ${myclientAddr} and tunerBonding is ${mytunerBond}"            
                    sendEvent(name: myclientAddr, value: myclientNameDescriptive,  isStateChange:true)
                    
                    if (myclientAddr == "0"){
                        state.server = mylocationname
                    }
                    else{
                        if (debugMode) log.debug "getLocations: client address is not zero with ${myclientAddr}"
                        state["client-"+ i + "-" + myclientAddr]=mylocationname
                    }        
                }// end for loop
        }



        def sendKey(keyName){
            /*
            this function is used to send a remote control button press. it cannot be used to send a string -the API only accepts the specific keys mentioned by name. There is no API for sending a string/word.

            the API command is /remote/processKey?KEYNAME

            where KEYNAME is any of the following per DirecTV SHEF specification v1.3c are: 
                power (toggle), poweron, poweroff, format, pause, rew, replay, stop, advance, ffwd, record, play, guide, active, list, exit, back, menu, info, up, down, left, right, select, red, green, yellow, blue, chanup, chandown, prev, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, dash, enter
            
            note: current remotes do not have blue (bookmark), yellow, green, stop, active, format, active  buttons anymore but the legacy commands still work

            as this simulates remote buttons, it is not advised to use this to change channels because there is a very short window on the receiver for accepting channel numbers before it will assume you're done and change (i.e. pressing "2" then "2" then "9" to change to HGTV 229.)  Instead, use "tuneTo" for that where you pass the complete channel number at one shot, but you can certainly use the chanup or chandown buttons
            */

            sendDirecTVCommand("sendKey", keyName, "none")

        }


        def sendFavoriteKey_1(keyName){
            // favorite key 1
            sendDirecTVCommand("sendKey", keyName, "none")
        }

        def sendFavoriteKey_2(keyName){
            // favorite key 1
            sendDirecTVCommand("sendKey", keyName, "none")
        }

        def sendFavoriteKey_3(keyName){
            // favorite key 1
            sendDirecTVCommand("sendKey", keyName, "none")
        }

        def sendFavoriteKey_4(keyName){
            // favorite key 1
            sendDirecTVCommand("sendKey", keyName, "none")
        }

        def sendFavoriteKey_5(keyName){
            // favorite key 1
            sendDirecTVCommand("sendKey", keyName, "none")
        }

        def sendFavoriteKey_6(keyName){
            // favorite key 1
            sendDirecTVCommand("sendKey", keyName, "none")
        }

        def sendFavoriteKey_7(keyName){
            // favorite key 1
            sendDirecTVCommand("sendKey", keyName, "none")
        }

        def sendFavoriteKey_8(keyName){
            // favorite key 1
            sendDirecTVCommand("sendKey", keyName, "none")
        }

        def sendFavoriteKey_9(keyName){
            // favorite key 1
            sendDirecTVCommand("sendKey", keyName, "none")
        }


        def tuneTo(channelNum){
            /*
            this function is used to change the channel 
            the API command is /tv/tune/major=MAJORCHANNELNUMBER&minor=MINORCHANNELNUMBER&clientAddr=CLIENTMAC
            
            where MAJORCHANNELNUMBER is the major part of the channel number as represented in the guide
            
            where MINORCHANNELNUMBER is the optional .# of the sub-channel (such as 229-1), if not specified, assumes no minor channel number (65535)
            
            where CLIENTMAC is the optional hexadecimal client mac address, without colons. if not specified, assumes the primary server (location 0) [ future usage, not implemented]
            
            this driver will accept a channel number in the format major.minor such as 272.1, which would be major 272, minor 1 and send the correct interpretation to the receiver
            */
            if (debugMode) log.debug "tuneto: trying to change channel to ${channelNum}"

            def tempchannelnum = "" + channelNum;
            channelNum = tempchannelnum
            def major=channelNum
            def minor="none"

            if (channelNum.contains(".")){
                //check for a period in the passed channel number to look for a minor channel number
                if (debugMode) log.debug "tuneto: minor channel number detected"
                def majorminor = channelNum.split('\\.')
                major = majorminor[0]
                minor = majorminor[1]
                if (debugMode) log.debug "tuneto: major channel is ${major} minor channel is ${minor}"
                }

            sendDirecTVCommand("tuneTo", major, minor)

            if ( minor != "none")
            {
            if (debugMode) log.debug "SendDirectvcommand: minor is not NONE  with value: major= ${major}, minor=${minor}"
            channelNum=major    
                // now check receiver to confirm channel has changed, this section doesn't support minor channel numbers
                def  origChannelNum = channelNum
                if (debugMode) log.debug "tuneto:  original requested channel is ${channelNum}, ${origChannelNum}"

                getTuned()
                if (debugMode) log.debug "tuneto:  response ${res}"

                if (res.major != origChannelNum){
                if (debugMode) log.debug "tuneto: requested channel ${origChannelNum} is not equal to current channel ${res.major}, retrying..."
                    pauseExecution(1000)
                    getTuned()
                if (debugMode) log.debug "tuneto: new channel = ${res.major}, original requested channel was ${origChannelNum}"
                if (debugMode) log.debug "tuneto: res.major is type " + getObjectClassName(res.major)
                if (res.major == origChannelNum){
                    if (debugMode) log.debug "tuneto: current channel is now expected channel"
                    }
                    else{
                    if (debugMode)log.debug "tuneto: something is wrong res.major not equal origChannelNum"
                    }
                }
                else{
                    if (debugMode) log.debug "tuneto: requested channel = ${origChannelNum}, current channel ${res.major}"
                }
            }
        }

        def powerToggle() {
            sendkey("power")
            getMode() // to change the state attributes to match
        }

        def on() {
            sendKey("poweron")
            sendEvent(name: "switch", value: "on",  isStateChange:true)
            // it woudld probably make sense to do a "getTuned" here

        }

        def off() {
            sendKey("poweroff")
            sendEvent(name: "switch", value: "off",  isStateChange:true)
            clearState()
        }

