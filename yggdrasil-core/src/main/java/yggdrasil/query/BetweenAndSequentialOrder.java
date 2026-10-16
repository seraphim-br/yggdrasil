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
import yggdrasil.block.SequentialSortNode;
import yggdrasil.meta.Sort;
import yggdrasil.storage.SequentialSort;
import yggdrasil.storage.Session;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 * @param <M>
 */
public class BetweenAndSequentialOrder
        <M extends Sort<M> & Comparable<M>> extends AbstractStrategy<M> {

    private final M lower, upper;

    public BetweenAndSequentialOrder(SequentialSort<M> sequential, M lower,M upper) {
        super(sequential);
        this.lower =  (lower.compareTo(upper) <= 0) ? lower : upper;
        this.upper = (upper.compareTo(lower) >= 0) ? upper : lower;
    }

    @Override
    public Collection<M> solve() {
        List<M> result = new LinkedList<>();
        if (lower == null || upper == null) {
            return result;
        }
        long firstNode = this.getStructure().getRootPageId();
        if (firstNode == 0) {
            return result;
        }
        Session se = this.getStructure().getWorkspace().openSession();
        try {
            long actualPageId = firstNode;
            long firstPageId = actualPageId;
            int total;
            do {
                SequentialSortNode<M> actualSeqNode = 
                        new SequentialSortNode<>(se.load(actualPageId), this.getStructure().getObjectClass());
                total = actualSeqNode.readNumberOfKeys();
                for (int i = 0; i < total; i++) {
                    M build = actualSeqNode.buildKey(i);
                    
                    if (build != null && build.compareTo(upper) <= 0 && build.compareTo(lower) >= 0) {
                        result.add(build);
                    }

                }
                actualPageId = actualSeqNode.readNextPageId();
            } while (actualPageId != 0 && actualPageId != firstPageId);
        } finally {
            se.close();
        }

        return result;
    }
}