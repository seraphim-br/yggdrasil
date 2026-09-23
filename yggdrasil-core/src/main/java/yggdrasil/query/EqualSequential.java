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
import yggdrasil.block.SequentialNode;
import yggdrasil.storage.Session;
import yggdrasil.meta.Entity;
import yggdrasil.storage.Sequential;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 * @param <M>
 */
public abstract class EqualSequential
        <E extends Entity<E>> extends AbstractStrategy<E>{

    private final E object;
    
    public EqualSequential(Sequential<E> sequential, E object) {
        super(sequential);
        this.object = object;
    }

    @Override
    public Collection<E> solve() {
        Session se = this.getStructure().getWorkspace().openSession();
        List<E> result = new LinkedList<>();
        long firstNode = this.getStructure().getRootPageId();
        if (firstNode != 0) {
            long actualPageId = firstNode;
            long firstPageId = actualPageId;
            int total;
            do {
                SequentialNode<E> actualSeqNode = 
                        new SequentialNode<>(se.load(actualPageId), this.getStructure().getObjectClass());
                total = actualSeqNode.readNumberOfEntitries();
                for (int i = 0; i < total; i++) {
                    E build = actualSeqNode.buildEntity(i);
                    if (object.isEqual(build)) {
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
