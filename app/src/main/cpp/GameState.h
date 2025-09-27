#ifndef MAGEVOICE_MODEL_H
#define MAGEVOICE_MODEL_H

// Simple 2D Vector
struct Vector2 {
    float x = 0.0f;
    float y = 0.0f;
};

// State for a single player
struct PlayerState {
    Vector2 position;
    Vector2 velocity;
    int hp = 100;
    int mana = 100;
};

// Represents the entire game world state
struct Model {
    PlayerState player;
    // In the future, we can add lists of enemies, projectiles, etc.
    // std::vector<EnemyState> enemies;
};

#endif //MAGEVOICE_MODEL_H
