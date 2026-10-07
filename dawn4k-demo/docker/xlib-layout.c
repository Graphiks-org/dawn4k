#include <X11/Xlib.h>
#include <stddef.h>
#include <stdio.h>

int main(void) {
    printf("XEvent.size=%zu\n", sizeof(XEvent));
    printf("XErrorEvent.size=%zu\n", sizeof(XErrorEvent));
    printf("XErrorEvent.resourceid=%zu\n", offsetof(XErrorEvent, resourceid));
    printf("XErrorEvent.error_code=%zu\n", offsetof(XErrorEvent, error_code));
    printf("XErrorEvent.request_code=%zu\n", offsetof(XErrorEvent, request_code));
    printf("unsigned_long.size=%zu\n", sizeof(unsigned long));
    printf("XWindowAttributes.size=%zu\n", sizeof(XWindowAttributes));
    printf("XWindowAttributes.map_state=%zu\n", offsetof(XWindowAttributes, map_state));
    return 0;
}
