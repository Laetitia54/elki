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
package elki.distance.probabilistic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.data.HyperBoundingBox;
import elki.distance.AbstractDistanceTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for triangular distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class TriangularDistanceTest extends AbstractDistanceTest {
  @Test
  public void testBasic() {
    // Also test the builder - we could have just used .STATIC
    TriangularDistance df = new ELKIBuilder<>(TriangularDistance.class).build();
    basicChecks(df);
    nonnegativeSpatialConsistency(df);
  }

  @Test
  public void testTriganular() {
    double[][] vecs = TOY_VECTORS;
    double[][] distances = TriangularDiscriminationDistanceTest.TOY_DISTANCES;

    TriangularDistance df = new ELKIBuilder<>(TriangularDistance.class).build();
    for(int i = 0; i < vecs.length; i++) {
      DoubleVector vi = DoubleVector.wrap(vecs[i]);
      HyperBoundingBox mbri = new HyperBoundingBox(vecs[i], vecs[i]);
      for(int j = 0; j < vecs.length; j++) {
        DoubleVector vj = DoubleVector.wrap(vecs[j]);
        assertEquals("Distance " + i + "," + j, Math.sqrt(distances[i][j]), df.distance(vi, vj), 1e-15);
        compareDistances(vj, vi, mbri, df);
      }
    }
  }
}
