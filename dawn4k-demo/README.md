# Dawn particle demo — Linux desktop

From the repository root, on an ARM64 Mac with Docker Desktop:

```bash
docker compose -f dawn4k-demo/docker/compose.yaml up --build -d --wait
docker compose -f dawn4k-demo/docker/compose.yaml exec desktop /opt/demo/demo.sh
```

Open <http://localhost:6080/vnc.html?autoconnect=1&shared=1&resize=scale>.
All browser clients view the same desktop. The desktop can be started without
building the demo; the first demo launch downloads Gradle and Maven dependencies.

Sway is Wayland, but Compose/AWT and the Dawn viewport use X11 through XWayland.
Vulkan uses Mesa lavapipe on the CPU, not the Mac GPU. This environment validates
functionality, not GPU performance. The web endpoint is local-only and has no
authentication: do not publish it on a public interface.

```bash
# Terminal; run /opt/demo/demo.sh from here too.
docker compose -f dawn4k-demo/docker/compose.yaml exec -u demo desktop bash
# Logs
docker compose -f dawn4k-demo/docker/compose.yaml logs desktop
# Stop without deleting Gradle/build caches
docker compose -f dawn4k-demo/docker/compose.yaml down
```

The checkout is mounted read/write. Linux build outputs and caches are separate
named volumes, not the Mac's build directories. Use `-p another-name` and
`DAWN_DESKTOP_PORT=6081` for another checkout running at the same time.
