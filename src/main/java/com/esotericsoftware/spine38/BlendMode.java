package com.esotericsoftware.spine38;

import com.badlogic.gdx.graphics.GL20;

/** Determines how images are blended with existing pixels when drawn. */
public enum BlendMode {
	normal(GL20.GL_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA), //
	additive(GL20.GL_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE), //
	multiply(GL20.GL_DST_COLOR, GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA), //
	screen(GL20.GL_ONE, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR), //
	;

	final int source;
	final int sourcePMA;
	final int dest;

	BlendMode (int source, int sourcePremultipledAlpha, int dest) {
		this.source = source;
		this.sourcePMA = sourcePremultipledAlpha;
		this.dest = dest;
	}

	public int getSource (boolean premultipliedAlpha) {
		return premultipliedAlpha ? sourcePMA : source;
	}

	public int getDest () {
		return dest;
	}

	static public final BlendMode[] values = values();
}
