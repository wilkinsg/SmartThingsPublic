/**
 *  Generic HTTP Device v1.0.20160402
 *
 *  Source code can be found here: https://github.com/JZ-SmartThings/SmartThings/blob/master/Devices/Generic%20HTTP%20Device/GenericHTTPDevice.groovy
 *
 *  Copyright 2016 JZ
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
 */

metadata {
	definition (name: "ESP8266 Lock", author: "Graham Wilkinson", namespace:"wilkinsg") {
		capability "Lock"
        capability "Actuator"
		capability "Refresh"
        capability "Polling"
        capability "Battery"
	}

	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter port 80 or your device's Port", required: true, displayDuringSetup: true)
		input("DevicePath", "string", title:"URL Path", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
	}

	simulator {
	}

	// UI tile definitions
	tiles {
		standardTile("lock", "device.lock", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "locked", label: 'Locked', action: "lock.unlock", icon: "st.Entertainment.entertainment13", backgroundColor: "#00A0DC", nextState: 'unlocked'
			state "unlocked", label: 'Unlocked', action: "lock.lock", icon: "st.Entertainment.entertainment14", backgroundColor: "#ffffff", nextState: 'locked'
            state "unknown", label: 'Unknown', action: "lock.unlock", icon: "st.Entertainment.entertainment14", backgroundColor: "#ff0000", nextState: 'unlocked'
		}
        standardTile("refresh", "device.lock", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
			state "battery", label: '${currentValue}% battery', unit: ""
		}

		main "lock"
		details (["lock", "refresh", "battery"])
	}
}

def refresh() {
    log.debug "poll for state"
    def now = (new Date()).getTime()
    def timeDiff = now - state.lastUpdate
    log.debug "time since last update is $timeDiff"
    
    if( timeDiff > 60*60*1000 ){
    	sendEvent(name:"lock", value: "unknown")
        sendEvent(name:"battery", value: 0)
    } else {
    	sendEvent(name:"battery", value: 100)
    }
    
    String host = "$DeviceIP:$DevicePort"
    try {
		sendHubCommand(new physicalgraph.device.HubAction("""GET / HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: calledBackHandler]))
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on hubAction"
        sendEvent(name:"lock", value: "unknown")
	}
}

def poll() {
	refresh()
}

void calledBackHandler(physicalgraph.device.HubResponse hubResponse) {
    def body = hubResponse.body
    log.debug "body in calledBackHandler() is: ${body}"
    
    try {
        def espStateRegex = /State = ([A-Z]+)/
        def espStateMatcher = ( body=~ espStateRegex )

        def espState = espStateMatcher[0][1]
        log.debug "State = " + espState
        if( espState == "LOCKED" || espState == "UNLOCKED" ){
        	state.lastUpdate = (new Date()).getTime()
            sendEvent(name:"lock", value: espState == "LOCKED" ? "locked" : "unlocked")
        }
    }
    catch (Exception e) {
		log.debug "Hit Exception $e parsing state"
        sendEvent(name:"lock", value: "unknown")
	}
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    state.lastUpdate = 0
    refresh()
}

def lock() {
	log.debug "Triggered LOCK!!!"
    sendEvent(name:"lock", value: "locked")
	runCmd("lock")
}

def unlock() {
	log.debug "Triggered UNLOCK!!!"
	sendEvent(name:"lock", value: "unlocked")
	runCmd("unlock")
}

def runCmd(String varCommand) {
	def host = DeviceIP
	def hosthex = convertIPtoHex(host).toUpperCase()
	def porthex = convertPortToHex(DevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"

	log.debug "The device id configured is: $device.deviceNetworkId"

	def path = DevicePath + varCommand
	log.debug "path is: $path"
	def body = ""
	log.debug "body is: $body"

	def headers = [:]
	headers.put("HOST", "$host:$DevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	log.debug "The Header is $headers"
	def method = "GET"
	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: path,
			body: body,
			headers: headers
			)
		hubAction.options = [outputMsgToS3:false]
		hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

def parse(String description) {
	log.debug "Parsing '${description}'"
}

private String convertIPtoHex(ipAddress) {
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	//log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
	return hex
}
private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	//log.debug hexport
	return hexport
}
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	//log.debug("Convert hex to ip: $hex")
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
	//log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}