package com.dfsek.terra.biome.pipeline.source;

import com.dfsek.terra.api.world.biome.TerraBiome;

public interface BiomeSource {
    TerraBiome getBiome(int x, int z);
}