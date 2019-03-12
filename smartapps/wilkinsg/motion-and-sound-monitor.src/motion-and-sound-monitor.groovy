/**
 *  Lock Monitor
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
    name: "Motion and Sound Monitor",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Set motion status for a motion and sound detector",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section ("Allow external service to control these things...") {
    input "motionsensor", "capability.motionSensor", multiple: false, required: false
    input "soundsensor", "capability.soundSensor", multiple: false, required: false
    input "imagesensor", "capability.imageCapture", multiple: false, required: false
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
	
}

mappings {
	//use apiServerUrl to send our URL to the server?
  path("/motion/:command") {
    action: [
      PUT: "updateMotion"
    ]
  }
  path("/sound/:command") {
    action: [
      PUT: "updateSound"
    ]
  }
  path("/image/:command") {
    action: [
      PUT: "updateImage"
    ]
  }
}

def updateImage() {
	def command = params.command
    log.debug command
    
    //imagesensor?.parse("imageName: ${command}")
    imagesensor?.setImage(command)
    imagesensor?.take()
}

def updateSound() {
    def command = params.command
	
    log.debug command
    switch(command) {
        case "detected":
        	if(soundsensor.currentValue("sound") != "detected"){
            	soundsensor.detected()
            }
            break
        case "notdetected":
        	if(soundsensor.currentValue("sound") != "not detected"){
            	soundsensor.not_detected()
            }
            break
        default:
            httpError(400, "$command")
    }
}

def updateMotion() {
    def command = params.command

	log.debug command
    switch(command) {
        case "active":
        	if(motionsensor.currentValue("motion") != "active"){
            	motionsensor.active()
            }
            break
        case "inactive":
        	if(motionsensor.currentValue("motion") != "inactive"){
            	motionsensor.inactive()
            }
            break
        default:
            httpError(400, "$command")
    }
}