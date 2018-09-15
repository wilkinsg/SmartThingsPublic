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
 *  Light Follows Me
 *
 *  Author: SmartThings
 */

definition(
    name: "Light Follows Me With Override",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Turn your lights on when motion is detected and then off again once the motion stops for a set period of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet@2x.png"
)

preferences {
	section("Turn on when there's movement..."){
		input "motion1", "capability.motionSensor", title: "Where?", required: true
	}
	section("And off when there's been no movement for..."){
		input "minutes1", "number", title: "Minutes?", required: true
	}
        section("And this switch is off"){
		input "override", "capability.switch", required: true
	}
	section("Turn on/off light(s)..."){
		input "switches", "capability.switchLevel", multiple: true, required: true
	}
}

def installed() {
	subscribe(motion1, "motion", motionHandler)
    subscribe(override, "switch.off", switchOffHandler)
}

def updated() {
	unsubscribe()
    subscribe(motion1, "motion", motionHandler)
	subscribe(override, "switch.off", switchOffHandler)
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
    if (override.currentState("switch").value == 'off'){
        if (evt.value == "active") {
            log.debug "turning on lights"
            switches.setLevel(255)
        } else if (evt.value == "inactive") {
            runIn(minutes1 * 60, scheduleCheck, [overwrite: false])
        }
    }else{
    	log.debug "Ignored motion due to override switch"
    }
}

def switchOffHandler(evt) {
	log.info "switchoffHandler Event: ${evt.value}"
    runIn(30, scheduleCheck, [overwrite: false])
}

def scheduleCheck() {
	log.debug "schedule check"
    if (override.currentState("switch").value == 'off'){
        def motionState = motion1.currentState("motion")
        if (motionState.value == "inactive") {
            def elapsed = now() - motionState.rawDateCreated.time
            def threshold = 1000 * 60 * minutes1 - 1000
            if (elapsed >= threshold ) {
                log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning lights off"
                switches.off()
            } else {
                log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
                runIn(60, scheduleCheck, [overwrite: false])
            }
        } else {
            log.debug "Motion is active, do nothing and wait for inactive"
        }
    } else {
		log.debug "skipped schedule check due to override switch"
    }
}