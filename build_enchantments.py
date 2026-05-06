#!/usr/bin/env python3
import os
import subprocess
import json
import requests
import sys
from datetime import datetime, timezone

# File used to store the Discord message ID between Jenkins stages
ID_STORAGE_FILE = ".discord_msg_id.json"

def get_config():
    """
    Loads all required parameters from environment variables.
    Strictly follows the zero-hardcoding rule.
    """
    return {
        "WEBHOOK_URL": os.getenv("DISCORD_WEBHOOK_URL"),
        "THREAD_ID": os.getenv("THREAD_ID"),
        "ICON_URL": os.getenv("ICON_URL"),
        "THUMB_URL": os.getenv("THUMBNAIL_URL"),
        "BOT_NAME": os.getenv("BOT_NAME"),
        "COLOR_PENDING": int(os.getenv("COLOR_PENDING", 16766720)),
        "COLOR_SUCCESS": int(os.getenv("COLOR_SUCCESS", 5763719)),
        "COLOR_FAIL": int(os.getenv("COLOR_FAIL", 15548997)),
        "NO_CHANGELOG": os.getenv("NO_CHANGELOG_TEXT", "No changelog provided."),
        "FAIL_DESC": os.getenv("FAIL_DESC_TEXT", "Build failed. Check logs.")
    }

def get_git_info():
    """Retrieves commit message, author, and short hash from Git."""
    msg = subprocess.check_output(["git", "log", "-1", "--pretty=%B"], text=True).strip()
    author = subprocess.check_output(["git", "log", "-1", "--pretty=%an"], text=True).strip()
    commit_hash = subprocess.check_output(["git", "log", "-1", "--pretty=%h"], text=True).strip()
    return msg, author, commit_hash

def get_project_version():
    """Fetches the version from gradle.properties or build.gradle."""
    try:
        cmd = ["./gradlew", "properties", "-q", "--no-daemon"]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        for line in result.stdout.splitlines():
            if line.strip().startswith("version:"):
                return line.split(":", 1)[1].strip()
    except:
        pass
    return "Unknown"

def send_initial_message(config, author, changelog, footer):
    """Sends the 'Building' message and returns the ID for later patching."""
    url = f"{config['WEBHOOK_URL']}?thread_id={config['THREAD_ID']}&wait=true"
    payload = {
        "username": config["BOT_NAME"],
        "avatar_url": config["ICON_URL"],
        "embeds": [{
            "color": config["COLOR_PENDING"],
            "author": {"name": f"{author} triggered a build", "icon_url": config["ICON_URL"]},
            "title": "⏳ Building SinceEnchantments...",
            "description": changelog if changelog else config["NO_CHANGELOG"],
            "thumbnail": {"url": config["THUMB_URL"]},
            "footer": {"text": footer},
            "timestamp": datetime.now(timezone.utc).isoformat()
        }]
    }
    r = requests.post(url, json=payload)
    if r.status_code in [200, 201]:
        msg_id = r.json().get("id")
        with open(ID_STORAGE_FILE, 'w') as f:
            json.dump({"id": msg_id}, f)
        return msg_id
    return None

def patch_message(config, msg_id, title, desc, color, footer, jar_path=None):
    """Updates the existing Discord message with success/fail status."""
    url = f"{config['WEBHOOK_URL']}/messages/{msg_id}?thread_id={config['THREAD_ID']}"
    payload = {
        "embeds": [{
            "color": color,
            "author": {"name": config["BOT_NAME"], "icon_url": config["ICON_URL"]},
            "title": title,
            "description": desc,
            "thumbnail": {"url": config["THUMB_URL"]},
            "footer": {"text": footer},
            "timestamp": datetime.now(timezone.utc).isoformat()
        }]
    }

    if jar_path and os.path.exists(jar_path):
        with open(jar_path, 'rb') as f:
            files = {
                'payload_json': (None, json.dumps(payload), 'application/json'),
                'file': (os.path.basename(jar_path), f, 'application/java-archive')
            }
            requests.patch(url, files=files)
    else:
        requests.patch(url, json=payload)

def main():
    config = get_config()
    commit_msg, author, commit_hash = get_git_info()
    version = get_project_version()

    is_start = "--start" in sys.argv
    is_fail = "--fail" in sys.argv

    if is_start:
        send_initial_message(config, author, commit_msg, f"Version {version} • {commit_hash}")
    else:
        if not os.path.exists(ID_STORAGE_FILE): return
        with open(ID_STORAGE_FILE, 'r') as f:
            msg_id = json.load(f).get("id")

        if is_fail:
            patch_message(config, msg_id, "Build Failed ❌", config["FAIL_DESC"], config["COLOR_FAIL"], f"Version {version} • Failed • {commit_hash}")
        else:
            # Find the generated JAR (excluding -original.jar)
            jar_dir = "build/libs"
            jar_file = next((f for f in os.listdir(jar_dir) if f.endswith(".jar") and "-original" not in f), None)
            path = os.path.join(jar_dir, jar_file) if jar_file else None

            patch_message(config, msg_id, "Build Successful! 🚀", commit_msg, config["COLOR_SUCCESS"], f"Version {version} • {commit_hash}", path)

        if os.path.exists(ID_STORAGE_FILE):
            os.remove(ID_STORAGE_FILE)

if __name__ == "__main__":
    main()