// Automatically generated. Make future change here.
definition(
    name: "Mode Watcher",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Perform an action when the mode changes",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/areas.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/areas@2x.png"
)

preferences {
	section("Switches and modes") {
        input "modes", "mode", 
        	multiple: true,
        	title: "In these modes",  
            required: false
            
		input "switches", "capability.switch", 
			multiple: true, 
			title: "Turn on these switches", 
			required: false
            
        input "thermostats", "capability.thermostat", 
			multiple: true, 
			title: "Turn up these thermostat fans", 
			required: false
	}
}

def installed()
{
	subscribe(location, "mode", modeChangeHandler)
}

def updated()
{
	unsubscribe()
	subscribe(location, "mode", modeChangeHandler)
}

def modeChangeHandler(evt)
{
	log.info "switchOnHandler Event: ${evt.value}"
    log.info "${modes}"
    
    if (modes.contains(evt.value)){
    	switches?.on()
        thermostats?.setThermostatFanMode("on")
    }else{
    	switches?.off()
        thermostats?.setThermostatFanMode("auto")
    }
}