/**
 *  Copyright 2015 SmartThings
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
	definition (name: "Edimax_Outlet", namespace: "hlyi", author: "H Yi") {
		capability "Energy Meter"
		capability "Actuator"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"

		command "reset"

	}

	// simulator metadata
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		for (int i = 0; i <= 100; i += 10) {
			status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV2.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
		}

		// reply messages
		reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"

	}

	// tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
		valueTile("energy", "device.energy", decoration: "flat") {
			state "default", label:'${currentValue} kWh'
		}
		standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat") {
			state "default", label:'reset kWh', action:"reset"
		}
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "switch"
		details(["switch","energy","reset","refresh"])
	}
}

preferences {
	input("outletIP", "text", title: "Outlet IP", required: true, displayDuringSetup: true)
	input("username", "text", title: "Username", required: true, displayDuringSetup: true)
	input("password", "text", title: "Password", required: true, displayDuringSetup: true)
}


private getOutletStatus(){
}

private getOutletPower(){
}

def hubActionCallback(response){
	log.debug "Edimax resp: " + response
}

private sendCommand(command){
	log.debug "Edimax: Command"

//	def userpassascii = "${username}:${password}"
	def userpassascii = "admin:54321"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
	def uri = "/smartplug.cgi"
	def headers = [:]
	headers.put("HOST", "10.10.10.217:10000")
	headers.put("Authorization", userpass)
	log.debug "Headers are ${headers}"
	deviceNetworkId = "0A0A0AD9:2710"

	try {
		sendHubCommand(new physicalgraph.device.HubAction([
			method: "POST",
			path: uri,
			headers: headers
			body: URLEncoder.encode('<?xml version="1.0" encoding="UTF8"?> <SMARTPLUG id="edimax"> <CMD id="get"> <Device.System.Power.State/> </CMD> </SMARTPLUG>"')]
			deviceNetworkId,
			[callback: "hubActionCallback"]
		))
	} catch(e){
		//handle exception here.
		log.debug "Http Error" + e.message
	}

}

def parse(String description) {
	log.debug("${description}")
}

def on() {
	sendCommand ("ON")
	getOutletStatus()
}

def off() {
	sendCommand ("OFF")
	getOutletStatus()
}

def poll() {
	getOutletStatus()
	getOutletPower()
}

def refresh() {
	getOutletStatus()
}

def reset() {
	log.debug "Edimax: Reset"
	def params = [
            uri: "http://admin:54321@10.10.10.217:10000/smartplug.cgi",
            body: URLEncoder.encode('<?xml version="1.0" encoding="UTF8"?> <SMARTPLUG id="edimax"> <CMD id="get"> <Device.System.Power.State/> </CMD> </SMARTPLUG>"')
        ]
               
        try {
            httpPost(params) { resp -> 
               log.debug resp.data;
               //do stuff here
            }
       }
       catch(e){
           //handle exception here.
       }
	
}
