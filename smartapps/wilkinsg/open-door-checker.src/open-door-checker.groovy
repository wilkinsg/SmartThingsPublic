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
    name: "Open door checker",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Notify when the door stays open.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/text_accelerometer.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text_accelerometer@2x.png"
)

preferences {
	section("Check when this opens..."){
		input "contact1", "capability.contactSensor", title: "Where?"
	}
	section("And stays open for..."){
		input "minutes1", "number", title: "Minutes?"
	}
}

def installed() {
	subscribe(contact1, "contact", motionHandler)
    state["open"] = "closed"
}

def updated() {
	unsubscribe()
    subscribe(contact1, "contact", motionHandler)
    state["open"] = "closed"
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "open") {
		state["open"] = "open"
        state["opentime"] = now()
        runIn(minutes1 * 60, scheduleCheck, [overwrite: false])
	} else if (evt.value == "closed") {
        state["open"] = "closed"
	}
}

def scheduleCheck() {
	log.debug "schedule check"
	def contactState = state["open"]
    log.debug(contactState)
    if (contactState == "open") {
        def elapsed = now() - state["opentime"]
    	def threshold = 1000 * 60 * minutes1 - 1000
    	if (elapsed >= threshold) {
            log.debug "Contact has stayed open long enough since last check ($elapsed ms): notifying"
            String msg = "Door stuck open"
            Map options = [:]
            options = [translatable: true]
            log.debug 'Sending push'
            options.method = 'push'
            sendNotification(msg, options)
    	} else {
        	log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
    	log.debug "Contact is closed, do nothing"
    }
}