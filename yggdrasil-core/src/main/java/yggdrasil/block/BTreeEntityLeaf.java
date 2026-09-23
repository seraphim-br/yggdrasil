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
 * This class implements a B+ Tree Leaf Node.</p>
 *
 * @param <E>
 *
 * <blockquote><pre>
 * {@code
 * Design:
 *
 *                 +------------------------------------+
 *                 |   +---------------------------+    |
 *                 |   |   +------------------+    |    |
 *                 |   |   |                  |    |    |
 * +---------------|---|---|------------------V----V----V----+
 * |      |number|off|off|off|                |obj2|obj1|obj0|
 * |      |  of  |set|set|set|<--free space-->|              |
 * |      |entit.|[0]|[1]|[2]|                |              |
 * |header|feat. |  entries  |                |   entities   |
 * +---------------------------------------------------------+
 * }
 * </pre></blockquote>
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 */
public class BTreeEntityLeaf<E extends Entity<E>> extends BTreeEntityNode<E> {

    public static final int nodeType = 12;

    /**
     *
     * @param node
     * @param entityClass
     */
    public BTreeEntityLeaf(Node node, Class<E> entityClass) {
        super(node, entityClass);
        initialize(node);
    }

    /**
     *
     * @param entity
     * @return
     */
    public final boolean addFirstEntity(E entity) {
        int off;
        int size = entity.sizeOfEntity();
        //reset node
        clear();
        if (size + this.sizeOfEntry() < freeSpace()) {
            //insert offset
            off = this.sizeOfArray() - size;
            this.writeOffset(0, off);
            //push the entity in page
            entity.pushEntity(this.getArray(), off);
            this.incrementNumberOfEntries();
            return true;
        } else {
            return false;
        }//endif
    }

    /**
     *
     * @param entity
     * @return
     */
    public boolean addEntity(E entity) {
        int i;
        int total = this.readNumberOfEntries();
        int len, dest, src, idx = 0, off;
        int size = entity.sizeOfEntity();
        E obj;

        if ((total > 0) && (size + this.sizeOfEntry() < this.freeSpace())) {
            //positions dest and source
            dest = this.readOffset(total - 1) - size;
            src = this.readOffset(total - 1);

            //locating position
            for (i = total; i > 0; i--) {
                obj = this.buildEntity(i - 1);
                calculatedComparisons++;
                if (obj.getUuid().compareTo(entity.getUuid()) < 0) {
                    idx = i;
                    break;
                } else {
                    //move offset
                    this.writeOffset(i, (this.readOffset(i - 1) - size));
                }
            }
            //if is first position
            if (idx == 0) {
                len = this.sizeOfArray() - src;
                off = this.sizeOfArray() - size;
            } else {
                len = this.readOffset(idx - 1) - src;
                off = this.readOffset(idx - 1) - size;
            }//endif

            // move entities			
            this.move(dest, src, len);

            //offset
            this.writeOffset(idx, off);
            //push the entity in page
            entity.pushEntity(this.getArray(), off);
            this.incrementNumberOfEntries();

            return true;
        } else {
            return false;
        }//endif
    }

    /**
     *
     * @param idx
     * @return
     */
    public E buildEntity(int idx) {
        //instantiation by reflection
        if ((idx >= 0) && (idx < this.readNumberOfEntries())) {
            E entity = this.newGenericType();
            //pull the entity of the page
            entity.pullEntity(this.getArray(), this.readOffset(idx));
            return entity;
        }//endif

        return null;
    }

    /**
     *
     * @param uuid
     * @return
     */
    public int indexOfUuid(Uuid uuid) {
        // binary search
        E objHalf;
        int half;
        int lower = 0;
        int upper = this.readNumberOfEntries() - 1;

        //find binary seach
        while (lower <= upper) {
            half = lower + ((upper - lower) / 2);
            objHalf = buildEntity(half);
            calculatedComparisons++;
            if (uuid.compareTo(objHalf.getUuid()) == 0) {
                return half;
            } else {
                calculatedComparisons++;
                if (uuid.compareTo(objHalf.getUuid()) < 0) {
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
     * @param uuid
     * @return
     */
    public E findEntity(Uuid uuid) {
        // binary search
        E objHalf;
        int half;
        int lower = 0;
        int upper = this.readNumberOfEntries() - 1;

        //find binary seach
        while (lower <= upper) {
            half = lower + ((upper - lower) / 2);
            objHalf = buildEntity(half);
            calculatedComparisons++;
            if (uuid.compareTo(objHalf.getUuid()) == 0) {
                return objHalf;
            } else {
                calculatedComparisons++;
                if (uuid.compareTo(objHalf.getUuid()) < 0) {
                    upper = half - 1;
                } else {
                    lower = half + 1;
                }//endif
            }
        }//endwhile		
        return null;
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
                    - this.sizeOfFeatures() - (num * this.sizeOfEntry())
                    - (this.sizeOfArray() - this.readOffset(num - 1));
        }//endif
    }

    /**
     *
     * @return
     */
    @Override
    protected int getNodeType() {
        return BTreeEntityLeaf.nodeType;
    }

    /**
     *
     * @param node
     * @return
     */
    public static boolean matchNodeType(Node node) {
        return node.readNodeType() == BTreeEntityLeaf.nodeType;
    }

    /**
     *
     * @param idx
     * @return
     */
    protected int readOffset(int idx) {
        int pos = BTreeEntityNode.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry());
        return this.readInteger(pos);
    }

    /**
     *
     * @param idx
     * @return
     */
    public boolean remove(int idx) {
        int totalEntity = this.readNumberOfEntries();
        int dest, src, len, sizeEntity;

        if ((idx >= 0) && (idx < totalEntity)) {
            if (idx == 0) {
                sizeEntity = this.sizeOfArray() - this.readOffset(0);
            } else {
                sizeEntity = this.readOffset(idx - 1) - this.readOffset(idx);
            }
            src = this.readOffset(totalEntity - 1);
            dest = src + sizeEntity;
            len = this.readOffset(idx) - this.readOffset(totalEntity - 1);

            this.move(dest, src, len);

            for (int i = idx; i < (totalEntity - 1); i++) {
                this.writeOffset(i, this.readOffset(i + 1) + sizeEntity);
            }

            this.decrementNumberOfEntries();
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
        return Node.sizeOfInteger;
    }

    /**
     *
     * @return
     */
    @Override
    protected int sizeOfFeatures() {
        return Node.sizeOfInteger;
    }

    /**
     *
     * @param idx
     * @param off
     */
    protected void writeOffset(int idx, int off) {
        int pos = BTreeEntityNode.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry());
        this.writeInteger(pos, off);
    }

}
