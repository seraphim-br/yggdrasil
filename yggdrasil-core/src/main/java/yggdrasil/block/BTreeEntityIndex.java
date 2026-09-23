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

import yggdrasil.meta.Entity;
import yggdrasil.meta.Uuid;

/**
 * <p>
 * This class implements a B+ Tree Index Node.</p>
 *
 * @param <E>
 *
 * <blockquote><pre>
 * {@code
 * Design:
 *
 * +------------------------------------------------------------------+
 * |      |number|first| sub|ent.| sub|ent.| sub|ent.|                |
 * |      |  of  | sub | ndo|uuid| ndo|uuid| ndo|uuid|<--free space-->|
 * |      |entit.|     | entry[0]| entry[1]| entry[2]|                |
 * |header|  features  |           entries           |                |
 * +------------------------------------------------------------------+
 * }
 * </pre></blockquote>
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 */
public class BTreeEntityIndex<E extends Entity<E>>  extends BTreeEntityNode<E> {

    /**
     *
     */
    public static final int nodeType = 13;

    /**
     *
     * @param node
     * @param entityClass
     */
    public BTreeEntityIndex(Node node, Class<E> entityClass) {
        super(node, entityClass);
        initialize(node);
    }

    /**
     *
     * @param uuid
     * @param leftId
     * @param rightId
     * @return
     */
    public final boolean addFirstEntity(Uuid uuid, long leftId, long rightId) {
        //reset node
        clear();
        if (this.sizeOfEntry() < this.freeSpace()) {
            //push the uuid in page
            writeEntityUuid(0, uuid);
            //insert subPages
            writeSubPageId(0, leftId);
            writeSubPageId(1, rightId);
            incrementNumberOfEntries();
            return true;
        } else {
            return false;
        }//endif
    }

    /**
     *
     * @param uuid
     * @param pageId
     * @return
     */
    public boolean addEntity(Uuid uuid, long pageId) {
        int i;
        int total = this.readNumberOfEntries();
        int idx = 0;
        Uuid uuidObj;

        if ((total > 0) && (this.sizeOfEntry() < this.freeSpace())) {
            //locating position
            for (i = total; i > 0; i--) {
                uuidObj = this.readEntityUuid(i - 1);
                calculatedComparisons++;
                if (uuidObj.compareTo(uuid) < 0) {
                    idx = i;
                    break;
                } else {
                    //move subPageId
                    this.writeSubPageId(i + 1, this.readSubPageId(i));
                    this.writeEntityUuid(i, uuidObj);
                }//endif
            }//endfor
            this.writeEntityUuid(idx, uuid);
            this.writeSubPageId(idx + 1, pageId);
            //increment
            this.incrementNumberOfEntries();

            return true;
        } else {
            return false;
        }//endif	
    }

    /**
     *
     * @return
     */
    protected int freeSpace() {
        int num = this.readNumberOfEntries();
        if (num == 0) {
            return this.sizeOfArray() - BTreeEntityNode.sizeOfHeader() - this.sizeOfFeatures();
        } else {
            return this.sizeOfArray() - BTreeEntityNode.sizeOfHeader()
                    - this.sizeOfFeatures() - (num * this.sizeOfEntry());
        }//endif
    }

    /**
     *
     * @param uuid
     * @return
     */
    public int indexOfUuid(Uuid uuid) {
        // binary search
        Uuid uuidHalf;
        int half;
        int lower = 0;
        int upper = this.readNumberOfEntries() - 1;

        //find binary seach
        while (lower <= upper) {
            half = lower + ((upper - lower) / 2);
            uuidHalf = readEntityUuid(half);
            calculatedComparisons++;
            if (uuid.compareTo(uuidHalf) == 0) {
                return half;
            } else {
                calculatedComparisons++;
                if (uuid.compareTo(uuidHalf) < 0) {
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
    protected int getNodeType() {
        return nodeType;
    }

    /**
     *
     * @param uuid
     * @return
     */
    public int indexOfQualify(Uuid uuid) {
        // binary search
        Uuid uuidObj;
        int idx;
        int total = this.readNumberOfEntries();
        int half = 0;
        int lower = 0;
        int upper = total;
        //find binary seach
        while (lower < upper) {
            half = lower + ((upper - lower) / 2);
            uuidObj = readEntityUuid(half);
            calculatedComparisons++;
            if (uuid.compareTo(uuidObj) <= 0) {
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
        uuidObj = readEntityUuid(idx);
        while (uuid.compareTo(uuidObj) >= 0) {
            calculatedComparisons++;
            idx++;
            if (idx < total) {
                uuidObj = readEntityUuid(idx);
            } else {
                break;
            }
        }
        calculatedComparisons++;
        return idx;

    }

    /**
     *
     * @param node
     * @return
     */
    public static boolean matchNodeType(Node node) {
        return node.readNodeType() == BTreeEntityIndex.nodeType;
    }

    /**
     *
     * @param idx
     * @return
     */
    public Uuid readEntityUuid(int idx)
    {
	int pos = AbstractNode.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry()) + Node.sizeOfLong;
	return this.readUuid(pos);
    }

    /**
     *
     * @param idx
     * @return
     */
    public long readSubPageId(int idx) {
        int pos;
        if (idx == 0) {
            pos = BTreeEntityIndex.sizeOfHeader() + Node.sizeOfInteger;
        } else {
            pos = BTreeEntityIndex.sizeOfHeader() + this.sizeOfFeatures() + ((idx - 1) * this.sizeOfEntry());
        }//endif       
        return this.readLong(pos);
    }

    /**
     *
     * @return
     */
    @Override
    protected int sizeOfEntry() {
        return Node.sizeOfLong + Node.sizeOfUuid;
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
     * @param uuid
     */
    protected final void writeEntityUuid(int idx, Uuid uuid)
    {
	int pos = AbstractNode.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry()) + Node.sizeOfLong;
	this.writeUuid(pos, uuid);
    }

    /**
     *
     * @param idx
     * @param pageId
     */
    protected final void writeSubPageId(int idx, long pageId) {
        int pos;
        if (idx == 0) {
            pos = BTreeEntityIndex.sizeOfHeader() + Node.sizeOfInteger;
        } else {
            pos = BTreeEntityIndex.sizeOfHeader() + this.sizeOfFeatures() + ((idx - 1) * this.sizeOfEntry());
        }//endif
        this.writeLong(pos, pageId);
    }

}
