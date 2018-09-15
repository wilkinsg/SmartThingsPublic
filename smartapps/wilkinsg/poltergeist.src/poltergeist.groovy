// Automatically generated. Make future change here.
definition(
    name: "Poltergeist",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Make everything go nuts",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MiscHacking/mindcontrol.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MiscHacking/mindcontrol@2x.png"
)

import org.apache.commons.lang.math.RandomUtils

preferences {
	section("Switches") {
    	input "masters", "capability.switch", 
			multiple: true, 
			title: "Master Switch", 
			required: true
            
		input "slaves", "capability.switchLevel", 
			multiple: true, 
			title: "Dimmable Switch(es)...", 
			required: false
            
		input "slaves2", "capability.switch", 
			multiple: true, 
			title: "On/Off Switch(es)...", 
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
    unschedule(randomizeState)
	subscribe(masters, "switch.on", switchOnHandler)
	subscribe(masters, "switch.off", switchOffHandler)
	subscribe(masters, "switch", switchHandler)
	log.info "subscribed to all of switches events"
}

def switchHandler(evt){}

def randomizeState(evt){
	if(state.running){
        log.info "Randomizing state"
        Random rand = new Random()
        slaves2?.each {
            if(rand.nextBoolean()){
                it.on()
            }
            else{
                it.off()
            }
        }
        slaves?.each {
            if(rand.nextBoolean()){
                it.setLevel(rand.nextInt(101))
            }
            else{
                it.off()
            }
        }
    	runIn(rand.nextInt(4) + 1, randomizeState)
    }
}

def switchOffHandler(evt) {
	log.info "switchoffHandler Event: ${evt.value}"
    state.running = 0
    unschedule(randomizeState)
    slaves?.each {
    	log.info it.id
        if(state.startStates[it.id] == 0){
        	it.off()
        }else{
        	it.setLevel(state.startStates[it.id])
        }
    }
    slaves2?.each {
    	log.info it.id
       	if(state.startStates2[it.id] == "on"){
        	it.on()
        }else{
        	it.off()
        }
    }
}

def switchOnHandler(evt) {
	log.info "switchOnHandler Event: ${evt.value}"
    //state.collection = []
    //slaves?.each {
    //	log.warn it.id
	//}
    state.running = 1
    state.startStates = [a: 0]
    state.startStates2 = [a: 0]
    slaves?.each {
    	log.info it.id
        if(it.currentState("switch").value == "off"){
        	state.startStates[it.id] = 0
        }else{
        	state.startStates[it.id] = it.currentState("level").value
        }
    }
    slaves2?.each {
    	log.info it.id
    	state.startStates2[it.id] = it.currentState("switch").value
    }
	randomizeState()
}