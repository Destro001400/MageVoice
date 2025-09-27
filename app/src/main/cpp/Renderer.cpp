#include "Renderer.h"

#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window.h>
#include <memory>

#include "AndroidOut.h"
#include "Shader.h"
#include "Utility.h"
#include "GameState.h"
#include "Model.h"
#include "Vertex.h"

#define CORNFLOWER_BLUE 100 / 255.f, 149 / 255.f, 237 / 255.f, 1.0f

// --- Shader Source --- 
const std::string VERTEX_SHADER = R"shader(
    precision mediump float;
    attribute vec3 aPosition;
    attribute vec2 aUV;

    uniform mat4 uProjectionMatrix;
    uniform mat4 uModelMatrix;

    varying vec2 vUV;

    void main() {
        gl_Position = uProjectionMatrix * uModelMatrix * vec4(aPosition, 1.0);
        vUV = aUV;
    }
)shader";

const std::string FRAGMENT_SHADER = R"shader(
    precision mediump float;
    varying vec2 vUV;
    uniform sampler2D uTexture;

    void main() {
        gl_FragColor = texture2D(uTexture, vUV);
    }
)shader";

// Constants for orthographic projection
constexpr float kProjectionHalfHeight = 10.0f;
constexpr float kProjectionNearPlane = -10.0f;
constexpr float kProjectionFarPlane = 10.0f;

// --- Player Quad Data ---
const Vertex g_playerVertices[] = {
    {{-0.5f, -0.5f, 0.0f}, {0.0f, 1.0f}},
    {{0.5f, -0.5f, 0.0f}, {1.0f, 1.0f}},
    {{0.5f,  0.5f, 0.0f}, {1.0f, 0.0f}},
    {{-0.5f,  0.5f, 0.0f}, {0.0f, 0.0f}}
};

const GLushort g_playerIndices[] = { 0, 1, 2, 0, 2, 3 };


Renderer::Renderer() :
    display_(EGL_NO_DISPLAY),
    surface_(EGL_NO_SURFACE),
    context_(EGL_NO_CONTEXT),
    width_(0),
    height_(0),
    shaderNeedsNewProjectionMatrix_(true) {}

Renderer::~Renderer() {
    if (display_ != EGL_NO_DISPLAY) {
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (context_ != EGL_NO_CONTEXT) {
            eglDestroyContext(display_, context_);
        }
        if (surface_ != EGL_NO_SURFACE) {
            eglDestroySurface(display_, surface_);
        }
        eglTerminate(display_);
    }
    display_ = EGL_NO_DISPLAY;
    surface_ = EGL_NO_SURFACE;
    context_ = EGL_NO_CONTEXT;
}

void Renderer::init(ANativeWindow* window) {
    const EGLint attribs[] = { EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT, EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8, EGL_DEPTH_SIZE, 16, EGL_NONE };
    EGLConfig config;
    EGLint numConfigs;

    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(display_, nullptr, nullptr);
    eglChooseConfig(display_, attribs, &config, 1, &numConfigs);

    const EGLint context_attribs[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
    context_ = eglCreateContext(display_, config, nullptr, context_attribs);

    surface_ = eglCreateWindowSurface(display_, config, window, nullptr);

    eglMakeCurrent(display_, surface_, surface_, context_);

    eglQuerySurface(display_, surface_, EGL_WIDTH, &width_);
    eglQuerySurface(display_, surface_, EGL_HEIGHT, &height_);

    // Initialize shader
    shader_.reset(Shader::loadShader(VERTEX_SHADER, FRAGMENT_SHADER, "aPosition", "aUV", "uProjectionMatrix", "uModelMatrix"));
    if (!shader_) {
        aout << "Failed to load shader" << std::endl;
        return;
    }

    // Create a dummy 1x1 white texture
    GLuint dummyTextureId;
    glGenTextures(1, &dummyTextureId);
    glBindTexture(GL_TEXTURE_2D, dummyTextureId);
    GLubyte whitePixel[] = {255, 255, 255, 255};
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, whitePixel);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    auto dummyTexture = std::make_shared<TextureAsset>(dummyTextureId);

    // Initialize player model
    playerModel_ = std::make_unique<Model>(g_playerVertices, 4, g_playerIndices, 6, dummyTexture);

    glEnable(GL_DEPTH_TEST);
    glClearColor(CORNFLOWER_BLUE);
    shader_->activate();
}

void Renderer::update(GameState& model) {
    const float speed = 0.1f; // Player movement speed
    model.player.position.x += model.player.velocity.x * speed;
    model.player.position.y -= model.player.velocity.y * speed; // Y is inverted in screen coordinates

    // Boundary checks
    float aspect = float(width_) / height_;
    float worldHalfWidth = kProjectionHalfHeight * aspect;
    float worldHalfHeight = kProjectionHalfHeight;

    if (model.player.position.x > worldHalfWidth) model.player.position.x = worldHalfWidth;
    if (model.player.position.x < -worldHalfWidth) model.player.position.x = -worldHalfWidth;
    if (model.player.position.y > worldHalfHeight) model.player.position.y = worldHalfHeight;
    if (model.player.position.y < -worldHalfHeight) model.player.position.y = -worldHalfHeight;
}

void Renderer::render(const GameState& model) {
    if (display_ == EGL_NO_DISPLAY || !shader_) {
        return;
    }

    updateRenderArea();

    if (shaderNeedsNewProjectionMatrix_) {
        float projectionMatrix[16] = {0};
        Utility::buildOrthographicMatrix(projectionMatrix, kProjectionHalfHeight, float(width_) / height_, kProjectionNearPlane, kProjectionFarPlane);
        shader_->setProjectionMatrix(projectionMatrix);
        shaderNeedsNewProjectionMatrix_ = false;
    }

    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    // Render Player
    if (playerModel_) {
        float playerModelMatrix[16] = {0};
        Utility::buildTranslationMatrix(playerModelMatrix, model.player.position.x, model.player.position.y, 0.0f);
        shader_->setModelMatrix(playerModelMatrix);
        shader_->drawModel(*playerModel_);
    }

    eglSwapBuffers(display_, surface_);
}

void Renderer::handleInput() {
    // Not used in this architecture
}

void Renderer::updateRenderArea() {
    EGLint currentWidth = 0, currentHeight = 0;
    eglQuerySurface(display_, surface_, EGL_WIDTH, &currentWidth);
    eglQuerySurface(display_, surface_, EGL_HEIGHT, &currentHeight);

    if (width_ != currentWidth || height_ != currentHeight) {
        width_ = currentWidth;
        height_ = currentHeight;
        glViewport(0, 0, width_, height_);
        shaderNeedsNewProjectionMatrix_ = true;
    }
}
