#define _POSIX_C_SOURCE 200809L
#include "wayland-host.h"
#include "wayland-host-state.h"
#include <assert.h>
#include <dirent.h>
#include <limits.h>
#include <pthread.h>
#include <stdatomic.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <wayland-server-core.h>
#include <wayland-server-protocol.h>
#include <xkbcommon/xkbcommon-keysyms.h>

static int fd_count(void) {
    DIR *directory = opendir("/proc/self/fd");
    assert(directory);
    int count = 0;
    while (readdir(directory)) ++count;
    closedir(directory);
    return count;
}

struct fixture { struct wl_display *display; atomic_int stopping; };
static void *serve(void *data) {
    struct fixture *fixture = data;
    while (!atomic_load(&fixture->stopping)) {
        assert(wl_event_loop_dispatch(wl_display_get_event_loop(fixture->display), 10) >= 0);
        wl_display_flush_clients(fixture->display);
    }
    return NULL;
}
static void bind_compositor(struct wl_client *client, void *data, uint32_t version, uint32_t id) {
    (void)data;
    assert(wl_resource_create(client, &wl_compositor_interface, (int)version, id));
}
static void missing_xdg_shell_cleans_partially_created_objects(void) {
    int before = fd_count();
    char runtime[] = "/var/tmp/dawn4k-wayland-test-XXXXXX";
    assert(mkdtemp(runtime));
    assert(chmod(runtime, 0700) == 0);
    setenv("XDG_RUNTIME_DIR", runtime, 1);
    struct fixture fixture = {.display=wl_display_create()};
    assert(fixture.display);
    assert(wl_global_create(fixture.display, &wl_compositor_interface, 4, NULL, bind_compositor));
    const char *socket = wl_display_add_socket_auto(fixture.display);
    assert(socket);
    setenv("WAYLAND_DISPLAY", socket, 1);
    pthread_t thread;
    assert(pthread_create(&thread, NULL, serve, &fixture) == 0);
    for (int i = 0; i < 10; ++i) {
        char error[256] = {0};
        assert(dawn_wl_open("partial setup", 640, 480, error, sizeof(error)) == NULL);
        assert(strstr(error, "Wayland") != NULL);
    }
    atomic_store(&fixture.stopping, 1);
    assert(pthread_join(thread, NULL) == 0);
    wl_display_destroy_clients(fixture.display);
    wl_display_destroy(fixture.display);
    assert(rmdir(runtime) == 0);
    assert(fd_count() == before);
}

int main(void) {
    dawn_wl_state state = {640, 480, 1};
    assert(dawn_wl_apply_configure(&state, 0, 0, 2) == 0);
    assert(state.width == 640 && state.height == 480 && state.scale == 2);
    assert(dawn_wl_apply_configure(&state, 900, 600, 1) == 0);
    assert(state.width == 900 && state.height == 600 && state.scale == 1);
    assert(dawn_wl_apply_configure(&state, -1, 600, 1) == -1);
    assert(dawn_wl_apply_configure(&state, 900, 600, 0) == -1);
    assert(dawn_wl_apply_configure(&state, INT_MAX, 600, 2) == -1);
    assert(state.width == 900 && state.height == 600 && state.scale == 1);
    assert(dawn_wl_key_action(0, XKB_KEY_space) == 0);
    assert(dawn_wl_key_action(1, XKB_KEY_space) == DAWN_WL_PAUSE);
    assert(dawn_wl_key_action(1, XKB_KEY_r) == DAWN_WL_RESET);
    assert(dawn_wl_key_action(1, XKB_KEY_R) == DAWN_WL_RESET);
    assert(dawn_wl_key_action(1, XKB_KEY_plus) == DAWN_WL_COUNT_UP);
    assert(dawn_wl_key_action(1, XKB_KEY_KP_Add) == DAWN_WL_COUNT_UP);
    assert(dawn_wl_key_action(1, XKB_KEY_minus) == DAWN_WL_COUNT_DOWN);
    assert(dawn_wl_key_action(1, XKB_KEY_Escape) == DAWN_WL_CLOSE);
    assert(dawn_wl_key_action(1, XKB_KEY_a) == 0);

    unsetenv("WAYLAND_SOCKET");
    setenv("WAYLAND_DISPLAY", "/nonexistent/dawn4k-test-wayland", 1);
    int before = fd_count();
    for (int i = 0; i < 100; ++i) {
        char error[256] = {0};
        assert(dawn_wl_open("test", 640, 480, error, sizeof(error)) == NULL);
        assert(strstr(error, "Wayland") != NULL);
        dawn_wl_close(NULL);
    }
    assert(fd_count() == before);
    missing_xdg_shell_cleans_partially_created_objects();
    puts("native Wayland state, controls and failed-connection cleanup: PASS");
    return 0;
}
