package com.esotericsoftware.spine35;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import static com.esotericsoftware.spine35.utils.TrigUtils.*;

/**
 * Stores the current pose for a transform constraint. A transform constraint adjusts the world transform of the constrained
 * bones to match that of the target bone.
 * <p>
 * See <a href="http://esotericsoftware.com/spine-transform-constraints">Transform constraints</a> in the Spine User Guide.
 */
public class TransformConstraint implements Constraint {
    final TransformConstraintData data;
    final Array<Bone> bones;
    final Vector2 temp = new Vector2();
    Bone target;
    float rotateMix, translateMix, scaleMix, shearMix;

    public TransformConstraint(TransformConstraintData data, Skeleton skeleton) {
        if (data == null) throw new IllegalArgumentException("data cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        this.data = data;
        rotateMix = data.rotateMix;
        translateMix = data.translateMix;
        scaleMix = data.scaleMix;
        shearMix = data.shearMix;
        bones = new Array(data.bones.size);
        for (BoneData boneData : data.bones)
            bones.add(skeleton.findBone(boneData.name));
        target = skeleton.findBone(data.target.name);
    }

    /**
     * Copy constructor.
     */
    public TransformConstraint(TransformConstraint constraint, Skeleton skeleton) {
        if (constraint == null) throw new IllegalArgumentException("constraint cannot be null.");
        if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");
        data = constraint.data;
        bones = new Array(constraint.bones.size);
        for (Bone bone : constraint.bones)
            bones.add(skeleton.bones.get(bone.data.index));
        target = skeleton.bones.get(constraint.target.data.index);
        rotateMix = constraint.rotateMix;
        translateMix = constraint.translateMix;
        scaleMix = constraint.scaleMix;
        shearMix = constraint.shearMix;
    }

    /**
     * Applies the constraint to the constrained bones.
     */
    public void apply() {
        update();
    }

    public void update() {
        float rotateMix = this.rotateMix, translateMix = this.translateMix, scaleMix = this.scaleMix, shearMix = this.shearMix;
        Bone target = this.target;
        float ta = target.a, tb = target.b, tc = target.c, td = target.d;
        float degRadReflect = ta * td - tb * tc > 0 ? degRad : -degRad;
        float offsetRotation = data.offsetRotation * degRadReflect, offsetShearY = data.offsetShearY * degRadReflect;
        Array<Bone> bones = this.bones;
        for (int i = 0, n = bones.size; i < n; i++) {
            Bone bone = bones.get(i);
            boolean modified = false;

            if (rotateMix != 0) {
                float a = bone.a, b = bone.b, c = bone.c, d = bone.d;
                float r = atan2(tc, ta) - atan2(c, a) + offsetRotation;
                if (r > PI)
                    r -= PI2;
                else if (r < -PI) r += PI2;
                r *= rotateMix;
                float cos = cos(r), sin = sin(r);
                bone.a = cos * a - sin * c;
                bone.b = cos * b - sin * d;
                bone.c = sin * a + cos * c;
                bone.d = sin * b + cos * d;
                modified = true;
            }

            if (translateMix != 0) {
                Vector2 temp = this.temp;
                target.localToWorld(temp.set(data.offsetX, data.offsetY));
                bone.worldX += (temp.x - bone.worldX) * translateMix;
                bone.worldY += (temp.y - bone.worldY) * translateMix;
                modified = true;
            }

            if (scaleMix > 0) {
                float s = (float) Math.sqrt(bone.a * bone.a + bone.c * bone.c);
                float ts = (float) Math.sqrt(ta * ta + tc * tc);
                if (s > 0.00001f) s = (s + (ts - s + data.offsetScaleX) * scaleMix) / s;
                bone.a *= s;
                bone.c *= s;
                s = (float) Math.sqrt(bone.b * bone.b + bone.d * bone.d);
                ts = (float) Math.sqrt(tb * tb + td * td);
                if (s > 0.00001f) s = (s + (ts - s + data.offsetScaleY) * scaleMix) / s;
                bone.b *= s;
                bone.d *= s;
                modified = true;
            }

            if (shearMix > 0) {
                float b = bone.b, d = bone.d;
                float by = atan2(d, b);
                float r = atan2(td, tb) - atan2(tc, ta) - (by - atan2(bone.c, bone.a));
                if (r > PI)
                    r -= PI2;
                else if (r < -PI) r += PI2;
                r = by + (r + offsetShearY) * shearMix;
                float s = (float) Math.sqrt(b * b + d * d);
                bone.b = cos(r) * s;
                bone.d = sin(r) * s;
                modified = true;
            }

            if (modified) bone.appliedValid = false;
        }
    }

    public int getOrder() {
        return data.order;
    }

    /**
     * The bones that will be modified by this transform constraint.
     */
    public Array<Bone> getBones() {
        return bones;
    }

    /**
     * The target bone whose world transform will be copied to the constrained bones.
     */
    public Bone getTarget() {
        return target;
    }

    public void setTarget(Bone target) {
        this.target = target;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained rotations.
     */
    public float getRotateMix() {
        return rotateMix;
    }

    public void setRotateMix(float rotateMix) {
        this.rotateMix = rotateMix;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained translations.
     */
    public float getTranslateMix() {
        return translateMix;
    }

    public void setTranslateMix(float translateMix) {
        this.translateMix = translateMix;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained scales.
     */
    public float getScaleMix() {
        return scaleMix;
    }

    public void setScaleMix(float scaleMix) {
        this.scaleMix = scaleMix;
    }

    /**
     * A percentage (0-1) that controls the mix between the constrained and unconstrained scales.
     */
    public float getShearMix() {
        return shearMix;
    }

    public void setShearMix(float shearMix) {
        this.shearMix = shearMix;
    }

    /**
     * The transform constraint's setup pose data.
     */
    public TransformConstraintData getData() {
        return data;
    }

    public String toString() {
        return data.name;
    }
}