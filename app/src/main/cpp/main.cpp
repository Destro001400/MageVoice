#include <jni.h>
#include <vector>

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <game-activity/GameActivity.h>

#include "AndroidOut.h"
#include "Renderer.h"

// --- Game State ---
static std::vector<PlayerState> g_players;
static std::vector<ProjectileState> g_projectiles;

// --- JNI Bridge ---

extern "C" {

JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_updatePlayers(JNIEnv *env, jobject thiz, jfloatArray players_data) {
    g_players.clear();
    jsize len = env->GetArrayLength(players_data);
    jfloat *data = env->GetFloatArrayElements(players_data, 0);

    // Data is structured as [id, x, y, id, x, y, ...]
    for (int i = 0; i < len; i += 3) {
        g_players.push_back({data[i], data[i+1], data[i+2]});
    }

    env->ReleaseFloatArrayElements(players_data, data, 0);
}

JNIEXPORT void JNICALL
Java_com_game_voicespells_presentation_activities_GameActivity_updateProjectiles(JNIEnv *env, jobject thiz, jfloatArray projectile_data) {
    g_projectiles.clear();
    jsize len = env->GetArrayLength(projectile_data);
    jfloat *data = env->GetFloatArrayElements(projectile_data, 0);

    for (int i = 0; i < len; i += 4) {
        g_projectiles.push_back({data[i], data[i+1], data[i+2], data[i+3]});
    }

    env->ReleaseFloatArrayElements(projectile_data, data, 0);
}

// --- Android App Lifecycle ---

void handle_cmd(android_app *pApp, int32_t cmd) { /* ... */ }

bool motion_event_filter_func(const GameActivityMotionEvent *motionEvent) { /* ... */ }

void android_main(struct android_app *pApp) {
    // ...
    do {
        // ...
        if (pApp->userData) {
            auto *pRenderer = reinterpret_cast<Renderer *>(pApp->userData);
            pRenderer->handleInput();
            pRenderer->render(g_players, g_projectiles);
        }
    } while (!pApp->destroyRequested);
}

}
