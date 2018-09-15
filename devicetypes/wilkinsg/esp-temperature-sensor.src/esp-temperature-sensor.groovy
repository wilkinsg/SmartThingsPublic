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
	definition (name: "ESP Temperature Sensor", namespace: "wilkinsg", author: "Graham Wilkinson") {
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
		capability "Polling"
		capability "Sensor"
        capability "Refresh"
	}

	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter port 80 or your device's Port", required: true, displayDuringSetup: true)
	}

	// simulator metadata
	simulator {
		for (int i = 0; i <= 100; i += 10) {
			status "${i}F": "temperature: $i F"
		}

		for (int i = 0; i <= 100; i += 10) {
			status "${i}%": "humidity: ${i}%"
		}
	}

	// UI tile definitions
	tiles {
		valueTile("temperature", "device.temperature", width: 2, height: 2, canChangeIcon: true) {
			state("temperature", label:'${currentValue}°', icon: "st.Weather.weather2",
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
			)
		}
		valueTile("humidity", "device.humidity") {
			state "humidity", label:'${currentValue}%', unit:"", icon: "st.Weather.weather12", 
            backgroundColors:[
                [value: 0, color: "#ffff00"],
                [value: 30, color: "#888888"],
                [value: 60, color: "#0000ff"]
            ]
		}
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "temperature"
		details(["temperature", "humidity", "refresh"])
	}
}

def refresh() {
    log.debug "poll for temperature & humidity only V7"
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
        def tempregex = /Temp = -?([0-9.]+)/
        def tempmatcher = ( body=~ tempregex )
        log.debug "temperature = " + tempmatcher[0][1]
        def tempc = tempmatcher[0][1].toFloat()
        def tempf = Math.round(tempc * 1.8f + 32)
        log.debug "temp F = " + tempf

        if( tempc >= 0 && tempc < 100 ){
            sendEvent(name:"temperature", value: tempf)
        }
    }
    catch (Exception e) {
		log.debug "Hit Exception $e parsing temperature"
	}
    
    try{
        def humidregex = /Humidity = -?([0-9]+)/
        def humidmatcher = ( body=~ humidregex )
        log.debug "humidity = " + humidmatcher[0][1]
        def humidity = humidmatcher[0][1].toInteger()
        if( humidity >= 0 && humidity <= 100 ) {
            sendEvent(name:"humidity", value: humidity)
        }
    }
    catch (Exception e) {
		log.debug "Hit Exception $e parsing humidity"
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "parse"
	def name = parseName(description)
	def value = parseValue(description)
	def unit = name == "temperature" ? getTemperatureScale() : (name == "humidity" ? "%" : null)
	def result = createEvent(name: name, value: value, unit: unit)
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

private String parseName(String description) {
	log.debug "parseName"
	if (description?.startsWith("temperature: ")) {
		return "temperature"
	} else if (description?.startsWith("humidity: ")) {
		return "humidity"
	}
	null
}

private String parseValue(String description) {
	log.debug "parseValue"
	if (description?.startsWith("temperature: ")) {
		return zigbee.parseHATemperatureValue(description, "temperature: ", getTemperatureScale())
	} else if (description?.startsWith("humidity: ")) {
		def pct = (description - "humidity: " - "%").trim()
		if (pct.isNumber()) {
			return Math.round(new BigDecimal(pct)).toString()
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