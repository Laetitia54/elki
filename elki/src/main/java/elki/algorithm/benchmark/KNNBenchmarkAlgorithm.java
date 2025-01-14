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
package elki.algorithm.benchmark;

import elki.AbstractDistanceBasedAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.Relation;
import elki.datasource.DatabaseConnection;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MeanVariance;
import elki.utilities.Util;
import elki.utilities.exceptions.IncompatibleDataException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Benchmarking algorithm that computes the k nearest neighbors for each query
 * point. The query points can either come from a separate data source, or from
 * the original database.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @param <O> Object type
 *
 * @assoc - - - KNNQuery
 */
public class KNNBenchmarkAlgorithm<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, Void> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNBenchmarkAlgorithm.class);

  /**
   * Number of neighbors to retrieve.
   */
  protected int k = 10;

  /**
   * The alternate query point source. Optional.
   */
  protected DatabaseConnection queries = null;

  /**
   * Sampling size.
   */
  protected double sampling = -1;

  /**
   * Random generator factory
   */
  protected RandomFactory random;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param k K parameter
   * @param queries Query data set (may be null!)
   * @param sampling Sampling rate
   * @param random Random factory
   */
  public KNNBenchmarkAlgorithm(Distance<? super O> distanceFunction, int k, DatabaseConnection queries, double sampling, RandomFactory random) {
    super(distanceFunction);
    this.k = k;
    this.queries = queries;
    this.sampling = sampling;
    this.random = random;
  }

  /**
   * Run the algorithm.
   *
   * @param relation Relation
   * @return Null result
   */
  public Void run(Relation<O> relation) {
    // Get a kNN query instance.
    KNNQuery<O> knnQuery = relation.getKNNQuery(getDistance(), k);
    // No query set - use original database.
    if(queries == null) {
      final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);
      FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
      int hash = 0;
      MeanVariance mv = new MeanVariance(), mvdist = new MeanVariance();
      for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
        KNNList knns = knnQuery.getKNNForDBID(iditer, k);
        int ichecksum = 0;
        for(DBIDIter it = knns.iter(); it.valid(); it.advance()) {
          ichecksum += DBIDUtil.asInteger(it);
        }
        hash = Util.mixHashCodes(hash, ichecksum);
        mv.put(knns.size());
        mvdist.put(knns.getKNNDistance());
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics("Result hashcode: " + hash);
        LOG.statistics("Mean number of results: " + mv.getMean() + " +- " + mv.getNaiveStddev());
        if(mvdist.getCount() > 0) {
          LOG.statistics("Mean k-distance: " + mvdist.getMean() + " +- " + mvdist.getNaiveStddev());
        }
      }
    }
    else {
      // Separate query set.
      TypeInformation res = getDistance().getInputTypeRestriction();
      MultipleObjectsBundle bundle = queries.loadData();
      int col = -1;
      for(int i = 0; i < bundle.metaLength(); i++) {
        if(res.isAssignableFromType(bundle.meta(i))) {
          col = i;
          break;
        }
      }
      if(col < 0) {
        throw new IncompatibleDataException("No compatible data type in query input was found. Expected: " + res.toString());
      }
      // Random sampling is a bit of hack, sorry.
      // But currently, we don't (yet) have an "integer random sample" function.
      DBIDRange sids = DBIDUtil.generateStaticDBIDRange(bundle.dataLength());
      final DBIDs sample = DBIDUtil.randomSample(sids, sampling, random);
      FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
      int hash = 0;
      MeanVariance mv = new MeanVariance(), mvdist = new MeanVariance();
      for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
        int off = sids.binarySearch(iditer);
        assert (off >= 0);
        @SuppressWarnings("unchecked")
        O o = (O) bundle.data(off, col);
        KNNList knns = knnQuery.getKNNForObject(o, k);
        int ichecksum = 0;
        for(DBIDIter it = knns.iter(); it.valid(); it.advance()) {
          ichecksum += DBIDUtil.asInteger(it);
        }
        hash = Util.mixHashCodes(hash, ichecksum);
        mv.put(knns.size());
        mvdist.put(knns.getKNNDistance());
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics("Result hashcode: " + hash);
        LOG.statistics("Mean number of results: " + mv.getMean() + " +- " + mv.getNaiveStddev());
        if(mvdist.getCount() > 0) {
          LOG.statistics("Mean k-distance: " + mvdist.getMean() + " +- " + mvdist.getNaiveStddev());
        }
      }
    }
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
   * Parameterization class
   *
   * @hidden
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<Distance<? super O>> {
    /**
     * Parameter for the number of neighbors.
     */
    public static final OptionID K_ID = new OptionID("knnbench.k", "Number of neighbors to retreive for kNN benchmarking.");

    /**
     * Parameter for the query dataset.
     */
    public static final OptionID QUERY_ID = new OptionID("knnbench.query", "Data source for the queries. If not set, the queries are taken from the database.");

    /**
     * Parameter for the sampling size.
     */
    public static final OptionID SAMPLING_ID = new OptionID("knnbench.sampling", "Sampling size parameter. If the value is less or equal 1, it is assumed to be the relative share. Larger values will be interpreted as integer sizes. By default, all data will be used.");

    /**
     * Parameter for the random generator
     */
    public static final OptionID RANDOM_ID = new OptionID("knnbench.random", "Random generator for sampling.");

    /**
     * K parameter
     */
    protected int k = 10;

    /**
     * The alternate query point source. Optional.
     */
    protected DatabaseConnection queries = null;

    /**
     * Sampling size.
     */
    protected double sampling = -1;

    /**
     * Random generator factory
     */
    protected RandomFactory random;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }
      ObjectParameter<DatabaseConnection> queryP = new ObjectParameter<>(QUERY_ID, DatabaseConnection.class);
      queryP.setOptional(true);
      if(config.grab(queryP)) {
        queries = queryP.instantiateClass(config);
      }
      DoubleParameter samplingP = new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .setOptional(true);
      if(config.grab(samplingP)) {
        sampling = samplingP.doubleValue();
      }
      RandomParameter randomP = new RandomParameter(RANDOM_ID, RandomFactory.DEFAULT);
      if(config.grab(randomP)) {
        random = randomP.getValue();
      }
    }

    @Override
    protected KNNBenchmarkAlgorithm<O> makeInstance() {
      return new KNNBenchmarkAlgorithm<>(distanceFunction, k, queries, sampling, random);
    }
  }
}
