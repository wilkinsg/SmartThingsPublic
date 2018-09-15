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
        definition (name: "Timed Switch", namespace: "wilkinsg", author: "Graham Wilkinson") {
        capability "Switch"
    }

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true, canChangeBackground: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
    			attributeState "off", label: '${name}', action: "switch.on", icon: "st.Health & Wellness.health7", backgroundColor: "#ffffff", nextState: "turningOn"
		      	attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
		      	attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
        	}
		}
		main "switch"
		details(["switch"])

	}
    
    preferences {
        section("Choose input sensors"){
            input "delayTime", "number", title:"Delay (seconds) before turning off", defaultValue:5
        }
    }
    
}

def parse(String description) {
}

def on() {
	log.debug "on"
	sendEvent(name: "switch", value: "on")
    runIn(delayTime, off)
}

def off() {
	log.debug "off"
	sendEvent(name: "switch", value: "off")
}
