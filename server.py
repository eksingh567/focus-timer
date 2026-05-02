from datetime import datetime, timedelta
from threading import Lock
import socket

from flask import Flask, jsonify, render_template, request

app = Flask(__name__)

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        return "127.0.0.1"

# Categorized App Groups (Clubs)
APP_CLUBS = {
    "Social Club": [
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.facebook.katana"
    ],
    "Video Club": [
        "com.google.android.youtube",
        "com.netflix.mediaclient"
    ],
    "Work Club": [
        "com.slack",
        "com.microsoft.teams",
        "com.google.android.apps.docs"
    ]
}

state_lock = Lock()
focus_state = {
    "mode": "unlock",
    "ends_at": None,
    "enabled_clubs": ["Social Club", "Video Club"], # Default clubs to block
    "history": [
        {"name": "Deep Work Block", "date": "Apr 09, 2026", "duration": "45m", "status": "Completed"},
        {"name": "Social Block", "date": "Apr 09, 2026", "duration": "15m", "status": "Completed"},
        {"name": "Coding Session", "date": "Apr 08, 2026", "duration": "120m", "status": "Completed"},
        {"name": "Research Spike", "date": "Apr 08, 2026", "duration": "30m", "status": "Completed"}
    ],
    "local_ip": get_local_ip() or "localhost",
    "active_nodes": {}
}

def _get_blocked_packages():
    """Returns a flat list of package names based on enabled clubs."""
    packages = []
    for club in focus_state["enabled_clubs"]:
        if club in APP_CLUBS:
            packages.extend(APP_CLUBS[club])
    return list(set(packages))

def _seconds_remaining(now: datetime, ends_at: datetime | None) -> int:
    if not ends_at:
        return 0
    remaining = int((ends_at - now).total_seconds())
    return max(remaining, 0)


def _state_snapshot() -> dict:
    with state_lock:
        now = datetime.now()
        ends_at = focus_state["ends_at"]

        if focus_state["mode"] == "lock" and _seconds_remaining(now, ends_at) == 0:
            focus_state["mode"] = "unlock"
            focus_state["ends_at"] = None
            # Log completed session
            focus_state["history"].insert(0, {
                "name": "Focus Session",
                "date": datetime.now().strftime("%b %d, %Y"),
                "duration": "25m",
                "status": "Completed"
            })

        remaining = _seconds_remaining(now, focus_state["ends_at"])

        if not focus_state["local_ip"] or focus_state["local_ip"] in ["localhost", "127.0.0.1", "..."]:
            focus_state["local_ip"] = get_local_ip()

        return {
            "mode": focus_state["mode"],
            "seconds_remaining": remaining,
            "blocked_apps": _get_blocked_packages(),
            "enabled_clubs": focus_state["enabled_clubs"],
            "available_clubs": list(APP_CLUBS.keys()),
            "local_ip": focus_state["local_ip"],
            "history": focus_state["history"][:5],
            "active_nodes": {
                ip: info for ip, info in focus_state["active_nodes"].items() 
                if (datetime.now() - info["last_seen"]).total_seconds() < 60
            }
        }


@app.route('/', methods=['GET'])
def home():
    user_agent = request.headers.get('User-Agent', '').lower()
    is_mobile = any(x in user_agent for x in ['iphone', 'android', 'mobile'])
    if is_mobile:
        return render_template('mobile.html', ip=focus_state["local_ip"])
    return render_template('dash.html', ip=focus_state["local_ip"])

@app.route('/mobile', methods=['GET'])
def mobile_unified():
    return render_template('mobile.html', ip=focus_state["local_ip"])

# Deprecated separate routes (redirect to unified for backwards compatibility)
@app.route('/mobile_home', methods=['GET'])
@app.route('/mobile_lock', methods=['GET'])
@app.route('/mobile_celeb', methods=['GET'])
def mobile_legacy():
    return render_template('mobile.html')


@app.route('/start_timer', methods=['POST'])
def start_timer():
    payload = request.get_json(silent=True) or {}
    minutes = int(payload.get("minutes", 25))
    clubs = payload.get("clubs")
    
    minutes = max(1, min(minutes, 240))

    with state_lock:
        focus_state["mode"] = "lock"
        focus_state["ends_at"] = datetime.now() + timedelta(minutes=minutes)
        if clubs is not None:
            focus_state["enabled_clubs"] = clubs

    return jsonify(_state_snapshot())


@app.route('/unlock', methods=['POST'])
def unlock_focus():
    with state_lock:
        focus_state["mode"] = "unlock"
        focus_state["ends_at"] = None
    return jsonify(_state_snapshot())


@app.route('/toggle_club', methods=['POST'])
def toggle_club():
    payload = request.get_json(silent=True) or {}
    club_name = payload.get("club")
    
    with state_lock:
        if club_name in focus_state["enabled_clubs"]:
            focus_state["enabled_clubs"].remove(club_name)
        else:
            focus_state["enabled_clubs"].append(club_name)
            
    return jsonify(_state_snapshot())


@app.route('/status', methods=['GET'])
def status_focus():
    print(">>> [DEBUG] STATUS POLL FROM BROWSER <<<", flush=True)
    ua = request.headers.get('User-Agent', '').lower()
    device_type = "Mobile" if any(x in ua for x in ['android', 'iphone', 'mobile']) else "Laptop"
    ip = request.remote_addr
    print(f"[HUB] Incoming status check from {ip} ({device_type})", flush=True)
    
    with state_lock:
        ip = request.remote_addr
        focus_state["active_nodes"][ip] = {
            "last_seen": datetime.now(),
            "type": device_type
        }
    return jsonify(_state_snapshot())


if __name__ == '__main__':
    print(f"[HUB] Starting FocusLock Studio on port 8000...", flush=True)
    app.run(host='0.0.0.0', port=8000)
