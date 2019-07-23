/**
*  Copyright 2018 Hubitat
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*  Home Audio Device Type
*
*  Author: bryan.li@gmail.com
*
*  Date: 2015-10-02
*/
import java.net.URLEncoder

metadata {
	definition (name: "Home Audio Zone", namespace: "joyfulhouse", author: "Bryan Li") {
    	capability "Configuration"
		capability "Switch"
        capability "Switch Level"
		capability "Refresh"
        capability "Music Player"
	}

	// simulator metadata
	simulator {
    
	}
    
	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name: "mediaMulti", type:"mediaPlayer", width:6, height:4) {
            tileAttribute("device.status", key: "PRIMARY_CONTROL") {
                attributeState("paused", label:"Paused",)
                attributeState("playing", label:"Playing")
                attributeState("stopped", label:"Stopped")
            }
            tileAttribute("device.status", key: "MEDIA_STATUS") {
                attributeState("paused", label:"Paused", action:"music Player.play", nextState: "playing")
                attributeState("playing", label:"Playing", action:"music Player.pause", nextState: "stopped")
                attributeState("stopped", label:"Stopped", action:"music Player.play", nextState: "playing")
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState("level", action:"music Player.setLevel")
            }
            tileAttribute ("device.mute", key: "MEDIA_MUTED") {
                attributeState("unmuted", action:"music Player.mute", nextState: "muted")
                attributeState("muted", action:"music Player.unmute", nextState: "unmuted")
            }
        }
        
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: '${currentValue}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc"
        }
        
        standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "configure", label:'Configure', action:"configuration.configure", icon:"st.secondary.tools"
		}

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"polling.poll", icon:"st.secondary.refresh"
		}

		main "switch"
		details(["mediaMulti","switch","siren","refresh","configure"])
	}
}

def initialize() {
 	installed()
}
def installed() {
    unschedule()
	refresh()
}

def updated() {
    unschedule()
    refresh()
}

def parseResp(resp) {
    state = resp.data
    
    synchronize(resp.data)
}

def synchronize(data){
    log.debug "Synchronizing status: ${data}"
    // Power
    if(device.currentValue("switch") != data.power) {
        if (data.power == "on") {
            sendEvent(name: "switch", value: "on")
            sendEvent(name: "status", value: "playing")
        } else {
            sendEvent(name: "switch", value: "off")
            sendEvent(name: "status", value: "stopped")
        }
    }

    // Volume
    def volume = ((data.volume / 38) * 100) as int
        if (volume != device.currentValue("level"))
        	sendEvent(name:"level", value:volume)


    // Mute
    if (device.currentValue("mute") == "unmuted" && data.mute == "on")
    	sendEvent(name:'mute', value:'muted')
    else if (device.currentValue("mute") == "muted" && data.mute == "off")
        sendEvent(name:'mute', value:'unmuted')
    else if (device.currentValue("mute") == null)
        sendEvent(name:'mute', value:'unmuted')

    // Channel/Track
    if (device.currentValue("track") != data.channel)
    	sendEvent(name:'track', value: data.channel)
}

// Switch Capabilities
def on() {
    // Set to default zone then power on
    sendEthernet("api/control/${device.data.zone}/channel/${device.data.zone}")
	sendEthernet("api/control/${device.data.zone}/power/on")
    
	sendEvent(name: "switch", value: "on")
    sendEvent(name: "status", value: "playing")
}

def off() {
	if(device.currentValue("alarm") == "on" || device.currentValue("alarm") =="both") {
    	sendEthernet("api/alarm/off")
        sendEvent(name: "alarm", value: "off")
    }
    else {
        sendEthernet("api/control/${device.data.zone}/power/off")
        sendEvent(name: "switch", value: "off")
        sendEvent(name: "status", value: "stopped")
    }
}

// Music Player Capabilities
def play(){
	on()
}

def pause(){
	off()
}

def stop(){
	off()
}

def mute(){
	sendEthernet("api/control/${device.data.zone}/mute/on")
    sendEvent(name:'mute', value:'muted')
}

def unmute(){
	sendEthernet("api/control/${device.data.zone}/mute/off")
    sendEvent(name:'mute', value:'unmuted')
}

def setLevel(number) {
    if (device.currentValue('mute') == 'muted') {
        unmute()
    }
    
    def volume = ((number * 38) / 100) as int
    sendEthernet("api/control/${device.data.zone}/volume/${volume}")
    sendEvent(name:"level", value:number)
}

def nextTrack() {
    if(state.channel < 4)
    	state.channel++
    else
        state.channel = 1
    
    changeChannel(state.channel)
}

def previousTrack() {
    if(state.channel > 1)
    	state.channel--
    else
        state.channel = 4
    
    changeChannel(state.channel)
}

def changeChannel(newChannel){
    def message = "Zone ${device.data.zone} is now playing channel ${state.channel}"
    log.debug message
    sendEthernet("api/notify/${device.data.zone}/" + URLEncoder.encode(message, "UTF-8"))
    
    sendEthernet("api/control/${device.data.zone}/channel/${newChannel}")
}

// Notification Capabilities
def playText(message, level){
  on()
  setLevel(level)
  sendEthernet("api/notify/${device.data.zone}" + URLEncoder.encode(message, "UTF-8"))
}

def playTextAndResume(message, level) {
	playText(message, level)
}

def playTextAndRestore(message, level) {
	playText(message, level)
}


// Device Functions
def refresh() {
    parent.refresh()
}

def sendEthernet(path) {
    if(device.data.ip != null && device.data.port != null){
        def params = [
            uri: "http://${device.data.ip}:${device.data.port}",
            path: "${path}",
            headers: [:]
        ]

        try {
            httpGet(params) { resp ->
                parseResp(resp)
            }
        } catch (e) {
            log.error "something went wrong: $e"
        }
    }
}
