package com.ellosan.amazing.block;

import net.minecraft.block.Block;

/**
 * An Amazing Express Elevator pad. Stand on it and JUMP to ride up to the
 * next pad, SNEAK to ride down. The whoosh, particles and camera pull are
 * handled by {@link com.ellosan.amazing.economy.ElevatorManager}.
 */
public class ElevatorBlock extends Block {

	public ElevatorBlock(Settings settings) {
		super(settings);
	}
}
