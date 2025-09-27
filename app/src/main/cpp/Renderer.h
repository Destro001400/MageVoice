#ifndef MAGEVOICE_RENDERER_H
#define MAGEVOICE_RENDERER_H

#include <EGL/egl.h>
#include <memory>

#include "GameState.h"
class Model; // Forward declaration for the drawable model
#include "Shader.h"

struct ANativeWindow;

class Renderer {
public:
    Renderer();
    virtual ~Renderer();

    // Initialize the renderer with a native window
    void init(ANativeWindow* window);

    // Update the game state
    void update(Model& model);

    // Render the game state
    void render(const Model& model);

    // Handle any continuous input (not joystick)
    void handleInput();

private:
    void initRenderer();
    void updateRenderArea();

    EGLDisplay display_;
    EGLSurface surface_;
    EGLContext context_;
    EGLint width_;
    EGLint height_;

    bool shaderNeedsNewProjectionMatrix_;

    std::unique_ptr<Shader> shader_;
    std::unique_ptr<Model> playerModel_;
};

#endif //MAGEVOICE_RENDERER_H
