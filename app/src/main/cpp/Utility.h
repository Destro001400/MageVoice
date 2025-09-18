#ifndef ANDROIDGLINVESTIGATIONS_UTILITY_H
#define ANDROIDGLINVESTIGATIONS_UTILITY_H

#include <cassert>

class Utility {
public:
    static bool checkAndLogGlError(bool alwaysLog = false);

    static inline void assertGlError() { assert(checkAndLogGlError()); }

    static float *buildOrthographicMatrix(
            float *outMatrix,
            float halfHeight,
            float aspect,
            float near,
            float far);

    static float *buildIdentityMatrix(float *outMatrix);

    static float *buildTranslationMatrix(float *outMatrix, float x, float y, float z);

    static float *buildScaleMatrix(float *outMatrix, float x, float y, float z);

    static float *multiplyMatrices(float *outMatrix, float *matrixA, float *matrixB);
};

#endif //ANDROIDGLINVESTIGATIONS_UTILITY_H