// Automatically generated. Make future change here.
definition(
    name: "Switch Linker",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Follows the switch state of another switch",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png"
)

preferences {
	section("When this...") { 
		input "masters", "capability.switch", 
			multiple: false, 
			title: "Master Switch...", 
			required: true
	}

	section("Then these will follow with on/off...") {
		input "slaves2", "capability.switch", 
			multiple: true, 
			title: "Slave On/Off Switch(es)...", 
			required: false
	}
    
    section("Then these will follow with maximum brightness...") {
		input "slaves", "capability.switchLevel", 
			multiple: true, 
			title: "Slave On/Off Switch(es)...", 
			required: false
	}
}

def installed()
{
	subscribe(masters, "switch.on", switchOnHandler)
	subscribe(masters, "switch.off", switchOffHandler)
	subscribe(masters, "switch", switchHandler)
}

def updated()
{
	unsubscribe()
	subscribe(masters, "switch.on", switchOnHandler)
	subscribe(masters, "switch.off", switchOffHandler)
	subscribe(masters, "switch", switchHandler)
	log.info "subscribed to all of switches events"
}

def switchHandler(evt){}

def switchOffHandler(evt) {
	log.info "switchoffHandler Event: ${evt.value}"
    slaves?.off()
	slaves2?.off()
}

def switchOnHandler(evt) {
	log.info "switchOnHandler Event: ${evt.value}"
    slaves?.setLevel(100)
	slaves2?.on()
}