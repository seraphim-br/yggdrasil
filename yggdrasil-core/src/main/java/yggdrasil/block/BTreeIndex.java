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
 * This class implements a B+ Tree Index Node.</p>
 *
 * @param <K>
 *
 * <blockquote><pre>
 * {@code
 * Design:
 *
 *                            +----------------------------------------------+
 *                            |        +--------------------------------+    |
 *                            |        |        +------------------+    |    |
 *                            |        |        |                  |    |    |
 * +--------------------------|--------|--------|------------------V----V----V----+
 * |      |number|first| sub|off| sub|off| sub|off|                |obj2|obj1|obj0|
 * |      |  of  | sub | ndo|set| ndo|set| ndo|set|<--free space-->|              |
 * |      | keys |     |entry[0]|entry[1]|entry[2]|                |              |
 * |header|  features  |          entries         |                |     keys     |
 * +------------------------------------------------------------------------------+
 * }
 * </pre></blockquote>
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 */
public class BTreeIndex<K extends Sort<K> & Comparable<K>> extends BTreeNode<K> {

    /**
     *
     */
    public static final int nodeType = 5;

    /**
     *
     * @param node
     * @param keyClass
     */
    public BTreeIndex(Node node, Class<K> keyClass) {
        super(node, keyClass);
        initialize(node);
    }

    /**
     *
     * @param key
     * @param leftId
     * @param rightId
     * @return
     */
    public final boolean addFirstKey(K key, long leftId, long rightId) {
        int off;
        int size = key.sizeOfKey();
        //reset node
        clear();
        if (size + this.sizeOfEntry() < this.freeSpace()) {
            //insert offset
            off = this.sizeOfArray() - size;
            writeOffset(0, off);
            //push the key in page
            key.pushKey(this.getArray(), off);
            //insert subPages
            writeSubPageId(0, leftId);
            writeSubPageId(1, rightId);
            incrementNumberOfKey();
            return true;
        } else {
            return false;
        }//endif
    }

    /**
     *
     * @param key
     * @param pageId
     * @return
     */
    public boolean addKey(K key, long pageId) {
        int i;
        int total = this.readNumberOfKeys();
        int len, dest, src, idx = 0, off;
        int size = key.sizeOfKey();
        K obj;

        if ((total > 0) && (size + this.sizeOfEntry() < this.freeSpace())) {
            //positions dest and source
            dest = this.readOffset(total - 1) - size;
            src = this.readOffset(total - 1);

            //locating position
            for (i = total; i > 0; i--) {
                obj = this.buildKey(i - 1);
                calculatedComparisons++;
                if (obj.compareTo(key) < 0) {
                    idx = i;
                    break;
                } else {
                    //move offset
                    this.writeOffset(i, (this.readOffset(i - 1) - size));
                    //move subPageId
                    this.writeSubPageId(i + 1, this.readSubPageId(i));
                }//endif
            }//endfor
            //if is first position
            if (idx == 0) {
                len = this.sizeOfArray() - src;
                off = this.sizeOfArray() - size;
            } else {
                len = this.readOffset(idx - 1) - src;
                off = this.readOffset(idx - 1) - size;
            }//endif

            // move keys			
            this.move(dest, src, len);

            //offset
            this.writeOffset(idx, off);
            this.writeSubPageId(idx + 1, pageId);
            //push the key in page
            key.pushKey(this.getArray(), off);
            //increment
            this.incrementNumberOfKey();

            return true;
        } else {
            return false;
        }//endif	
    }

    /**
     *
     * @param key
     * @return
     */
    public int indexOfQualify(K key) {
        // binary search
        K obj;
        int idx;
        int total = this.readNumberOfKeys();
        int half = 0;
        int lower = 0;
        int upper = total;
        //find binary seach
        while (lower < upper) {
            half = lower + ((upper - lower) / 2);
            obj = buildKey(half);
            calculatedComparisons++;
            if (key.compareTo(obj) <= 0) {
                if (upper == half) {
                    break;
                }
                upper = half;
            } else {
                if (lower == half) {
                    break;
                }
                lower = half;
            }//endif
        }
        if (half > 0) {
            idx = half - 1;
        } else {
            idx = half;
        }
        obj = buildKey(idx);
        while (key.compareTo(obj) >= 0) {
            calculatedComparisons++;
            idx++;
            if (idx < total) {
                obj = buildKey(idx);
            } else {
                break;
            }
        }
        calculatedComparisons++;
        return idx;
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
        return node.readNodeType() == BTreeIndex.nodeType;
    }

    /**
     *
     * @param idx
     * @return
     */
    public long readSubPageId(int idx) {
        int pos;
        if (idx == 0) {
            pos = BTreeIndex.sizeOfHeader() + Node.sizeOfInteger;
        } else {
            pos = BTreeIndex.sizeOfHeader() + this.sizeOfFeatures() + ((idx - 1) * this.sizeOfEntry());
        }//endif
        return this.readLong(pos);
    }

    /**
     *
     * @return
     */
    @Override
    protected int sizeOfEntry() {
        return Node.sizeOfLong + Node.sizeOfInteger;
    }

    /**
     *
     * @return
     */
    @Override
    protected int sizeOfFeatures() {
        return Node.sizeOfInteger + Node.sizeOfLong;
    }

    /**
     *
     * @param idx
     * @param pageId
     */
    protected final void writeSubPageId(int idx, long pageId) {
        int pos;
        if (idx == 0) {
            pos = BTreeIndex.sizeOfHeader() + Node.sizeOfInteger;
        } else {
            pos = BTreeIndex.sizeOfHeader() + this.sizeOfFeatures() + ((idx - 1) * this.sizeOfEntry());
        }//endif
        this.writeLong(pos, pageId);
    }
}
