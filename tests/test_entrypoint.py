import os
import pathlib
import subprocess
import time
import unittest
import urllib.error
import urllib.request
import uuid


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


class EntryPointTests(unittest.TestCase):
    def test_given_env_example_when_read_then_contains_required_vars(self):
        """Alias: env-example-vars"""
        # Arrange
        content = (ROOT / ".env.example").read_text(encoding="utf-8")

        # Act
        has_token = "NGROK_AUTHTOKEN=" in content
        has_upstreams = "UPSTREAMS=app:8080" in content

        # Assert
        self.assertTrue(has_token)
        self.assertTrue(has_upstreams)
        self.assertIn("NGROK_AUTHTOKEN=", content)
        self.assertIn("UPSTREAMS=app:8080", content)
        print(
            "PASS: .env.example defines NGROK_AUTHTOKEN and UPSTREAMS (baseline config for local runs)."
        )

    def test_given_compose_when_read_then_has_nginx_ngrok_and_ports(self):
        """Alias: compose-wiring"""
        # Arrange
        content = (ROOT / "docker-compose.yml").read_text(encoding="utf-8")

        # Act
        has_nginx = "nginx:alpine" in content
        has_ngrok = "ngrok/ngrok:alpine" in content
        has_port_8080 = "\"8080:80\"" in content
        has_port_4040 = "\"4040:4040\"" in content
        has_network = "edge-net" in content
        has_healthz = "/healthz" in content

        # Assert
        self.assertTrue(has_nginx)
        self.assertTrue(has_ngrok)
        self.assertTrue(has_port_8080)
        self.assertTrue(has_port_4040)
        self.assertTrue(has_network)
        self.assertTrue(has_healthz)
        self.assertIn("nginx:alpine", content)
        self.assertIn("ngrok/ngrok:alpine", content)
        self.assertIn("\"8080:80\"", content)
        self.assertIn("\"4040:4040\"", content)
        self.assertIn("edge-net", content)
        self.assertIn("/healthz", content)
        print(
            "PASS: docker-compose.yml wires nginx/ngrok, ports, healthcheck, and network as expected."
        )

    def test_given_entrypoint_when_read_then_validates_upstreams(self):
        """Alias: entrypoint-validation"""
        # Arrange
        content = (ROOT / "docker" / "nginx" / "entrypoint.sh").read_text(
            encoding="utf-8"
        )

        # Act
        has_required = "UPSTREAMS is required" in content
        has_format = "UPSTREAMS entry must be host:port" in content
        has_numeric = "UPSTREAMS port must be numeric" in content

        # Assert
        self.assertTrue(has_required)
        self.assertTrue(has_format)
        self.assertTrue(has_numeric)
        self.assertIn("UPSTREAMS is required", content)
        self.assertIn("UPSTREAMS entry must be host:port", content)
        self.assertIn("UPSTREAMS port must be numeric", content)
        print(
            "PASS: entrypoint.sh includes explicit validation for UPSTREAMS format and numeric port."
        )

    def test_given_nginx_template_when_read_then_contains_proxy_rules(self):
        """Alias: nginx-proxy-rules"""
        # Arrange
        content = (ROOT / "docker" / "nginx" / "nginx.conf.template").read_text(
            encoding="utf-8"
        )

        # Act
        has_limit = "limit_req_zone" in content
        has_healthz = "location = /healthz" in content
        has_header = "proxy_set_header X-Forwarded-For" in content
        has_timeout = "proxy_connect_timeout 5s" in content
        has_body_size = "client_max_body_size 2m" in content

        # Assert
        self.assertTrue(has_limit)
        self.assertTrue(has_healthz)
        self.assertTrue(has_header)
        self.assertTrue(has_timeout)
        self.assertTrue(has_body_size)
        self.assertIn("limit_req_zone", content)
        self.assertIn("location = /healthz", content)
        self.assertIn("proxy_set_header X-Forwarded-For", content)
        self.assertIn("proxy_connect_timeout 5s", content)
        self.assertIn("client_max_body_size 2m", content)
        print(
            "PASS: nginx.conf.template configures rate limit, /healthz, headers, and timeouts."
        )

    def test_given_docker_when_checked_then_is_available(self):
        """Alias: prereq-docker"""
        # Arrange
        available = docker_available()

        # Act
        is_available = available is True

        # Assert
        if not is_available:
            self.fail(
                "PREREQ FAILED: Docker is not available. This blocks test_given_compose_up_when_run_then_healthz_ready."
            )
        print("PASS: Docker is available, integration test can run.")

    def test_given_env_when_checked_then_ngrok_token_is_set(self):
        """Alias: prereq-ngrok-token"""
        # Arrange
        token = os.environ.get("NGROK_AUTHTOKEN")

        # Act
        has_token = bool(token)

        # Assert
        if not has_token:
            self.fail(
                "PREREQ FAILED: NGROK_AUTHTOKEN is required. This blocks test_given_compose_up_when_run_then_healthz_ready."
            )
        print("PASS: NGROK_AUTHTOKEN is set, ngrok container can start.")

    def test_given_compose_up_when_run_then_healthz_ready(self):
        """Alias: compose-up-healthz"""
        # Arrange
        token = os.environ.get("NGROK_AUTHTOKEN")
        if not token or not docker_available():
            self.fail(
                "PREREQ FAILED: Docker and NGROK_AUTHTOKEN are required. "
                "This blocks test_given_compose_up_when_run_then_healthz_ready."
            )

        env = os.environ.copy()
        env["NGROK_AUTHTOKEN"] = token
        env["UPSTREAMS"] = env.get("UPSTREAMS", "app:8080")

        project = f"entrypoint-test-{uuid.uuid4().hex[:8]}"

        # Act
        try:
            run(["docker", "compose", "-p", project, "up", "-d"], env=env)

            deadline = time.time() + 30
            last_error = None
            while time.time() < deadline:
                try:
                    with urllib.request.urlopen(
                        "http://localhost:8080/healthz", timeout=2
                    ) as response:
                        body = response.read().decode("utf-8").strip()
                        if response.status == 200 and body == "ok":
                            print(
                                "PASS: docker compose up succeeded and /healthz returned 200 ok."
                            )
                            return
                except (urllib.error.URLError, ValueError) as exc:
                    last_error = exc
                time.sleep(1)

            # Assert
            self.fail(f"/healthz did not become ready: {last_error}")
        finally:
            run(["docker", "compose", "-p", project, "down", "-v"], env=env)


if __name__ == "__main__":
    try:
        import unittest_colorful  # type: ignore

        runner = unittest_colorful.ColorfulTestRunner(verbosity=2)
    except Exception:
        runner = unittest.TextTestRunner(verbosity=2)

    unittest.main(testRunner=runner, verbosity=2)
