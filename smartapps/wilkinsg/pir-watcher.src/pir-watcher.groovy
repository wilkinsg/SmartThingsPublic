/**
 *  PIR Watcher
 *
 *  Copyright 2017 Graham Wilkinson
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
definition(
    name: "PIR Watcher",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Get motion events from a generic PIR sensor",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section ("Allow external service to control these things...") {
    input "motionSensors", "capability.motionSensor", multiple: true, required: true
  }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

mappings {
  path("/motionSensors") {
    action: [
      GET: "listMotionSensors"
    ]
  }
  path("/motionSensors/:command") {
    action: [
      PUT: "updateMotionSensors"
    ]
  }
}

def listMotionSensors() {
    def resp = []
    motionSensors.each {
      resp << [name: it.displayName, value: it.currentValue("motionSensor")]
    }
    return resp
}

void updateMotionSensors() {
    // use the built-in request object to get the command parameter
    def command = params.command

    // execute the command on all motion sensors
    // (note we can do this on the array - the command will be invoked on every element
    switch(command) {
        case "active":
            motionSensors.active()
            break
        case "inactive":
            motionSensors.inactive()
            break
        default:
            httpError(400, "$command is not a valid command for all motion sensors specified")
    }
}