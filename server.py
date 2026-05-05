from datetime import datetime, timedelta, timezone

from flask import Flask, jsonify, render_template, request
from flask_cors import CORS

app = Flask(__name__, template_folder="templates")
CORS(app)

# Shared IoT timer state
focus_state = {
    "mode": "home",  # home | lock | celeb
    "is_running": False,
    "duration_seconds": 0,
    "seconds_remaining": 0,
    "ends_at": None,
}


def _now_utc() -> datetime:
    return datetime.now(timezone.utc)


def _sync_timer_state() -> None:
    """Recalculate remaining time from ends_at and transition modes when finished."""
    if not focus_state["is_running"] or not focus_state["ends_at"]:
        return

    ends_at = datetime.fromisoformat(focus_state["ends_at"])
    remaining = int((ends_at - _now_utc()).total_seconds())

    if remaining <= 0:
        focus_state["seconds_remaining"] = 0
        focus_state["is_running"] = False
        focus_state["mode"] = "celeb"
        focus_state["ends_at"] = None
    else:
        focus_state["seconds_remaining"] = remaining


@app.route("/")
def dashboard() -> str:
    return render_template("dash.html")


@app.route("/history")
def history() -> str:
    return render_template("history.html")


@app.route("/devices")
def devices() -> str:
    return render_template("devices.html")


@app.route("/settings")
def settings() -> str:
    return render_template("settings.html")


@app.route("/mobile/home")
def mobile_home() -> str:
    return render_template("home.html")


@app.route("/mobile/lock")
def mobile_lock() -> str:
    return render_template("lock.html")


@app.route("/mobile/celeb")
def mobile_celeb() -> str:
    return render_template("celeb.html")


@app.route("/status", methods=["GET", "POST"])
def status():
    if request.method == "POST":
        payload = request.get_json(silent=True) or {}
        for key in focus_state.keys():
            if key in payload:
                focus_state[key] = payload[key]

    _sync_timer_state()
    return jsonify(focus_state)


@app.route("/start_timer", methods=["POST"])
def start_timer():
    payload = request.get_json(silent=True) or {}
    minutes = int(payload.get("minutes", 25))
    duration_seconds = max(1, minutes * 60)

    ends_at = _now_utc() + timedelta(seconds=duration_seconds)
    focus_state["mode"] = "lock"
    focus_state["is_running"] = True
    focus_state["duration_seconds"] = duration_seconds
    focus_state["seconds_remaining"] = duration_seconds
    focus_state["ends_at"] = ends_at.isoformat()

    return jsonify(focus_state)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
