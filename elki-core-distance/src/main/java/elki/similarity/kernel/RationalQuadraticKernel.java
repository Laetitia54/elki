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
package elki.similarity.kernel;

import elki.data.NumberVector;
import elki.distance.AbstractNumberVectorDistance;
import elki.similarity.AbstractVectorSimilarity;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Rational quadratic kernel, a less computational approximation of the Gaussian
 * RBF kernel ({@link RadialBasisFunctionKernel}).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class RationalQuadraticKernel extends AbstractVectorSimilarity {
  /**
   * Constant term c.
   */
  private final double c;

  /**
   * Constructor.
   * 
   * @param c Constant term c.
   */
  public RationalQuadraticKernel(double c) {
    super();
    this.c = c;
  }

  @Override
  public double similarity(NumberVector o1, NumberVector o2) {
    final int dim = AbstractNumberVectorDistance.dimensionality(o1, o2);
    double sim = 0.;
    for(int i = 0; i < dim; i++) {
      final double v = o1.doubleValue(i) - o2.doubleValue(i);
      sim += v * v;
    }
    return 1. - sim / (sim + c);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * C parameter
     */
    public static final OptionID C_ID = new OptionID("kernel.rationalquadratic.c", "Constant term in the rational quadratic kernel.");

    /**
     * C parameter
     */
    protected double c = 1.;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter cP = new DoubleParameter(C_ID, 1.) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(cP)) {
        c = cP.doubleValue();
      }
    }

    @Override
    protected RationalQuadraticKernel makeInstance() {
      return new RationalQuadraticKernel(c);
    }
  }
}
