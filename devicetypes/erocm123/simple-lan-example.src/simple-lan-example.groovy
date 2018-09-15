import groovy.json.JsonSlurper

metadata {
	definition (name: "Simple LAN Example", namespace: "erocm123", author: "Eric Maycock") {
	capability "Sensor"
        
	}
   
	tiles (scale: 2){      
        valueTile("hubInfoPlain", "device.hubInfo", decoration: "flat", height: 2, width: 6, inactiveLabel: false, canChangeIcon: false) {
            state "hubInfo", icon: "st.motion.motion.inactive", label:''
        }
        valueTile("hubInfo", "device.hubInfo", decoration: "flat", height: 2, width: 6, inactiveLabel: false, canChangeIcon: false) {
            state "hubInfo", label:'${currentValue}'
        }
    }
	main("hubInfoPlain")
	details(["hubInfo"])
}


def parse(description) {
    def events = []
    def descMap = parseDescriptionAsMap(description)
    def body = new String(descMap["body"].decodeBase64())
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    log.debug result

    if (result.containsKey("message")) {
       events << createEvent(name:"hubInfo", value:result.message)
    }
    return events
}

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
        
        if (nameAndValue.length == 2) map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
        else map += [(nameAndValue[0].trim()):""]
	}
}