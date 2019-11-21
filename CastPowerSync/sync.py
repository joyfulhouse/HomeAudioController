#!/usr/bin/python3
import sys
import logging
import time
import pychromecast
import urllib.request

class SyncCastDevice:
  def __init__(self, zone, cc):
    self.zone = zone
    self.device = cc
    self.baseUrl = 'http://localhost:5000/api/control/'
    self.status = 'off'

print("Initializing...")
print("Getting all Chromecasts on the network.")
all_ccs = pychromecast.get_chromecasts()

"""
  Assign relevant Chromecasts/Groups
  Zones assigned in order of list/tuple
"""
cc_list = ("Dining Room group", "Guest Bathroom group", "Office group", "Master Bathroom group")
cc_devices = []

for cc in all_ccs:
  for sel in cc_list:
    if(cc.device.friendly_name == sel):
      zone = cc_list.index(cc.device.friendly_name) + 1
      print("Adding Zone " + str(zone) + ": " + sel)
      cc_devices.append(SyncCastDevice(zone, cc))

if len(cc_devices) < len(cc_list):
  print("Could not find all Chromecasts... exiting")
  sys.exit(1)

cc_devices = sorted(cc_devices, key=lambda cc: cc.zone)

print("Done.\n\n")

while True:
  for cast in cc_devices:
    cast.device.wait()
    if cast.device.media_controller.status.player_state == "PLAYING":
      currStatus = 'on'
    else:
      currStatus = 'off'

    if cast.status != currStatus:
      print("zone" + str(cast.zone) + ":power_on")
      reqUrl = cast.baseUrl + str(cast.zone) + '/power/' + currStatus
      with urllib.request.urlopen(reqUrl) as response:
        resp = response.read()

      if currStatus == 'on':
        reqUrl = cast.baseUrl + str(cast.zone) + '/channel/' + str(cast.zone)
        with urllib.request.urlopen(reqUrl) as response:
          resp = response.read()
        reqUrl = cast.baseUrl + str(cast.zone) + '/volume/15'
        with urllib.request.urlopen(reqUrl) as response:
          resp = response.read()

      cast.status = currStatus 

    message = "zone" + str(cast.zone) + ":" + cast.device.device.friendly_name + ": "
    if currStatus == 'on':
      try:
        message += "playing:"
        message += cast.device.media_controller.status.artist.replace(" ", "_").lower()
        message += "/" + cast.device.media_controller.status.title.replace(" ", "_").lower()
      except:
        message += "playing:unknown/unknown"
    else:
      message += "idle"

    print(message)
  print("")
  time.sleep(10)
  
