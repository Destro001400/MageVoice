#include "Renderer.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <GLES3/gl3.h>

#include "AndroidOut.h"
#include "Shader.h"
#include "Utility.h"

#define CORNFLOWER_BLUE 100 / 255.f, 149 / 255.f, 237 / 255.f, 1

// ... (shader strings remain the same) ...

void Renderer::render(const std::vector<PlayerState> &players, const std::vector<ProjectileState> &projectiles) {
    updateRenderArea();

    if (shaderNeedsNewProjectionMatrix_) {
        float projectionMatrix[16] = {0};
        Utility::buildOrthographicMatrix(projectionMatrix, kProjectionHalfHeight, float(width_) / height_, kProjectionNearPlane, kProjectionFarPlane);
        shader_->setProjectionMatrix(projectionMatrix);
        shaderNeedsNewProjectionMatrix_ = false;
    }

    glClear(GL_COLOR_BUFFER_BIT);

    if (models_.empty()) return;

    // Render Players
    for (const auto &player : players) {
        float playerModelMatrix[16] = {0};
        Utility::buildTranslationMatrix(playerModelMatrix, player.x, player.y, 0.0f);
        shader_->setModelMatrix(playerModelMatrix);
        shader_->drawModel(models_[0]);
    }

    // Render Projectiles
    for (const auto &projectile : projectiles) {
        float transMatrix[16] = {0};
        float scaleMatrix[16] = {0};
        float modelMatrix[16] = {0};

        Utility::buildTranslationMatrix(transMatrix, projectile.x, projectile.y, 0.0f);
        Utility::buildScaleMatrix(scaleMatrix, 0.25f, 0.25f, 1.0f);
        Utility::multiplyMatrices(modelMatrix, transMatrix, scaleMatrix);

        shader_->setModelMatrix(modelMatrix);
        shader_->drawModel(models_[0]);
    }

    eglSwapBuffers(display_, surface_);
}

// ... (rest of the file remains the same: initRenderer, createModels, etc.) ...
