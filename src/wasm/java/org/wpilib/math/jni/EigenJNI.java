package org.wpilib.math.jni;

/**
 * Wasm stand-in for Eigen's matrix exponential. The motor simulation only exponentiates the 2×2
 * plant matrix, so this uses scaling and squaring plus a truncated series instead of {@code
 * wpimathjni}.
 */
public final class EigenJNI {
  private static final int SERIES_TERMS = 12;

  private EigenJNI() {}

  /**
   * Computes {@code exp(A)} for a square row-major matrix.
   *
   * @param src row-major source matrix
   * @param rows row count
   * @param dst row-major destination
   */
  public static void exp(double[] src, int rows, double[] dst) {
    double[] scaled = src.clone();
    int squarings = 0;
    double norm = infinityNorm(scaled, rows);
    while (norm > 0.5 && squarings < 20) {
      scale(scaled, 0.5);
      norm *= 0.5;
      squarings++;
    }

    double[] term = identity(rows);
    double[] sum = identity(rows);
    for (int k = 1; k <= SERIES_TERMS; k++) {
      term = multiply(term, scaled, rows);
      scale(term, 1.0 / k);
      add(sum, term);
    }
    for (int i = 0; i < squarings; i++) {
      sum = multiply(sum, sum, rows);
    }
    System.arraycopy(sum, 0, dst, 0, rows * rows);
  }

  /**
   * Computes {@code A^exponent}. The sim path does not call this; it remains so a later wpimath
   * call fails here instead of as an unsatisfied link.
   *
   * @param src row-major source matrix
   * @param rows row count
   * @param exponent matrix power
   * @param dst row-major destination
   */
  public static void pow(double[] src, int rows, double exponent, double[] dst) {
    if (exponent != 1.0) {
      throw new UnsupportedOperationException("Wasm EigenJNI.pow only supports exponent 1");
    }
    System.arraycopy(src, 0, dst, 0, rows * rows);
  }

  private static double infinityNorm(double[] matrix, int rows) {
    double norm = 0.0;
    for (int row = 0; row < rows; row++) {
      double sum = 0.0;
      for (int col = 0; col < rows; col++) {
        sum += Math.abs(matrix[row * rows + col]);
      }
      norm = Math.max(norm, sum);
    }
    return norm;
  }

  private static double[] identity(int rows) {
    double[] matrix = new double[rows * rows];
    for (int i = 0; i < rows; i++) {
      matrix[i * rows + i] = 1.0;
    }
    return matrix;
  }

  private static double[] multiply(double[] left, double[] right, int rows) {
    double[] product = new double[rows * rows];
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < rows; col++) {
        double sum = 0.0;
        for (int k = 0; k < rows; k++) {
          sum += left[row * rows + k] * right[k * rows + col];
        }
        product[row * rows + col] = sum;
      }
    }
    return product;
  }

  private static void scale(double[] matrix, double factor) {
    for (int i = 0; i < matrix.length; i++) {
      matrix[i] *= factor;
    }
  }

  private static void add(double[] sum, double[] term) {
    for (int i = 0; i < sum.length; i++) {
      sum[i] += term[i];
    }
  }
}
