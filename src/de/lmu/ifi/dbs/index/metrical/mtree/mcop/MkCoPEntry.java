package de.lmu.ifi.dbs.index.metrical.mtree.mcop;

import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtree.Entry;
import de.lmu.ifi.dbs.data.MetricalObject;

/**
 * Defines the requirements for an entry in a MCop-Tree node. Additionally to an entry in a M-Tree
 * getter and setter methods for the knn distances are provided.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

interface MkCoPEntry extends Entry<NumberDistance> {

  /**
   * Returns the conservative approximated knn distance of the entry.
   *
   * @param k the parameter k of the knn distance
   * @param distanceFunction the distance function
   *
   * @return the conservative approximated knn distance of the entry
   */
  public <O extends MetricalObject> NumberDistance approximateConservativeKnnDistance(int k, DistanceFunction<O, NumberDistance> distanceFunction);

  /**
   * Returns the conservative approximation line.
   *
   * @return the conservative approximation line
   */
  public ApproximationLine getConservativeKnnDistanceApproximation();

  /**
   * Sets the conservative approximation line
   *
   * @param conservativeApproximation the conservative approximation line to be set
   */
  public void setConservativeKnnDistanceApproximation(ApproximationLine conservativeApproximation);
}
