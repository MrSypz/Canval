package com.sypztep.canval.util.math;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

public class MatrixStack {
    private final Deque<Entry> stack = new ArrayDeque<>();

    public MatrixStack() {
        Matrix4f positionMatrix = new Matrix4f();
        Matrix3f normalMatrix = new Matrix3f();
        stack.addLast(new Entry(positionMatrix, normalMatrix));
    }

    /**
     * Applies the translation transformation to the top entry.
     */
    public void translate(double x, double y, double z) {
        translate((float) x, (float) y, (float) z);
    }

    public void translate(float x, float y, float z) {
        Entry entry = stack.getLast();
        entry.positionMatrix.translate(x, y, z);
    }
    //Don't increase scale on text increase size of font instead it make font blur!
    /**
     * Applies the scale transformation to the top entry.
     */
    public void scale(float x, float y, float z) {
        Entry entry = stack.getLast();
        entry.positionMatrix.scale(x, y, z);

        if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
            // Uniform scaling
            if (x < 0.0f || y < 0.0f || z < 0.0f) {
                entry.normalMatrix.scale(Math.signum(x), Math.signum(y), Math.signum(z));
            }
        } else {
            // Non-uniform scaling
            entry.normalMatrix.scale(1.0f / x, 1.0f / y, 1.0f / z);
            entry.canSkipNormalization = false;
        }
    }

    /**
     * Applies the rotation transformation to the top entry.
     */
    public void multiply(Quaternionf quaternion) {
        Entry entry = stack.getLast();
        entry.positionMatrix.rotate(quaternion);
        entry.normalMatrix.rotate(quaternion);
    }

    public void multiply(Quaternionf quaternion, float originX, float originY, float originZ) {
        Entry entry = stack.getLast();
        entry.positionMatrix.rotateAround(quaternion, originX, originY, originZ);
        entry.normalMatrix.rotate(quaternion);
    }

    /**
     * Rotates around X axis (pitch)
     */
    public void rotateX(float angle) {
        Entry entry = stack.getLast();
        entry.positionMatrix.rotateX(angle);
        entry.normalMatrix.rotateX(angle);
    }

    /**
     * Rotates around Y axis (yaw)
     */
    public void rotateY(float angle) {
        Entry entry = stack.getLast();
        entry.positionMatrix.rotateY(angle);
        entry.normalMatrix.rotateY(angle);
    }

    /**
     * Rotates around Z axis (roll)
     */
    public void rotateZ(float angle) {
        Entry entry = stack.getLast();
        entry.positionMatrix.rotateZ(angle);
        entry.normalMatrix.rotateZ(angle);
    }

    /**
     * Pushes a copy of the top entry onto this stack.
     */
    public void push() {
        stack.addLast(new Entry(stack.getLast()));
    }

    /**
     * Removes the entry at the top of this stack.
     */
    public void pop() {
        if (stack.size() <= 1) {
            throw new IllegalStateException("Cannot pop the root matrix");
        }
        stack.removeLast();
    }

    /**
     * Returns the entry at the top of this stack.
     */
    public Entry peek() {
        return stack.getLast();
    }

    /**
     * Returns whether this stack contains exactly one entry.
     */
    public boolean isEmpty() {
        return stack.size() == 1;
    }

    /**
     * Sets the top entry to be the identity matrix.
     */
    public void loadIdentity() {
        Entry entry = stack.getLast();
        entry.positionMatrix.identity();
        entry.normalMatrix.identity();
        entry.canSkipNormalization = true;
    }

    /**
     * Multiplies the top position matrix with the given matrix.
     */
    public void multiplyPositionMatrix(Matrix4f matrix) {
        Entry entry = stack.getLast();
        entry.positionMatrix.mul(matrix);

        // Check if we need to update the normal matrix
        if (!isTranslation(matrix)) {
            if (isOrthonormal(matrix)) {
                Matrix3f normalTransform = new Matrix3f(matrix);
                entry.normalMatrix.mul(normalTransform);
            } else {
                entry.computeNormal();
            }
        }
    }

    /**
     * Applies perspective projection
     */
    public void perspective(float fov, float aspect, float near, float far) {
        Entry entry = stack.getLast();
        Matrix4f perspective = new Matrix4f().perspective(fov, aspect, near, far);
        entry.positionMatrix.mul(perspective);
        entry.canSkipNormalization = false;
    }

    /**
     * Applies orthographic projection
     */
    public void ortho(float left, float right, float bottom, float top, float near, float far) {
        Entry entry = stack.getLast();
        Matrix4f ortho = new Matrix4f().ortho(left, right, bottom, top, near, far);
        entry.positionMatrix.mul(ortho);
        entry.canSkipNormalization = false;
    }

    /**
     * Creates a look-at transformation
     */
    public void lookAt(float eyeX, float eyeY, float eyeZ,
                       float centerX, float centerY, float centerZ,
                       float upX, float upY, float upZ) {
        Entry entry = stack.getLast();
        Matrix4f lookAt = new Matrix4f().lookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        entry.positionMatrix.mul(lookAt);
        entry.computeNormal();
    }

    private boolean isTranslation(Matrix4f matrix) {
        return matrix.m00() == 1 && matrix.m01() == 0 && matrix.m02() == 0 &&
                matrix.m10() == 0 && matrix.m11() == 1 && matrix.m12() == 0 &&
                matrix.m20() == 0 && matrix.m21() == 0 && matrix.m22() == 1;
    }

    private boolean isOrthonormal(Matrix4f matrix) {
        // Simplified check - in practice you might want a more robust implementation
        float det = matrix.determinant();
        return Math.abs(Math.abs(det) - 1.0f) < 1e-6f;
    }

    /**
     * Matrix stack entry containing position and normal matrices
     */
    public static final class Entry {
        final Matrix4f positionMatrix;
        final Matrix3f normalMatrix;
        boolean canSkipNormalization = true;

        Entry(Matrix4f positionMatrix, Matrix3f normalMatrix) {
            this.positionMatrix = positionMatrix;
            this.normalMatrix = normalMatrix;
        }

        Entry(Entry other) {
            this.positionMatrix = new Matrix4f(other.positionMatrix);
            this.normalMatrix = new Matrix3f(other.normalMatrix);
            this.canSkipNormalization = other.canSkipNormalization;
        }

        void computeNormal() {
            // Extract 3x3 part and compute inverse transpose for normal transformation
            this.normalMatrix.set(this.positionMatrix).invert().transpose();
            this.canSkipNormalization = false;
        }

        /**
         * Returns the matrix used to transform positions.
         */
        public Matrix4f getPositionMatrix() {
            return positionMatrix;
        }

        /**
         * Returns the matrix used to transform normal vectors.
         */
        public Matrix3f getNormalMatrix() {
            return normalMatrix;
        }

        /**
         * Transforms a normal vector using the normal matrix.
         */
        public Vector3f transformNormal(Vector3f vec, Vector3f dest) {
            return transformNormal(vec.x, vec.y, vec.z, dest);
        }

        public Vector3f transformNormal(float x, float y, float z, Vector3f dest) {
            normalMatrix.transform(x, y, z, dest);
            return canSkipNormalization ? dest : dest.normalize();
        }

        /**
         * Transforms a position vector using the position matrix.
         */
        public Vector3f transformPosition(Vector3f vec, Vector3f dest) {
            return transformPosition(vec.x, vec.y, vec.z, dest);
        }

        public Vector3f transformPosition(float x, float y, float z, Vector3f dest) {
            return positionMatrix.transformPosition(x, y, z, dest);
        }

        /**
         * Creates a copy of this entry.
         */
        public Entry copy() {
            return new Entry(this);
        }
    }
}
