package net.megaplanet.endportalminer;

import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public enum MineFace {

    DOWN(0.5, 0, 0.5),
    UP(0.5, 1, 0.5),
    NORTH(0, 0.5, 0.5),
    SOUTH(1, 0.5, 0.5),
    WEST(0.5, 0.5, 0),
    EAST(0.5, 0.5, 1);

    private final Vector offset;

    MineFace(double x, double y, double z) {
        this.offset = new Vector(x, y, z);
    }

    public Vector getOffset() {
        return offset;
    }

    public static MineFace getFromDirection(EnumWrappers.Direction direction) {
        return TRANSLATIONS.get(direction);
    }

    private final static Map<EnumWrappers.Direction, MineFace> TRANSLATIONS = new HashMap<>();

    static {
        for (EnumWrappers.Direction value : EnumWrappers.Direction.values()) {
            TRANSLATIONS.put(value, MineFace.valueOf(value.name()));
        }
    }
}
