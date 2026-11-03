//
// Created by Vera-Firefly on 17.01.2025.
//
#include <EGL/egl.h>
#include <stdbool.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <limits.h>
#include <dlfcn.h>
#include <android/dlext.h>
#include "nsbypass.h"
#include "GL/gl.h"

//#define ADRENO_POSSIBLE
#ifdef ADRENO_POSSIBLE

bool checkDriverGpuFamily(const char* gpu) {
    if (gpu && strcmp(gpu, "any") == 0)
        return true;
    bool want_mali = gpu && strcmp(gpu, "mali") == 0;

    EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (eglDisplay == EGL_NO_DISPLAY || eglInitialize(eglDisplay, NULL, NULL) != EGL_TRUE) {
        printf("CustomDriver: GPU check failed: eglGetDisplay/eglInitialize (egl error 0x%x)\n", eglGetError());
        return false;
    }

    EGLint egl_attributes[] = {
        EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8, EGL_DEPTH_SIZE, 24, EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL_NONE
    };

    EGLint num_configs = 0;
    if (eglChooseConfig(eglDisplay, egl_attributes, NULL, 0, &num_configs) != EGL_TRUE || num_configs == 0) {
        printf("CustomDriver: GPU check failed: no matching EGL config (egl error 0x%x)\n", eglGetError());
        eglTerminate(eglDisplay);
        return false;
    }

    EGLConfig eglConfig;
    eglChooseConfig(eglDisplay, egl_attributes, &eglConfig, 1, &num_configs);

    const EGLint egl_context_attributes[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
    EGLContext context = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, egl_context_attributes);
    if (context == EGL_NO_CONTEXT) {
        printf("CustomDriver: GPU check failed: eglCreateContext (egl error 0x%x)\n", eglGetError());
        eglTerminate(eglDisplay);
        return false;
    }

    if (eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, context) != EGL_TRUE) {
        printf("CustomDriver: GPU check failed: eglMakeCurrent (egl error 0x%x)\n", eglGetError());
        eglDestroyContext(eglDisplay, context);
        eglTerminate(eglDisplay);
        return false;
    }

    const char* vendor = (const char*)glGetString(GL_VENDOR);
    const char* renderer = (const char*)glGetString(GL_RENDERER);

    bool matched;
    if (want_mali)
        matched = (renderer && strstr(renderer, "Mali") != NULL);
    else
        matched = (vendor && renderer && strcmp(vendor, "Qualcomm") == 0 && strstr(renderer, "Adreno") != NULL);

    printf("CustomDriver: GPU check (want=%s) vendor=%s renderer=%s -> %s\n",
           gpu ? gpu : "adreno", vendor ? vendor : "(null)", renderer ? renderer : "(null)",
           matched ? "match" : "no match");

    eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(eglDisplay, context);
    eglTerminate(eglDisplay);

    return matched;
}

/**
 * Load a custom Vulkan driver (Turnip/panvk/...) through the linker namespace bypass.
 * @param driver_lib driver library file name, NULL/empty falls back to libvulkan_freedreno.so
 * @param driver_gpu GPU family gate: "adreno" (default), "mali" or "any"
 */
void* loadCustomVulkan(const char* driver_lib, const char* driver_gpu, const char* native_dir, const char* cache_dir) {
    if (!checkDriverGpuFamily(driver_gpu))
        return NULL;

    if (!native_dir || !cache_dir) {
        printf("CustomDriver: missing native_dir (%s) or cache_dir (%s)\n",
               native_dir ? native_dir : "(null)", cache_dir ? cache_dir : "(null)");
        return NULL;
    }

    // Plugin folders usually only carry the driver itself, while liblinkerhook.so lives in the
    // launcher's native dir, so expose both to the driver namespace. Vendor dirs are appended
    // because the system libvulkan.so loader may depend on vendor libs (e.g. MediaTek libgpud_sys.so).
    static const char* VENDOR_PATHS = "/vendor/lib64:/vendor/lib64/hw:/system_ext/lib64";
    char search_path[PATH_MAX * 2];
    const char* launcher_dir = getenv("POJAV_NATIVEDIR");
    if (launcher_dir && *launcher_dir && strcmp(launcher_dir, native_dir) != 0)
        snprintf(search_path, sizeof(search_path), "%s:%s:%s", native_dir, launcher_dir, VENDOR_PATHS);
    else
        snprintf(search_path, sizeof(search_path), "%s:%s", native_dir, VENDOR_PATHS);

    if (!linker_ns_load(search_path)) {
        printf("CustomDriver: linker_ns_load failed for %s\n", search_path);
        return NULL;
    }

    void* linkerhook = linker_ns_dlopen("liblinkerhook.so", RTLD_LOCAL | RTLD_NOW);
    if (!linkerhook) {
        printf("CustomDriver: dlopen liblinkerhook.so failed: %s\n", dlerror());
        return NULL;
    }

    const char* target_driver = (driver_lib && strlen(driver_lib) > 0) ? driver_lib : "libvulkan_freedreno.so";
    void* driver_handle = linker_ns_dlopen(target_driver, RTLD_LOCAL | RTLD_NOW);

    if (!driver_handle) {
        printf("CustomDriver: dlopen %s failed: %s\n", target_driver, dlerror());
        dlclose(linkerhook);
        return NULL;
    }

    void* dl_android = linker_ns_dlopen("libdl_android.so", RTLD_LOCAL | RTLD_LAZY);
    if (!dl_android) {
        printf("CustomDriver: dlopen libdl_android.so failed: %s\n", dlerror());
        dlclose(linkerhook);
        dlclose(driver_handle);
        return NULL;
    }

    void* android_get_exported_namespace = dlsym(dl_android, "android_get_exported_namespace");
    void (*linkerhookPassHandles)(void*, void*, void*) = dlsym(linkerhook, "linker_hook_set_handles");

    if (!linkerhookPassHandles || !android_get_exported_namespace) {
        printf("CustomDriver: missing symbols (linker_hook_set_handles=%p, android_get_exported_namespace=%p)\n",
               (void*) linkerhookPassHandles, android_get_exported_namespace);
        dlclose(dl_android);
        dlclose(linkerhook);
        dlclose(driver_handle);
        return NULL;
    }

    linkerhookPassHandles(driver_handle, android_dlopen_ext, android_get_exported_namespace);

    void* libvulkan = linker_ns_dlopen_unique(cache_dir, "libvulkan.so", RTLD_LOCAL | RTLD_NOW);
    if (!libvulkan) {
        printf("CustomDriver: dlopen_unique libvulkan.so failed in %s: %s\n", cache_dir, dlerror());
        dlclose(dl_android);
        dlclose(linkerhook);
        dlclose(driver_handle);
        return NULL;
    }

    printf("CustomDriver: %s loaded via linkerhook\n", target_driver);
    return libvulkan;
}

#endif