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
 * 
 * @param <E>
 * 
 *            <blockquote>
 * 
 *            <pre>
 * {@code
 * Design:
 *
 *                  +-----------------------------------------+
 *                  |     +------------------------------+    |
 *                  |     |     +-------------------+    |    |
 *                  |     |     |                   |    |    |
 * +----------------|-----|-----|-------------------V----V----V----+
 * |      |number| off | off | off |                |obj2|obj1|obj0|
 * |      |  of  | set | set | set |                |              |
 * |      | keys | [0] | [1] | [2] |                |              |
 * |header| feat.|     entries     |<--free space-->|    objects   |
 * +---------------------------------------------------------------+
 * }
 * </pre>
 * 
 *            </blockquote>
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 */
public class SequentialMetricNode<M extends Metric<M>> extends KeyNode<M> {
    protected static int sizeOfFeatures = SequentialMetricNode.sizeOfLong + SequentialMetricNode.sizeOfInteger;
    protected static int sizeOfEntry = SequentialMetricNode.sizeOfInteger;
    public static final int nodeType = 3;
    protected long verifications = 0;

    public SequentialMetricNode(Node node, Class<M> clazz) {
        super(node, clazz);
        initialize(node);
    }

    public long getVerifications() {
        return verifications;
    }

    public boolean addKey(M key) {
        int off;
        int idx = this.readNumberOfKeys();
        int size = key.sizeOfKey();
        if (size + sizeOfEntry < freeSpace()) {
            // if is first position
            if (idx == 0) {
                off = this.sizeOfArray() - size;
            } else {
                off = readOffset(idx - 1) - size;
            } // endif
              // position
            writeOffset(idx, off);
            // push the key in page
            key.pushKey(this.getArray(), off);
            // increment
            incrementNumberKeys();
            return true;
        } else {
            return false;
        } // endif
    }

    /**
     *
     * @param idx
     * @return
     */
    public M buildKey(int idx) {
        // instantiation by reflection
        if ((idx >= 0) && (idx < this.readNumberOfKeys())) {
            M key = this.newGenericType();
            key.pullKey(this.getArray(), this.readOffset(idx));
            return key;
        } // endif

        return null;
    }

    /**
     *
     * @param idx
     * @param key
     */
    public void rebuildKey(int idx, M key) {
        // instantiation by reflection
        if ((idx >= 0) && (idx < this.readNumberOfKeys())) {
            key.pullKey(this.getArray(), this.readOffset(idx));
        } // endif
    }

    /**
     *
     */
    @Override
    public void clear() {
        // zering next page
        int pos = SequentialMetricNode.sizeOfHeader();
        this.writeInteger(pos, 0);
        // zering numberOfKey
        pos = SequentialMetricNode.sizeOfHeader() + SequentialMetricNode.sizeOfLong;
        this.writeInteger(pos, 0);
    }

    /**
     *
     */
    protected void decrementNumberOfKeys() {
        int num = this.readNumberOfKeys() + 1;
        int pos = SequentialMetricNode.sizeOfHeader() + SequentialMetricNode.sizeOfLong;
        this.writeInteger(pos, num);
    }

    /**
     *
     * @param key
     * @return
     */
    public Uuid findKey(M key) {
        int i;
        int total = this.readNumberOfKeys();
        M objSer;
        for (i = 0; i < total; i++) {
            objSer = buildKey(i);
            verifications++;
            if (objSer.hasSameKey(key) == true) {
                return objSer.getUuid();
            } // endif
        } // endfor
        return null;
    }

    /**
     *
     * @param uuid
     * @return
     */
    public M findUuid(Uuid uuid) {
        int i;
        int total = this.readNumberOfKeys();
        M objSer;
        for (i = 0; i < total; i++) {
            objSer = buildKey(i);
            verifications++;
            if (objSer.getUuid().equals(uuid)) {
                return objSer;
            } // endif
        } // endfor
        return null;
    }

    /**
     *
     * @return
     */
    protected int freeSpace() {
        int size = this.readNumberOfKeys();
        if (size == 0) {
            return this.sizeOfArray() - SequentialMetricNode.sizeOfHeader();
        } else {
            return this.sizeOfArray() - SequentialMetricNode.sizeOfHeader()
                    - SequentialMetricNode.sizeOfFeatures - (size * SequentialMetricNode.sizeOfEntry)
                    - (this.sizeOfArray() - this.readOffset(size - 1));
        } // endif
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
     */
    protected void incrementNumberKeys() {
        int num = this.readNumberOfKeys() + 1;
        int pos = SequentialMetricNode.sizeOfHeader() + SequentialMetricNode.sizeOfLong;
        this.writeInteger(pos, num);
    }

    /**
     *
     * @param node
     * @return
     */
    public static boolean matchNodeType(Node node) {
        return node.readNodeType() == SequentialMetricNode.nodeType;
    }

    /**
     *
     * @return
     */
    public int readNumberOfKeys() {
        int pos = SequentialMetricNode.sizeOfHeader() + SequentialMetricNode.sizeOfLong;
        return this.readInteger(pos);
    }

    /**
     *
     * @param idx
     * @return
     */
    protected int readOffset(int idx) {
        int pos = SequentialMetricNode.sizeOfHeader() + SequentialMetricNode.sizeOfFeatures + (idx * SequentialMetricNode.sizeOfEntry);
        return this.readInteger(pos);
    }

    /**
     *
     * @param idx
     * @param off
     */
    protected void writeOffset(int idx, int off) {
        int pos = SequentialMetricNode.sizeOfHeader() + SequentialMetricNode.sizeOfFeatures + (idx * SequentialMetricNode.sizeOfEntry);
        this.writeInteger(pos, off);
    }

}
