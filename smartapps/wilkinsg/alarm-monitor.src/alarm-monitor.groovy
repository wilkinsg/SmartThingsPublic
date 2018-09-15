// Automatically generated. Make future change here.
definition(
    name: "Alarm Monitor",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Follows the switch state of another switch",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine@2x.png"
)

preferences {
	page(name: "selectActions")
}

def selectActions() {
    dynamicPage(name: "selectActions", title: "Select Routine Action to Execute", install: true, uninstall: true) {            
        section("When this alarm is triggered...") { 
            input "masters", "capability.alarm", 
                multiple: true, 
                title: "Alarm", 
                required: true
        }

        section("Then these will turn on...") {
            input "slaves", "capability.switch", 
                multiple: true, 
                title: "Slave On/Off Switch(es)...", 
                required: false
        }
        def actions = location.helloHome?.getPhrases()*.label
        if (actions) {
            // sort them alphabetically
            actions.sort()
            section("And this routine will be run") {
                log.trace actions
                // use the actions as the options for an enum input
                input "action", "enum", title: "Select an action to execute", options: actions, required: false
            }
        }
    }
}

def installed()
{
	subscribe(masters, "alarm.strobe", alarmHandler)
	subscribe(masters, "alarm.siren", alarmHandler)
	subscribe(masters, "alarm.both", alarmHandler)
}

def updated()
{
	unsubscribe()
	subscribe(masters, "alarm.strobe", alarmHandler)
	subscribe(masters, "alarm.siren", alarmHandler)
	subscribe(masters, "alarm.both", alarmHandler)
	log.info "subscribed to all alarm events"
}

def alarmHandler(evt){
	log.info "alarmHandler Event: ${evt.value}"
    if( evt.value == 'both' || evt.value == 'strobe' ){
    	log.info 'Activating Strobe'
        slaves?.on()
    }
    if( evt.value == 'both' || evt.value == 'siren' ){
    	log.info 'Siren Not Implemented'
    }
    if( settings.action ){
    	location.helloHome?.execute(settings.action)
    }
}
