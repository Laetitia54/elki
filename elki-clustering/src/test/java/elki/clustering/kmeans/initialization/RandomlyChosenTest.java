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
package elki.clustering.kmeans.initialization;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.SingleAssignmentKMeans;
import elki.clustering.kmedoids.CLARA;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Performs a single assignment with different k-means initializations.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class RandomlyChosenTest extends AbstractClusterAlgorithmTest {
  /**
   * Run KMeans with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testRandomlyChosenInitialMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<SingleAssignmentKMeans<DoubleVector>>(SingleAssignmentKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .with(KMeans.INIT_ID, RandomlyChosen.class) //
        .build().run(db);
    testFMeasure(db, result, 0.702733);
    testClusterSizes(result, new int[] { 64, 95, 202, 306, 333 });
  }

  /**
   * Run CLARA with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testRandomlyChosenInitialMedoids() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<?> result = new ELKIBuilder<CLARA<DoubleVector>>(CLARA.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .with(KMeans.INIT_ID, RandomlyChosen.class) //
        .with(KMeans.MAXITER_ID, 1) //
        .with(CLARA.Parameterizer.NOKEEPMED_ID) //
        .with(CLARA.Parameterizer.SAMPLESIZE_ID, 10) //
        .with(CLARA.Parameterizer.RANDOM_ID, 0) //
        .build().run(db);
    testFMeasure(db, result, 0.99602);
    testClusterSizes(result, new int[] { 198, 200, 200, 200, 202 });
  }
}
