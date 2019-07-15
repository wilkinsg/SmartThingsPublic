/**
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

definition(
    name: "Persistent Sound Notify",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Notify when sound stays active.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/text_accelerometer.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text_accelerometer@2x.png"
)

preferences {
	section("Check when this opens..."){
		input "sound1", "capability.soundSensor", title: "Where?", required: true
	}
	section("And stays open for..."){
		input "minutes1", "number", title: "Minutes?", required: true
	}
}

def installed() {
	subscribe(sound1, "sound", soundHandler)
    state["active"] = false
}

def updated() {
	unsubscribe()
    subscribe(sound1, "sound", soundHandler)
    state["active"] = false
}

def soundHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "detected") {
		state["active"] = true
        state["activeTime"] = now()
        runIn(minutes1 * 60, scheduleCheck, [overwrite: false])
	} else if (evt.value == "not detected") {
        state["active"] = false
	}
}

def scheduleCheck() {
	log.debug "schedule check"
	def deviceState = state["active"]
    log.debug(deviceState)
    if (deviceState == true) {
        def elapsed = now() - state["activeTime"]
    	def threshold = 1000 * 60 * minutes1 - 1000
    	if (elapsed >= threshold) {
            log.debug "Device has stayed active long enough since last check ($elapsed ms): notifying"
            String msg = "Noise at home"
            Map options = [:]
            options = [translatable: true]
            log.debug 'Sending push'
            options.method = 'push'
            sendNotification(msg, options)
    	} else {
        	log.debug "Device has not stayed active long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
    	log.debug "Device is inactive, do nothing"
    }
}