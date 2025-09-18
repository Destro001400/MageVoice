#ifndef ANDROIDGLINVESTIGATIONS_RENDERER_H
#define ANDROIDGLINVESTIGATIONS_RENDERER_H

#include <EGL/egl.h>
#include <memory>
#include <vector>

#include "Model.h"
#include "Shader.h"

struct android_app;

// Structs to hold game state information passed from the JNI layer
struct PlayerState {
    float id;
    float x;
    float y;
};

struct ProjectileState {
    float id;
    float x;
    float y;
    float type;
};

class Renderer {
public:
    inline Renderer(android_app *pApp) :
            app_(pApp),
            display_(EGL_NO_DISPLAY),
            surface_(EGL_NO_SURFACE),
            context_(EGL_NO_CONTEXT),
            width_(0),
            height_(0),
            shaderNeedsNewProjectionMatrix_(true) {
        initRenderer();
    }

    virtual ~Renderer();

    void handleInput();

    /*!
     * Renders the game state.
     * @param players A vector of all active players.
     * @param projectiles A vector of all active projectiles.
     */
    void render(const std::vector<PlayerState> &players, const std::vector<ProjectileState> &projectiles);

private:
    void initRenderer();
    void updateRenderArea();
    void createModels();

    android_app *app_;
    EGLDisplay display_;
    EGLSurface surface_;
    EGLContext context_;
    EGLint width_;
    EGLint height_;

    bool shaderNeedsNewProjectionMatrix_;

    std::unique_ptr<Shader> shader_;
    std::vector<Model> models_;
};

#endif //ANDROIDGLINVESTIGATIONS_RENDERER_H