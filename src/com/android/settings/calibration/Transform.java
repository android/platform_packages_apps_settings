/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.calibration;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;

import java.util.Map;

/**
 * Describes a transformation between a set of input coordinates and
 * a set of output coordinates. A Transform may be used to transform
 * points back and forth between two coordinate spaces.
 */
class Transform implements Parcelable {
    
    Matrix m;
    
    public static final Parcelable.Creator<Transform> CREATOR =
            new Parcelable.Creator<Transform>() {
        public Transform createFromParcel(Parcel in) {
            return new Transform(in);
        }
        
        public Transform[] newArray(int size) {
            return new Transform[size];
        }
    };
    
    private Transform(Parcel in) {
        float[] values = new float[9];
        in.readFloatArray(values);
        m = new Matrix();
        m.setValues(values);
    }
    
    public void writeToParcel(Parcel out, int flags) {
        float[] values = new float[9];
        m.getValues(values);
        out.writeFloatArray(values);
    }
    
    public int describeContents() {
        return 0;
    }
    
    /**
     * Constructs a new Transform which will (as nearly as possible)
     * satisfy the provided mapping from input to output points.
     * 
     * @param mapping
     */
    public Transform(Map<PointF,PointF> mapping) {
        double[][] A = new double[mapping.size()][3];
        double[][] b = new double[mapping.size()][3];
        int i = 0;
        
        for (Map.Entry<PointF,PointF> map : mapping.entrySet()) {
            A[i][0] = map.getValue().x;
            A[i][1] = map.getValue().y;
            A[i][2] = 1.0;
            
            b[i][0] = map.getKey().x;
            b[i][1] = map.getKey().y;
            b[i][2] = 1.0;
            
            i++;
        }
        
        double[][] x = MatrixOps.leastSquaresSolution(A, b);
        x = MatrixOps.transpose(x);
        
        m = MatrixOps.toMatrix(x);
    }
    
    /**
     * Constructs a new Transform which maps points on the screen
     * of the given context to points on the device of the given
     * MotionEvent.
     * 
     * @param event
     * @param ctx
     */
    public Transform(MotionEvent event, Context ctx) {
        int rotation = Calibrator.getRotation(ctx);
        float xscale = event.getXPrecision();
        float yscale = event.getYPrecision();
        
        m = new Matrix();
        
        switch (rotation)
        {
            case Surface.ROTATION_0:   /* no rotation needed */       break;
            case Surface.ROTATION_90:  m.postRotate( 90, 0.5f, 0.5f); break;
            case Surface.ROTATION_180: m.postRotate(180, 0.5f, 0.5f); break;
            case Surface.ROTATION_270: m.postRotate(270, 0.5f, 0.5f); break;
        }
        
        m.postScale(xscale, yscale);
    }
    
    /**
     * Constructs a new Transform which maps points on the given
     * View to points on the device of the given MotionEvent.
     * 
     * @param event
     * @param v
     */
    public Transform(MotionEvent event, View v) {
        this(event, v.getContext());
        int[] coords = new int[2];
        v.getLocationOnScreen(coords);
        m.preTranslate(coords[0], coords[1]);
    }
    
    public Transform(Matrix m) {
    	this.m = m;
    }
    
    public Transform() {
        m = new Matrix();
        m.reset();
    }
    
    public PointF transform(PointF in) {
        float[] pts = {in.x, in.y};
        m.mapPoints(pts);
        return new PointF(pts[0], pts[1]);
    }
    
    public Transform invert() throws NonInvertableException {
        Matrix inv = new Matrix();
        if (m.invert(inv))
            return new Transform(inv);
        else
            throw new NonInvertableException();
    }
    
    public Transform concat(Transform t) {
        Matrix c = new Matrix();
        c.set(m);
        c.postConcat(t.m);
        return new Transform(c);
    }
    
    public String toString() {
        return m.toShortString();
    }
}


class NonInvertableException extends Exception { }


class MatrixOps {
    
    public static double[][] leastSquaresSolution(double[][] A, double[][] b) {
        double[][] At = transpose(A);
        double[][] x = multiply(At, A);
        x = MatrixOps.invert(x);
        x = MatrixOps.multiply(x, At);
        x = MatrixOps.multiply(x, b);

        return x;
    }

    public static Matrix toMatrix(double[][] a) {
        if (a.length != 3 || a[0].length != 3)
            throw new IllegalArgumentException("Cannot convert a " + a.length + "x" + a[0].length + " matrix to Matrix");
        
        double[] md = toRowMajor(a);
        float[] mf = new float[md.length];
        for (int i = 0; i < md.length; i++) {
            mf[i] = (float)md[i];
        }
        
        Matrix m = new Matrix();
        m.setValues(mf);
        return m;
    }

    public static double[] toRowMajor(double[][] a) {
        int a_rows = a.length, a_cols = a[0].length;

        double[] output = new double[a_rows * a_cols];

        for (int i = 0; i < a_rows; i++)
            for (int j = 0; j < a_cols; j++)
                output[i*a_cols + j] = a[i][j];

        return output;
    }

    public static double[][] fromRowMajor(double[] a, int rows, int cols) {
        double [][] output = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int idx = i * cols + j;
                output[i][j] = idx < a.length ? a[idx] : 0;
            }
        }
        return output;
    }

    public static double[][] transpose(double[][] a) {
        int a_rows = a.length, a_cols = a[0].length;

        double[][] output = new double[a_cols][a_rows];
        
        for (int i = 0; i < a_cols; i++)
            for (int j = 0; j < a_rows; j++)
                output[i][j] = a[j][i];

        return output;
    }

    public static double[][] multiply(double n, double[][] a) {
        int a_rows = a.length, a_cols = a[0].length;

        for (int i = 0; i < a_rows; i++)
            for (int j = 0; j < a_cols; j++)
                a[i][j] *= n;

        return a;
    }

    public static double[][] multiply(double[][] a, double[][] b) {
        int a_rows = a.length, a_cols = a[0].length;
        int b_rows = b.length, b_cols = b[0].length;

        if (a_cols != b_rows)
            throw new IllegalArgumentException("Mismatched dimensions: " +
                 a_rows + "x" + a_cols + " and " + b_rows + "x" + b_cols
            );

        double[][] output = new double[a_rows][b_cols];

        for (int i = 0; i < a_rows; i++)
            for (int j = 0; j < b_cols; j++)
                for (int k = 0; k < a_cols; k++)
                    output[i][j] += a[i][k] * b[k][j];

        return output;
    }

    public static double determinant(double[][] a) {
        int a_rows = a.length, a_cols = a[0].length;

        if (a_rows != a_cols)
            throw new IllegalArgumentException("Non-square dimension: " +
                a_rows + "x" + a_cols
            );

        double result = 0.0;

        if (a_rows == 1) {
            result = a[0][0];
        }
        else if (a_rows == 2) {
            result = a[0][0] * a[1][1] - a[0][1] * a[1][0];
        }
        else {
            for (int i = 0; i < a_cols; i++) {
                result += a[i][0] * MatrixOps.cofactor(a, i, 0);
            }
        }

        return result;
    }

    public static double cofactor(double[][] a, int i, int j) {
        int a_rows = a.length, a_cols = a[0].length;

        if (a_rows != a_cols)
            throw new IllegalArgumentException("Non-square dimension: " +
                a_rows + "x" + a_cols
            );

        int a_size = a_rows;
        double temp[][] = new double[a_size - 1][a_size - 1];

        for (int x=0; x < i; x++) {
            System.arraycopy(a[x], 0,   temp[x], 0, j);
            System.arraycopy(a[x], j+1, temp[x], j, a_size - j - 1);
        }
        for (int x=i+1; x < a_size; x++) {
            System.arraycopy(a[x], 0,   temp[x-1], 0, j);
            System.arraycopy(a[x], j+1, temp[x-1], j, a_size - j - 1);
        }

        return Math.pow(-1.0, i + j) * MatrixOps.determinant(temp);
    }

    public static double[][] adjugate(double[][] a) {
        return MatrixOps.transpose(cofactor(a));
    }

    public static double[][] cofactor(double[][] a) {
        int a_rows = a.length, a_cols = a[0].length;
        int a_size = a_rows;

        if (a_rows != a_cols)
            throw new IllegalArgumentException("Non-square dimension: " +
                a_rows + "x" + a_cols
            );

        double[][] output = new double[a_size][a_size];

        for (int i = 0; i < a_size; i++)
            for (int j = 0; j < a_size; j++)
                output[i][j] = cofactor(a, i, j);

        return output;
    }

    public static double[][] invert(double[][] a) {
        return MatrixOps.multiply(1.0 / MatrixOps.determinant(a), MatrixOps.adjugate(a));
    }
    
    public static String toString(double[][] a) {
        int a_rows = a.length, a_cols = a[0].length;

        String output = "[";
        
        for (int i = 0; i < a_rows; i++) {
            output += "[ ";
            for (int j = 0; j < a_cols; j++) {
                output += a[i][j];
                if (j + 1 < a_cols)
                    output += ",";
                output += " ";
            }
            output += "]";
        }

        output += "]";
        return output;
    }

}
