/**
 *  Copyright 2015 SmartThings
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
	definition (name: "ESP Light Controller", namespace: "wilkinsg", author: "Graham Wilkinson") {
	    capability "Switch"
        capability "Refresh"
        capability "Switch Level"
        capability "Polling"
	}

	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter port 80 or your device's Port", required: true, displayDuringSetup: true)
	}

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true, canChangeBackground: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
    			attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
		      	attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
		      	attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
        	}
        		tileAttribute("device.level", key: "SLIDER_CONTROL") {
            		attributeState "level", action:"switch level.setLevel"
        		}
        		tileAttribute("level", key: "SECONDARY_CONTROL") {
              		attributeState "level", label: 'Light dimmed to ${currentValue}%'
        		}    
		}
        standardTile("switchMain", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label:'${name}', action:"switch.off", backgroundColor:"#00A0DC", nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", backgroundColor:"#ffffff", nextState:"turningOn"
			state "turningOn", label:'${name}', action:"switch.off", backgroundColor:"#00A0DC", nextState:"turningOff"
			state "turningOff", label:'${name}', action:"switch.on", backgroundColor:"#ffffff", nextState:"turningOn"
		}
    
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main "switchMain"
		details(["switch","lValue","refresh"])

	}
}

def refresh() {
    log.debug "poll for light level"
    String host = "$DeviceIP:$DevicePort"
    try {
		sendHubCommand(new physicalgraph.device.HubAction("""GET / HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: calledBackHandler]))
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on hubAction"
	}
}

def poll() {
	refresh()
}

void calledBackHandler(physicalgraph.device.HubResponse hubResponse) {
    def body = hubResponse.body
    log.debug "body in calledBackHandler() is: ${body}"
    
    try {
        def levelregex = /Level = -?([0-9.]+)/
        def levelmatcher = ( body=~ levelregex )
        log.debug "Level = " + levelmatcher[0][1]

        def level = levelmatcher[0][1].toFloat()
        level = (100*level/255.0).toInteger()
        log.debug "Level Percentage = " + level
        if( level >= 0 && level <= 255 ){
            sendEvent(name:"level", value: level)
            sendEvent(name:"switch", value: level > 0 ? "on" : "off")
        }else{
        	log.debug "Invalid level"
        }
    }
    catch (Exception e) {
		log.debug "Hit Exception $e parsing level"
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "parse '$description'"
	def name = parseName(description)
	def value = parseValue(description)
	def result = createEvent(name: name, value: value)
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

private String parseName(String description) {
	log.debug "parseName"
	if (description?.startsWith("level: ")) {
		return "level"
	}
	null
}

private String parseValue(String description) {
	log.debug "parseValue"
	if (description?.startsWith("level: ")) {
		def reslt = (description - "level: ").trim()
		if (reslt.isNumber()) {
			return Math.round(new BigDecimal(reslt)).toString()
		}
	}
	null
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

def on() {
	sendEvent(name: "switch", value: "on")
    sendEvent(name: "level", value: "100")
    runCmd("set?level=255")
}

def off() {
	sendEvent(name: "switch", value: "off")
    sendEvent(name: "level", value: "0")
	runCmd("set?level=0")
}

def setLevel(val){
    log.info "setLevel $val"
    
    // make sure we don't drive switches past allowed values (command will hang device waiting for it to
    // execute. Never comes back)
    if (val < 0){
    	val = 0
    }
    
    if( val > 100){
    	val = 100
    }
    
    if (val == 0){ 
    	off()
    }
    else{
    	sendEvent(name: "level", value: val)
    	runCmd("set?level=" + (255*val/100).toInteger())
    }
}

def runCmd(String varCommand) {
	def host = DeviceIP
	def hosthex = convertIPtoHex(host).toUpperCase()
	def porthex = convertPortToHex(DevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"

	log.debug "The device id configured is: $device.deviceNetworkId"

	def path = "/" + varCommand
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