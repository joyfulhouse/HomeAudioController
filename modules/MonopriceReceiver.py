#!/usr/bin/python3

import serial, time

class Receiver:
  def __init__(self, serialport):
    self.data = {}
    i=6
    count=0

    self.ser = serial.Serial(serialport,9600)

    self.ser.bytesize = serial.EIGHTBITS
    self.ser.parity = serial.PARITY_NONE
    self.ser.stopbits = serial.STOPBITS_ONE
    self.ser.timeout = 0.5
    self.ser.xonxoff = False      #disable software flow control
    self.ser.rtscts = False       #disable hardware (RTS/CTS) flow control
    self.ser.dsrdtr = False       #disable hardware (DSR/DTR) flow control
    self.ser.writeTimeout = 0     #timeout for write

    if not self.ser.isOpen():
      self.ser.open()

    if self.ser.isOpen():
      try:
        self.ser.flushInput()
        self.ser.flushOutput()
        self.getZoneInfo("0")
      except Exception as e:
        print("error communicating...: " + str(e))

  def getZoneInfo(self, zone):
    self.ser.flushInput()
    self.ser.flushOutput()
    self.ser.write(("?1" + zone + "\r").encode())
    time.sleep(0.5)

    while self.ser.inWaiting():
      self.parseStatus(self.ser.readline().decode("utf-8"))

    self.ser.flushOutput()

    if zone == "0":
      return self.data
    else:
      return self.data[int(zone)]

  def setPower(self,zone,value):
    self.sendToggleCommand(zone,"PR",value)
    return self.getZoneInfo(zone)

  def setMute(self,zone,value):
    self.sendToggleCommand(zone,"MU",value)
    return self.getZoneInfo(zone)

  def setPA(self,zone,value):
    self.sendToggleCommand(zone,"PA",value)
    return self.getZoneInfo(zone)

  def setDND(self,zone,value):
    self.sendToggleCommand(zone,"DT",value)
    return self.getZoneInfo(zone)

  def setVolume(self,zone,value):
    self.sendValueCommand(zone,"VO",value,0,38)
    return self.getZoneInfo(zone)

  def setTreble(self,zone,value):
    self.sendValueCommand(zone,"TR",value,0,14)
    return self.getZoneInfo(zone)

  def setBass(self,zone,value):
    self.sendValueCommand(zone,"BS",value,0,14)
    return self.getZoneInfo(zone)

  def setBalance(self,zone,value):
    self.sendValueCommand(zone,"BL",value,0,14)
    return self.getZoneInfo(zone)

  def setChannel(self,zone,value):
    self.sendValueCommand(zone,"CH",value,0,6)
    return self.getZoneInfo(zone)

  def sendToggleCommand(self, zone, command, value):
    self.ser.flushInput()
    self.ser.flushOutput()

    if value == "on":
      self.ser.write(("<1" + zone + command + "01" + "\r").encode())
      time.sleep(0.1)
      self.ser.flushOutput()
    elif value == "off":
      self.ser.write(("<1" + zone + command + "00" + "\r").encode())
      time.sleep(0.1)
      self.ser.flushOutput()
    else:
      return "badValue"

  def sendValueCommand(self, zone, command, value, min, max):
    try:
      intValue = int(value)
      if intValue < 10:
        value = "0" + value
    except:
      return "badValue"

    if intValue >= min and intValue <= max:
      self.ser.write(("<1" + zone + command + value + "\r").encode())
      time.sleep(0.1)
      self.ser.flushOutput()
    else:
      response = "badValue"

  def parseStatus(self,status):
    if status[0:3] == "#>1":
      i = int(status[3:4])
      self.data[i] = {}
      self.data[i]['zone'] = i
      self.data[i]['PA'] = "on" if status[4:6] == "01" else "off"
      self.data[i]['power'] = "on" if status[6:8] == "01" else "off"
      self.data[i]['mute'] = "on" if status[8:10] == "01" else "off"
      self.data[i]['dnd'] = "on" if status[10:12] == "01" else "off"
      self.data[i]['volume'] = int(status[12:14])
      self.data[i]['treble'] = int(status[14:16])
      self.data[i]['bass'] = int(status[16:18])
      self.data[i]['balance'] = int(status[18:20])
      self.data[i]['channel'] = int(status[20:22])
      self.data[i]['keypad'] = status[22:24]
