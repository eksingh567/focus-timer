<div align="center">
  <h1>⏱️ Focus Timer System</h1>
  <p><strong>A shared focus-timer experience with a Flask backend and Android WebView client scaffolding.</strong></p>

  [![Python](https://img.shields.io/badge/Python-14354C?style=for-the-badge&logo=python&logoColor=white)](https://www.python.org/)
  [![Flask](https://img.shields.io/badge/Flask-000000?style=for-the-badge&logo=flask&logoColor=white)](https://flask.palletsprojects.com/)
  [![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
  [![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
</div>

---

## 🌟 What's Included

- `server.py`: Flask server hosting desktop/mobile HTML routes and timer APIs.
- `requirements.txt`: Python dependencies.
- `activity_main.xml` & `activity_lock_screen.xml`: Full-screen `WebView` layouts for Android.
- `MainActivity.java`: Android activity loading the mobile home screen.
- `LockScreenActivity.java`: Android activity loading the lock screen in immersive mode.

## 🚀 Flask Backend Setup

### Install Dependencies
```bash
python -m venv .venv
source .venv/bin/activate  # On Windows use: .venv\Scripts\activate
pip install -r requirements.txt
```

### Run Server
```bash
python server.py
```
*Server listens on `0.0.0.0:5000`.*

### 📍 API Routes
- `GET /` → Desktop dashboard
- `GET /mobile/home` → Mobile home screen
- `GET /mobile/lock` → Mobile lock screen
- `GET /mobile/celeb` → Celebration screen
- `GET /status` → Current timer state
- `POST /status` → Partial timer state update
- `POST /start_timer` → Starts a lock timer with `{"minutes": <number>}`

## 📱 Android Integration Notes

1. **Layouts**: Place your XML files into `app/src/main/res/layout/`.
2. **Activities**: Place your Java activities into your app package under `app/src/main/java/...`.
3. **Permissions**: Ensure internet access in `AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```
4. **Configuration**: Set `SERVER_IP` constants in `MainActivity` and `LockScreenActivity` to your server's LAN IP. Ensure the Android device and server are on the same network.

## ✅ Quick Sanity Checks
```bash
python -m py_compile server.py
```
