IT_CMD="adb -s emulator-5558 wait-for-device shell getprop init.svc.bootanim"

until $WAIT_CMD | grep -m 1 stopped; do
  echo "Waiting for emulator23..."
  sleep 1
done

