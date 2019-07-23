#!/usr/bin/python3
import sys
import logging
import time
import pychromecast
import urllib.request

class CastDevice:
  def __init__(self, zone, ipAddress):
    self.status = ''
    self.zone = zone
    self.ip = ipAddress
    self.castDevice = pychromecast.Chromecast(ipAddress) 
    self.mc = self.castDevice.media_controller.status
    self.baseUrl = 'http://localhost:5000/api/control/'

Chromecasts = [
        CastDevice('1','172.16.11.1'),
        CastDevice('2','172.16.11.2'),
        CastDevice('3','172.16.11.3'),
        CastDevice('4','172.16.11.4')
]

while True:
  print("Checking Chromecasts")
  i = 0
  while i < len(Chromecasts):
    cast = Chromecasts[i]
    print("IP: " + cast.ip)
    if not cast.castDevice.status.status_text:
      currStatus = 'off'
    else:
      currStatus = 'on'

    if cast.status != currStatus:
      reqUrl = cast.baseUrl + cast.zone + '/power/' + currStatus
      print(reqUrl)
      with urllib.request.urlopen(reqUrl) as response:
        resp = response.read()

      if currStatus == 'on':
        reqUrl = cast.baseUrl + cast.zone + '/channel/' + cast.zone
        print(reqUrl)
        with urllib.request.urlopen(reqUrl) as response:
          resp = response.read()
        reqUrl = cast.baseUrl + cast.zone + '/volume/15'
        print(reqUrl)
        with urllib.request.urlopen(reqUrl) as response:
          resp = response.read()

      cast.status = currStatus 

    if currStatus == 'on':
      try:
        print("Now Playing: " + cast.mc.artist + " - " + cast.mc.title)
      except:
        print("Now Playing: Unknown - Unknown")
    else:
      print("Nothing playing.")
    i += 1
  time.sleep(5)

