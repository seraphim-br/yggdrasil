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

import yggdrasil.block.BTreeIndex;
import yggdrasil.block.BTreeLeaf;
import yggdrasil.block.Node;
import yggdrasil.storage.Session;
import yggdrasil.meta.Sort;
import yggdrasil.storage.BTree;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 * @param <M>
 */
public abstract class EqualBTree
        <S extends Sort<S> & Comparable<S>> extends AbstractStrategy<S>{

    private final S object;
    
    public EqualBTree(BTree<S> btree, S object) {
        super(btree);
        this.object = object;
    }

    @Override
    public Collection<S> solve() {
        Session se = this.getStructure().getWorkspace().openSession();
        List<S> result = new LinkedList<>();
        long rootNode = this.getStructure().getRootPageId();
        if (rootNode != 0) {
            Node node = se.load(rootNode);
            BTreeIndex<S> index;
            BTreeLeaf<S> leaf;
            int idx, total;
            while(node.readNodeType()==BTreeIndex.nodeType){
                index = new BTreeIndex<S>(node, this.getStructure().getObjectClass());
                idx = index.indexOfQualify(object);
                node = se.load(index.readSubPageId(idx));
            }
            leaf = new BTreeLeaf<>(node, this.getStructure().getObjectClass());
            total = leaf.readNumberOfKeys();
            
            int low = 0;
            int high = total - 1;
            int firstMatch = -1;

            while(low <= high) {
                int mid = (low + high) / 2;
                S build = leaf.buildKey(mid);
                int cmp = object.compareTo(build);

                if(cmp == 0) {
                    firstMatch = mid;
                    high = mid - 1;
                } 
                else if(cmp < 0) {
                    high = mid - 1;
                } 
                else {
                    low = mid + 1;
                }
            }

            if(firstMatch != -1) {
                for(int i = firstMatch; i < total; i++) {
                    S build = leaf.buildKey(i);
                    if(object.compareTo(build) == 0) {
                        result.add(build);
                    } 
                    else {
                        break;
                    }
                }
            }
        }
        se.close();

        return result;
    }

}
