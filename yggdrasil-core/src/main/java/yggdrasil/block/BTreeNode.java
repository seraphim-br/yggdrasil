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

import yggdrasil.meta.Sort;

/**
 * <p>
 * This class implements a B+ Tree Node.</p>
 *
 * @param <K>
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 *
 */
public abstract class BTreeNode<K extends Sort<K> & Comparable<K>> extends KeyNode<K> {

    protected long calculatedComparisons;

    /**
     *
     * @param node
     * @param keyClass
     */
    public BTreeNode(Node node, Class<K> keyClass) {
        super(node, keyClass);
    }

    /**
     * Returns the number of comparisons ({@Code compareTo}) made by this class.
     *
     * @return the number of comparisons
     */
    public long getCalculatedComparisons() {
        return calculatedComparisons;
    }

    /**
     *
     * @param idx
     * @return
     */
    public K buildKey(int idx) {
        //instantiation by reflection
        if ((idx >= 0) && (idx < this.readNumberOfKeys())) {
            K key;
            if(this instanceof BTreeLeaf){
                BTreeLeaf<K> leaf = (BTreeLeaf<K>)this;
                key = this.newGenericType(leaf.readEntityUuid(idx));
            }else{
                key = this.newGenericType();
            }
            //pull the key of the page
            key.pullKey(this.getArray(), this.readOffset(idx));
            return key;
        }//endif

        return null;
    }

    /**
     *
     */
    @Override
    public void clear() {
        //zering numberOfKeys
        int pos = BTreeNode.sizeOfHeader();
        this.writeInteger(pos, 0);
    }

    /**
     *
     */
    protected void decrementNumberOfKey() {
        int num = this.readNumberOfKeys() - 1;
        int pos = BTreeNode.sizeOfHeader();
        this.writeInteger(pos, num);
    }

    /**
     *
     * @return
     */
    protected int freeSpace() {
        int num = this.readNumberOfKeys();
        if (num == 0) {
            return this.sizeOfArray() - BTreeNode.sizeOfHeader() - this.sizeOfFeatures();
        } else {
            return this.sizeOfArray() - BTreeNode.sizeOfHeader()
                    - this.sizeOfFeatures() - (num * this.sizeOfEntry())
                    - (this.sizeOfArray() - this.readOffset(num - 1));
        }//endif
    }

    /**
     *
     */
    protected void incrementNumberOfKey() {
        int num = this.readNumberOfKeys() + 1;
        int pos = BTreeNode.sizeOfHeader();
        this.writeInteger(pos, num);
    }

    /**
     *
     * @param key
     * @return
     */
    public int indexOfKey(K key) {
        // binary search
        K objHalf;
        int half;
        int lower = 0;
        int upper = this.readNumberOfKeys() - 1;

        //find binary seach
        while (lower <= upper) {
            half = lower + ((upper - lower) / 2);
            objHalf = buildKey(half);
            calculatedComparisons++;
            if (key.compareTo(objHalf) == 0) {
                return half;
            } else {
                calculatedComparisons++;
                if (key.compareTo(objHalf) < 0) {
                    upper = half - 1;
                } else {
                    lower = half + 1;
                }//endif
            }
        }//endwhile		
        return -1;
    }

    /**
     *
     * @return
     */
    @Override
    public int readNumberOfKeys() {
        int pos = BTreeNode.sizeOfHeader();
        return this.readInteger(pos);
    }

    /**
     *
     * @param idx
     * @return
     */
    protected int readOffset(int idx) {
        int pos = BTreeNode.sizeOfHeader() + this.sizeOfFeatures() + ((idx + 1) * this.sizeOfEntry()) - Node.sizeOfInteger;
        return this.readInteger(pos);
    }

    /**
     *
     * @return
     */
    protected abstract int sizeOfEntry();

    /**
     *
     * @return
     */
    protected abstract int sizeOfFeatures();

    /**
     *
     * @param idx
     * @param off
     */
    protected void writeOffset(int idx, int off) {
        int pos = BTreeNode.sizeOfHeader() + this.sizeOfFeatures() + ((idx + 1) * this.sizeOfEntry()) - Node.sizeOfInteger;
        this.writeInteger(pos, off);
    }
}
