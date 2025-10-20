#include <jni.h>
#include <android/native_window_jni.h>
#include <thread>
#include <atomic>
#include <android/log.h>
#include <mutex>

#include "AndroidOut.h"
#include "Renderer.h"
#include "Model.h" // Corrected include

#define LOG_TAG "MageVoiceNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// --- Global State ---
static const char* LOCAL_PLAYER_ID = "local_player";
static Renderer* g_renderer = nullptr;
static Model g_model; // Use the correct Model struct
static std::atomic<bool> g_rendering(false);
static std::thread g_render_thread;
static std::mutex g_model_mutex; // Mutex to protect access to g_model

// --- Render Loop ---
void render_loop() {
    LOGI("render_loop() started");
    while (g_rendering) {
        try {
            if (g_renderer) {
                std::lock_guard<std::mutex> lock(g_model_mutex);
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

        // Initialize the game model with a local player
        {
            std::lock_guard<std::mutex> lock(g_model_mutex);
            PlayerState local_player;
            local_player.position = {0.0f, 0.0f};
            local_player.velocity = {0.0f, 0.0f};
            g_model.players[LOCAL_PLAYER_ID] = local_player;
            LOGI("Local player initialized in the model");
        }

        g_rendering = true;
        g_render_thread = std::thread(render_loop);
        LOGI("Render thread started");
    } else {
        LOGE("Failed to create ANativeWindow");
    }
    LOGI("JNI initNative() finished\n");
}

JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_onJoystickMovedNative(
        JNIEnv *env,
        jobject /* this */,
        jfloat x,
        jfloat y) {
    // No need for a full lock if we only modify velocity, but it's safer
    std::lock_guard<std::mutex> lock(g_model_mutex);
    auto it = g_model.players.find(LOCAL_PLAYER_ID);
    if (it != g_model.players.end()) {
        it->second.velocity.x = x;
        it->second.velocity.y = y;
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
    
    {
        std::lock_guard<std::mutex> lock(g_model_mutex);
        g_model.players.clear();
    }
    LOGI("JNI cleanupNative() finished");
}

// New JNI function to allow Kotlin to update player states
JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_updatePlayerStateNative(
        JNIEnv *env,
        jobject /* this */,
        jstring playerId,
        jfloat x,
        jfloat y,
        jint hp,
        jint mana) {
    const char* id = env->GetStringUTFChars(playerId, 0);
    
    {
        std::lock_guard<std::mutex> lock(g_model_mutex);
        PlayerState& player = g_model.players[id]; // Creates a new player if ID doesn't exist
        player.position.x = x;
        player.position.y = y;
        player.hp = hp;
        player.mana = mana;
        // Note: We don't update velocity here, as that's controlled by the local joystick
        // or would be part of a more complex network model. Position is updated directly.
    }
    
    env->ReleaseStringUTFChars(playerId, id);
}


} // extern "C"