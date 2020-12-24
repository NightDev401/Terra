package com.dfsek.terra.api.structures.script.functions;

import com.dfsek.terra.api.math.vector.Location;
import com.dfsek.terra.api.platform.world.Chunk;
import com.dfsek.terra.api.structures.parser.lang.functions.Function;
import com.dfsek.terra.api.structures.structure.Rotation;
import com.dfsek.terra.api.structures.tokenizer.Position;

public class RecursionsFunction implements Function<Number> {
    private final Position position;

    public RecursionsFunction(Position position) {
        this.position = position;
    }

    @Override
    public String name() {
        return "recursions";
    }

    @Override
    public ReturnType returnType() {
        return ReturnType.NUMBER;
    }

    @Override
    public Number apply(Location location, Rotation rotation, int recursions) {
        return recursions;
    }

    @Override
    public Number apply(Location location, Chunk chunk, Rotation rotation, int recursions) {
        return recursions;
    }

    @Override
    public Position getPosition() {
        return position;
    }
}