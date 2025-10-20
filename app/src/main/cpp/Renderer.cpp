#include "Renderer.h"

#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window.h>
#include <memory>
#include <android/log.h>

#include "AndroidOut.h"
#include "Shader.h"
#include "Utility.h"
#include "Model.h"
#include "Vertex.h"

#define LOG_TAG "MageVoiceNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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
    shaderNeedsNewProjectionMatrix_(true) {
    LOGI("Renderer constructor");
}

Renderer::~Renderer() {
    LOGI("Renderer destructor");
    if (display_ != EGL_NO_DISPLAY) {
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (context_ != EGL_NO_CONTEXT) eglDestroyContext(display_, context_);
        if (surface_ != EGL_NO_SURFACE) eglDestroySurface(display_, surface_);
        eglTerminate(display_);
    }
    display_ = EGL_NO_DISPLAY;
    surface_ = EGL_NO_SURFACE;
    context_ = EGL_NO_CONTEXT;
}

void Renderer::init(ANativeWindow* window) {
    LOGI("Renderer::init() start");
    const EGLint attribs[] = { EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT, EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8, EGL_DEPTH_SIZE, 16, EGL_NONE };
    EGLConfig config;
    EGLint numConfigs;

    LOGI("Calling eglGetDisplay");
    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display_ == EGL_NO_DISPLAY) { LOGE("eglGetDisplay failed"); return; }

    LOGI("Calling eglInitialize");
    if (!eglInitialize(display_, nullptr, nullptr)) { LOGE("eglInitialize failed"); return; }

    LOGI("Calling eglChooseConfig");
    if (!eglChooseConfig(display_, attribs, &config, 1, &numConfigs)) { LOGE("eglChooseConfig failed"); return; }

    LOGI("Calling eglCreateContext");
    const EGLint context_attribs[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
    context_ = eglCreateContext(display_, config, nullptr, context_attribs);
    if (context_ == EGL_NO_CONTEXT) { LOGE("eglCreateContext failed"); return; }

    LOGI("Calling eglCreateWindowSurface");
    surface_ = eglCreateWindowSurface(display_, config, window, nullptr);
    if (surface_ == EGL_NO_SURFACE) { LOGE("eglCreateWindowSurface failed"); return; }

    LOGI("Calling eglMakeCurrent");
    if (!eglMakeCurrent(display_, surface_, surface_, context_)) { LOGE("eglMakeCurrent failed"); return; }

    LOGI("Calling eglQuerySurface");
    eglQuerySurface(display_, surface_, EGL_WIDTH, &width_);
    eglQuerySurface(display_, surface_, EGL_HEIGHT, &height_);

    LOGI("Loading shader");
    shader_.reset(Shader::loadShader(VERTEX_SHADER, FRAGMENT_SHADER, "aPosition", "aUV", "uProjectionMatrix", "uModelMatrix"));
    if (!shader_) { LOGE("Shader::loadShader failed"); return; }

    LOGI("Creating dummy texture");
    GLuint dummyTextureId;
    glGenTextures(1, &dummyTextureId);
    glBindTexture(GL_TEXTURE_2D, dummyTextureId);
    GLubyte whitePixel[] = {255, 255, 255, 255};
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, whitePixel);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    auto dummyTexture = std::make_shared<TextureAsset>(dummyTextureId);

    LOGI("Creating player model");
    // This is the mesh and texture for a player, it will be reused for all players.
    playerModel_ = std::make_unique<Model>(g_playerVertices, 4, g_playerIndices, 6, dummyTexture);

    glEnable(GL_DEPTH_TEST);
    glClearColor(CORNFLOWER_BLUE);
    shader_->activate();
    LOGI("Renderer::init() finished");
}

// Update logic now iterates through all players
void Renderer::update(Model& model) {
    const float speed = 0.1f;
    float aspect = float(width_) / height_;
    float worldHalfWidth = kProjectionHalfHeight * aspect;
    float worldHalfHeight = kProjectionHalfHeight;

    for (auto& pair : model.players) {
        PlayerState& player = pair.second;
        player.position.x += player.velocity.x * speed;
        player.position.y -= player.velocity.y * speed;

        // World boundaries check
        if (player.position.x > worldHalfWidth) player.position.x = worldHalfWidth;
        if (player.position.x < -worldHalfWidth) player.position.x = -worldHalfWidth;
        if (player.position.y > worldHalfHeight) player.position.y = worldHalfHeight;
        if (player.position.y < -worldHalfHeight) player.position.y = -worldHalfHeight;
    }
}

// Render logic now iterates through all players
void Renderer::render(const Model& model) {
    if (display_ == EGL_NO_DISPLAY || !shader_) return;

    updateRenderArea();
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    if (playerModel_) {
        for (const auto& pair : model.players) {
            const PlayerState& player = pair.second;
            float playerModelMatrix[16] = {0};
            Utility::buildTranslationMatrix(playerModelMatrix, player.position.x, player.position.y, 0.0f);
            shader_->setModelMatrix(playerModelMatrix);
            shader_->drawModel(*playerModel_);
        }
    }

    if (eglSwapBuffers(display_, surface_) != EGL_TRUE) {
        LOGE("eglSwapBuffers failed!");
    }
}

void Renderer::handleInput() {}

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
