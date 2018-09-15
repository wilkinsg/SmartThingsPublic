/**
 *  Basic Thermostat
 *
 *  Copyright 2018 Graham Wilkinson
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
	definition (name: "ESP8266 Thermostat", namespace: "wilkinsg", author: "Graham Wilkinson") {
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Thermostat"
        capability "Polling"
		capability "Sensor"
        capability "Refresh"
        
        command "setpointUp"
        command "setpointDown"
        command "lowSpeed"
        command "hiSpeed"
        command "checkState"
        command "toggleOn"
	}
    
    preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter port 80 or your device's Port", required: true, displayDuringSetup: true)
	}

	tiles(scale: 2) {
        multiAttributeTile(name:"thermostatMulti", type:"thermostat", width:6, height:4) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState("default", label:'${currentValue}°', unit:"dF") //, action: "toggleOn"
            }
            tileAttribute("device.thermostatSetpoint", key: "VALUE_CONTROL") {
                attributeState("VALUE_UP", action: "setpointUp")
                attributeState("VALUE_DOWN", action: "setpointDown")
            }
            tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
                attributeState("default", label: '${currentValue}%', unit: "%", icon: "st.Weather.weather12")
            }
            tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
                attributeState("idle", backgroundColor:"#888888")
                attributeState("heating", backgroundColor:"#e86d13")
                attributeState("cooling", backgroundColor:"#00A0DC")
            }
            tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
                attributeState("off", label:'${name}')
                attributeState("heat", label:'${name}')
                attributeState("cool", label:'${name}')
                attributeState("auto", label:'${name}')
            }
            tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
                attributeState("default", label: '${currentValue}', unit: "°F")
            }
            tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
                attributeState("default", label: '${currentValue}', unit: "°F")
            }
        }
        standardTile("temperature", "device.temperature", width: 2, height: 2, canChangeIcon: true) {
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
		valueTile("humidity", "device.humidity", width: 2, height: 2) {
			state "humidity", label:'${currentValue}%', unit:"", icon: "st.Weather.weather12", 
            backgroundColors:[
                [value: 0, color: "#ffff00"],
                [value: 30, color: "#888888"],
                [value: 60, color: "#0000ff"]
            ]
		}
        standardTile("changeMode", "device.thermostatMode", width: 2, height: 2) {
        	state "off", label:'Disabled', action: "cool", backgroundColor:"#FFFFFF", nextState: "cool"
            state "cool", label:'Armed', action: "off", backgroundColor:"#00A0DC", nextState: "off"
        }
        valueTile("thermostatState", "device.thermostatOperatingState", width: 2, height: 2) {
        	state "idle", label:'Idle', backgroundColor:"#FFFFFF"
            state "cooling", label:'Cooling', backgroundColor:"#00A0DC"
        }
        standardTile("ChangeSpeed", "device.thermostatFanMode", width: 2, height: 2) {
        	state "on", label:'High', action: "lowSpeed", icon: "st.Appliances.appliances11", backgroundColor:"#ffffff", nextState: "auto"
            state "auto", label:'Low', action: "hiSpeed", icon: "st.Appliances.appliances11", backgroundColor:"#ffffff", nextState: "on"
        }
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        main "temperature"
		details(["thermostatMulti", "changeMode", "ChangeSpeed", "refresh"])
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def refresh() {
    log.debug "refresh temperature & humidity"
    String host = "$DeviceIP:$DevicePort"
    try {
		sendHubCommand(new physicalgraph.device.HubAction("""GET / HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: calledBackHandler]))
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on hubAction"
	}
    checkState(false)
}

def poll() {
	log.debug "poll for AC state"
	checkState(false)
}

def Integer getTemperature() {
	return device.currentState("temperature").getIntegerValue()
}

def checkState(boolean force) {
	log.debug("checkState")
	if( getThermostatMode() == "cool" ){
        if( getTemperature() > getThermostatSetpoint() ){
        	log.debug("Low temp, AC on")
            goCooling(force)
        } else {
        	log.debug("Low temp, AC idle")
            goIdle()
        }
    }
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

private runCmd(String varCommand) {
	def host = DeviceIP
	def path = '/' + varCommand
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

def installed() {
    log.trace "Executing 'installed'"
    initialize()
}

def updated() {
    log.trace "Executing 'updated'"
    initialize()
}

def configure() {
    log.trace "Executing 'configure'"
    initialize()
}

private initialize() {
	sendEvent(name: "temperature", value: 0, unit: "°F")
    sendEvent(name: "humidity", value: 0, unit: "%")
    sendEvent(name: "thermostatSetpoint", value: 0, unit: "°F")
    sendEvent(name: "heatingSetpoint", value: 99, unit: "°F")
    sendEvent(name: "coolingSetpoint", value: 0, unit: "°F")
    sendEvent(name: "thermostatMode", value: "off")
    sendEvent(name: "thermostatOperatingState", value: "idle")
    sendEvent(name: "thermostatFanMode", value: "auto")
    off()
    refresh()
    unschedule()
    log.debug "Initialized"
}

private setpointUp() {
	def newSetPoint = getThermostatSetpoint() + 1
    setThermostatSetpoint(newSetPoint)
}

private setpointDown() {
	def newSetPoint = getThermostatSetpoint() - 1
    setThermostatSetpoint(newSetPoint)
}

def Integer getThermostatSetpoint() {
	return device.currentState("thermostatSetpoint").getIntegerValue()
}

def setThermostatSetpoint(Double degreesF) {
	log.debug "Executing 'setThermostatSetpoint'"
    sendEvent(name: "heatingSetpoint", value: degreesF.toInteger())
    sendEvent(name: "coolingSetpoint", value: degreesF.toInteger())
    sendEvent(name: "thermostatSetpoint", value: degreesF.toInteger())
    checkState(true)
}

def setHeatingSetpoint(Double degreesF) {
	setThermostatSetpoint(degreesF)
}

def setCoolingSetpoint(Double degreesF) {
	setThermostatSetpoint(degreesF)
}

def lowSpeed(){
	log.debug "Low speed"
    sendEvent(name: "thermostatFanMode", value: "auto")
    checkState(true)
}

def hiSpeed(){
	log.debug "High speed"
    sendEvent(name: "thermostatFanMode", value: "on")
    checkState(true)
}

private String getThermostatMode() {
	return device.currentState("thermostatMode").value
}

private String getThermostatOperatingState() {
	return device.currentState("thermostatOperatingState").value
}

private String getURLFanSpeed() {
	if( device.currentState("thermostatFanMode").value == "on" ){
    	return 'hi'
    } else {
    	return 'low'
    }
}

private goIdle() {
	if(getThermostatOperatingState() != "idle"){
		sendEvent(name: "thermostatOperatingState", value: 'idle')
    	runCmd("close")
    }
}

private goCooling(boolean force) {
	if(getThermostatOperatingState() != "cooling" || force){
		sendEvent(name: "thermostatOperatingState", value: 'cooling')
    	runCmd("open?speed=" + getURLFanSpeed()  + "&temp=" + (getThermostatSetpoint()-5))
    }
}

def toggleOn() {
	log.debug "Toggling State"
    if(getThermostatMode() == 'cool') {
    	off()
    } else {
    	cool()
    }
}

def off() {
	log.debug "Executing 'off'"
	sendEvent(name: "thermostatMode", value: 'off')
    goIdle()
}

def cool() {
	log.debug "Executing 'cool'"
	sendEvent(name: "thermostatMode", value: 'cool')
    checkState(false)
}

def setThermostatFanMode(String value) {
	log.trace "Executing 'setThermostatFanMode' $value"
    if (value == "auto") {
        lowSpeed()
    } else if (value == "on") {
    	hiSpeed()
    } else if (value == "circulate") {
        log.warn "'$value' is not a supported mode."
        hiSpeed()
    } else if (value == "followschedule") {
        log.warn "'$value' is not a supported mode."
        lowSpeed()
    }
}

def setThermostatMode(String value) {
    log.trace "Executing 'setThermostatMode' $value"
    if (value == "cool") {
        cool()
    } else if (value == "off") {
    	off()
    } else if (value == "auto") {
        log.warn "'$value' is not a supported mode."
        cool()
    } else if (value == "heat") {
        log.warn "'$value' is not a supported mode."
        off()
    }
}
