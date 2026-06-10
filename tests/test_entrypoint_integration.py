import json
import os
import pathlib
import subprocess
import time
import urllib.error
import urllib.request
import uuid

import pytest


ROOT = pathlib.Path(__file__).resolve().parents[1]


def run(cmd, env=None):
    return subprocess.run(
        cmd,
        cwd=ROOT,
        env=env,
        check=True,
        capture_output=True,
        text=True,
    )


def docker_available():
    try:
        subprocess.run(
            ["docker", "version"],
            check=True,
            capture_output=True,
            text=True,
        )
        return True
    except (OSError, subprocess.CalledProcessError):
        return False


def wait_for_healthz(timeout_seconds=30):
    deadline = time.time() + timeout_seconds
    last_error = None
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(
                "http://localhost:8080/healthz", timeout=2
            ) as response:
                body = response.read().decode("utf-8").strip()
                if response.status == 200 and body == "ok":
                    return True, None
        except (urllib.error.URLError, ValueError) as exc:
            last_error = exc
        time.sleep(1)
    return False, last_error


def wait_for_public_url(timeout_seconds=15):
    deadline = time.time() + timeout_seconds
    last_error = None
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(
                "http://localhost:4040/api/tunnels", timeout=5
            ) as response:
                if response.status != 200:
                    last_error = f"status {response.status}"
                else:
                    payload = json.loads(response.read().decode("utf-8"))
                    tunnels = payload.get("tunnels", [])
                    if tunnels:
                        public_url = tunnels[0].get("public_url")
                        if public_url:
                            return public_url, None
        except (urllib.error.URLError, ValueError, json.JSONDecodeError) as exc:
            last_error = exc
        time.sleep(2)
    return None, last_error


def require_prereqs(needs_token=True):
    if not docker_available():
        pytest.skip("Docker is not available.")
    token = os.environ.get("NGROK_AUTHTOKEN")
    if needs_token and not token:
        pytest.skip("NGROK_AUTHTOKEN is required to start ngrok.")
    return token


def compose_env(token):
    env = os.environ.copy()
    if token:
        env["NGROK_AUTHTOKEN"] = token
    env["UPSTREAMS"] = env.get("UPSTREAMS", "app:8080")
    return env


def project_name():
    return f"entrypoint-integration-{uuid.uuid4().hex[:8]}"


def test_nginx_local_healthz():
    """AC #38: GET /healthz returns 200 ok without backend."""
    token = require_prereqs(needs_token=True)
    env = compose_env(token)
    project = project_name()

    try:
        run(["docker", "compose", "-p", project, "up", "-d"], env=env)
        ok, error = wait_for_healthz(timeout_seconds=30)
        assert ok, f"/healthz did not become ready: {error}"
    finally:
        run(["docker", "compose", "-p", project, "down", "-v"], env=env)


def test_ngrok_public_url():
    """AC #36/#37: ngrok exposes HTTPS URL and forwards to Nginx."""
    token = require_prereqs(needs_token=True)
    env = compose_env(token)
    project = project_name()

    try:
        run(["docker", "compose", "-p", project, "up", "-d"], env=env)

        ok, error = wait_for_healthz(timeout_seconds=30)
        assert ok, f"/healthz did not become ready: {error}"

        public_url, url_error = wait_for_public_url(timeout_seconds=15)
        assert public_url, f"ngrok did not expose public URL: {url_error}"
        assert public_url.startswith("https://")

        request = urllib.request.Request(
            public_url, headers={"ngrok-skip-browser-warning": "true"}
        )
        with urllib.request.urlopen(request, timeout=10) as response:
            assert response.status == 200
    finally:
        run(["docker", "compose", "-p", project, "down", "-v"], env=env)
