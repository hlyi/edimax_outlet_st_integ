/**
 *  Copyright 2016 H. Yi
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
			state "off", label:'off', action:"switch.on",  icon:"st.switches.switch.off", backgroundColor:"#ffffff"
			state "on",  label:'on', action:"switch.off", icon:"st.switches.switch.on",  backgroundColor:"#79b821"
//			state "turningOn", label:'Turning on', icon:"st.switches.switch.on", backgroundColor:"#B6F59A", nextState: "on"
//			state "turningOff", label:'Turning off', icon:"st.switches.switch.off", backgroundColor:"#D3D3D3", nextState: "off"
		}
		standardTile("refresh", "device.power", decoration: "flat") {
			state "power", label:'${currentValue} W', action:"refresh", defaultState: true
		}

		main "switch"
		details(["switch","refresh"])
	}
}

preferences {
	input("outletIP", "text", title: "Outlet IP", required: true, displayDuringSetup: true)
	input("username", "text", title: "Username", required: true, displayDuringSetup: true)
	input("password", "text", title: "Password", required: true, displayDuringSetup: true)
}


private getOutletStatus(){
	sendMsgToOutlet('get', "<Device.System.Power.State/>")
}

private getOutletPower(){
	sendMsgToOutlet('get', "<NOW_POWER><Device.System.Power.NowPower/></NOW_POWER>")
}

def hubActionCallback(response){
//	log.debug "Edimax resp HDR: " + response.headers
//	log.debug "Edimax resp Body: " + response.body
	def rsp =new XmlSlurper().parseText( response.body)
//	log.debug "Status: " + rsp.CMD."Device.System.Power.State"
	def status = rsp?.CMD?."Device.System.Power.State"
	if ( status == "ON" || status== "OFF" ){
		status = "on"
		if ( rsp == "OFF" ){
			status = "off"
		}
//		log.debug "CURRENT status: " + status
		sendEvent(name: "switch", value: status, isStateChange: true)
		return status
	}
	status = rsp?.CMD?.NOW_POWER?."Device.System.Power.NowPower"
	if ( status && status != "" ) {
//		log.debug "POWER: " + status
		sendEvent ( name: "power", value: status.toFloat(), unit: "W", isStateChange: true)
	}
}


private sendStateCmd (command){
	sendMsgToOutlet('setup', "<Device.System.Power.State>${command}</Device.System.Power.State>")
}

private sendMsgToOutlet(cmd, msg){
//	log.debug "Edimax: Command"

	def userpassascii = "${settings.username}:${settings.password}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
	def uri = "/smartplug.cgi"
	def headers = [:]
	headers.put("HOST", "${settings.outletIP}:10000")
	headers.put("Authorization", userpass)
//	log.debug "Headers are ${headers}"
	def deviceNetworkId = "0A0A0ABD:2710"
	def body = '<?xml version="1.0" encoding="UTF8"?> <SMARTPLUG id="edimax"> <CMD id="' + cmd + '">' + msg + '</CMD> </SMARTPLUG>"'
//	log.debug ( "SEND BODY: ${body}")
	try {
		sendHubCommand(new physicalgraph.device.HubAction([
			method: "POST",
			path: uri,
			headers: headers,
			body: body ],
			deviceNetworkId,
			[callback: "hubActionCallback"]
		))
	} catch(e){
		//handle exception here.
		log.debug "Edimax: Http Error" + e.message
	}
}


def parse(String description) {
	log.debug("Parsing ${description}")
}

def on() {
//	sendEvent(name: "switch", value: "turningOn", isStateChange: true)
	sendStateCmd ("ON")
    sendEvent(name: "switch", value: "on", isStateChange: true)
	getOutletStatus()
}

def off() {
//	sendEvent(name: "switch", value: "turningOff", isStateChange: true)
	sendStateCmd ("OFF")
    sendEvent(name: "switch", value: "off", isStateChange: true)
	getOutletStatus()
}

def poll() {
	getOutletStatus()
	getOutletPower()
}

def refresh() {
	getOutletStatus()
	getOutletPower()
	return ''
}
