/**
 *  Virtual Temperature Tile
 *
 *  Copyright (c) 2014 Statusbits.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License. You may obtain a
 *  copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License  for the specific language governing permissions and limitations
 *  under the License.
 *
 *  2014-08-28  V1.1.0  parse takes 'temperature:<value>' as an argument
 *  2014-08-10  V1.0.0  Initial release
 *  08-27-2016 : Modified the device handler for my liking, primarly for looks and feel.
 *
 */

metadata {
    definition (name:"My Virtual Temp Tile", namespace:"jscgs350", author:"geko@statusbits.com") {
        capability "Temperature Measurement"
        capability "Sensor"

        // custom commands
        command "parse"     // (String "temperature:<value>")
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
        main(["temperature"])
        details(["temperature"])
    }

    simulator {
        for (int i = 20; i <= 110; i += 10) {
            status "Temperature ${i}°": "temperature:${i}"
        }
        status "Invalid message" : "foobar:100.0"
    }
}

def parse(String message) {
    TRACE("parse(${message})")

    Map msg = stringToMap(message)
    if (!msg.containsKey("temperature")) {
        log.error "Invalid message: ${message}"
        return null
    }

    Float temp = msg.temperature.toFloat()
    def event = [
        name  : "temperature",
        value : (int)temp,//.round(1),
        unit  : tempScale,
    ]

    TRACE("event: (${event})")
    sendEvent(event)
}

private def TRACE(message) {
    //log.debug message
}