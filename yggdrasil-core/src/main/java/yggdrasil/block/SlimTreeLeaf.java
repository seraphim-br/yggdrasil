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
package yggdrasil.block;

import yggdrasil.meta.Metric;
import yggdrasil.meta.Uuid;

/**
 * <p>
 * This class implements an SlimTree Leaf node.</p>
 *
 * @param <K>
 *
 * <blockquote><pre>
 * {@code
 * Design:
 *
 *                                     +--------------------------------------------------------+
 *                                     |             +-------------------------------------+    |
 *                                     |             |             +------------------+    |    |
 *                                     |             |             |                  |    |    |
 * +--------------+--------+-----------|-------------|-------------|-+----------------V----V----V----+
 * |node|prev|next| number |ent.|dist|off|ent.|dist|off|ent.|dist|off|                |key2|key1|key0|
 * |type|page|page|   of   |uuid|par |set|uuid|par |set|uuid|par |set|                |              |
 * |    | Id | Id |  keys  |-------------+-------------+-------------|<--free space-->|              |
 * |    |    |    |        |  entry[0]   |  entry[1]   |  entry[2]   |                |              |
 * |--------------+--------+-------------+-------------+-------------|                |--------------|
 * |   header     |features|                entries                  |                |     keys     |
 * +--------------+--------+-----------------------------------------+----------------+--------------+
 * }
 * </pre></blockquote>
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 */
public class SlimTreeLeaf<K extends Metric<K>> extends SlimTreeNode<K> {

    public static final int nodeType = 20;

    /**
     *
     * @param node
     * @param keyClass
     */
    public SlimTreeLeaf(Node node, Class<K> keyClass) {
        super(node, keyClass);
        initialize(node);
    }

    /**
     *
     * @param key
     * @param dist
     * @param uuid
     * @return
     */
    public boolean addKey(K key, double dist, Uuid uuid) {
        int total = this.readNumberOfKeys();

        int size = key.sizeOfKey();

        if (size + this.sizeOfEntry() > this.freeSpace()) // It there is not space.
        {
            return false;
        } else {
            this.incrementNumberOfKeys();
            int off = total == 0 ? this.sizeOfArray() - size : this.readOffset(total - 1) - size;
            key.pushKey(this.getArray(), off); // Adding
            this.writeOffset(total, off);
            this.writeDistanceToParent(total, dist);
            this.writeEntityUuid(total, uuid);

            return true;
        }
    }

    /**
     *
     * @return
     */
    @Override
    protected int getNodeType() {
        return nodeType;
    }

    /**
     *
     * @param node
     * @return
     */
    public static boolean matchNodeType(Node node) {
        return node.readNodeType() == SlimTreeLeaf.nodeType;
    }

    /**
     *
     * @param idx
     * @return
     */
    public final Uuid readEntityUuid(int idx) {
        int pos = SlimTreeLeaf.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry());
        return this.readUuid(pos);
    }

    /**
     *
     * @param idx
     * @return 
     */
    @Override
    public final boolean remove(int idx) {
        int total = this.readNumberOfKeys();
        if ((idx >= 0) && (idx < total)) {
            int sizeKey = idx == 0
                    ? this.sizeOfArray() - this.readOffset(0)
                    : this.readOffset(idx - 1) - this.readOffset(idx);
            int length = this.readOffset(idx) - this.readOffset(total - 1);
            int source = this.readOffset(total - 1);
            int dest = source + sizeKey;

            this.move(dest, source, length);

            for (int k = idx; k < total - 1; k++) {
                this.writeOffset(k, this.readOffset(k + 1) + sizeKey);
                this.writeDistanceToParent(k, this.readDistanceToParent(k + 1));
                this.writeEntityUuid(k, this.readEntityUuid(k + 1));
            }
            this.decrementNumberOfKeys();
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @return
     */
    @Override
    protected int sizeOfEntry() {
        return SlimTreeLeaf.sizeOfUuid + // uuid
                SlimTreeLeaf.sizeOfDouble + // dist par
                SlimTreeLeaf.sizeOfInteger; // off
    }
    
    /**
     *
     * @return
     */
    protected final int sizeOfFeatures()
    {
	    return SlimTreeNode.sizeOfInteger; // number of keys
    }

    /**
     *
     * @param idx
     * @param uuid
     */
    protected final void writeEntityUuid(int idx, Uuid uuid) {
        int pos = SlimTreeLeaf.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry());
        this.writeUuid(pos, uuid);
    }
}
