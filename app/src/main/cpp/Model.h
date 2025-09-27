#ifndef MAGEVOICE_MODEL_H
#define MAGEVOICE_MODEL_H

#include <GLES3/gl3.h>
#include <memory>

#include "Vertex.h"
#include "TextureAsset.h"

class Model {
public:
    Model(const Vertex* vertices, size_t vertex_count, const GLushort* indices, GLsizei index_count, std::shared_ptr<TextureAsset> texture) :
        vertex_data_(vertices),
        vertex_count_(vertex_count),
        index_data_(indices),
        index_count_(index_count),
        texture_(texture) {}

    ~Model() = default;

    const Vertex* getVertexData() const { return vertex_data_; }
    const GLushort* getIndexData() const { return index_data_; }
    GLsizei getIndexCount() const { return index_count_; }
    const TextureAsset& getTexture() const { return *texture_; }

private:
    const Vertex* vertex_data_;
    size_t vertex_count_;
    const GLushort* index_data_;
    GLsizei index_count_;
    std::shared_ptr<TextureAsset> texture_;
};

#endif //MAGEVOICE_MODEL_H