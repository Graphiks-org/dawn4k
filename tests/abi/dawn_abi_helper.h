#ifndef DAWN_ABI_HELPER_H
#define DAWN_ABI_HELPER_H

#include <dawn/webgpu.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Receives a WGPUBufferMapCallbackInfo by value and invokes its callback with a
 * temporary WGPUStringView ("ab\0cd", explicit length 5).
 */
int dawn_abi_invoke_buffer_map(WGPUBufferMapCallbackInfo info, int status);

#ifdef __cplusplus
}
#endif

#endif /* DAWN_ABI_HELPER_H */
