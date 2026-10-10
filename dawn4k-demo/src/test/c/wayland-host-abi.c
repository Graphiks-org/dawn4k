#include "wayland-host.h"
#include <stddef.h>
#include <stdio.h>

int main(void) {
    printf("size=%zu align=%zu type=%zu width=%zu height=%zu scale=%zu serial=%zu code=%zu\n",
        sizeof(dawn_wl_event), _Alignof(dawn_wl_event),
        offsetof(dawn_wl_event, type), offsetof(dawn_wl_event, width),
        offsetof(dawn_wl_event, height), offsetof(dawn_wl_event, scale),
        offsetof(dawn_wl_event, serial), offsetof(dawn_wl_event, code));
    return 0;
}
