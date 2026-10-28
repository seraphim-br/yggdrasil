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
import yggdrasil.meta.Sort;
import yggdrasil.storage.BTree;
import yggdrasil.storage.Session;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 * @author Paulo Vinícius Garcia Barbosa Rodrigues <d2024004081@unifei.edu.br>
 * @param <S>
 */

public abstract class MoreThanBTree
        <S extends Sort<S> & Comparable<S>> extends AbstractStrategy<S> {

    private final S object;

    public MoreThanBTree(BTree<S> btree, S object) {
        super(btree);
        this.object = object;
    }

    @Override
    public Collection<S> solve() {
        Session se = this.getStructure().getWorkspace().openSession();
        List<S> result = new LinkedList<>();
        long rootNode = this.getStructure().getRootPageId();

        if(rootNode != 0) {
            Node node = se.load(rootNode);
            BTreeIndex<S> index;
            BTreeLeaf<S> leaf;
            int idx;

            while(node.readNodeType() == BTreeIndex.nodeType) {
                index = new BTreeIndex<>(node, this.getStructure().getObjectClass());
                idx = index.indexOfQualify(object);
                node = se.load(index.readSubPageId(idx));
            }

            leaf = new BTreeLeaf<>(node, this.getStructure().getObjectClass());
            int total = leaf.readNumberOfKeys();

            int low = 0;
            int high = total - 1;
            int startIndex = total;

            while(low <= high) {
                int mid = (low + high) / 2;
                S build = leaf.buildKey(mid);
                
                if (build.compareTo(object) > 0) {
                    startIndex = mid;
                    high = mid - 1;
                } 
                else {
                    low = mid + 1;
                }
            }

            // Adiciona os elementos restantes da primeira folha
            for (int i = startIndex; i < total; i++) {
                result.add(leaf.buildKey(i));
            }

            // Percorre as folhas seguintes
            long nextPageId = leaf.readNextPageId();
            while (nextPageId != 0) {
                leaf = new BTreeLeaf<>(se.load(nextPageId), this.getStructure().getObjectClass());
                total = leaf.readNumberOfKeys();
                for (int i = 0; i < total; i++) {
                    result.add(leaf.buildKey(i));
                }
                nextPageId = leaf.readNextPageId();
            }
        }

        se.close();
        return result;
    }

}