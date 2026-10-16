/*
 Copyright (C) 2013     Enzo Seraphim

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 or visit <http://www.gnu.org/licenses/>
 */

package yggdrasil.query;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import yggdrasil.block.SequentialMetricNode;
import yggdrasil.meta.Metric;
import yggdrasil.storage.SequentialMetric;
import yggdrasil.storage.Session;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 * @author Daniel Baracat Franqueira Cavazza <d2024001670@unifei.edu.br>
 * @param <M>
 */

public abstract class PointQuerySequentialMetric
        <M extends Metric<M>> extends AbstractStrategy<M> {

    private final M object;

    public PointQuerySequentialMetric(SequentialMetric<M> sequential, M object) {
        super(sequential);
        this.object = object;
    }

    @Override
    public Collection<M> solve() {
        Session se = this.getStructure().getWorkspace().openSession();
        List<M> result = new LinkedList<>();
        long firstNode = this.getStructure().getRootPageId();
        if (firstNode != 0) {
            long actualPageId = firstNode;
            long firstPageId = actualPageId;
            int total;
            double dist;
            do {
                SequentialMetricNode<M> actualSeqNode =
                        new SequentialMetricNode<>(se.load(actualPageId), this.getStructure().getObjectClass());
                total = actualSeqNode.readNumberOfKeys();
                for (int i = 0; i < total; i++) {
                    M build = actualSeqNode.buildKey(i);
                    dist = object.distanceTo(build);
                    if (dist == 0) {
                        build.setPreservedDistance(dist);
                        result.add(build);
                    }
                }
                actualPageId = actualSeqNode.readNextPageId();
            } while (actualPageId != firstPageId); 
        }
        se.close();
        return result;
    }
}