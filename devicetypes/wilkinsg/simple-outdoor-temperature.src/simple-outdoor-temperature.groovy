/**
 *  Simple Outdoor Temperature
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
metadata {
	definition (name: "Simple Outdoor Temperature", namespace: "wilkinsg", author: "Graham Wilkinson") {
		capability "Temperature Measurement"
        capability "Polling"
        capability "Refresh"
	}
    
    preferences {
		input name: "zipCode", "text", title: "Zip Code (optional)", required: false
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
    
    def result = null
    try {
        httpGet(params) { resp ->
            result = "${resp.data}".toInteger()
        }
    } catch (e) {
        log.error "something went wrong: $e"
        result = null
    }
    return result
}

private Integer get_current() {
	def val = readTemp("/current.txt")
    if( val == null ){
    	log.error "Falling back to API Temperature"
        val = getTwcConditions(zipCode)?.temperature
    }
    return val
}

private Integer get_low(current, forecast) {
    def temperatureMin = forecast?.temperatureMin
    def val = null
    if(temperatureMin != null){
    	val = temperatureMin[0]
    }
    if( val == null ){
    	log.error "Falling back to NWS Low"
    	val = readTemp("/low.txt")
    }
    if( current != null && val != null){
    	val = Math.min(val, current)
    }
    return val
}

private Integer get_high(current, forecast) {
	def temperatureMax = forecast?.temperatureMax
    def val = null
    if(temperatureMax != null){
    	val = temperatureMax[0]
    }
    if( val == null ){
    	log.error "Falling back to NWS High"
    	val = readTemp("/high.txt")
    }
    if( current != null && val != null){
        val = Math.max(val, current)
    }
    return val
}

def refresh() {
    def obs
    def current = get_current()
    def forecast = getTwcForecast(zipCode)
    
    if( tempType == "High" ){
    	obs = get_high(current, forecast)
    }else if( tempType == "Low" ){
    	obs = get_low(current, forecast)
    }else{
    	obs = current
    }
    log.debug "$tempType = ${obs}"
	if (obs) {
		sendEvent(name: "temperature", value: obs, unit: "F")
    }
}