from flask import Flask, jsonify, render_template, request
from flask_cors import CORS

app = Flask(__name__, template_folder="templates")
CORS(app)

# Shared IoT timer state
focus_state = {
    "is_running": False,
    "is_locked": False,
    "duration_seconds": 1500,
    "remaining_seconds": 1500,
    "last_updated": None,
}


@app.route("/")
def dashboard() -> str:
    return render_template("dash.html")


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

    return jsonify(focus_state)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
