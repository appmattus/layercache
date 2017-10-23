IT_CMD="adb -s emulator-5556 wait-for-device shell getprop init.svc.bootanim"

until $WAIT_CMD | grep -m 1 stopped; do
  echo "Waiting for emulator19..."
  sleep 1
done

