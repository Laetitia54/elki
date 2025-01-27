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
package elki.algorithm.statistics;

import elki.AbstractDistanceBasedAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.math.statistics.intrinsicdimensionality.GEDEstimator;
import elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import elki.utilities.datastructures.QuickSelect;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

/**
 * Estimate global average intrinsic dimensionality of a data set.
 * <p>
 * Note: this algorithm does not produce a result, but only logs statistics.
 * FIXME: make this an application instead?
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Data type
 */
public class EstimateIntrinsicDimensionality<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, Void> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(EstimateIntrinsicDimensionality.class);

  /**
   * Number of neighbors to use.
   */
  protected double krate;

  /**
   * Number of samples to draw.
   */
  protected double samples;

  /**
   * Estimation method.
   */
  protected IntrinsicDimensionalityEstimator estimator;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param estimator Estimator
   * @param krate kNN rate
   * @param samples Sample size
   */
  public EstimateIntrinsicDimensionality(Distance<? super O> distanceFunction, IntrinsicDimensionalityEstimator estimator, double krate, double samples) {
    super(distanceFunction);
    this.estimator = estimator;
    this.krate = krate;
    this.samples = samples;
  }

  public Void run(Relation<O> relation) {
    DBIDs allids = relation.getDBIDs();
    // Number of samples to draw.
    int ssize = (int) ((samples > 1.) ? samples : Math.ceil(samples * allids.size()));
    // Number of neighbors to fetch (+ query point)
    int kk = 1 + (int) ((krate > 1.) ? krate : Math.ceil(krate * allids.size()));

    DBIDs sampleids = DBIDUtil.randomSample(allids, ssize, RandomFactory.DEFAULT);
    KNNQuery<O> knnq = relation.getKNNQuery(getDistance(), kk);

    double[] idim = new double[ssize];
    int samples = 0;
    for(DBIDIter iter = sampleids.iter(); iter.valid(); iter.advance()) {
      idim[samples++] = estimator.estimate(knnq, iter, kk);
    }
    double id = (samples > 1) ? QuickSelect.median(idim, 0, samples) : -1;
    LOG.statistics(new DoubleStatistic(EstimateIntrinsicDimensionality.class.getName() + ".intrinsic-dimensionality", id));
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<Distance<? super O>> {
    /**
     * Estimation method
     */
    public static final OptionID ESTIMATOR_ID = new OptionID("idist.estimator", "Estimation method for intrinsic dimensionality.");

    /**
     * Number of kNN to use for each object.
     */
    public static final OptionID KRATE_ID = new OptionID("idist.k", "Number of kNN (absolute or relative)");

    /**
     * Number of samples to draw from the data set.
     */
    public static final OptionID SAMPLES_ID = new OptionID("idist.sampling", "Sample size (absolute or relative)");

    /**
     * Estimation method.
     */
    protected IntrinsicDimensionalityEstimator estimator;

    /**
     * Number of neighbors to use.
     */
    protected double krate;

    /**
     * Number of samples to draw.
     */
    protected double samples;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<IntrinsicDimensionalityEstimator> estimatorP = new ObjectParameter<>(ESTIMATOR_ID, IntrinsicDimensionalityEstimator.class, GEDEstimator.class);
      if(config.grab(estimatorP)) {
        estimator = estimatorP.instantiateClass(config);
      }
      DoubleParameter krateP = new DoubleParameter(KRATE_ID, 50) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(krateP)) {
        krate = krateP.doubleValue();
      }
      DoubleParameter samplesP = new DoubleParameter(SAMPLES_ID, .1) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(samplesP)) {
        samples = samplesP.doubleValue();
      }
    }

    @Override
    protected EstimateIntrinsicDimensionality<O> makeInstance() {
      return new EstimateIntrinsicDimensionality<>(distanceFunction, estimator, krate, samples);
    }
  }
}
