package org.wpilib.math.jni;

/**
 * Wasm stand-in for Eigen's matrix exponential. Motor discretization exponentiates an augmented 3×3
 * state/input matrix, so this uses scaling and squaring plus a convergent Taylor series instead of
 * {@code wpimathjni}.
 */
public final class EigenJNI {
  private static final int SERIES_TERMS = 18;

  private EigenJNI() {}

  /**
   * Computes {@code exp(A)} for a square row-major matrix.
   *
   * @param src row-major source matrix
   * @param rows row count
   * @param dst row-major destination
   */
  public static void exp(double[] src, int rows, double[] dst) {
    int size = rows * rows;
    double[] scaled = src.clone();
    int squarings = 0;
    double norm = maxAbs(scaled, size);
    // rows * maxAbs bounds the infinity norm, without overflowing the row sums.
    double threshold = 0.5 / rows;
    while (norm > threshold && Double.isFinite(norm)) {
      scale(scaled, 0.5);
      norm *= 0.5;
      squarings++;
    }

    double[] term = new double[size];
    double[] product = new double[size];
    double[] sum = dst;
    for (int i = 0; i < size; i++) {
      sum[i] = 0.0;
    }
    for (int i = 0; i < rows; i++) {
      term[i * rows + i] = 1.0;
      sum[i * rows + i] = 1.0;
    }
    for (int k = 1; k <= SERIES_TERMS; k++) {
      multiply(term, scaled, rows, product);
      scale(product, 1.0 / k);
      double[] previousTerm = term;
      term = product;
      product = previousTerm;
      if (!add(sum, term, size)) {
        break;
      }
    }
    for (int i = 0; i < squarings; i++) {
      multiply(sum, sum, rows, product);
      double[] previousSum = sum;
      sum = product;
      product = previousSum;
    }
    if (sum != dst) {
      System.arraycopy(sum, 0, dst, 0, size);
    }
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

  private static double maxAbs(double[] matrix, int size) {
    double norm = 0.0;
    for (int i = 0; i < size; i++) {
      norm = Math.max(norm, Math.abs(matrix[i]));
    }
    return norm;
  }

  private static void multiply(double[] left, double[] right, int rows, double[] product) {
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < rows; col++) {
        double sum = 0.0;
        for (int k = 0; k < rows; k++) {
          sum += left[row * rows + k] * right[k * rows + col];
        }
        product[row * rows + col] = sum;
      }
    }
  }

  private static void scale(double[] matrix, double factor) {
    for (int i = 0; i < matrix.length; i++) {
      matrix[i] *= factor;
    }
  }

  private static boolean add(double[] sum, double[] term, int size) {
    boolean changed = false;
    for (int i = 0; i < size; i++) {
      double next = sum[i] + term[i];
      changed |= next != sum[i];
      sum[i] = next;
    }
    return changed;
  }
}
