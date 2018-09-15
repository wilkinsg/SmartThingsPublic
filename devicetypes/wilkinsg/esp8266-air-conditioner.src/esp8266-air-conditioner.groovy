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
	definition (name: "ESP8266 Air Conditioner", author: "Graham Wilkinson", namespace:"wilkinsg") {
		capability "Switch"
        capability "Switch Level"
        capability "Temperature Measurement"
		attribute "triggerswitch", "string"
        attribute "actuallevel", "number"
        attribute "speed", "string"
        attribute "actualspeed", "string"
		command "DeviceTrigger"
        command "tempUp"
        command "tempDown"
        command "lowSpeed"
        command "hiSpeed"
	}


	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter port 80 or your device's Port", required: true, displayDuringSetup: true)
		input("DevicePath", "string", title:"URL Path", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
	}

	simulator {
	}

	tiles(scale: 2) {
		standardTile("DeviceTrigger", "device.triggerswitch", width: 4, height: 4, canChangeIcon: true, canChangeBackground: true) {
			state "triggeroff", label:'OFF' , action: "on", icon: "st.Appliances.appliances11", backgroundColor:"#ffffff", nextState: "trying"
			state "triggeron", label: 'ON', action: "off", icon: "st.Appliances.appliances11", backgroundColor: "#00A0DC", nextState: "trying"
			state "trying", label: 'TRYING', action: "", icon: "st.Appliances.appliances111", backgroundColor: "#ffffff"
		}
        standardTile("ForceOn", "device.triggerswitch", width: 2, height: 2) {
        	state "triggeroff", label:'On' , action: "on", backgroundColor:"#00A0DC"
        }
        standardTile("ForceOff", "device.triggerswitch", width: 2, height: 2) {
        	state "triggeron", label:'Off' , action: "off", backgroundColor:"#ffffff"
        }
        standardTile("ChangeSpeed", "device.speed", width: 2, height: 2) {
        	state "hi", label:'High', action: "lowSpeed", icon: "st.Appliances.appliances11", backgroundColor:"#ffffff", nextState: "low"
            state "low", label:'Low', action: "hiSpeed", icon: "st.Appliances.appliances11", backgroundColor:"#ffffff", nextState: "hi"
        }
        multiAttributeTile(name:"thermostatFull", type:"thermostat", width:6, height:4) {
            tileAttribute("actuallevel", key: "PRIMARY_CONTROL") {
                attributeState("actuallevel", label:'${currentValue}°', unit:"dF", defaultState: true)
            }
            tileAttribute("device.level", key: "VALUE_CONTROL") {
                attributeState("VALUE_UP", action: "tempUp")
                attributeState("VALUE_DOWN", action: "tempDown")
            }
            tileAttribute("device.triggerswitch", key: "OPERATING_STATE") {
                attributeState("triggeroff", label:'Off', backgroundColor:"#888888")
                attributeState("triggeron", label:'On', backgroundColor:"#00A0DC")
            }
            tileAttribute("device.actualspeed", key: "SECONDARY_CONTROL") {
        		attributeState("hi", label:'High', icon: "st.Appliances.appliances11")
                attributeState("low", label:'Low', icon: "st.Appliances.appliances11")
    		}
        }
		main "DeviceTrigger"
		details(["thermostatFull", "ForceOn", "ForceOff", "ChangeSpeed"])
	}
}

def lowSpeed(){
	log.debug "Low speed"
	sendEvent(name:"speed", value: "low")
}

def hiSpeed(){
	log.debug "High speed"
	sendEvent(name:"speed", value: "hi")
}

def tempUp(){
	sendEvent(name:"level", value: device.currentValue("level") + 1)
}

def tempDown(){
	sendEvent(name:"level", value: device.currentValue("level") - 1)
}

def on() {
	log.debug "Triggered ON!!!"
    sendEvent(name: "actuallevel", value: device.currentValue("level"))
    sendEvent(name: "actualspeed", value: device.currentValue("speed"))
	sendEvent(name: "triggerswitch", value: "triggeron", isStateChange: true)
    state.blinds = "on";
	runCmd("open?speed=" + device.currentValue("speed") + "&temp=" + device.currentValue("level"))
}

def off() {
	log.debug "Triggered OFF!!!"
	sendEvent(name: "triggerswitch", value: "triggeroff", isStateChange: true)
    state.blinds = "off";
	runCmd("close")
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
	def whichTile = ''	
	log.debug "state.blinds " + state.blinds
	
    if (state.blinds == "on") {
    	//sendEvent(name: "triggerswitch", value: "triggergon", isStateChange: true)
        whichTile = 'mainon'
    }
    if (state.blinds == "off") {
    	//sendEvent(name: "triggerswitch", value: "triggergoff", isStateChange: true)
        whichTile = 'mainoff'
    }
	
    //RETURN BUTTONS TO CORRECT STATE
	log.debug 'whichTile: ' + whichTile
    switch (whichTile) {
        case 'mainon':
			def result = createEvent(name: "switch", value: "on", isStateChange: true)
			return result
        case 'mainoff':
			def result = createEvent(name: "switch", value: "off", isStateChange: true)
			return result
        default:
			def result = createEvent(name: "testswitch", value: "default", isStateChange: true)
			//log.debug "testswitch returned ${result?.descriptionText}"
			return result
    }
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