# FocusTimerSystem (IoT Demo)

A phone app locking prototype controlled by a laptop timer over local network.

## Deployment (Render)

This project is configured for automated deployment on Render using the included `render.yaml`.

**Live Link:** [https://focus-timer.onrender.com](https://focus-timer.onrender.com) *(Update this with your actual Render URL after deploying)*

## Project idea (IoT narrative)

- **Edge controller (Laptop):** Flask app provides a timer dashboard and lock state API.
- **IoT client (Phone):** Android app polls the laptop API and enforces app-lock behavior.
- **Transport:** Wi-Fi LAN HTTP (`/status`) between laptop and phone.
- **Outcome:** blocked apps unlock only after timer expires (or manual unlock).

This is suitable for an IoT presentation because one device (laptop hub) sends policy to another device (phone endpoint) in real time over local network.

---

## 1) Run Flask server (Laptop)

```bash
cd FocusTimerSystem
pip install flask
python server.py
```

Open: `http://LAPTOP_IP:5000`.

Use the UI to:
- start lock timer,
- view countdown,
- view blocked package list,
- unlock manually.

Default blocked packages:
- `com.instagram.android`
- `com.zhiliaoapp.musically`
- `com.google.android.youtube`

---

## 2) Android app setup

1. Open `FocusTimerSystem/android-app` in Android Studio.
2. In:
   - `MainActivity.java`
   - `AppLockService.java`

   replace `http://LAPTOP_IP:5000/status` with your laptop IP.
3. Run on Android device (same Wi-Fi).
4. In app, tap **Grant Usage Access** and enable usage access for this app.
5. Tap **Start Polling + Lock Service**.

When timer mode is `lock`, opening blocked apps should show the lock screen.

---

## API endpoints

- `GET /` → dashboard
- `GET /status` → JSON state
- `POST /start_timer` body: `{"minutes": 25}`
- `POST /unlock`

---

## Notes for demo judges

- This is a prototype and uses usage-access foreground detection for enforcement.
- For production-grade blocking, integrate accessibility/device-owner policies and hardened background execution handling.
