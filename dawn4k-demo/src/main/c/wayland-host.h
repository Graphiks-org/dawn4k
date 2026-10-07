#ifndef DAWN4K_WAYLAND_HOST_H
#define DAWN4K_WAYLAND_HOST_H
#include <stdint.h>

typedef struct dawn_wl_host dawn_wl_host;
enum dawn_wl_event_type {
    DAWN_WL_CONFIGURE=1, DAWN_WL_PAUSE=2, DAWN_WL_RESET=3,
    DAWN_WL_COUNT_UP=4, DAWN_WL_COUNT_DOWN=5, DAWN_WL_CLOSE=6, DAWN_WL_ERROR=7
};
typedef struct dawn_wl_event {
    int32_t type, width, height, scale;
    uint32_t serial;
    int32_t code;
} dawn_wl_event;

dawn_wl_host *dawn_wl_open(const char *title, int32_t width, int32_t height,
                          char *error, uint32_t error_capacity);
void *dawn_wl_display(dawn_wl_host *host);
void *dawn_wl_surface(dawn_wl_host *host);
/* Owner thread only. Returns 1 for an event, 0 for timeout/wake, -1 for error. */
int32_t dawn_wl_next_event(dawn_wl_host *host, dawn_wl_event *event, int32_t timeout_ms);
int32_t dawn_wl_set_scale(dawn_wl_host *host, int32_t scale);
int32_t dawn_wl_set_title(dawn_wl_host *host, const char *title);
/* The only cross-thread operation; caller ensures host remains alive. */
void dawn_wl_wake(dawn_wl_host *host);
void dawn_wl_close(dawn_wl_host *host);
#endif
