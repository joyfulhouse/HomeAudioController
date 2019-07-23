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
	definition (name: "Home Audio Controller", namespace: "joyfulhouse", author: "Bryan Li") {
    	capability "Configuration"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
        capability "Music Player"
        capability "Alarm"
        capability "Audio Notification"
	}

	// simulator metadata
	simulator {
    
	}
    
    // Preferences
    preferences {
        input "ip", "text", title: "IP Address", description: "IP Address in form 192.168.1.226", required: true, displayDuringSetup: true
        input "port", "text", title: "Port", description: "port in form of 8090", required: true, displayDuringSetup: true
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
	runEvery1Minute(refresh)
}

def updated() {
    unschedule()
	runEvery1Minute(refresh)
}

def parseResp(resp) {
    dataKeySet = resp.data.keySet().toArray()
    for(int i = 0; i < resp.data.size(); i++) {
    	def zoneData = resp.data.get(dataKeySet[i])
        def zoneName = ("" + zoneData.zone).padLeft(4,'0')
        def childDevice = null
        
        try {
            childDevices.each {
                
                try{
                    if (it.deviceNetworkId == "${device.deviceNetworkId}-${zoneName}") {
                        childDevice = it
                    }
                }
                catch (e) {
                    log.debug e
                }
            }
            
            if (childDevice == null) {
                addChildDevice("Home Audio Zone", "${device.deviceNetworkId}-${zoneName}",
                               [label: "Home Audio Zone ${zoneData.zone}",
                                name: "HomeAudioZone${zoneData.zone}",
                                ip: settings.ip,
                                port: settings.port,
                                zone: zoneData.zone])
            } else {
             	childDevice.synchronize(zoneData)
            }
        }
        catch (e) {
            log.debug e
        }
    }
}

// Switch Capabilities
def on() {
	sendEthernet("api/control/all/power/on")
	sendEvent(name: "switch", value: "on")
    sendEvent(name: "status", value: "playing")
}

def off() {
	if(device.currentValue("alarm") == "on" || device.currentValue("alarm") =="both") {
    	sendEthernet("api/alarm/off")
        sendEvent(name: "alarm", value: "off")
    }
    else {
        sendEthernet("api/control/all/power/off")
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
	sendEthernet("api/control/all/mute/on")
    sendEvent(name:'mute', value:'muted')
}

def unmute(){
	sendEthernet("api/control/all/mute/off")
    sendEvent(name:'mute', value:'unmuted')
}

def setLevel(number) {
    if (device.currentValue('mute') == 'muted') {
        unmute()
    }
    
    def volume = ((number * 38) / 100) as int
    sendEthernet("api/control/all/volume/${volume}")
    sendEvent(name:"level", value:number)
}

// Alarm Capabilities
def both(){
  siren()
}

def siren(){
  on()
  sendEthernet("api/alarm/on")
  sendEvent(name:"alarm", value:"on")
}

def strobe(){
  siren()
}

// Notification Capabilities
def playText(message, level){
  on()
  setLevel(level)
  sendEthernet("api/notify/0/" + URLEncoder.encode(message, "UTF-8"))
}

def playTextAndResume(message, level) {
	playText(message, level)
}

def playTextAndRestore(message, level) {
	playText(message, level)
}

// Device Functions
def poll() {
	refresh()
}

def refresh() {
	sendEthernet("api/status")
}

private getHostAddress() {
    def ip = settings.ip
    def port = settings.port
    
    return ip + ":" + port
}

def sendEthernet(path) {
    if(settings.ip != null && settings.port != null){
        def params = [
            uri: "http://${settings.ip}:${settings.port}",
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
