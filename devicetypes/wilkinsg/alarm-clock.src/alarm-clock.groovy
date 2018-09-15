/**
 *  Timed Switch
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
        definition (name: "Alarm Clock", namespace: "wilkinsg", author: "Graham Wilkinson") {
        capability "Switch"
        capability "Alarm"
        attribute "waitTime", "number"
        attribute "alarmType", "enum", ["strobe", "siren", "both"]
        attribute "hoursDisplay", "string"
        attribute "clockTime", "string"
        
        command "waitUp"
        command "waitDown"
        command "alarmOff"
        command "doAlarm"
        command "setSiren"
        command "setStrobe"
        command "setBoth"
    }

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles(scale: 2) {
    	multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true, canChangeBackground: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
    			attributeState "off", label: 'off', action: "switch.on", icon: "st.Health & Wellness.health7", backgroundColor: "#ffffff", nextState: "on", defaultState: true
		      	attributeState "on", label: 'Armed', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "off"
        	}
		}
    	multiAttributeTile(name:"valueTile", type:"generic", width:6, height:4) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label:'Off', action:"switch.on", backgroundColor:"#ffffff", nextState:"on", defaultState: true
                attributeState "on", label:'Armed', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00A0DC", nextState:"off"
            }
            tileAttribute("device.clockTime", key: "SECONDARY_CONTROL") {
                attributeState "clockTime", label:'${currentValue}', defaultState: true
            }
            tileAttribute("device.hoursDisplay", key: "VALUE_CONTROL") {
                attributeState "VALUE_UP", action: "waitUp"
                attributeState "VALUE_DOWN", action: "waitDown"
            }
        }
        standardTile("alarm", "device.alarm", width: 2, height: 2) {
			state "Test", label:'Test', action:'doAlarm', backgroundColor:"#ffffff", defaultState: true
			state "strobe", label:'', action:'alarmOff', icon:"st.Lighting.light11", backgroundColor:"#e86d13"
			state "siren", label:'', action:'alarmOff', icon:"st.alarm.beep.beep", backgroundColor:"#e86d13"
			state "both", label:'', action:'alarmOff', icon:"st.alarm.alarm.alarm", backgroundColor:"#e86d13"
		}
        standardTile("alarmType", "device.alarmType", width: 2, height: 2) {
			state "strobe", label:'', action:'setSiren', nextState:"siren", icon:"st.Lighting.light11", backgroundColor:"#ffffff", defaultState: true
			state "siren", label:'', action:'setBoth', nextState:"both", icon:"st.alarm.beep.beep", backgroundColor:"#ffffff"
			state "both", label:'', action:'setStrobe', nextState:"strobe", icon:"st.alarm.alarm.alarm", backgroundColor:"#ffffff"
		}
		main "switch"
		details(["valueTile", "alarm", "alarmType"])

	}
    
    preferences {
        section("Choose input sensors"){
            input "delayTime", "number", title:"Delay (seconds) before turning off", defaultValue:10
        }
    }
    
}

def parse(String description) {
}

def updateHoursDisplay(newValue) {
	log.debug "newValue " + newValue
	def waitSeconds = newValue
	def waitMinutesTotal = (int)(waitSeconds / 60)
    def waitHours = (int)(waitMinutesTotal / 60)
    def waitMinutes = waitMinutesTotal % 60
    def time_string = sprintf('%d:%02d', waitHours, waitMinutes)
    sendEvent(name: "hoursDisplay", value: time_string)
    sendEvent(name: "waitTime", value: waitSeconds )
    setClockTime(newValue)
}

def setClockTime(newValueSeconds){
	def myDate = new Date((long)((new Date()).getTime() + newValueSeconds * 1000))
    def clock_string = myDate.format("h:mm a", location.timeZone)
    sendEvent(name: "clockTime", value: clock_string)
}

def waitUp() {
	sendEvent(name: "switch", value: "off")
    def currentValue = getWaitSeconds()
    def newValue = currentValue + 15*60
    updateHoursDisplay(newValue)
}

def waitDown() {
	sendEvent(name: "switch", value: "off")
	def currentValue = getWaitSeconds()
    def newValue = currentValue - 15*60
    updateHoursDisplay(newValue)
}

def getWaitSeconds(){
	def currentValue = device.currentValue("waitTime")
	if( currentValue == null ){
    	currentValue = 0
    }
    return currentValue
}

def on() {
	def waitSeconds = getWaitSeconds()
    if( waitSeconds < 5 ){
    	waitSeconds = 5
    }
	log.debug "on in " + waitSeconds
	sendEvent(name: "switch", value: "on")
    setClockTime(waitSeconds)
    runIn(waitSeconds, tryAlarm)
}

def setSiren(){
	log.debug "setSiren"
	sendEvent(name: "alarmType", value: "siren")
}

def setStrobe(){
	log.debug "setStrobe"
	sendEvent(name: "alarmType", value: "strobe")
}

def setBoth(){
	log.debug "setBoth"
	sendEvent(name: "alarmType", value: "both")
}

def off() {
	log.debug "off"
	sendEvent(name: "switch", value: "off")
    sendEvent(name: "clockTime", value: "")
}

def tryAlarm() {
	log.debug "current value: " + device.currentValue("switch").value
	if( device.currentValue("switch") == 'on' ){
    	doAlarm()
    }
}

def doAlarm() {
    log.debug "Doing alarm"
    def currentType = device.currentValue("alarmType")
    if( currentType == "strobe" ){
    	strobe();
    }else if( currentType == "siren" ){
    	siren();
    }else{
    	both();
    }
}

def strobe() {
	off()
	sendEvent(name: "alarm", value: "strobe")
    runIn(delayTime, alarmOff)
}

def siren() {
	off()
	sendEvent(name: "alarm", value: "siren")
    runIn(delayTime, alarmOff)
}

def both() {
	off()
	sendEvent(name: "alarm", value: "both")
    runIn(delayTime, alarmOff)
}

def alarmOff() {
	sendEvent(name: "alarm", value: "off")
}