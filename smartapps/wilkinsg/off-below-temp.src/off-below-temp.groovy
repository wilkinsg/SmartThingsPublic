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
		name: "Off Below Temp",
		namespace: "wilkinsg",
		author: "Graham Wilkinson",
		description: "Turn a switch off when the temperature drops below a specified level.",
		category: "Convenience",
    	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/its_getting_cold.png",
    	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/its_getting_cold@2x.png"
)

preferences {
	section("Choose input sensors"){
		input "tempSensor", "capability.temperatureMeasurement", title: "Temperature Sensor", required: true, multiple: false
        input "targetSwitch", "capability.switch", title: "Switch", required: true, multiple: false
        input "targetTemp", "number", title:"Set desired minimum temperature", defaultValue:72
	}
	section("Minimum time between changes (optional)") {
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
	subscribe(tempSensor, "temperature", eventHandler)
}

def eventHandler(evt) {
    log.debug "Notify got evt for ${evt.deviceId}"
    log.debug "The value of this event is ${evt.value}"
    
    def lastTime = state[evt.deviceId]
    if (lastTime == null || now() - lastTime >= frequency * 60000) {
    	if (evt.value.toFloat() <= targetTemp.toFloat()) {
        	log.debug "Turning off"
        	targetSwitch.off()
            state[evt.deviceId] = now()
        } else {
        	log.debug "Skipping due to temperature"
        }
    } else {
    	log.debug "Skipping due to time"
    }
}
