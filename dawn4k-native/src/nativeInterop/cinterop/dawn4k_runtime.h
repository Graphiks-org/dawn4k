#ifndef DAWN4K_RUNTIME_H
#define DAWN4K_RUNTIME_H
#ifdef __linux__
/* NULL after successful process-wide initialization; otherwise a stable error. */
const char * dawn4kDawnLoadError(void);
#else
static inline const char * dawn4kDawnLoadError(void) { return 0; }
#endif
#endif
