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
import java.util.Stack;

import yggdrasil.block.MTreeIndex;
import yggdrasil.block.MTreeLeaf;
import yggdrasil.block.Node;
import yggdrasil.storage.Session;
import yggdrasil.meta.Metric;
import yggdrasil.storage.MTree;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 * @param <M>
 */
public abstract class RangeQueryMTree
        <M extends Metric<M>> extends AbstractStrategy<M>{

    private final M object;
    private final double range;
    
    public RangeQueryMTree(MTree<M> mtree, M object, double range) {
        super(mtree);
        this.object = object;
        this.range = range;
    }

    private class Qualify{
        private final long pageId;
        private final double distanceParentToPointQuery;
        public Qualify(long pageId, double distanceParentToPointQuery) {
            this.pageId = pageId;
            this.distanceParentToPointQuery = distanceParentToPointQuery;
        }
        public long getPageId() {
            return pageId;
        }
        public double getDistanceParentToPointQuery() {
            return distanceParentToPointQuery;
        }        
    }

    @Override
    public Collection<M> solve() {
        Session se = this.getStructure().getWorkspace().openSession();
        List<M> result = new LinkedList<>();
        long rootNode = this.getStructure().getRootPageId();
        Stack<Qualify> qualified = new Stack<>();
        qualified.push(new Qualify(this.getStructure().getRootPageId(), Double.MAX_VALUE));
        if (rootNode != 0) {
            
            MTreeIndex<M> index;
            MTreeLeaf<M> leaf;
            int total;
            double dist;
            while(!qualified.isEmpty()){
                Qualify qualify = qualified.pop();

                Node node = se.load(qualify.getPageId());    
                if(node.readNodeType()==MTreeIndex.nodeType){//index
                    index = new MTreeIndex<>(node, this.getStructure().getObjectClass());
                    total = index.readNumberOfKeys();
                    for (int i = 0; i < total; i++) {
                        if(qualify.getDistanceParentToPointQuery() - range <=
                                index.readDistanceToParent(i) + index.readCoveringRadius(i)){
                            M build = index.buildKey(i);
                            dist = object.distanceTo(build);
                            if (dist<=range+index.readCoveringRadius(i)) {
                                qualified.push(new Qualify(index.readSubPageId(i), dist));
                            }
                        }
                    }
                }else{//leaf
                    leaf = new MTreeLeaf<>(node, this.getStructure().getObjectClass());
                    total = leaf.readNumberOfKeys();
                    for (int i = 0; i < total; i++) {
                        M build = leaf.buildKey(i);
                        if (object.distanceTo(build)<=range) {
                            result.add(build);
                        }
                    }
                }
            }
        }
        se.close();

        return result;
    }

}
