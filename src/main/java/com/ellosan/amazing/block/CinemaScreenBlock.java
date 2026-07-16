package com.ellosan.amazing.block;

import net.minecraft.block.Block;

/**
 * One tile of a cinema screen. The magic is in the texture: an animated
 * 8-frame strip (via .mcmeta) so a wall of these plays a looping "movie".
 * Always on, always showing the good stuff. Popcorn sold separately.
 */
public class CinemaScreenBlock extends Block {

	public CinemaScreenBlock(Settings settings) {
		super(settings);
	}
}
