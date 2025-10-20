#include <jni.h>
#include <android/native_window_jni.h>
#include <thread>
#include <atomic>
#include <android/log.h>

#include "AndroidOut.h"
#include "Renderer.h"
#include "GameState.h"

#define LOG_TAG "MageVoiceNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// --- Global State ---
static Renderer* g_renderer = nullptr;
static GameState g_model; // Changed from Model to GameState
static std::atomic<bool> g_rendering(false);
static std::thread g_render_thread;

// --- Render Loop ---
void render_loop() {
    LOGI("render_loop() started");
    while (g_rendering) {
        try {
            if (g_renderer) {
                g_renderer->update(g_model);
                g_renderer->render(g_model);
            }
        } catch (const std::exception& e) {
            LOGE("Exception in render_loop: %s", e.what());
        } catch (...) {
            LOGE("Unknown exception in render_loop");
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
    LOGI("render_loop() finished");
}

// --- JNI Bridge Implementation ---
extern "C" {

JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_initNative(
        JNIEnv *env,
        jobject /* this */,
        jobject surface) {
    LOGI("JNI initNative() called");
    if (g_renderer) {
        LOGI("Deleting existing renderer");
        delete g_renderer;
        g_renderer = nullptr;
    }

    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    if (window) {
        LOGI("ANativeWindow created successfully");
        g_renderer = new Renderer();
        g_renderer->init(window);
        ANativeWindow_release(window);

        g_rendering = true;
        g_render_thread = std::thread(render_loop);
        LOGI("Render thread started");
    } else {
        LOGE("Failed to create ANativeWindow");
    }
    LOGI("JNI initNative() finished
");
}

JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_onJoystickMovedNative(
        JNIEnv *env,
        jobject /* this */,
        jfloat x,
        jfloat y) {
    if (g_renderer) {
        g_model.player.velocity.x = x;
        g_model.player.velocity.y = y;
    }
}

JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_cleanupNative(
        JNIEnv *env,
        jobject /* this */) {
    LOGI("JNI cleanupNative() called");
    g_rendering = false;
    if (g_render_thread.joinable()) {
        LOGI("Joining render thread");
        g_render_thread.join();
        LOGI("Render thread joined");
    }

    if (g_renderer) {
        LOGI("Deleting renderer");
        delete g_renderer;
        g_renderer = nullptr;
    }
    LOGI("JNI cleanupNative() finished");
}

} // extern "C"
