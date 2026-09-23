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

/**
 * <p>
 * This class implements an SlimTree Index.</p>
 *
 * @param <K>
 *
 * <blockquote><pre>
 * {@code
 * Design:
 *
 *                                            +----------------------------------------------------------------------+
 *                                            |                    +--------------------------------------------+    |
 *                                            |                    |                    +------------------+    |    |
 *                                            |                    |                    |                  |    |    |
 * +--------------+--------+------------------|--------------------|--------------------|-+----------------V----V----V----+
 * |node|prev|next| number |sub|cov|#n |dist|off|sub|cov|#n |dist|off|sub|cov|#n |dist|off|                |key2|key1|key0|
 * |type|page|page|   of   |nde|rad|sub|par |set|nde|rad|sub|par |set|nde|rad|sub|par |set|                |              |
 * |    | Id | Id |  keys  |--------------------+--------------------+--------------------|<--free space-->|              |
 * |    |    |    |        |      entry[0]      |      entry[1]      |      entry[2]      |                |              |
 * |--------------+--------+--------------------+--------------------+--------------------|                |--------------|
 * |   header     |features|                         entries                              |                |     keys     |
 * +--------------+--------+--------------------------------------------------------------+----------------+--------------+
 * }
 * </pre></blockquote>
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 */
public class SlimTreeIndex<K extends Metric<K>> extends SlimTreeNode<K> {

    public static final int nodeType = 21;

    /**
     *
     * @param node
     * @param keyClass
     */
    public SlimTreeIndex(Node node, Class<K> keyClass) {
        super(node, keyClass);
        initialize(node);
    }

    /**
     *
     * @param key
     * @param dist
     * @param subPageId
     * @param covRad
     * @param nEntriesSub
     * @return
     */
    public boolean addKey(K key, double dist, long subPageId, double covRad, int nEntriesSub) {
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
            this.writeSubPageId(total, subPageId);
            this.writeCoveringRadius(total, covRad);
            this.writeNumEntriesSub(total, nEntriesSub);

            return true;
        }
    }

    /**
     *
     * @param key1
     * @param dist1
     * @param subPageId1
     * @param covRad1
     * @param nEntSub1
     * @param key2
     * @param dist2
     * @param subPageId2
     * @param covRad2
     * @param nEntSub2
     * @return
     */
    public final boolean addKey(K key1, double dist1, long subPageId1, double covRad1, int nEntSub1, 
                                K key2, double dist2, long subPageId2, double covRad2, int nEntSub2) {
        int sizeKey1 = key1.sizeOfKey();
        int sizeKey2 = key2.sizeOfKey();

        if (sizeKey1 + sizeKey2 + 2 * this.sizeOfEntry() > this.freeSpace()) {
            return false;
        } else {
            addKey(key1, dist1, subPageId1, covRad1, nEntSub1);
            addKey(key2, dist2, subPageId2, covRad2, nEntSub2);
            return true;
        }
    }

    /**
     *
     * @param idx
     * @param insertKey
     * @return
     */
    public double distanceToQualify(int idx, K insertKey) {
        K key = this.buildKey(idx);

        calculatedDistance++;
        return insertKey.distanceTo(key);
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
     * @param key
     * @return
     */
    public final int indexOfInsertion(K key) {
        double distIn = Double.MAX_VALUE; // Minimum distance if key is inside some obj covering radius.
        double distOut = Double.MAX_VALUE; // Minimum distance if key is outside some obj covering radius.
        double newCoverage = Double.MAX_VALUE;
        int idxIn = 0; // Index of obj in which key has minimum distance (inside cov rad).
        int idxOut = 0; // Index of obj in which key has minimum distance (outside cov rad).
        boolean in = false; // Weather key is inside some cov rad.
        int total = this.readNumberOfKeys();

        for (int i = 0; i < total; i++) {
            K storedKey = this.buildKey(i); // Key stored in the node.

            double rad = this.readCoveringRadius(i); // storedKey cov rad: r(Or)

            calculatedDistance++;
            double dist = storedKey.distanceTo(key); // Distance from key to storedKey: d(Or, On)

            if (in) {
                //exists a radii covering
                if (dist <= rad && dist < distIn) // Looking for minimum distance.
                {
                    //these radius too cover and is less
                    distIn = dist; // Minimum distance between storedKey and key.
                    idxIn = i; // storedKey in which distance to key is minimum.
                }
            } else {
                //does not exists a radii covering
                if (dist <= rad) // key is inside storedKey covering radius.
                {
                    //these radius cover 
                    in = true; // !in to set true once.
                    distIn = dist; // Minimum distance between storedKey and key.
                    idxIn = i; // storedKey in which distance to key is minimum.
                } else {
                    //these radius does not cover 
                    if (dist - rad < distOut) // If key is outside any storedKey cov rad
                    {
                        //minimize distOut
                        distOut = dist - rad; // Minimum enlargement of cov rad.
                        idxOut = i; // storedKey to minimum cov rad enlargement.
                        newCoverage = dist;
                    }
                }
            }
        }

        if (in) // Key is inside entry[idxIn] covering radius and the distance is minimum
        {
            return idxIn;
        } else // No entry cov rad contains key.
        {
	    // Increase the most suitable (minimum enlargement) entry cov rad.			
            //this.writeCoveringRadius(idxOut, this.readCoveringRadius(idxOut) + distOut);
            this.writeCoveringRadius(idxOut, newCoverage + SlimTreeNode.precisionError );
            return idxOut;
        }
    }

    /**
     *
     * @param pageId
     * @return
     */
    public final int indexOfSubPageId(long pageId) {
        int total = this.readNumberOfKeys();
        for (int i = 0; i < total; i++) {
            if (this.readSubPageId(i) == pageId) {
                return i;
            }
        }

        return -1;
    }

    /**
     *
     * @param node
     * @return
     */
    public static boolean matchNodeType(Node node) {
        return node.readNodeType() == SlimTreeIndex.nodeType;
    }

    /**
     *
     * @param idx
     * @return
     */
    public final double readCoveringRadius(int idx) {
        int pos = SlimTreeIndex.sizeOfHeader()
                + this.sizeOfFeatures()
                + (idx * this.sizeOfEntry())
                + SlimTreeIndex.sizeOfLong; // sub

        return this.readDouble(pos);
    }

    /**
     *
     * @param idx
     * @return
     */
    public final int readNumEntriesSub(int idx) {
        int pos = SlimTreeIndex.sizeOfHeader()
                + this.sizeOfFeatures()
                + (idx * this.sizeOfEntry())
                + SlimTreeIndex.sizeOfLong
                + SlimTreeIndex.sizeOfDouble;
        return this.readInteger(pos);
    }    
    /**
     *
     * @param idx
     * @return
     */
    public final long readSubPageId(int idx) {
        int pos = SlimTreeIndex.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry());
        return this.readLong(pos);
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
                this.writeSubPageId(k, this.readSubPageId(k + 1));
                this.writeNumEntriesSub(k, this.readNumEntriesSub(k + 1));
                this.writeCoveringRadius(k, this.readCoveringRadius(k + 1));
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
        return SlimTreeIndex.sizeOfLong // sub
                + SlimTreeIndex.sizeOfDouble // cov rad
                + SlimTreeIndex.sizeOfInteger // numEntriesSub
                + SlimTreeIndex.sizeOfDouble // dist par
                + SlimTreeIndex.sizeOfInteger; // off
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
     * @param rad
     */
    public final void writeCoveringRadius(int idx, double rad) {
        int pos = SlimTreeIndex.sizeOfHeader()
                + this.sizeOfFeatures()
                + (idx * this.sizeOfEntry())
                + SlimTreeIndex.sizeOfLong; // sub

        this.writeDouble(pos, rad);
    }
    
    /**
     *
     * @param idx
     * @param nEntriesSub
     */
    protected final void writeNumEntriesSub(int idx, int nEntriesSub) {
        int pos = SlimTreeIndex.sizeOfHeader()
                + this.sizeOfFeatures()
                + (idx * this.sizeOfEntry())
                + SlimTreeIndex.sizeOfLong
                + SlimTreeIndex.sizeOfDouble;
        this.writeInteger(pos, nEntriesSub);
    }
    
    /**
     *
     * @param idx
     * @param pageId
     */
    protected final void writeSubPageId(int idx, long pageId) {
        int pos = SlimTreeIndex.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry());
        this.writeLong(pos, pageId);
    }

    
    
}
