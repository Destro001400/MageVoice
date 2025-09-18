#include "Utility.h"
#include "AndroidOut.h"

#include <GLES3/gl3.h>

bool Utility::checkAndLogGlError(bool alwaysLog) { /* ... implementation ... */ return true; }

float *Utility::buildOrthographicMatrix(float *outMatrix, float halfHeight, float aspect, float near, float far) {
    float halfWidth = halfHeight * aspect;
    outMatrix[0] = 1.f / halfWidth; outMatrix[4] = 0.f; outMatrix[8] = 0.f; outMatrix[12] = 0.f;
    outMatrix[1] = 0.f; outMatrix[5] = 1.f / halfHeight; outMatrix[9] = 0.f; outMatrix[13] = 0.f;
    outMatrix[2] = 0.f; outMatrix[6] = 0.f; outMatrix[10] = -2.f / (far - near); outMatrix[14] = -(far + near) / (far - near);
    outMatrix[3] = 0.f; outMatrix[7] = 0.f; outMatrix[11] = 0.f; outMatrix[15] = 1.f;
    return outMatrix;
}

float *Utility::buildIdentityMatrix(float *outMatrix) {
    outMatrix[0] = 1.f; outMatrix[4] = 0.f; outMatrix[8] = 0.f; outMatrix[12] = 0.f;
    outMatrix[1] = 0.f; outMatrix[5] = 1.f; outMatrix[9] = 0.f; outMatrix[13] = 0.f;
    outMatrix[2] = 0.f; outMatrix[6] = 0.f; outMatrix[10] = 1.f; outMatrix[14] = 0.f;
    outMatrix[3] = 0.f; outMatrix[7] = 0.f; outMatrix[11] = 0.f; outMatrix[15] = 1.f;
    return outMatrix;
}

float *Utility::buildTranslationMatrix(float *outMatrix, float x, float y, float z) {
    buildIdentityMatrix(outMatrix);
    outMatrix[12] = x;
    outMatrix[13] = y;
    outMatrix[14] = z;
    return outMatrix;
}

float *Utility::buildScaleMatrix(float *outMatrix, float x, float y, float z) {
    buildIdentityMatrix(outMatrix);
    outMatrix[0] = x;
    outMatrix[5] = y;
    outMatrix[10] = z;
    return outMatrix;
}

float *Utility::multiplyMatrices(float *outMatrix, float *matrixA, float *matrixB) {
    float result[16];
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            result[i * 4 + j] = 0;
            for (int k = 0; k < 4; k++) {
                result[i * 4 + j] += matrixA[k * 4 + j] * matrixB[i * 4 + k];
            }
        }
    }
    for (int i = 0; i < 16; i++) {
        outMatrix[i] = result[i];
    }
    return outMatrix;
}