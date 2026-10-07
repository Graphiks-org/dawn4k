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
#include <xkbcommon/xkbcommon.h>
#include "xdg-shell-server-protocol.h"

static int fd_count(void) {
    DIR *directory = opendir("/proc/self/fd");
    assert(directory);
    int count = 0;
    while (readdir(directory)) ++count;
    closedir(directory);
    return count;
}

struct fixture {
    struct wl_display *display;
    struct wl_global *seat;
    struct wl_resource *xdg_surface, *toplevel;
    atomic_int stopping, remove_seat;
};
static void *serve(void *data) {
    struct fixture *fixture = data;
    while (!atomic_load(&fixture->stopping)) {
        assert(wl_event_loop_dispatch(wl_display_get_event_loop(fixture->display), 10) >= 0);
        wl_display_flush_clients(fixture->display);
        if (atomic_exchange(&fixture->remove_seat, 0) && fixture->seat) {
            wl_global_destroy(fixture->seat);
            fixture->seat = NULL;
        }
    }
    return NULL;
}
static void bind_compositor(struct wl_client *client, void *data, uint32_t version, uint32_t id) {
    (void)data;
    assert(wl_resource_create(client, &wl_compositor_interface, (int)version, id));
}

static void destroy_resource(struct wl_client *client, struct wl_resource *resource) {
    (void)client; wl_resource_destroy(resource);
}
static void top_string(struct wl_client *client, struct wl_resource *resource, const char *text) {
    (void)client; (void)resource; (void)text;
}
static const struct xdg_toplevel_interface top_requests = {
    .destroy=destroy_resource, .set_title=top_string, .set_app_id=top_string
};
static void get_toplevel(struct wl_client *client, struct wl_resource *resource, uint32_t id) {
    struct fixture *fixture = wl_resource_get_user_data(resource);
    fixture->toplevel = wl_resource_create(client, &xdg_toplevel_interface, 1, id);
    wl_resource_set_implementation(fixture->toplevel, &top_requests, fixture, NULL);
}
static void ack_configure(struct wl_client *client, struct wl_resource *resource, uint32_t serial) {
    (void)client; (void)resource; assert(serial == 1);
}
static const struct xdg_surface_interface xdg_requests = {
    .destroy=destroy_resource, .get_toplevel=get_toplevel, .ack_configure=ack_configure
};
static void get_xdg_surface(struct wl_client *client, struct wl_resource *resource, uint32_t id, struct wl_resource *surface) {
    (void)surface;
    struct fixture *fixture = wl_resource_get_user_data(resource);
    fixture->xdg_surface = wl_resource_create(client, &xdg_surface_interface, 1, id);
    wl_resource_set_implementation(fixture->xdg_surface, &xdg_requests, fixture, NULL);
}
static const struct xdg_wm_base_interface wm_requests = {.destroy=destroy_resource, .get_xdg_surface=get_xdg_surface};
static void bind_wm(struct wl_client *client, void *data, uint32_t version, uint32_t id) {
    struct wl_resource *resource = wl_resource_create(client, &xdg_wm_base_interface, (int)version, id);
    wl_resource_set_implementation(resource, &wm_requests, data, NULL);
}
static void commit_surface(struct wl_client *client, struct wl_resource *resource) {
    (void)client; struct fixture *fixture = wl_resource_get_user_data(resource);
    struct wl_array states; wl_array_init(&states);
    xdg_toplevel_send_configure(fixture->toplevel, 0, 0, &states);
    xdg_surface_send_configure(fixture->xdg_surface, 1);
    wl_array_release(&states);
}
static const struct wl_surface_interface surface_requests = {.destroy=destroy_resource, .commit=commit_surface};
static void create_surface(struct wl_client *client, struct wl_resource *resource, uint32_t id) {
    struct wl_resource *surface = wl_resource_create(client, &wl_surface_interface, 4, id);
    wl_resource_set_implementation(surface, &surface_requests, wl_resource_get_user_data(resource), NULL);
}
static const struct wl_compositor_interface compositor_requests = {.create_surface=create_surface};
static void bind_real_compositor(struct wl_client *client, void *data, uint32_t version, uint32_t id) {
    struct wl_resource *resource = wl_resource_create(client, &wl_compositor_interface, (int)version, id);
    wl_resource_set_implementation(resource, &compositor_requests, data, NULL);
}
static const struct wl_keyboard_interface keyboard_requests = {.release=destroy_resource};
static void get_keyboard(struct wl_client *client, struct wl_resource *resource, uint32_t id) {
    struct wl_resource *keyboard = wl_resource_create(client, &wl_keyboard_interface, 7, id);
    wl_resource_set_implementation(keyboard, &keyboard_requests, wl_resource_get_user_data(resource), NULL);
}
static const struct wl_seat_interface seat_requests = {.get_keyboard=get_keyboard, .release=destroy_resource};
static void bind_seat(struct wl_client *client, void *data, uint32_t version, uint32_t id) {
    struct wl_resource *resource = wl_resource_create(client, &wl_seat_interface, (int)version, id);
    wl_resource_set_implementation(resource, &seat_requests, data, NULL);
    wl_seat_send_capabilities(resource, WL_SEAT_CAPABILITY_KEYBOARD);
    wl_seat_send_name(resource, "fixture-seat");
}
static void live_seat_removal_preserves_the_surface_and_closes_keyboard(void) {
    int before = fd_count();
    char runtime[] = "/var/tmp/dawn4k-wayland-seat-XXXXXX";
    assert(mkdtemp(runtime));
    setenv("XDG_RUNTIME_DIR", runtime, 1);
    struct fixture fixture = {.display=wl_display_create()};
    assert(wl_global_create(fixture.display, &wl_compositor_interface, 4, &fixture, bind_real_compositor));
    assert(wl_global_create(fixture.display, &xdg_wm_base_interface, 1, &fixture, bind_wm));
    fixture.seat = wl_global_create(fixture.display, &wl_seat_interface, 7, &fixture, bind_seat);
    setenv("WAYLAND_DISPLAY", wl_display_add_socket_auto(fixture.display), 1);
    pthread_t thread; assert(pthread_create(&thread, NULL, serve, &fixture) == 0);
    char error[256] = {0};
    dawn_wl_host *host = dawn_wl_open("seat removal", 640, 480, error, sizeof(error));
    assert(host);
    dawn_wl_event event = {0};
    for (int i = 0; i < 100 && event.type != DAWN_WL_CONFIGURE; ++i)
        assert(dawn_wl_next_event(host, &event, 16) >= 0);
    assert(event.type == DAWN_WL_CONFIGURE && event.width == 640 && event.height == 480);
    atomic_store(&fixture.remove_seat, 1);
    for (int i = 0; i < 10; ++i) assert(dawn_wl_next_event(host, &event, 16) >= 0);
    assert(dawn_wl_surface(host) != NULL);
    dawn_wl_close(host);
    atomic_store(&fixture.stopping, 1);
    assert(pthread_join(thread, NULL) == 0);
    wl_display_destroy_clients(fixture.display);
    wl_display_destroy(fixture.display);
    assert(rmdir(runtime) == 0);
    assert(fd_count() == before);
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
    struct xkb_context *context = xkb_context_new(XKB_CONTEXT_NO_FLAGS);
    assert(context);
    struct xkb_rule_names names = {.layout="us"};
    struct xkb_keymap *keymap = xkb_keymap_new_from_names(context, &names, XKB_KEYMAP_COMPILE_NO_FLAGS);
    assert(keymap);
    struct xkb_state *keys = xkb_state_new(keymap);
    assert(keys);
    xkb_state_update_mask(keys, 1u << xkb_keymap_mod_get_index(keymap, XKB_MOD_NAME_SHIFT), 0, 0, 0, 0, 0);
    assert(dawn_wl_key_action(1, xkb_state_key_get_one_sym(keys, xkb_keymap_key_by_name(keymap, "AE12"))) == DAWN_WL_COUNT_UP);
    xkb_state_unref(keys); xkb_keymap_unref(keymap);
    names.layout = "fr";
    keymap = xkb_keymap_new_from_names(context, &names, XKB_KEYMAP_COMPILE_NO_FLAGS);
    assert(keymap);
    keys = xkb_state_new(keymap);
    assert(keys);
    assert(dawn_wl_key_action(1, xkb_state_key_get_one_sym(keys, xkb_keymap_key_by_name(keymap, "AE06"))) == DAWN_WL_COUNT_DOWN);
    xkb_state_unref(keys); xkb_keymap_unref(keymap); xkb_context_unref(context);

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
    live_seat_removal_preserves_the_surface_and_closes_keyboard();
    puts("native Wayland state, controls and failed-connection cleanup: PASS");
    return 0;
}
