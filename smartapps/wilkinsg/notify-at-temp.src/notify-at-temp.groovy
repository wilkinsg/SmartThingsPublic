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
		name: "Notify At Temp",
		namespace: "wilkinsg",
		author: "Graham Wilkinson",
		description: "Receive notifications when one temperature passes another.",
		category: "Convenience",
    	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/text_accelerometer.png",
    	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text_accelerometer@2x.png"
)

preferences {
	section("Choose input sensors"){
		input "indoorSensor", "capability.temperatureMeasurement", title: "Indoor Temperature", required: true, multiple: false
        input "outdoorSensor", "capability.temperatureMeasurement", title: "Outdoor Temperature", required: true, multiple: false
        input "targetTemp", "number", title:"Set desired indoor temperature", defaultValue:75
	}
	section("Minimum time between messages (optional, defaults to every message)") {
		input "frequency", "decimal", title: "Minutes", required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(indoorSensor, "temperature", eventHandler)
    subscribe(outdoorSensor, "temperature", eventHandler)
    log.debug "Indoor: " + indoorSensor.getId()
    state["indoorTemp"] = 0
    log.debug "Outdoor: " + outdoorSensor.getId()
    state["outdoorTemp"] = 0
}

def eventHandler(evt) {
    log.debug "Notify got evt for ${evt.deviceId}"
    log.debug "The value of this event is ${evt.value}"

	def indoorTemp = state["indoorTemp"]
    def outdoorTemp = state["outdoorTemp"]

    if( evt.deviceId == indoorSensor.getId() ){
        log.debug "Got indoor change"
        indoorTemp = evt.value
    }else{
        log.debug "Got outdoor change"
        outdoorTemp = evt.value
    }
    
    def lastTime = state[evt.deviceId]
    if (lastTime == null || now() - lastTime >= frequency * 60000) {
        sendMessage(evt, indoorTemp, outdoorTemp)
    }
    
    state["indoorTemp"] = indoorTemp
    state["outdoorTemp"] = outdoorTemp
}

private sendMessage(evt, newIndoorTemp, newOutdoorTemp) {
    def oldIndoorTemp = state["indoorTemp"]
    def oldOutdoorTemp = state["outdoorTemp"]
    def indoorAdjustment = 4

    log.debug "oldOutdoorTemp=" + oldOutdoorTemp
    log.debug "oldIndoorTemp=" + oldIndoorTemp
    log.debug "newOutdoorTemp=" + newOutdoorTemp
    log.debug "newIndoorTemp=" + newIndoorTemp
	if( oldOutdoorTemp.toFloat() > oldIndoorTemp.toFloat() && newOutdoorTemp.toFloat() <= newIndoorTemp.toFloat() && newIndoorTemp.toFloat() > targetTemp.toFloat() ){
        String msg = "Check fan, ${newOutdoorTemp}° outside and ${newIndoorTemp}° inside"
        Map options = [:]
        options = [translatable: true, triggerEvent: evt]
        log.debug "$evt.name:$evt.value"
        log.debug 'Sending push'
        options.method = 'push'
        sendNotification(msg, options)
        state[evt.deviceId] = now()
    }
}
