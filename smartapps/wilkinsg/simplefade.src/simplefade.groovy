/**
 *  Copyright 2016 SmartThings
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
 *  Gentle Wake Up
 *
 *  Author: Steve Vlaminck
 *  Date: 2013-03-11
 *
 * 	https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime.png
 * 	https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime%402x.png
 * 	Gentle Wake Up turns on your lights slowly, allowing you to wake up more
 * 	naturally. Once your lights have reached full brightness, optionally turn on
 * 	more things, or send yourself a text for a more gentle nudge into the waking
 * 	world (you may want to set your normal alarm as a backup plan).
 *
 */
definition(
	name: "SimpleFade",
	namespace: "wilkinsg",
	author: "Graham Wilkinson",
	description: "Slowly dim lights",
	category: "Health & Wellness",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime@2x.png"
)

preferences {
	section("Lights to dim") {
		input "slaves", "capability.switchLevel", 
			multiple: true, 
			title: "Slave Dimmer Switch(es)...", 
			required: true
	}
}

def installed() {
	log.debug "Installing 'Gentle Wake Up' with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updating 'Gentle Wake Up' with settings: ${settings}"

	def controller = getController()
	if (controller) {
		controller.label = app.label
	}

	initialize()
}

private initialize() {
	if (!getAllChildDevices()) {
		// create controller device and set name to the label used here
		def dni = "${new Date().getTime()}"
		log.debug "app.label: ${app.label}"
		addChildDevice("smartthings", "Gentle Wake Up Controller", dni, null, ["label": app.label])
		state.controllerDni = dni
	}
}

// ========================================================
// Controller
// ========================================================

def sendStartEvent(source) {
	log.trace "sendStartEvent(${source})"
	def eventData = [
			name: "sessionStatus",
			value: "running",
			descriptionText: "${app.label} has started dimming",
			displayed: true,
			linkText: app.label,
			isStateChange: true
	]
	if (source == "modeChange") {
		eventData.descriptionText += " because of a mode change"
	} else if (source == "schedule") {
		eventData.descriptionText += " as scheduled"
	} else if (source == "appTouch") {
		eventData.descriptionText += " because you pressed play on the app"
	} else if (source == "controller") {
		eventData.descriptionText += " because you pressed play on the controller"
	}

	sendControllerEvent(eventData)
}

def sendControllerEvent(eventData) {
	def controller = getController()
	if (controller) {
		controller.controllerEvent(eventData)
	}
}

def getController() {
	def dni = state.controllerDni
	if (!dni) {
		log.warn "no controller dni"
		return null
	}
	def controller = getChildDevice(dni)
	if (!controller) {
		log.warn "no controller"
		return null
	}
	log.debug "controller: ${controller}"
	return controller
}
