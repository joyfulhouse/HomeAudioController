#!/bin/bash

sleep 1m

cd /opt/HomeAudioController
/usr/bin/nohup /usr/bin/python3 /opt/HomeAudioController/app.py >/dev/null 2>&1 &

cd /opt/HomeAudioController/CastPowerSync
/usr/bin/nohup /usr/bin/python3 /opt/HomeAudioController/CastPowerSync/sync.py >/dev/null 2>&1 &

