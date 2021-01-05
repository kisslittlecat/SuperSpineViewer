package com.esotericsoftware.spine34.attachments;

import com.esotericsoftware.spine34.Skin;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

public class AtlasAttachmentLoader implements AttachmentLoader {
	private final TextureAtlas atlas;

	public AtlasAttachmentLoader (TextureAtlas atlas) {
		if (atlas == null) throw new IllegalArgumentException("atlas cannot be null.");
		this.atlas = atlas;
	}

	public RegionAttachment newRegionAttachment (Skin skin, String name, String path) {
		AtlasRegion region = atlas.findRegion(path);
		if (region == null) throw new RuntimeException("Region not found in atlas: " + path + " (region attachment: " + name + ")");
		RegionAttachment attachment = new RegionAttachment(name);
		attachment.setRegion(region);
		return attachment;
	}

	public MeshAttachment newMeshAttachment (Skin skin, String name, String path) {
		AtlasRegion region = atlas.findRegion(path);
		if (region == null) throw new RuntimeException("Region not found in atlas: " + path + " (mesh attachment: " + name + ")");
		MeshAttachment attachment = new MeshAttachment(name);
		attachment.setRegion(region);
		return attachment;
	}

	public BoundingBoxAttachment newBoundingBoxAttachment (Skin skin, String name) {
		return new BoundingBoxAttachment(name);
	}

	public PathAttachment newPathAttachment (Skin skin, String name) {
		return new PathAttachment(name);
	}
}
