#ifndef MAGEVOICE_VERTEX_H
#define MAGEVOICE_VERTEX_H

struct Vector3 {
    float x, y, z;
};

struct Vector2 {
    float x, y;
};

struct Vertex {
    Vector3 pos;
    Vector2 uv;
};

#endif //MAGEVOICE_VERTEX_H
