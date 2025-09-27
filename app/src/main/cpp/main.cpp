#include <jni.h>
#include <android/native_window_jni.h>
#include <thread>
#include <atomic>

#include "AndroidOut.h"
#include "Renderer.h"
#include "GameState.h" // Assuming Model holds player state

// --- Global State ---
static Renderer* g_renderer = nullptr;
static Model g_model; // A simple model to hold game state
static std::atomic<bool> g_rendering(false);
static std::thread g_render_thread;

// --- Render Loop ---
void render_loop() {
    while (g_rendering) {
        if (g_renderer) {
            g_renderer->handleInput(); // Handle any continuous input
            g_renderer->update(g_model); // Update game state
            g_renderer->render(g_model); // Render the model
        }
        // Sleep for a short duration to not burn CPU
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
}

// --- JNI Bridge Implementation ---
extern "C" {

JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_initNative(
        JNIEnv *env,
        jobject /* this */,
        jobject surface) {
    if (g_renderer) {
        delete g_renderer;
        g_renderer = nullptr;
    }

    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    if (window) {
        g_renderer = new Renderer();
        g_renderer->init(window);
        ANativeWindow_release(window); // Renderer should have its own reference if needed

        g_rendering = true;
        g_render_thread = std::thread(render_loop);
    }
}

JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_onJoystickMovedNative(
        JNIEnv *env,
        jobject /* this */,
        jfloat x,
        jfloat y) {
    if (g_renderer) {
        // Let's assume the renderer can directly update the player's velocity
        g_model.player.velocity.x = x;
        g_model.player.velocity.y = y;
    }
}

JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_cleanupNative(
        JNIEnv *env,
        jobject /* this */) {
    g_rendering = false;
    if (g_render_thread.joinable()) {
        g_render_thread.join();
    }

    if (g_renderer) {
        delete g_renderer;
        g_renderer = nullptr;
    }
}

} // extern "C"