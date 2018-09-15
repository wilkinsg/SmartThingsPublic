/**
 *  National Weather Service Outdoor Temperature
 *
 *  Copyright 2018 Graham Wilkinson
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
	definition (name: "NWS Outdoor Temperature", namespace: "wilkinsg", author: "Graham Wilkinson") {
		capability "Temperature Measurement"
        capability "Polling"
        capability "Refresh"
	}

	simulator {}
    
    preferences {
        input name: "tempType", type: "enum", title: "Temperature Type", options: ["Current", "High", "Low"], description: "Select the type of temperature to measure", required: true
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"temperature", type: "generic", width: 6, height: 4){
			tileAttribute ("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("temperature", icon:"st.Weather.weather2", label:'${currentValue}°', unit:"F",
                backgroundColors:[
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
            )
			}
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		
        main(["temperature"])
        details(["temperature", "refresh"])
    }
}

def parse(String description) {
	log.debug "Parsing '${description}'"
}

def poll() {
	refresh()
}

private Integer readTemp(String path) {
	def params = [
        uri: "http://winfred-louder.com",
        path: "$path"
    ]
    
    def result = 255
    try {
        httpGet(params) { resp ->
            result = "${resp.data}".toInteger()
        }
    } catch (e) {
        log.error "something went wrong: $e"
        result = 255
    }
    return result
}

def refresh() {
	log.debug "Executing 'refresh', location: ${location.name}"
    
    def obs = 255
    log.debug "Current= ${current}"
    if( tempType == "Current" ){
    	obs = readTemp("/current.txt")
    }else if( tempType == "High" ){
    	obs = readTemp("/high.txt")
    }else if( tempType == "Low" ){
    	obs = readTemp("/low.txt")
    }
	if (obs != 255) {
    	log.debug "Updated $tempType to $obs"
		sendEvent(name: "temperature", value: obs, unit: "F")
    }
}