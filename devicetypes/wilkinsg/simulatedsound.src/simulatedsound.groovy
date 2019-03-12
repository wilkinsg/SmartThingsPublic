/**
 *  SimulatedSound
 *
 *  Copyright 2019 Graham Wilkinson
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
	definition (name: "SimulatedSound", namespace: "wilkinsg", author: "Graham Wilkinson") {
		capability "Sound Sensor"
        capability "Sensor"
        capability "Actuator"
        capability "Switch"
        
        command "detected"
        command "not_detected"
	}

	simulator {
		status "detected": "sound:detected"
		status "not detected": "sound:not detected"
	}

	tiles {
		standardTile("sound", "device.sound", width: 1, height: 1, canChangeIcon: true, canChangeBackground: true) {
    			state "detected", label: 'Loud', action: "not_detected", icon: "st.Entertainment.entertainment15", backgroundColor: "#00A0DC"
		      	state "not detected", label: 'Quiet', action: "detected", icon: "st.Entertainment.entertainment15", backgroundColor: "#ffffff"
		}
		main "sound"
		details(["sound"])

	}
}

def parse(String description) {
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def detected() {
	detected_helper()
}

def not_detected() {
	not_detected_helper()
}

def on() {
	detected_helper()
}

def off() {
	not_detected_helper()
}

def detected_helper() {
	log.debug "detected"
	sendEvent(name: "sound", value: "detected", isStateChanged: true)
    sendEvent(name: "switch", value: "on", isStateChanged: true)
}

def not_detected_helper() {
	log.debug "not detected"
	sendEvent(name: "sound", value: "not detected", isStateChanged: true)
    sendEvent(name: "switch", value: "off", isStateChanged: true)
}
