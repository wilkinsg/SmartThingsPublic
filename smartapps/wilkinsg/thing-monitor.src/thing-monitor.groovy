definition(
    name: "Thing Monitor",
    namespace: "wilkinsg",
    author: "Graham Wilkinson",
    description: "Poll or refresh device status periodically.",
    category: "Convenience",
    //iconUrl: "http://winfred-louder.com/st/watch_0.png",
    //iconX2Url: "http://winfred-louder.com/st/watch_0_2x.png"
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Family/App-SmartCurfew.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Family/App-SmartCurfew@2x.png"
    )

preferences {
    section("Devices") {
        input "healthDevices", "capability.healthCheck", title:"Select devices to be pinged", multiple:true, required:false
        input "batteryDevices", "capability.battery", title:"Select devices with batteries", multiple:true, required:false
        input "interval", "number", title:"Set polling interval (in minutes)", defaultValue:5
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def onAppTouch(event) {
    LOG("onAppTouch(${event.value})")

    watchdog()
    pollingTask()
}

def pollingTask() {
    LOG("pollingTask()")

    state.trun = now()

    settings.healthDevices?.each {
        def result = it.ping()
        log.info it.id
        log.info result
    }

    settings.batteryDevices?.each {
        def result = it.currentValue("battery")
        log.info it.id
        log.info result
    }
}

def watchdog() {
    LOG("watchdog()")
    def interval = settings.interval.toInteger()
    def trun = state.trun

    if (interval && trun && ((now() - trun) > ((interval + 10) * 60000))) {
        log.warn "Polling task #${n} stalled. Restarting..."
        restart()
        return
    }
}

private def initialize() {
    LOG("initialize() with settings: ${settings}")
    state.trun = 0

    Random rand = new Random(now())
    def numTasks = 0
    def minutes = settings."interval".toInteger()
    def seconds = rand.nextInt(60)
    def size1 = settings["healthDevices"]?.size() ?: 0
    def size2 = settings["batteryDevices"]?.size() ?: 0

    safeUnschedule("pollingTask")

    if (minutes > 0 && (size1 + size2) > 0) {
        LOG("Scheduling polling task to run every ${minutes} minutes.")
        def sched = "${seconds} 0/${minutes} * * * ?"
        schedule(sched, "pollingTask")
        numTasks++
    }

    if (numTasks) {
        subscribe(app, onAppTouch)
    }

    LOG("state: ${state}")
}

private def safeUnschedule() {
    try {
        unschedule()
    }

    catch(e) {
        log.error ${e}
    }
}

private def safeUnschedule(handler) {
    try {
        unschedule(handler)
    }

    catch(e) {
        log.error ${e}
    }
}

private def restart() {
    updated()
}

private def LOG(message) {
    log.trace message
}