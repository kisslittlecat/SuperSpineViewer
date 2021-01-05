package com.esotericsoftware.spine36;

import com.badlogic.gdx.graphics.Color;

/** Stores the setup pose for a {@link Bone}. */
public class BoneData {
	final int index;
	final String name;
	final BoneData parent;
	float length;
	float x, y, rotation, scaleX = 1, scaleY = 1, shearX, shearY;
	TransformMode transformMode = TransformMode.normal;

	// Nonessential.
	final Color color = new Color(0.61f, 0.61f, 0.61f, 1); // 9b9b9bff

	/** @param parent May be null. */
	public BoneData (int index, String name, BoneData parent) {
		if (index < 0) throw new IllegalArgumentException("index must be >= 0.");
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		this.index = index;
		this.name = name;
		this.parent = parent;
	}

	/** Copy constructor.
	 * @param parent May be null. */
	public BoneData (BoneData bone, BoneData parent) {
		if (bone == null) throw new IllegalArgumentException("bone cannot be null.");
		index = bone.index;
		name = bone.name;
		this.parent = parent;
		length = bone.length;
		x = bone.x;
		y = bone.y;
		rotation = bone.rotation;
		scaleX = bone.scaleX;
		scaleY = bone.scaleY;
		shearX = bone.shearX;
		shearY = bone.shearY;
	}

	/** The index of the bone in {@link Skeleton#getBones()}. */
	public int getIndex () {
		return index;
	}

	/** The name of the bone, which is unique within the skeleton. */
	public String getName () {
		return name;
	}

	/** @return May be null. */
	public BoneData getParent () {
		return parent;
	}

	/** The bone's length. */
	public float getLength () {
		return length;
	}

	public void setLength (float length) {
		this.length = length;
	}

	/** The local x translation. */
	public float getX () {
		return x;
	}

	public void setX (float x) {
		this.x = x;
	}

	/** The local y translation. */
	public float getY () {
		return y;
	}

	public void setY (float y) {
		this.y = y;
	}

	public void setPosition (float x, float y) {
		this.x = x;
		this.y = y;
	}

	/** The local rotation. */
	public float getRotation () {
		return rotation;
	}

	public void setRotation (float rotation) {
		this.rotation = rotation;
	}

	/** The local scaleX. */
	public float getScaleX () {
		return scaleX;
	}

	public void setScaleX (float scaleX) {
		this.scaleX = scaleX;
	}

	/** The local scaleY. */
	public float getScaleY () {
		return scaleY;
	}

	public void setScaleY (float scaleY) {
		this.scaleY = scaleY;
	}

	public void setScale (float scaleX, float scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}

	/** The local shearX. */
	public float getShearX () {
		return shearX;
	}

	public void setShearX (float shearX) {
		this.shearX = shearX;
	}

	/** The local shearX. */
	public float getShearY () {
		return shearY;
	}

	public void setShearY (float shearY) {
		this.shearY = shearY;
	}

	/** The transform mode for how parent world transforms affect this bone. */
	public TransformMode getTransformMode () {
		return transformMode;
	}

	public void setTransformMode (TransformMode transformMode) {
		this.transformMode = transformMode;
	}

	/** The color of the bone as it was in Spine. Available only when nonessential data was exported. Bones are not usually
	 * rendered at runtime. */
	public Color getColor () {
		return color;
	}

	public String toString () {
		return name;
	}

	/** Determines how a bone inherits world transforms from parent bones. */
	public enum TransformMode {
		normal, onlyTranslation, noRotationOrReflection, noScale, noScaleOrReflection;

		static public final TransformMode[] values = TransformMode.values();
	}
}
