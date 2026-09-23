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
import yggdrasil.meta.Uuid;

/**
 * <p>This class implements a B+ Tree Leaf Node.</p>
 * 
 * @param <K> 
 * 
 * <blockquote><pre>
 * {@code
 * Design:
 *
 *                      +----------------------------------------------+
 *                      |        +--------------------------------+    |
 *                      |        |        +------------------+    |    |
 *                      |        |        |                  |    |    |
 * +--------------------|--------|--------|------------------V----V----V----+
 * |      |number|ent.|off|ent.|off|ent.|off|                |obj2|obj1|obj0|
 * |      |  of  |uuid|set|uuid|set|uuid|set|<--free space-->|              |
 * |      | keys |entry[0]|entry[1]|entry[2]|                |              |
 * |header|feat. |          entries         |                |     keys     |
 * +------------------------------------------------------------------------+
 * }
 * </pre></blockquote>
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 */
public class BTreeLeaf<K extends Sort<K> & Comparable<K>> extends BTreeNode<K>
{

    public static final int nodeType = 4;

    /**
     *
     * @param node
     * @param keyClass
     */
    public BTreeLeaf(Node node, Class<K> keyClass)
    {
	super(node, keyClass);
        initialize(node);
    }

    /**
     *
     * @param key
     * @param uuid
     * @return
     */
    public final boolean addFirstKey(K key, Uuid uuid)
    {
	int off;
	int size = key.sizeOfKey();
	//reset node
	clear();
	if (size + this.sizeOfEntry() < freeSpace())
	{
	    //insert offset
	    off = this.sizeOfArray() - size;
	    this.writeOffset(0, off);
	    //push the key in page
	    key.pushKey(this.getArray(), off);
	    //insert subPages
	    this.writeEntityUuid(0, uuid);
	    this.incrementNumberOfKey();
	    return true;
	}
	else
	{
	    return false;
	}//endif
    }

    /**
     *
     * @param key
     * @param uuid
     * @return
     */
    public boolean addKey(K key, Uuid uuid)
    {
	int i;
	int total = this.readNumberOfKeys();
	int len, dest, src, idx = 0, off;
	int size = key.sizeOfKey();
	K obj;

	if ((total > 0) && (size + this.sizeOfEntry() < this.freeSpace()))
	{
	    //positions dest and source
	    dest = this.readOffset(total - 1) - size;
	    src = this.readOffset(total - 1);

	    //locating position
	    for (i = total; i > 0; i--)
	    {
		obj = this.buildKey(i - 1);
                calculatedComparisons++;
		if (obj.compareTo(key) < 0)
		{
		    idx = i;
		    break;
		}
		else
		{
		    //move offset
		    this.writeOffset(i, (this.readOffset(i - 1) - size));
		    //move subPageId
		    this.writeEntityUuid(i, this.readEntityUuid(i - 1));
		}
	    }
	    //if is first position
	    if (idx == 0)
	    {
		len = this.sizeOfArray() - src;
		off = this.sizeOfArray() - size;
	    }
	    else
	    {
		len = this.readOffset(idx - 1) - src;
		off = this.readOffset(idx - 1) - size;
	    }//endif

	    // move keys			
	    this.move(dest, src, len);

	    //offset
	    this.writeOffset(idx, off);
	    this.writeEntityUuid(idx, uuid);
	    //push the key in page
	    key.pushKey(this.getArray(), off);
	    this.incrementNumberOfKey();

	    return true;
	}
	else
	{
	    return false;
	}//endif
    }

    /**
     *
     * @return
     */
    @Override
    protected int getNodeType()
    {
	return BTreeLeaf.nodeType;
    }

    /**
     *
     * @param node
     * @return
     */
    public static boolean matchNodeType(Node node)
    {
	return node.readNodeType() == BTreeLeaf.nodeType;
    }

    /**
     *
     * @param idx
     * @return
     */
    public Uuid readEntityUuid(int idx)
    {
	int pos = AbstractNode.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry());
	return this.readUuid(pos);
    }

    /**
     *
     * @param idx
     * @return
     */
    public boolean remove(int idx)
    {
	int totalKey = this.readNumberOfKeys();
	int dest, src, len, sizeKey;

	if ((idx >= 0) && (idx < totalKey))
	{
	    if (idx == 0)
	    {
		sizeKey = this.sizeOfArray() - this.readOffset(0);
	    }
	    else
	    {
		sizeKey = this.readOffset(idx - 1) - this.readOffset(idx);
	    }
	    src = this.readOffset(totalKey - 1);
	    dest = src + sizeKey;
	    len = this.readOffset(idx) - this.readOffset(totalKey - 1);

	    this.move(dest, src, len);

	    for (int i = idx; i < (totalKey - 1); i++)
	    {
		this.writeOffset(i, this.readOffset(i + 1) + sizeKey);
		this.writeEntityUuid(i, this.readEntityUuid(i + 1));
	    }

	    this.decrementNumberOfKey();
	    return true;
	}
	else
	{
	    return false;
	}
    }

    /**
     *
     * @return
     */
    @Override
    protected int sizeOfEntry()
    {
	return Node.sizeOfUuid + Node.sizeOfInteger;
    }

    /**
     *
     * @return
     */
    @Override
    protected int sizeOfFeatures()
    {
	return Node.sizeOfInteger;
    }

    /**
     *
     * @param idx
     * @param uuid
     */
    protected final void writeEntityUuid(int idx, Uuid uuid)
    {
	int pos = AbstractNode.sizeOfHeader() + this.sizeOfFeatures() + (idx * this.sizeOfEntry());
	this.writeUuid(pos, uuid);
    }
}
