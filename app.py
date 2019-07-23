from flask import Flask, jsonify, send_from_directory
from modules.MonopriceReceiver import Receiver
import os, subprocess

app = Flask(__name__)
r = Receiver("/dev/ttyUSB0")
cwd = os.getcwd()

@app.route('/')
def index():
  return 'Hello world'

@app.route('/favicon.ico')
def favicon():
    return send_from_directory(os.path.join(app.root_path, 'static'),
                               'favicon.ico', mimetype='image/vnd.microsoft.icon')

@app.route('/api')
def api():
  return 'Invalid function'

@app.route('/api/status', defaults={'zone':"0"})
@app.route('/api/status/<zone>')
def getZoneInfo(zone):
  return jsonify(r.getZoneInfo(zone))

@app.route('/api/control/<zone>/<function>/<value>')
def controlRequest(zone, function, value):
  if zone == "all":
    zone = "0"

  if function == "power":
    return jsonify(r.setPower(zone,value))
  elif function == "mute":
    return jsonify(r.setMute(zone,value))
  elif function == "pa":
    return jsonify(r.setPA(zone,value))
  elif function == "dnd":
    return jsonify(r.setDND(zone,value))
  elif function == "volume":
    return jsonify(r.setVolume(zone,value))
  elif function == "treble":
    return jsonify(r.setTreble(zone,value))
  elif function == "bass":
    return jsonify(r.setBass(zone,value))
  elif function == "balance":
    return jsonify(r.setBalance(zone,value))
  elif function == "channel":
    return jsonify(r.setChannel(zone,value))
  else:
    return "Function Not Implemented"

@app.route('/api/play/')
def playController():
  return 'To Be Completed' 

@app.route('/api/notify/<zone>/<message>')
def playNotification(zone,message):
  r.setChannel(zone,"5")
  subprocess.call([cwd + '/speech.sh', message])
  return jsonify(r.getZoneInfo(zone))


@app.route('/api/alarm/<command>')
def alarm(command):
  if command == "on":
    r.setChannel("0","5")
    r.setVolume("0","38")
    subprocess.Popen(
      ['/usr/bin/mplayer',
       '-ao','alsa',
       '-really-quiet',
       '-noconsolecontrols',
       '-loop','0',
       cwd + '/media/siren.wav'])
  elif command == "off":
    r.setVolume("0","25")
    r.setChannel("0","6")
    subprocess.Popen(['/usr/bin/killall','-9','mplayer'])

  return 'Alarm ' + command

if __name__ == '__main__':
  app.run(debug=True, host='0.0.0.0')
