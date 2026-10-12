#define _POSIX_C_SOURCE 200809L
#include "wayland-host.h"
#include "wayland-host-state.h"
#include "xdg-shell-client-protocol.h"
#include <errno.h>
#include <poll.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/eventfd.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>
#include <wayland-client.h>
#include <xkbcommon/xkbcommon.h>

#define EVENT_CAPACITY 256
struct output {
    struct output *next;
    struct dawn_wl_host *host;
    struct wl_output *proxy;
    uint32_t name;
    int scale, entered;
};
struct dawn_wl_host {
    struct wl_display *display;
    struct wl_event_queue *queue;
    struct wl_registry *registry;
    struct wl_compositor *compositor;
    struct xdg_wm_base *wm;
    struct wl_surface *surface;
    struct xdg_surface *xdg_surface;
    struct xdg_toplevel *toplevel;
    struct wl_seat *seat;
    struct wl_keyboard *keyboard;
    struct xkb_context *xkb_context;
    struct xkb_keymap *keymap;
    struct xkb_state *key_state;
    struct output *outputs;
    uint32_t compositor_name, wm_name, seat_name;
    dawn_wl_state state;
    int pending_width, pending_height, focused, configured, registry_done;
    int wake_fd, failed;
    dawn_wl_event events[EVENT_CAPACITY];
    unsigned head, count;
};

static void fail(dawn_wl_host *h, int code) { if (!h->failed) h->failed = code ? code : EIO; }
static void enqueue(dawn_wl_host *h, dawn_wl_event event) {
    if (h->count == EVENT_CAPACITY) { fail(h, ENOBUFS); return; }
    h->events[(h->head + h->count++) % EVENT_CAPACITY] = event;
}
static void publish_size(dawn_wl_host *h, uint32_t serial) {
    enqueue(h, (dawn_wl_event){DAWN_WL_CONFIGURE, h->state.width, h->state.height, h->state.scale, serial, 0});
}
static void update_scale(dawn_wl_host *h) {
    int scale = 1;
    for (struct output *o = h->outputs; o; o = o->next)
        if (o->entered && o->scale > scale) scale = o->scale;
    if (scale == h->state.scale) return;
    if (dawn_wl_apply_configure(&h->state, 0, 0, scale) < 0) { fail(h, EOVERFLOW); return; }
    if (h->configured) publish_size(h, 0);
}

static void output_geometry(void *data, struct wl_output *output, int32_t x, int32_t y,
    int32_t pw, int32_t ph, int32_t subpixel, const char *make, const char *model, int32_t transform) {
    (void)data; (void)output; (void)x; (void)y; (void)pw; (void)ph; (void)subpixel;
    (void)make; (void)model; (void)transform;
}
static void output_mode(void *data, struct wl_output *output, uint32_t flags,
    int32_t width, int32_t height, int32_t refresh) {
    (void)data; (void)output; (void)flags; (void)width; (void)height; (void)refresh;
}
static void output_done(void *data, struct wl_output *output) {
    (void)output; struct output *o = data; update_scale(o->host);
}
static void output_scale(void *data, struct wl_output *output, int32_t scale) {
    (void)output; struct output *o = data;
    if (scale <= 0) { fail(o->host, EINVAL); return; }
    o->scale = scale;
}
static const struct wl_output_listener output_listener = {
    .geometry=output_geometry, .mode=output_mode, .done=output_done, .scale=output_scale
};
static void surface_enter(void *data, struct wl_surface *surface, struct wl_output *output) {
    (void)surface; dawn_wl_host *h = data;
    for (struct output *o = h->outputs; o; o = o->next) if (o->proxy == output) o->entered = 1;
    update_scale(h);
}
static void surface_leave(void *data, struct wl_surface *surface, struct wl_output *output) {
    (void)surface; dawn_wl_host *h = data;
    for (struct output *o = h->outputs; o; o = o->next) if (o->proxy == output) o->entered = 0;
    update_scale(h);
}
static const struct wl_surface_listener surface_listener = {.enter=surface_enter, .leave=surface_leave};
static void wm_ping(void *data, struct xdg_wm_base *wm, uint32_t serial) {
    (void)data; xdg_wm_base_pong(wm, serial);
}
static const struct xdg_wm_base_listener wm_listener = {.ping=wm_ping};
static void toplevel_configure(void *data, struct xdg_toplevel *top, int32_t width,
    int32_t height, struct wl_array *states) {
    (void)top; (void)states; dawn_wl_host *h = data;
    h->pending_width = width; h->pending_height = height;
}
static void toplevel_close(void *data, struct xdg_toplevel *top) {
    (void)top; enqueue(data, (dawn_wl_event){.type=DAWN_WL_CLOSE});
}
static const struct xdg_toplevel_listener toplevel_listener = {
    .configure=toplevel_configure, .close=toplevel_close
};
static void surface_configure(void *data, struct xdg_surface *surface, uint32_t serial) {
    dawn_wl_host *h = data;
    xdg_surface_ack_configure(surface, serial);
    if (dawn_wl_apply_configure(&h->state, h->pending_width, h->pending_height, h->state.scale) < 0) {
        fail(h, EINVAL); return;
    }
    h->configured = 1;
    publish_size(h, serial);
}
static const struct xdg_surface_listener xdg_listener = {.configure=surface_configure};

static void clear_keymap(dawn_wl_host *h) {
    if (h->key_state) xkb_state_unref(h->key_state);
    if (h->keymap) xkb_keymap_unref(h->keymap);
    h->key_state = NULL; h->keymap = NULL;
}
static void keyboard_keymap(void *data, struct wl_keyboard *keyboard, uint32_t format,
    int32_t fd, uint32_t size) {
    (void)keyboard; dawn_wl_host *h = data;
    clear_keymap(h);
    if (format != WL_KEYBOARD_KEYMAP_FORMAT_XKB_V1 || !size) { close(fd); return; }
    char *map = mmap(NULL, size, PROT_READ, MAP_PRIVATE, fd, 0);
    close(fd);
    if (map == MAP_FAILED) { fail(h, errno); return; }
    if (map[size-1] != '\0') { munmap(map, size); fail(h, EINVAL); return; }
    h->keymap = xkb_keymap_new_from_string(h->xkb_context, map, XKB_KEYMAP_FORMAT_TEXT_V1, XKB_KEYMAP_COMPILE_NO_FLAGS);
    munmap(map, size);
    if (!h->keymap) { fail(h, EINVAL); return; }
    h->key_state = xkb_state_new(h->keymap);
    if (!h->key_state) fail(h, ENOMEM);
}
static void keyboard_enter(void *data, struct wl_keyboard *keyboard, uint32_t serial,
    struct wl_surface *surface, struct wl_array *keys) {
    (void)keyboard; (void)serial; (void)keys; dawn_wl_host *h = data;
    h->focused = surface == h->surface;
}
static void keyboard_leave(void *data, struct wl_keyboard *keyboard, uint32_t serial, struct wl_surface *surface) {
    (void)keyboard; (void)serial; (void)surface; dawn_wl_host *h = data; h->focused = 0;
}
static void keyboard_key(void *data, struct wl_keyboard *keyboard, uint32_t serial,
    uint32_t time, uint32_t key, uint32_t state) {
    (void)keyboard; (void)serial; (void)time; dawn_wl_host *h = data;
    if (state != WL_KEYBOARD_KEY_STATE_PRESSED || !h->key_state || key > UINT32_MAX-8) return;
    int action = dawn_wl_key_action(h->focused, xkb_state_key_get_one_sym(h->key_state, key+8));
    if (action) enqueue(h, (dawn_wl_event){.type=action});
}
static void keyboard_modifiers(void *data, struct wl_keyboard *keyboard, uint32_t serial,
    uint32_t depressed, uint32_t latched, uint32_t locked, uint32_t group) {
    (void)keyboard; (void)serial; dawn_wl_host *h = data;
    if (h->key_state) xkb_state_update_mask(h->key_state, depressed, latched, locked, 0, 0, group);
}
static void keyboard_repeat(void *data, struct wl_keyboard *keyboard, int32_t rate, int32_t delay) {
    (void)data; (void)keyboard; (void)rate; (void)delay; /* One control action per press. */
}
static const struct wl_keyboard_listener keyboard_listener = {
    .keymap=keyboard_keymap, .enter=keyboard_enter, .leave=keyboard_leave,
    .key=keyboard_key, .modifiers=keyboard_modifiers, .repeat_info=keyboard_repeat
};
static void remove_keyboard(dawn_wl_host *h) {
    h->focused = 0;
    if (h->keyboard) wl_keyboard_release(h->keyboard);
    h->keyboard = NULL; clear_keymap(h);
}
static void seat_capabilities(void *data, struct wl_seat *seat, uint32_t capabilities) {
    dawn_wl_host *h = data;
    if ((capabilities & WL_SEAT_CAPABILITY_KEYBOARD) && !h->keyboard) {
        h->keyboard = wl_seat_get_keyboard(seat);
        if (!h->keyboard) { fail(h, ENOMEM); return; }
        wl_proxy_set_queue((struct wl_proxy *)h->keyboard, h->queue);
        wl_keyboard_add_listener(h->keyboard, &keyboard_listener, h);
    } else if (!(capabilities & WL_SEAT_CAPABILITY_KEYBOARD)) remove_keyboard(h);
}
static void seat_name(void *data, struct wl_seat *seat, const char *name) {
    (void)data; (void)seat; (void)name;
}
static const struct wl_seat_listener seat_listener = {.capabilities=seat_capabilities, .name=seat_name};
static void registry_global(void *data, struct wl_registry *registry, uint32_t name,
    const char *interface, uint32_t version) {
    dawn_wl_host *h = data;
    if (!strcmp(interface, wl_compositor_interface.name) && !h->compositor) {
        if (version < 3) { fail(h, ENOTSUP); return; }
        h->compositor = wl_registry_bind(registry, name, &wl_compositor_interface, version < 4 ? version : 4);
        h->compositor_name = name;
        wl_proxy_set_queue((struct wl_proxy *)h->compositor, h->queue);
    } else if (!strcmp(interface, xdg_wm_base_interface.name) && !h->wm) {
        h->wm = wl_registry_bind(registry, name, &xdg_wm_base_interface, 1);
        h->wm_name = name;
        wl_proxy_set_queue((struct wl_proxy *)h->wm, h->queue);
        xdg_wm_base_add_listener(h->wm, &wm_listener, h);
    } else if (!strcmp(interface, wl_seat_interface.name) && !h->seat && version >= 5) {
        h->seat = wl_registry_bind(registry, name, &wl_seat_interface, version < 7 ? version : 7);
        h->seat_name = name;
        wl_proxy_set_queue((struct wl_proxy *)h->seat, h->queue);
        wl_seat_add_listener(h->seat, &seat_listener, h);
    } else if (!strcmp(interface, wl_output_interface.name) && version >= 2) {
        struct output *o = calloc(1, sizeof(*o));
        if (!o) { fail(h, ENOMEM); return; }
        o->host = h; o->name = name; o->scale = 1;
        o->proxy = wl_registry_bind(registry, name, &wl_output_interface, 2);
        wl_proxy_set_queue((struct wl_proxy *)o->proxy, h->queue);
        wl_output_add_listener(o->proxy, &output_listener, o);
        o->next = h->outputs; h->outputs = o;
    }
}
static void registry_remove(void *data, struct wl_registry *registry, uint32_t name) {
    (void)registry; dawn_wl_host *h = data;
    if (name == h->compositor_name || name == h->wm_name) fail(h, ENODEV);
    if (name == h->seat_name) {
        remove_keyboard(h);
        if (h->seat) wl_seat_release(h->seat);
        h->seat = NULL; h->seat_name = 0;
    }
    struct output **link = &h->outputs;
    while (*link) {
        struct output *o = *link;
        if (o->name == name) { *link = o->next; wl_output_destroy(o->proxy); free(o); update_scale(h); break; }
        link = &o->next;
    }
}
static const struct wl_registry_listener registry_listener = {.global=registry_global, .global_remove=registry_remove};
static void sync_done(void *data, struct wl_callback *callback, uint32_t serial) {
    (void)serial; dawn_wl_host *h = data; h->registry_done = 1; wl_callback_destroy(callback);
}
static const struct wl_callback_listener sync_listener = {.done=sync_done};
static int64_t now_ms(void) {
    struct timespec t; clock_gettime(CLOCK_MONOTONIC, &t);
    return (int64_t)t.tv_sec*1000 + t.tv_nsec/1000000;
}

/* Only dispatch the host's queue; Dawn/Vulkan WSI retains its own event queues. */
static int pump(dawn_wl_host *h, int timeout) {
    int64_t deadline = now_ms() + timeout;
    while (wl_display_prepare_read_queue(h->display, h->queue) != 0) {
        if (wl_display_dispatch_queue_pending(h->display, h->queue) < 0) { fail(h, errno); return -1; }
        if (h->failed || h->count) return h->failed ? -1 : 0;
        if (now_ms() >= deadline) return 0;
    }
    short events = POLLIN;
    if (wl_display_flush(h->display) < 0) {
        if (errno != EAGAIN) { wl_display_cancel_read(h->display); fail(h, errno); return -1; }
        events |= POLLOUT;
    }
    struct pollfd fds[2] = {{wl_display_get_fd(h->display), events, 0}, {h->wake_fd, POLLIN, 0}};
    int result;
    do {
        int64_t remaining = deadline - now_ms();
        result = poll(fds, 2, remaining > 0 ? (int)remaining : 0);
    } while (result < 0 && errno == EINTR && now_ms() < deadline);
    if (result < 0) { wl_display_cancel_read(h->display); fail(h, errno); return -1; }
    if (fds[0].revents & (POLLERR | POLLHUP | POLLNVAL)) {
        wl_display_cancel_read(h->display); fail(h, EPIPE); return -1;
    }
    if (fds[0].revents & POLLIN) {
        if (wl_display_read_events(h->display) < 0) { fail(h, errno); return -1; }
    } else wl_display_cancel_read(h->display);
    if (fds[1].revents & POLLIN) { uint64_t value; (void)read(h->wake_fd, &value, sizeof(value)); }
    if (fds[0].revents & POLLOUT)
        if (wl_display_flush(h->display) < 0 && errno != EAGAIN) { fail(h, errno); return -1; }
    if (wl_display_dispatch_queue_pending(h->display, h->queue) < 0) { fail(h, errno); return -1; }
    return h->failed ? -1 : 0;
}

dawn_wl_host *dawn_wl_open(const char *title, int32_t width, int32_t height, char *error, uint32_t capacity) {
    dawn_wl_host *h = calloc(1, sizeof(*h));
    if (!h) { if (error && capacity) snprintf(error, capacity, "Wayland allocation failed"); return NULL; }
    h->wake_fd = -1; h->state = (dawn_wl_state){width, height, 1};
    if (!title || dawn_wl_apply_configure(&h->state, width, height, 1) < 0) { fail(h, EINVAL); goto failed; }
    h->display = wl_display_connect(NULL);
    if (!h->display) { fail(h, errno); goto failed; }
    h->queue = wl_display_create_queue(h->display);
    h->wake_fd = eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);
    h->xkb_context = xkb_context_new(XKB_CONTEXT_NO_FLAGS);
    if (!h->queue || h->wake_fd < 0 || !h->xkb_context) { fail(h, ENOMEM); goto failed; }
    struct wl_display *wrapper = wl_proxy_create_wrapper(h->display);
    if (!wrapper) { fail(h, ENOMEM); goto failed; }
    wl_proxy_set_queue((struct wl_proxy *)wrapper, h->queue);
    h->registry = wl_display_get_registry(wrapper);
    struct wl_callback *sync = wl_display_sync(wrapper);
    wl_proxy_wrapper_destroy(wrapper);
    if (!h->registry || !sync) { if (sync) wl_callback_destroy(sync); fail(h, ENOMEM); goto failed; }
    wl_registry_add_listener(h->registry, &registry_listener, h);
    wl_callback_add_listener(sync, &sync_listener, h);
    int64_t deadline = now_ms() + 5000;
    while (!h->registry_done && !h->failed && now_ms() < deadline) pump(h, 16);
    if (!h->registry_done) {
        wl_callback_destroy(sync); fail(h, ETIMEDOUT); goto failed;
    }
    if (h->failed) goto failed;
    if (!h->compositor || !h->wm) { fail(h, ENOTSUP); goto failed; }
    h->surface = wl_compositor_create_surface(h->compositor);
    if (!h->surface) { fail(h, ENOMEM); goto failed; }
    wl_proxy_set_queue((struct wl_proxy *)h->surface, h->queue);
    wl_surface_add_listener(h->surface, &surface_listener, h);
    h->xdg_surface = xdg_wm_base_get_xdg_surface(h->wm, h->surface);
    if (!h->xdg_surface) { fail(h, ENOMEM); goto failed; }
    xdg_surface_add_listener(h->xdg_surface, &xdg_listener, h);
    h->toplevel = xdg_surface_get_toplevel(h->xdg_surface);
    if (!h->toplevel) { fail(h, ENOMEM); goto failed; }
    xdg_toplevel_add_listener(h->toplevel, &toplevel_listener, h);
    xdg_toplevel_set_app_id(h->toplevel, "org.graphiks.dawn4k.demo");
    xdg_toplevel_set_title(h->toplevel, title);
    wl_surface_commit(h->surface);
    if (wl_display_flush(h->display) < 0 && errno != EAGAIN) { fail(h, errno); goto failed; }
    return h;
failed:
    if (error && capacity) snprintf(error, capacity, "Wayland connection/setup failed: %s (code=%d)", strerror(h->failed), h->failed);
    dawn_wl_close(h);
    return NULL;
}
void *dawn_wl_display(dawn_wl_host *h) { return h ? h->display : NULL; }
void *dawn_wl_surface(dawn_wl_host *h) { return h ? h->surface : NULL; }
int32_t dawn_wl_next_event(dawn_wl_host *h, dawn_wl_event *event, int32_t timeout) {
    if (!h || !event || timeout < 0) return -1;
    if (!h->failed && !h->count) pump(h, timeout);
    if (h->failed) { *event = (dawn_wl_event){.type=DAWN_WL_ERROR, .code=h->failed}; return -1; }
    if (!h->count) return 0;
    *event = h->events[h->head]; h->head = (h->head+1) % EVENT_CAPACITY; --h->count;
    return 1;
}
int32_t dawn_wl_set_scale(dawn_wl_host *h, int32_t scale) {
    if (!h || h->failed || scale <= 0) return -1;
    wl_surface_set_buffer_scale(h->surface, scale); return 0;
}
int32_t dawn_wl_set_title(dawn_wl_host *h, const char *title) {
    if (!h || h->failed || !title) return -1;
    xdg_toplevel_set_title(h->toplevel, title); return 0;
}
void dawn_wl_wake(dawn_wl_host *h) {
    if (h && h->wake_fd >= 0) { uint64_t value = 1; (void)write(h->wake_fd, &value, sizeof(value)); }
}
void dawn_wl_close(dawn_wl_host *h) {
    if (!h) return;
    remove_keyboard(h);
    if (h->seat) wl_seat_release(h->seat);
    if (h->toplevel) xdg_toplevel_destroy(h->toplevel);
    if (h->xdg_surface) xdg_surface_destroy(h->xdg_surface);
    if (h->surface) wl_surface_destroy(h->surface);
    while (h->outputs) {
        struct output *o = h->outputs; h->outputs = o->next;
        wl_output_destroy(o->proxy); free(o);
    }
    if (h->wm) xdg_wm_base_destroy(h->wm);
    if (h->compositor) wl_compositor_destroy(h->compositor);
    if (h->registry) wl_registry_destroy(h->registry);
    if (h->queue) wl_event_queue_destroy(h->queue);
    if (h->display) wl_display_disconnect(h->display);
    if (h->xkb_context) xkb_context_unref(h->xkb_context);
    if (h->wake_fd >= 0) close(h->wake_fd);
    free(h);
}
