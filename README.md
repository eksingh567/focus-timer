# Focus Timer IoT Demo

This repository contains a small Flask backend and Android WebView client scaffolding for a shared focus-timer experience.

## What's included

- `server.py`: Flask server that hosts desktop/mobile HTML routes and timer APIs.
- `requirements.txt`: Python dependencies.
- `activity_main.xml`: Full-screen `WebView` layout for the Android main screen.
- `activity_lock_screen.xml`: Full-screen `WebView` layout for the Android lock screen.
- `MainActivity.java`: Android activity that loads `http://SERVER_IP:5000/mobile/home`.
- `LockScreenActivity.java`: Android activity that loads `http://SERVER_IP:5000/mobile/lock` in immersive full-screen mode.

## Flask backend

### Install

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### Run

```bash
python server.py
```

Server listens on `0.0.0.0:5000`.

### Routes

- `GET /` → desktop dashboard (`dash.html`)
- `GET /mobile/home` → mobile home (`home.html`)
- `GET /mobile/lock` → mobile lock screen (`lock.html`)
- `GET /mobile/celeb` → celebration screen (`celeb.html`)
- `GET /status` → current timer state
- `POST /status` → partial timer state update
- `POST /start_timer` → starts a lock timer with `{"minutes": <number>}`

## Android integration notes

1. Put your XML files into `app/src/main/res/layout/`.
2. Put your Java activities into your app package under `app/src/main/java/...`.
3. Ensure `INTERNET` permission is present in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

4. Set `SERVER_IP` constants in `MainActivity` and `LockScreenActivity` to your laptop/server LAN IP.
5. Make sure your Android device and laptop are on the same network.

## Quick sanity checks

```bash
python -m py_compile server.py
```
