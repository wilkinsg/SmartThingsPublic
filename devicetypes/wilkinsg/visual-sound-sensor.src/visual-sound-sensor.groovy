/**
 *  SimulatedSound
 *
 *  Copyright 2019 Graham Wilkinson
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
	definition (name: "Visual Sound Sensor", namespace: "wilkinsg", author: "Graham Wilkinson") {
		capability "Sound Sensor"
        capability "Sensor"
        capability "Actuator"
        capability "Switch"
        capability "Image Capture"
        
        command "detected"
        command "not_detected"
        command "setImage"
	}

	simulator {
		status "detected": "sound:detected"
		status "not detected": "sound:not detected"
	}
    
    preferences {
		input("imageUri", "text", title:"Server URL", description: "e.g. https://example.com", required: true, displayDuringSetup: true)
		input("imagePath", "text", title:"Server Path", description: "e.g. /example_path", required: true, displayDuringSetup: true)
        input("imageUseLogin", "bool", title:"Use Authentication?", description: "Use HTTP username and password?", required: true, displayDuringSetup: true)
        input("imageUser", "text", title:"Server User", description: "Login username", required: false, displayDuringSetup: true)
        input("imagePass", "password", title:"Server Password", description: "Login password", required: false, displayDuringSetup: true)
	}

	tiles {
    	carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }
		standardTile("sound", "device.sound", width: 1, height: 1, canChangeIcon: true, canChangeBackground: true) {
    			state "detected", label: 'Loud', action: "not_detected", icon: "st.Entertainment.entertainment15", backgroundColor: "#00A0DC"
		      	state "not detected", label: 'Quiet', action: "detected", icon: "st.Entertainment.entertainment15", backgroundColor: "#ffffff"
		}
		main "sound"
		details(["cameraDetails", "sound"])

	}
}

def parse(String description) {
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def setImage(String name){
	sendEvent(name: "image", value: name, isStateChanged: true)
}

def detected() {
	detected_helper()
}

def not_detected() {
	not_detected_helper()
}

def on() {
	detected_helper()
}

def off() {
	not_detected_helper()
}

def detected_helper() {
	log.debug "detected"
	sendEvent(name: "sound", value: "detected", isStateChanged: true)
    sendEvent(name: "switch", value: "on", isStateChanged: true)
}

def not_detected_helper() {
	log.debug "not detected"
	sendEvent(name: "sound", value: "not detected", isStateChanged: true)
    sendEvent(name: "switch", value: "off", isStateChanged: true)
}

def take() {
	def name = device.currentValue("image")    
    def params
    if(imageUseLogin){
    	def auth = "${imageUser}:${imagePass}".bytes.encodeBase64()
    	//log.debug "auth ${auth}"
        params = [
            uri: imageUri,
            path: "${imagePath}/${name}",
            headers: [Authorization: "Basic ${auth}" ]
        ]
    } else {
    	params = [
            uri: imageUri,
            path: "${imagePath}/${name}"
        ]
    }
    log.debug "Requesting ${params.uri}${params.path}"

    try {
        httpGet(params) { response ->
            // we expect a content type of "image/png" from the third party in this case
            if (response.status == 200 && response.headers.'Content-Type'.contains("image/png")) {
                def imageBytes = response.data
                if (imageBytes) {
                    try {
                        storeImage(name, imageBytes)
                        log.debug "Stored ${name}"
                    } catch (e) {
                        log.error "Error storing image ${name}: ${e}"
                    }

                }
            } else {
                log.error "Image response not successful or not a png response"
            }
        }
    } catch (err) {
        log.debug "Error making request: $err"
    }
}