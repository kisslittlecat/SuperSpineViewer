package com.esotericsoftware.SpineStandard.utils;

import com.QYun.SuperSpineViewer.RuntimesLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.PolygonBatch;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.NumberUtils;

import static com.esotericsoftware.SpineStandard.utils.SpineUtils.arraycopy;

public class TwoColorPolygonBatch implements PolygonBatch {
    static final byte VERTEX_SIZE = 2 + 1 + 1 + 2;
    static final byte SPRITE_SIZE = 4 * VERTEX_SIZE;

    private final Mesh mesh;
    private final float[] vertices;
    private final short[] triangles;
    private final Matrix4 transformMatrix = new Matrix4();
    private final Matrix4 projectionMatrix = new Matrix4();
    private final Matrix4 combinedMatrix = new Matrix4();
    private final Color light = new Color(1, 1, 1, 1);
    private final Color dark = new Color(0, 0, 0, 1);
    private final ShaderProgram shader;
    public byte totalRenderCalls = 0;
    private boolean blendingDisabled;
    private int vertexIndex, triangleIndex;
    private Texture lastTexture;
    private boolean drawing;
    private int blendSrcFunc = GL20.GL_SRC_ALPHA;
    private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
    private int blendSrcFuncAlpha = GL20.GL_SRC_ALPHA;
    private int blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;
    private boolean premultipliedAlpha;
    private float lightPacked = Color.WHITE.toFloatBits();

    public TwoColorPolygonBatch(int size) {
        this(size, size * 2);
    }

    public TwoColorPolygonBatch(int maxVertices, int maxTriangles) {
        if (maxVertices > 32767)
            throw new IllegalArgumentException("Can't have more than 32767 vertices per batch: " + maxTriangles);

        VertexDataType vertexDataType = VertexDataType.VertexArray;
        if (Gdx.gl30 != null) vertexDataType = VertexDataType.VertexBufferObjectWithVAO;
        mesh = new Mesh(vertexDataType, false, maxVertices, maxTriangles * 3,
                new VertexAttribute(Usage.Position, 2, "a_position"),
                new VertexAttribute(Usage.ColorPacked, 4, "a_light"),
                new VertexAttribute(Usage.ColorPacked, 4, "a_dark"),
                new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));

        vertices = new float[maxVertices * 6];
        triangles = new short[maxTriangles * 3];
        shader = createDefaultShader();
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void begin() {
        if (drawing) throw new IllegalStateException("end must be called before begin.");
        Gdx.gl.glDepthMask(false);
        shader.bind();
        setupMatrices();
        drawing = true;
    }

    @Override
    public void end() {
        if (!drawing) throw new IllegalStateException("begin must be called before end.");
        if (vertexIndex > 0) flush();
        Gdx.gl.glDepthMask(true);
        if (isBlendingEnabled()) Gdx.gl.glDisable(GL20.GL_BLEND);
        lastTexture = null;
        drawing = false;
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        light.set(r, g, b, a);
        lightPacked = light.toFloatBits();
    }

    @Override
    public Color getColor() {
        return light;
    }

    @Override
    public void setColor(Color tint) {
        light.set(tint);
        lightPacked = tint.toFloatBits();
    }

    @Override
    public float getPackedColor() {
        return lightPacked;
    }

    @Override
    public void setPackedColor(float packedColor) {
        Color.rgba8888ToColor(light, NumberUtils.floatToIntColor(packedColor));
        lightPacked = packedColor;
    }

    public Color getDarkColor() {
        return dark;
    }

    public void setDarkColor(Color tint) {
        dark.set(tint);
        float darkPacked = tint.toFloatBits();
    }

    public void draw(PolygonRegion region, float x, float y) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final short[] regionTriangles = region.getTriangles();
        // final int regionTrianglesLength = regionTriangles.length;
        // final float[] regionVertices = region.getVertices();
        // final int regionVerticesLength = regionVertices.length;
        //
        // final Texture texture = region.getRegion().getTexture();
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + regionTrianglesLength > triangles.length
        //         || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.length) flush();
        //
        // int triangleIndex = this.triangleIndex;
        // int vertexIndex = this.vertexIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        //
        // for (short regionTriangle : regionTriangles)
        //     triangles[triangleIndex++] = (short) (regionTriangle + startVertex);
        // this.triangleIndex = triangleIndex;
        //
        // final float[] vertices = this.vertices;
        // final float light = this.lightPacked;
        // final float dark = this.darkPacked;
        // final float[] textureCoords = region.getTextureCoords();
        //
        // for (int i = 0; i < regionVerticesLength; i += 2) {
        //     vertices[vertexIndex++] = regionVertices[i] + x;
        //     vertices[vertexIndex++] = regionVertices[i + 1] + y;
        //     vertices[vertexIndex++] = light;
        //     vertices[vertexIndex++] = dark;
        //     vertices[vertexIndex++] = textureCoords[i];
        //     vertices[vertexIndex++] = textureCoords[i + 1];
        // }
        // this.vertexIndex = vertexIndex;
    }

    public void draw(PolygonRegion region, float x, float y, float width, float height) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final short[] regionTriangles = region.getTriangles();
        // final int regionTrianglesLength = regionTriangles.length;
        // final float[] regionVertices = region.getVertices();
        // final int regionVerticesLength = regionVertices.length;
        // final TextureRegion textureRegion = region.getRegion();
        //
        // final Texture texture = textureRegion.getTexture();
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + regionTrianglesLength > triangles.length
        //         || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.length) flush();
        //
        // int triangleIndex = this.triangleIndex;
        // int vertexIndex = this.vertexIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        //
        // for (short regionTriangle : regionTriangles)
        //     triangles[triangleIndex++] = (short) (regionTriangle + startVertex);
        // this.triangleIndex = triangleIndex;
        //
        // final float[] vertices = this.vertices;
        // final float light = this.lightPacked;
        // final float dark = this.darkPacked;
        // final float[] textureCoords = region.getTextureCoords();
        // final float sX = width / textureRegion.getRegionWidth();
        // final float sY = height / textureRegion.getRegionHeight();
        //
        // for (int i = 0; i < regionVerticesLength; i += 2) {
        //     vertices[vertexIndex++] = regionVertices[i] * sX + x;
        //     vertices[vertexIndex++] = regionVertices[i + 1] * sY + y;
        //     vertices[vertexIndex++] = light;
        //     vertices[vertexIndex++] = dark;
        //     vertices[vertexIndex++] = textureCoords[i];
        //     vertices[vertexIndex++] = textureCoords[i + 1];
        // }
        // this.vertexIndex = vertexIndex;
    }

    public void draw(PolygonRegion region, float x, float y, float originX, float originY, float width, float height,
                     float scaleX, float scaleY, float rotation) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final short[] regionTriangles = region.getTriangles();
        // final int regionTrianglesLength = regionTriangles.length;
        // final float[] regionVertices = region.getVertices();
        // final int regionVerticesLength = regionVertices.length;
        // final TextureRegion textureRegion = region.getRegion();
        //
        // Texture texture = textureRegion.getTexture();
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + regionTrianglesLength > triangles.length
        //         || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.length) flush();
        //
        // int triangleIndex = this.triangleIndex;
        // int vertexIndex = this.vertexIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        //
        // for (short regionTriangle : regionTriangles)
        //     triangles[triangleIndex++] = (short) (regionTriangle + startVertex);
        // this.triangleIndex = triangleIndex;
        //
        // final float[] vertices = this.vertices;
        // final float light = this.lightPacked;
        // final float dark = this.darkPacked;
        // final float[] textureCoords = region.getTextureCoords();
        //
        // final float worldOriginX = x + originX;
        // final float worldOriginY = y + originY;
        // final float sX = width / textureRegion.getRegionWidth();
        // final float sY = height / textureRegion.getRegionHeight();
        // final float cos = MathUtils.cosDeg(rotation);
        // final float sin = MathUtils.sinDeg(rotation);
        //
        // float fx, fy;
        // for (int i = 0; i < regionVerticesLength; i += 2) {
        //     fx = (regionVertices[i] * sX - originX) * scaleX;
        //     fy = (regionVertices[i + 1] * sY - originY) * scaleY;
        //     vertices[vertexIndex++] = cos * fx - sin * fy + worldOriginX;
        //     vertices[vertexIndex++] = sin * fx + cos * fy + worldOriginY;
        //     vertices[vertexIndex++] = light;
        //     vertices[vertexIndex++] = dark;
        //     vertices[vertexIndex++] = textureCoords[i];
        //     vertices[vertexIndex++] = textureCoords[i + 1];
        // }
        // this.vertexIndex = vertexIndex;
    }

    @Override
    public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX,
                     float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
        //     flush();
        //
        // int triangleIndex = this.triangleIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        // triangles[triangleIndex++] = (short) startVertex;
        // triangles[triangleIndex++] = (short) (startVertex + 1);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 3);
        // triangles[triangleIndex++] = (short) startVertex;
        // this.triangleIndex = triangleIndex;
        //
        // final float worldOriginX = x + originX;
        // final float worldOriginY = y + originY;
        // float fx = -originX;
        // float fy = -originY;
        // float fx2 = width - originX;
        // float fy2 = height - originY;
        //
        // if (scaleX != 1 || scaleY != 1) {
        //     fx *= scaleX;
        //     fy *= scaleY;
        //     fx2 *= scaleX;
        //     fy2 *= scaleY;
        // }
        //
        // final float p1x = fx;
        // final float p1y = fy;
        // final float p2x = fx;
        // final float p2y = fy2;
        // final float p3x = fx2;
        // final float p3y = fy2;
        // final float p4x = fx2;
        // final float p4y = fy;
        //
        // float x1;
        // float y1;
        // float x2;
        // float y2;
        // float x3;
        // float y3;
        // float x4;
        // float y4;
        //
        // if (rotation != 0) {
        //     final float cos = MathUtils.cosDeg(rotation);
        //     final float sin = MathUtils.sinDeg(rotation);
        //
        //     x1 = cos * p1x - sin * p1y;
        //     y1 = sin * p1x + cos * p1y;
        //
        //     x2 = cos * p2x - sin * p2y;
        //     y2 = sin * p2x + cos * p2y;
        //
        //     x3 = cos * p3x - sin * p3y;
        //     y3 = sin * p3x + cos * p3y;
        //
        //     x4 = x1 + (x3 - x2);
        //     y4 = y3 - (y2 - y1);
        // } else {
        //     x1 = p1x;
        //     y1 = p1y;
        //
        //     x2 = p2x;
        //     y2 = p2y;
        //
        //     x3 = p3x;
        //     y3 = p3y;
        //
        //     x4 = p4x;
        //     y4 = p4y;
        // }
        //
        // x1 += worldOriginX;
        // y1 += worldOriginY;
        // x2 += worldOriginX;
        // y2 += worldOriginY;
        // x3 += worldOriginX;
        // y3 += worldOriginY;
        // x4 += worldOriginX;
        // y4 += worldOriginY;
        //
        // float u = srcX * invTexWidth;
        // float v = (srcY + srcHeight) * invTexHeight;
        // float u2 = (srcX + srcWidth) * invTexWidth;
        // float v2 = srcY * invTexHeight;
        //
        // if (flipX) {
        //     float tmp = u;
        //     u = u2;
        //     u2 = tmp;
        // }
        //
        // if (flipY) {
        //     float tmp = v;
        //     v = v2;
        //     v2 = tmp;
        // }
        //
        // float light = this.lightPacked;
        // float dark = this.darkPacked;
        // int idx = this.vertexIndex;
        // vertices[idx++] = x1;
        // vertices[idx++] = y1;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v;
        //
        // vertices[idx++] = x2;
        // vertices[idx++] = y2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = x3;
        // vertices[idx++] = y3;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = x4;
        // vertices[idx++] = y4;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v;
        // this.vertexIndex = idx;
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth,
                     int srcHeight, boolean flipX, boolean flipY) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
        //     flush();
        //
        // int triangleIndex = this.triangleIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        // triangles[triangleIndex++] = (short) startVertex;
        // triangles[triangleIndex++] = (short) (startVertex + 1);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 3);
        // triangles[triangleIndex++] = (short) startVertex;
        // this.triangleIndex = triangleIndex;
        //
        // float u = srcX * invTexWidth;
        // float v = (srcY + srcHeight) * invTexHeight;
        // float u2 = (srcX + srcWidth) * invTexWidth;
        // float v2 = srcY * invTexHeight;
        // final float fx2 = x + width;
        // final float fy2 = y + height;
        //
        // if (flipX) {
        //     float tmp = u;
        //     u = u2;
        //     u2 = tmp;
        // }
        //
        // if (flipY) {
        //     float tmp = v;
        //     v = v2;
        //     v2 = tmp;
        // }
        //
        // float light = this.lightPacked;
        // float dark = this.darkPacked;
        // int idx = this.vertexIndex;
        // vertices[idx++] = x;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v;
        //
        // vertices[idx++] = x;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v;
        // this.vertexIndex = idx;
    }

    @Override
    public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
        //     flush();
        //
        // int triangleIndex = this.triangleIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        // triangles[triangleIndex++] = (short) startVertex;
        // triangles[triangleIndex++] = (short) (startVertex + 1);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 3);
        // triangles[triangleIndex++] = (short) startVertex;
        // this.triangleIndex = triangleIndex;
        //
        // final float u = srcX * invTexWidth;
        // final float v = (srcY + srcHeight) * invTexHeight;
        // final float u2 = (srcX + srcWidth) * invTexWidth;
        // final float v2 = srcY * invTexHeight;
        // final float fx2 = x + srcWidth;
        // final float fy2 = y + srcHeight;
        //
        // float light = this.lightPacked;
        // float dark = this.darkPacked;
        // int idx = this.vertexIndex;
        // vertices[idx++] = x;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v;
        //
        // vertices[idx++] = x;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v;
        // this.vertexIndex = idx;
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
        //     flush();
        //
        // int triangleIndex = this.triangleIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        // triangles[triangleIndex++] = (short) startVertex;
        // triangles[triangleIndex++] = (short) (startVertex + 1);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 3);
        // triangles[triangleIndex++] = (short) startVertex;
        // this.triangleIndex = triangleIndex;
        //
        // final float fx2 = x + width;
        // final float fy2 = y + height;
        //
        // float light = this.lightPacked;
        // float dark = this.darkPacked;
        // int idx = this.vertexIndex;
        // vertices[idx++] = x;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v;
        //
        // vertices[idx++] = x;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v;
        // this.vertexIndex = idx;
    }

    @Override
    public void draw(Texture texture, float x, float y) {
        // draw(texture, x, y, texture.getWidth(), texture.getHeight());
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
        //     flush();
        //
        // int triangleIndex = this.triangleIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        // triangles[triangleIndex++] = (short) startVertex;
        // triangles[triangleIndex++] = (short) (startVertex + 1);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 3);
        // triangles[triangleIndex++] = (short) startVertex;
        // this.triangleIndex = triangleIndex;
        //
        // final float fx2 = x + width;
        // final float fy2 = y + height;
        // final float u = 0;
        // final float v = 1;
        // final float u2 = 1;
        // final float v2 = 0;
        //
        // float light = this.lightPacked;
        // float dark = this.darkPacked;
        // int idx = this.vertexIndex;
        // vertices[idx++] = x;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v;
        //
        // vertices[idx++] = x;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v;
        // this.vertexIndex = idx;
    }

    public void drawTwoColor(Texture texture, float[] polygonVertices, int verticesOffset, int verticesCount,
                             short[] polygonTriangles, int trianglesOffset, int trianglesCount) {
        if (!drawing) throw new IllegalStateException("begin must be called before draw.");

        final short[] triangles = this.triangles;
        final float[] vertices = this.vertices;

        if (texture != lastTexture) {
            switch (RuntimesLoader.spineVersion) {
                case 38, 37 -> switchTexture(texture);
                case 36 -> {
                    flush();
                    lastTexture = texture;
                }
            }
        } else if (triangleIndex + trianglesCount > triangles.length || vertexIndex + verticesCount > vertices.length)
            flush();

        int triangleIndex = this.triangleIndex;
        final int vertexIndex = this.vertexIndex;
        final int startVertex = vertexIndex / 6;

        for (int i = trianglesOffset, n = i + trianglesCount; i < n; i++)
            triangles[triangleIndex++] = (short) (polygonTriangles[i] + startVertex);
        this.triangleIndex = triangleIndex;

        arraycopy(polygonVertices, verticesOffset, vertices, vertexIndex, verticesCount);
        this.vertexIndex += verticesCount;
    }

    public void draw(Texture texture, float[] polygonVertices, int verticesOffset, int verticesCount, short[] polygonTriangles,
                     int trianglesOffset, int trianglesCount) {
        if (!drawing) throw new IllegalStateException("begin must be called before draw.");

        final short[] triangles = this.triangles;
        final float[] vertices = this.vertices;

        if (texture != lastTexture) {
            switchTexture(texture);
        } else if (triangleIndex + trianglesCount > triangles.length || vertexIndex + verticesCount / 5 * 6 > vertices.length)
            flush();

        int triangleIndex = this.triangleIndex;
        final int vertexIndex = this.vertexIndex;
        final int startVertex = vertexIndex / 6;

        for (int i = trianglesOffset, n = i + trianglesCount; i < n; i++)
            triangles[triangleIndex++] = (short) (polygonTriangles[i] + startVertex);
        this.triangleIndex = triangleIndex;

        int idx = this.vertexIndex;
        for (int i = verticesOffset, n = verticesOffset + verticesCount; i < n; i += 5) {
            vertices[idx++] = polygonVertices[i];
            vertices[idx++] = polygonVertices[i + 1];
            vertices[idx++] = polygonVertices[i + 2];
            vertices[idx++] = 0;
            vertices[idx++] = polygonVertices[i + 3];
            vertices[idx++] = polygonVertices[i + 4];
        }
        this.vertexIndex = idx;
    }

    @Override
    public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // final int triangleCount = count / 20 * 6;
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + triangleCount > triangles.length || vertexIndex + count / 5 * 6 > vertices.length)
        //     flush();
        //
        // final int vertexIndex = this.vertexIndex;
        // int triangleIndex = this.triangleIndex;
        // short vertex = (short) (vertexIndex / VERTEX_SIZE);
        // for (int n = triangleIndex + triangleCount; triangleIndex < n; triangleIndex += 6, vertex += (short) 4) {
        //     triangles[triangleIndex] = vertex;
        //     triangles[triangleIndex + 1] = (short) (vertex + 1);
        //     triangles[triangleIndex + 2] = (short) (vertex + 2);
        //     triangles[triangleIndex + 3] = (short) (vertex + 2);
        //     triangles[triangleIndex + 4] = (short) (vertex + 3);
        //     triangles[triangleIndex + 5] = vertex;
        // }
        // this.triangleIndex = triangleIndex;
        //
        // int idx = this.vertexIndex;
        // for (int i = offset, n = offset + count; i < n; i += 5) {
        //     vertices[idx++] = spriteVertices[i];
        //     vertices[idx++] = spriteVertices[i + 1];
        //     vertices[idx++] = spriteVertices[i + 2];
        //     vertices[idx++] = 0;
        //     vertices[idx++] = spriteVertices[i + 3];
        //     vertices[idx++] = spriteVertices[i + 4];
        // }
        // this.vertexIndex = idx;
    }

    @Override
    public void draw(TextureRegion region, float x, float y) {
        // draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float width, float height) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // Texture texture = region.getTexture();
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
        //     flush();
        //
        // int triangleIndex = this.triangleIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        // triangles[triangleIndex++] = (short) startVertex;
        // triangles[triangleIndex++] = (short) (startVertex + 1);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 3);
        // triangles[triangleIndex++] = (short) startVertex;
        // this.triangleIndex = triangleIndex;
        //
        // final float fx2 = x + width;
        // final float fy2 = y + height;
        // final float u = region.getU();
        // final float v = region.getV2();
        // final float u2 = region.getU2();
        // final float v2 = region.getV();
        //
        // float light = this.lightPacked;
        // float dark = this.darkPacked;
        // int idx = this.vertexIndex;
        // vertices[idx++] = x;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v;
        //
        // vertices[idx++] = x;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = fy2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = fx2;
        // vertices[idx++] = y;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v;
        // this.vertexIndex = idx;
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                     float scaleX, float scaleY, float rotation) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // Texture texture = region.getTexture();
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
        //     flush();
        //
        // int triangleIndex = this.triangleIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        // triangles[triangleIndex++] = (short) startVertex;
        // triangles[triangleIndex++] = (short) (startVertex + 1);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 3);
        // triangles[triangleIndex++] = (short) startVertex;
        // this.triangleIndex = triangleIndex;
        //
        // final float worldOriginX = x + originX;
        // final float worldOriginY = y + originY;
        // float fx = -originX;
        // float fy = -originY;
        // float fx2 = width - originX;
        // float fy2 = height - originY;
        //
        //
        // if (scaleX != 1 || scaleY != 1) {
        //     fx *= scaleX;
        //     fy *= scaleY;
        //     fx2 *= scaleX;
        //     fy2 *= scaleY;
        // }
        //
        // final float p1x = fx;
        // final float p1y = fy;
        // final float p2x = fx;
        // final float p2y = fy2;
        // final float p3x = fx2;
        // final float p3y = fy2;
        // final float p4x = fx2;
        // final float p4y = fy;
        //
        // float x1;
        // float y1;
        // float x2;
        // float y2;
        // float x3;
        // float y3;
        // float x4;
        // float y4;
        //
        // if (rotation != 0) {
        //     final float cos = MathUtils.cosDeg(rotation);
        //     final float sin = MathUtils.sinDeg(rotation);
        //
        //     x1 = cos * p1x - sin * p1y;
        //     y1 = sin * p1x + cos * p1y;
        //
        //     x2 = cos * p2x - sin * p2y;
        //     y2 = sin * p2x + cos * p2y;
        //
        //     x3 = cos * p3x - sin * p3y;
        //     y3 = sin * p3x + cos * p3y;
        //
        //     x4 = x1 + (x3 - x2);
        //     y4 = y3 - (y2 - y1);
        // } else {
        //     x1 = p1x;
        //     y1 = p1y;
        //
        //     x2 = p2x;
        //     y2 = p2y;
        //
        //     x3 = p3x;
        //     y3 = p3y;
        //
        //     x4 = p4x;
        //     y4 = p4y;
        // }
        //
        // x1 += worldOriginX;
        // y1 += worldOriginY;
        // x2 += worldOriginX;
        // y2 += worldOriginY;
        // x3 += worldOriginX;
        // y3 += worldOriginY;
        // x4 += worldOriginX;
        // y4 += worldOriginY;
        //
        // final float u = region.getU();
        // final float v = region.getV2();
        // final float u2 = region.getU2();
        // final float v2 = region.getV();
        //
        // float light = this.lightPacked;
        // float dark = this.darkPacked;
        // int idx = this.vertexIndex;
        // vertices[idx++] = x1;
        // vertices[idx++] = y1;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v;
        //
        // vertices[idx++] = x2;
        // vertices[idx++] = y2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = x3;
        // vertices[idx++] = y3;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = x4;
        // vertices[idx++] = y4;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v;
        // this.vertexIndex = idx;
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                     float scaleX, float scaleY, float rotation, boolean clockwise) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // Texture texture = region.getTexture();
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
        //     flush();
        //
        // int triangleIndex = this.triangleIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        // triangles[triangleIndex++] = (short) startVertex;
        // triangles[triangleIndex++] = (short) (startVertex + 1);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 3);
        // triangles[triangleIndex++] = (short) startVertex;
        // this.triangleIndex = triangleIndex;
        //
        // final float worldOriginX = x + originX;
        // final float worldOriginY = y + originY;
        // float fx = -originX;
        // float fy = -originY;
        // float fx2 = width - originX;
        // float fy2 = height - originY;
        //
        // if (scaleX != 1 || scaleY != 1) {
        //     fx *= scaleX;
        //     fy *= scaleY;
        //     fx2 *= scaleX;
        //     fy2 *= scaleY;
        // }
        //
        // final float p1x = fx;
        // final float p1y = fy;
        // final float p2x = fx;
        // final float p2y = fy2;
        // final float p3x = fx2;
        // final float p3y = fy2;
        // final float p4x = fx2;
        // final float p4y = fy;
        //
        // float x1;
        // float y1;
        // float x2;
        // float y2;
        // float x3;
        // float y3;
        // float x4;
        // float y4;
        //
        // if (rotation != 0) {
        //     final float cos = MathUtils.cosDeg(rotation);
        //     final float sin = MathUtils.sinDeg(rotation);
        //
        //     x1 = cos * p1x - sin * p1y;
        //     y1 = sin * p1x + cos * p1y;
        //
        //     x2 = cos * p2x - sin * p2y;
        //     y2 = sin * p2x + cos * p2y;
        //
        //     x3 = cos * p3x - sin * p3y;
        //     y3 = sin * p3x + cos * p3y;
        //
        //     x4 = x1 + (x3 - x2);
        //     y4 = y3 - (y2 - y1);
        // } else {
        //     x1 = p1x;
        //     y1 = p1y;
        //
        //     x2 = p2x;
        //     y2 = p2y;
        //
        //     x3 = p3x;
        //     y3 = p3y;
        //
        //     x4 = p4x;
        //     y4 = p4y;
        // }
        //
        // x1 += worldOriginX;
        // y1 += worldOriginY;
        // x2 += worldOriginX;
        // y2 += worldOriginY;
        // x3 += worldOriginX;
        // y3 += worldOriginY;
        // x4 += worldOriginX;
        // y4 += worldOriginY;
        //
        // float u1, v1, u2, v2, u3, v3, u4, v4;
        // if (clockwise) {
        //     u1 = region.getU2();
        //     v1 = region.getV2();
        //     u2 = region.getU();
        //     v2 = region.getV2();
        //     u3 = region.getU();
        //     v3 = region.getV();
        //     u4 = region.getU2();
        //     v4 = region.getV();
        // } else {
        //     u1 = region.getU();
        //     v1 = region.getV();
        //     u2 = region.getU2();
        //     v2 = region.getV();
        //     u3 = region.getU2();
        //     v3 = region.getV2();
        //     u4 = region.getU();
        //     v4 = region.getV2();
        // }
        //
        // float light = this.lightPacked;
        // float dark = this.darkPacked;
        // int idx = this.vertexIndex;
        // vertices[idx++] = x1;
        // vertices[idx++] = y1;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u1;
        // vertices[idx++] = v1;
        //
        // vertices[idx++] = x2;
        // vertices[idx++] = y2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = x3;
        // vertices[idx++] = y3;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u3;
        // vertices[idx++] = v3;
        //
        // vertices[idx++] = x4;
        // vertices[idx++] = y4;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u4;
        // vertices[idx++] = v4;
        // this.vertexIndex = idx;
    }

    @Override
    public void draw(TextureRegion region, float width, float height, Affine2 transform) {
        // if (!drawing) throw new IllegalStateException("begin must be called before draw.");
        //
        // final short[] triangles = this.triangles;
        // final float[] vertices = this.vertices;
        //
        // Texture texture = region.getTexture();
        // if (texture != lastTexture)
        //     switchTexture(texture);
        // else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length)
        //     flush();
        //
        // int triangleIndex = this.triangleIndex;
        // final int startVertex = vertexIndex / VERTEX_SIZE;
        // triangles[triangleIndex++] = (short) startVertex;
        // triangles[triangleIndex++] = (short) (startVertex + 1);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 2);
        // triangles[triangleIndex++] = (short) (startVertex + 3);
        // triangles[triangleIndex++] = (short) startVertex;
        // this.triangleIndex = triangleIndex;
        //
        // float x1 = transform.m02;
        // float y1 = transform.m12;
        // float x2 = transform.m01 * height + transform.m02;
        // float y2 = transform.m11 * height + transform.m12;
        // float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
        // float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
        // float x4 = transform.m00 * width + transform.m02;
        // float y4 = transform.m10 * width + transform.m12;
        //
        // final float u = region.getU();
        // final float v = region.getV2();
        // final float u2 = region.getU2();
        // final float v2 = region.getV();
        //
        // float light = this.lightPacked;
        // float dark = this.darkPacked;
        // int idx = vertexIndex;
        // vertices[idx++] = x1;
        // vertices[idx++] = y1;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v;
        //
        // vertices[idx++] = x2;
        // vertices[idx++] = y2;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = x3;
        // vertices[idx++] = y3;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v2;
        //
        // vertices[idx++] = x4;
        // vertices[idx++] = y4;
        // vertices[idx++] = light;
        // vertices[idx++] = dark;
        // vertices[idx++] = u2;
        // vertices[idx++] = v;
        // vertexIndex = idx;
    }

    @Override
    public void flush() {
        if (vertexIndex == 0) return;
        if (RuntimesLoader.spineVersion > 36)
            totalRenderCalls++;
        lastTexture.bind();
        Mesh mesh = this.mesh;
        mesh.setVertices(vertices, 0, vertexIndex);
        mesh.setIndices(triangles, 0, triangleIndex);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        if (blendSrcFunc != -1)
            Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
        mesh.render(shader, GL20.GL_TRIANGLES, 0, triangleIndex);

        vertexIndex = 0;
        triangleIndex = 0;
    }

    @Override
    public void disableBlending() {
        flush();
        blendingDisabled = true;
    }

    @Override
    public void enableBlending() {
        flush();
        blendingDisabled = false;
    }

    @Override
    public void dispose() {
        mesh.dispose();
        shader.dispose();
    }

    @Override
    public Matrix4 getProjectionMatrix() {
        return projectionMatrix;
    }

    @Override
    public void setProjectionMatrix(Matrix4 projection) {
        // if (drawing) flush();
        // projectionMatrix.set(projection);
        // if (drawing) setupMatrices();
    }

    @Override
    public Matrix4 getTransformMatrix() {
        return transformMatrix;
    }

    @Override
    public void setTransformMatrix(Matrix4 transform) {
        // if (drawing) flush();
        // transformMatrix.set(transform);
        // if (drawing) setupMatrices();
    }

    public void setPremultipliedAlpha(boolean premultipliedAlpha) {
        if (this.premultipliedAlpha == premultipliedAlpha) return;
        if (drawing) flush();
        this.premultipliedAlpha = premultipliedAlpha;
        if (drawing) setupMatrices();
    }

    private void setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        shader.setUniformf("u_pma", premultipliedAlpha ? 1 : 0);
        shader.setUniformMatrix("u_projTrans", combinedMatrix);
        shader.setUniformi("u_texture", 0);
    }

    private void switchTexture(Texture texture) {
        flush();
        lastTexture = texture;
        float invTexWidth = 1.0f / texture.getWidth();
        float invTexHeight = 1.0f / texture.getHeight();
    }

    @Override
    public ShaderProgram getShader() {
        return shader;
    }

    @Override
    public void setShader(ShaderProgram newShader) {
        // if (shader == newShader) return;
        // if (drawing)
        //     flush();
        //
        // shader = newShader == null ? defaultShader : newShader;
        // if (drawing) {
        //     shader.bind();
        //     setupMatrices();
        // }
    }

    @Override
    public boolean isBlendingEnabled() {
        return !blendingDisabled;
    }

    @Override
    public boolean isDrawing() {
        return drawing;
    }

    @Override
    public void setBlendFunction(int srcFunc, int dstFunc) {
        setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
    }

    @Override
    public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
        if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha
                && blendDstFuncAlpha == dstFuncAlpha) return;
        flush();
        blendSrcFunc = srcFuncColor;
        blendDstFunc = dstFuncColor;
        blendSrcFuncAlpha = srcFuncAlpha;
        blendDstFuncAlpha = dstFuncAlpha;
    }

    @Override
    public int getBlendSrcFunc() {
        return blendSrcFunc;
    }

    @Override
    public int getBlendDstFunc() {
        return blendDstFunc;
    }

    @Override
    public int getBlendSrcFuncAlpha() {
        return blendSrcFuncAlpha;
    }

    @Override
    public int getBlendDstFuncAlpha() {
        return blendDstFuncAlpha;
    }

    private ShaderProgram createDefaultShader() {
        String vertexShader = """
                attribute vec4 a_position;
                attribute vec4 a_light;
                attribute vec4 a_dark;
                attribute vec2 a_texCoord0;
                uniform mat4 u_projTrans;
                varying vec4 v_light;
                varying vec4 v_dark;
                varying vec2 v_texCoords;

                void main()
                {
                  v_light = a_light;
                  v_light.a = v_light.a * (255.0/254.0);
                  v_dark = a_dark;
                  v_texCoords = a_texCoord0;
                  gl_Position = u_projTrans * a_position;
                }
                """;

        String fragmentShader = """
                #ifdef GL_ES
                #define LOWP lowp
                precision mediump float;
                #else
                #define LOWP\s
                #endif
                varying LOWP vec4 v_light;
                varying LOWP vec4 v_dark;
                uniform float u_pma;
                varying vec2 v_texCoords;
                uniform sampler2D u_texture;
                void main()
                {
                  vec4 texColor = texture2D(u_texture, v_texCoords);
                  gl_FragColor.a = texColor.a * v_light.a;
                  gl_FragColor.rgb = ((texColor.a - 1.0) * u_pma + 1.0 - texColor.rgb) * v_dark.rgb + texColor.rgb * v_light.rgb;
                }""";

        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }
}
