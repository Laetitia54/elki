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
package elki.evaluation.clustering.internal;

import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.database.Database;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.math.MeanVariance;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.ResultUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.io.FormatUtil;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the silhouette of a data set.
 * <p>
 * Reference:
 * <p>
 * P. J. Rousseeuw<br>
 * Silhouettes: A graphical aid to the interpretation and validation of cluster
 * analysis<br>
 * In: Journal of Computational and Applied Mathematics Volume 20, November 1987
 * <p>
 * TODO: keep all silhouette values, and allow visualization!
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 *
 * @param <O> Object type
 */
@Reference(authors = "P. J. Rousseeuw", //
    title = "Silhouettes: A graphical aid to the interpretation and validation of cluster analysis", //
    booktitle = "Journal of Computational and Applied Mathematics, Volume 20", //
    url = "https://doi.org/10.1016/0377-0427(87)90125-7", //
    bibkey = "doi:10.1016/0377-04278790125-7")
public class EvaluateSilhouette<O> implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSilhouette.class);

  /**
   * Distance function to use.
   */
  private Distance<? super O> distance;

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption;

  /**
   * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  private boolean penalize = true;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateSilhouette.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseOption Handling of "noise" clusters.
   * @param penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  public EvaluateSilhouette(Distance<? super O> distance, NoiseHandling noiseOption, boolean penalize) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOption;
    this.penalize = penalize;
  }

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, instead of breaking them
   *        into singletons.
   */
  public EvaluateSilhouette(Distance<? super O> distance, boolean mergenoise) {
    this(distance, mergenoise ? NoiseHandling.MERGE_NOISE : NoiseHandling.TREAT_NOISE_AS_SINGLETONS, true);
  }

  /**
   * Evaluate a single clustering.
   *
   * @param rel Data relation
   * @param dq Distance query
   * @param c Clustering
   * @return Average silhouette
   */
  public double evaluateClustering(Relation<O> rel, DistanceQuery<O> dq, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    MeanVariance msil = new MeanVariance();
    int ignorednoise = 0;
    for(Cluster<?> cluster : clusters) {
      // Note: we treat 1-element clusters the same as noise.
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
          ignorednoise += cluster.size();
          continue; // Ignore noise elements
        case TREAT_NOISE_AS_SINGLETONS:
          // As suggested in Rousseeuw, we use 0 for singletons.
          msil.put(0., cluster.size());
          continue;
        case MERGE_NOISE:
          break; // Treat as cluster below
        }
      }
      ArrayDBIDs ids = DBIDUtil.ensureArray(cluster.getIDs());
      double[] as = new double[ids.size()]; // temporary storage.
      DBIDArrayIter it1 = ids.iter(), it2 = ids.iter();
      for(it1.seek(0); it1.valid(); it1.advance()) {
        // a: In-cluster distances
        double a = as[it1.getOffset()]; // Already computed distances
        for(it2.seek(it1.getOffset() + 1); it2.valid(); it2.advance()) {
          final double dist = dq.distance(it1, it2);
          a += dist;
          as[it2.getOffset()] += dist;
        }
        a /= (ids.size() - 1);
        // b: minimum average distance to other clusters:
        double b = Double.POSITIVE_INFINITY;
        for(Cluster<?> ocluster : clusters) {
          if(ocluster == /* yes, reference identity */cluster) {
            continue; // Same cluster
          }
          if(ocluster.size() <= 1 || ocluster.isNoise()) {
            switch(noiseOption){
            case IGNORE_NOISE:
              continue; // Ignore noise elements
            case TREAT_NOISE_AS_SINGLETONS:
              // Treat noise cluster as singletons:
              for(DBIDIter it3 = ocluster.getIDs().iter(); it3.valid(); it3.advance()) {
                final double dist = dq.distance(it1, it3);
                b = dist < b ? dist : b; // Minimum average
              }
              continue;
            case MERGE_NOISE:
              break; // Treat as cluster below
            }
          }
          final DBIDs oids = ocluster.getIDs();
          double btmp = 0.;
          for(DBIDIter it3 = oids.iter(); it3.valid(); it3.advance()) {
            btmp += dq.distance(it1, it3);
          }
          btmp /= oids.size(); // Average
          b = btmp < b ? btmp : b; // Minimum average
        }
        // One cluster only?
        b = b < Double.POSITIVE_INFINITY ? b : a;
        msil.put((b - a) / (b > a ? b : a));
      }
    }
    double penalty = 1.;
    // Only if {@link NoiseHandling#IGNORE_NOISE}:
    if(penalize && ignorednoise > 0) {
      penalty = (rel.size() - ignorednoise) / (double) rel.size();
    }
    final double meansil = penalty * msil.getMean();
    final double stdsil = penalty * msil.getSampleStddev();
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".silhouette.noise-handling", noiseOption.toString()));
      if(ignorednoise > 0) {
        LOG.statistics(new LongStatistic(key + ".silhouette.noise", ignorednoise));
      }
      LOG.statistics(new DoubleStatistic(key + ".silhouette.mean", meansil));
      LOG.statistics(new DoubleStatistic(key + ".silhouette.stddev", stdsil));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(c, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("Silhouette +-" + FormatUtil.NF2.format(stdsil), meansil, -1., 1., 0., false);
    Metadata.hierarchyOf(c).addChild(ev);
    // FIXME: notify of changes, if reused!
    return meansil;
  }

  @Override
  public void processNewResult(Object result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(result);
    Relation<O> relation = db.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> dq = relation.getDistanceQuery(distance);
    for(Clustering<?> c : crs) {
      evaluateClustering(relation, dq, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("silhouette.distance", "Distance function to use for computing the silhouette.");

    /**
     * Parameter to treat noise as a single cluster.
     */
    public static final OptionID NOISE_ID = new OptionID("silhouette.noisehandling", "Control how noise should be treated.");

    /**
     * Do not penalize ignored noise.
     */
    public static final OptionID NO_PENALIZE_ID = new OptionID("silhouette.no-penalize-noise", "Do not penalize ignored noise.");

    /**
     * Distance function to use.
     */
    private Distance<? super O> distance;

    /**
     * Noise handling
     */
    private NoiseHandling noiseOption;

    /**
     * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
     */
    private boolean penalize = true;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Distance<? super O>> distP = new ObjectParameter<>(DISTANCE_ID, Distance.class, EuclideanDistance.class);
      if(config.grab(distP)) {
        distance = distP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }

      if(noiseOption == NoiseHandling.IGNORE_NOISE) {
        Flag penalizeP = new Flag(NO_PENALIZE_ID);
        if(config.grab(penalizeP)) {
          penalize = penalizeP.isFalse();
        }
      }
    }

    @Override
    protected EvaluateSilhouette<O> makeInstance() {
      return new EvaluateSilhouette<>(distance, noiseOption, penalize);
    }
  }
}
