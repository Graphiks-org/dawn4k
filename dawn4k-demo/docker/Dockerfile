FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    sway xwayland wayvnc novnc websockify dbus-x11 foot \
    mesa-vulkan-drivers vulkan-tools libvulkan1 libx11-6 libx11-dev \
    libxext6 libxi6 libxrender1 libxtst6 libxrandr2 libgl1 libgl1-mesa-dri \
    libegl1 libfontconfig1 fonts-dejavu-core ca-certificates curl python3 \
    gcc libc6-dev binutils procps x11-utils xdotool grim gosu \
    libwayland-dev wayland-protocols libxkbcommon-dev pkg-config gcc-x86-64-linux-gnu \
    && rm -rf /var/lib/apt/lists/*

# Compile the x64 bridge without executing x64 programs or replacing ARM64 libraries.
RUN mkdir -p /opt/demo/wayland-sysroot /var/tmp/x64-debs /var/tmp/x64-apt/lists/partial \
    && printf '%s\n' 'deb [arch=amd64 signed-by=/usr/share/keyrings/ubuntu-archive-keyring.gpg] http://archive.ubuntu.com/ubuntu noble main universe' \
        > /var/tmp/x64-apt/sources.list \
    && apt-get -o APT::Architecture=amd64 -o APT::Architectures=amd64 \
        -o Dir::Etc::sourcelist=/var/tmp/x64-apt/sources.list -o Dir::Etc::sourceparts=- \
        -o Dir::State::lists=/var/tmp/x64-apt/lists update \
    && cd /var/tmp/x64-debs \
    && apt-get -o APT::Architecture=amd64 -o APT::Architectures=amd64 \
        -o Dir::Etc::sourcelist=/var/tmp/x64-apt/sources.list -o Dir::Etc::sourceparts=- \
        -o Dir::State::lists=/var/tmp/x64-apt/lists download \
        libwayland-dev libwayland-client0 libxkbcommon-dev libxkbcommon0 \
        libffi-dev libffi8 libc6-dev libc6 linux-libc-dev \
    && for package in *.deb; do dpkg-deb -x "$package" /opt/demo/wayland-sysroot; done \
    && ln -s usr/lib /opt/demo/wayland-sysroot/lib \
    && ln -s usr/lib64 /opt/demo/wayland-sysroot/lib64 \
    && ln -sfn ../lib/x86_64-linux-gnu/ld-linux-x86-64.so.2 \
        /opt/demo/wayland-sysroot/usr/lib64/ld-linux-x86-64.so.2 \
    && rm -rf /var/tmp/x64-debs /var/tmp/x64-apt

ARG JDK_URL=https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/OpenJDK25U-jdk_aarch64_linux_hotspot_25.0.4.1_1.tar.gz
ARG JDK_SHA256=69df11a02cfa3ef7d7ca645e03edce6778ec090e100f6ae2b42097865730ac52
RUN curl -fL "$JDK_URL" -o /var/tmp/jdk.tar.gz \
    && echo "$JDK_SHA256  /var/tmp/jdk.tar.gz" | sha256sum -c - \
    && mkdir -p /opt/java \
    && tar -xzf /var/tmp/jdk.tar.gz --strip-components=1 -C /opt/java \
    && rm /var/tmp/jdk.tar.gz

RUN if getent passwd 1000 >/dev/null; then \
      usermod -l demo -d /home/demo -m "$(getent passwd 1000 | cut -d: -f1)"; \
      groupmod -n demo "$(getent group 1000 | cut -d: -f1)"; \
    else groupadd -g 1000 demo && useradd -m -u 1000 -g 1000 demo; fi

ENV JAVA_HOME=/opt/java PATH=/opt/java/bin:$PATH \
    WLR_BACKENDS=headless WLR_HEADLESS_OUTPUTS=1 WLR_RENDERER=pixman \
    XDG_RUNTIME_DIR=/run/user/1000 XDG_SESSION_TYPE=wayland \
    _JAVA_AWT_WM_NONREPARENTING=1
COPY *.py *.sh /opt/demo/
COPY sway.conf wayvnc.conf /etc/demo/
RUN chmod +x /opt/demo/*.sh
WORKDIR /workspace
ENTRYPOINT ["/opt/demo/entrypoint.sh"]
