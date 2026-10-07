#ifndef DAWN4K_WAYLAND_HOST_STATE_H
#define DAWN4K_WAYLAND_HOST_STATE_H
#include "wayland-host.h"
#include <limits.h>
#include <xkbcommon/xkbcommon-keysyms.h>

typedef struct dawn_wl_state { int32_t width, height, scale; } dawn_wl_state;

static inline int dawn_wl_apply_configure(dawn_wl_state *state, int32_t width,
                                         int32_t height, int32_t scale) {
    if (width < 0 || height < 0 || scale <= 0) return -1;
    if (!width) width = state->width;
    if (!height) height = state->height;
    if (width <= 0 || height <= 0 || width > INT_MAX / scale || height > INT_MAX / scale) return -1;
    *state = (dawn_wl_state){width, height, scale};
    return 0;
}

static inline int dawn_wl_key_action(int focused, uint32_t symbol) {
    if (!focused) return 0;
    switch (symbol) {
        case XKB_KEY_space: return DAWN_WL_PAUSE;
        case XKB_KEY_r: case XKB_KEY_R: return DAWN_WL_RESET;
        case XKB_KEY_plus: case XKB_KEY_KP_Add: return DAWN_WL_COUNT_UP;
        case XKB_KEY_minus: case XKB_KEY_KP_Subtract: return DAWN_WL_COUNT_DOWN;
        case XKB_KEY_Escape: return DAWN_WL_CLOSE;
        default: return 0;
    }
}
#endif
