/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.math.statistics.distribution.estimator;

import java.util.Arrays;

import elki.math.MeanVariance;
import elki.math.linearalgebra.fitting.GaussianFittingFunction;
import elki.math.linearalgebra.fitting.LevenbergMarquardtMethod;
import elki.math.statistics.KernelDensityEstimator;
import elki.math.statistics.distribution.LogNormalDistribution;
import elki.math.statistics.kernelfunctions.GaussianKernelDensityFunction;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Distribution parameter estimation using Levenberg-Marquardt iterative
 * optimization and a kernel density estimation.
 * <p>
 * Note: this estimator is rather expensive, and needs optimization in the KDE
 * phase, which currently is O(n²)!
 * <p>
 * This estimator is primarily attractive when only part of the distribution was
 * observed.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - LogNormalDistribution
 */
public class LogNormalLevenbergMarquardtKDEEstimator implements DistributionEstimator<LogNormalDistribution> {
  /**
   * Static estimator for small sample sizes and <em>partial</em> data.
   */
  public static final LogNormalLevenbergMarquardtKDEEstimator STATIC = new LogNormalLevenbergMarquardtKDEEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private LogNormalLevenbergMarquardtKDEEstimator() {
    super();
  }

  @Override
  public <A> LogNormalDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    // We first need the basic parameters:
    final int len = adapter.size(data);
    MeanVariance mv = new MeanVariance();
    // X positions of samples
    double[] x = new double[len];
    for(int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i);
      if(!(val > 0)) {
        throw new ArithmeticException("Cannot fit logNormal to a data set which includes non-positive values: " + val);
      }
      x[i] = FastMath.log(val);
      mv.put(x[i]);
    }
    // Sort our copy.
    Arrays.sort(x);
    double median = (x[len >> 1] + x[(len + 1) >> 1]) * .5;

    // Height = density, via KDE.
    KernelDensityEstimator de = new KernelDensityEstimator(x, GaussianKernelDensityFunction.KERNEL, 1e-6);
    double[] y = de.getDensity();

    // Weights:
    double[] s = new double[len];
    Arrays.fill(s, 1.0);

    // Initial parameter estimate:
    double[] params = { median, mv.getSampleStddev(), 1 };
    boolean[] dofit = { true, true, false };
    LevenbergMarquardtMethod fit = new LevenbergMarquardtMethod(GaussianFittingFunction.STATIC, params, dofit, x, y, s);
    fit.run();
    double[] ps = fit.getParams();
    return new LogNormalDistribution(ps[0], ps[1], 0.);
  }

  @Override
  public Class<? super LogNormalDistribution> getDistributionClass() {
    return LogNormalDistribution.class;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LogNormalLevenbergMarquardtKDEEstimator makeInstance() {
      return STATIC;
    }
  }
}
