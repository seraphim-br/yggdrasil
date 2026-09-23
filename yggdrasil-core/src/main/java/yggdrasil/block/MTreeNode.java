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
 *<p>This class implements an M-Tree Node.</p>
 * 
 * @param <K> 
 * 
 * <blockquote><pre>
 * {@code
 * Design:
 *
 * +--------------+--------+----------------------------------------------+
 * |node|prev|next| number |                                              |
 * |type|page|page|   of   |                                              |
 * |    | Id | Id |  keys  |<---------------- free space ---------------->|
 * |--------------+--------|                                              |
 * |   header     |features|                                              |
 * +--------------+--------+----------------------------------------------+
 * }
 * </pre></blockquote>
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 */
public abstract class MTreeNode<K extends Metric<K>> extends KeyNode<K>
{
    public static final double precisionError = 0.000000001d; //incrementa   
    protected long calculatedDistance;

    /**
     *
     * @param node
     * @param keyClass
     */
    public MTreeNode(Node node, Class<K> keyClass)
    {
	super(node, keyClass);
    }
    
    /**
     * Returns the number of distance calculations ({@Code compareTo}) made by this class.
     *
     * @return the number of distance calculations
     */
    public long getCalculatedDistance() {
        return calculatedDistance;
    }

    /**
     *
     * @param idx
     * @return
     */
    public K buildKey(int idx)
    {
	//instantiation by reflection
	if ((idx >= 0) && (idx < this.readNumberOfKeys()))
	{
            K key;
            if(this instanceof MTreeLeaf){
                MTreeLeaf<K> leaf = (MTreeLeaf<K>)this;
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
    public final void clear()
    {
	// Zering number of keys.
	this.writeInteger(MTreeNode.sizeOfHeader(), 0);
    }

    /**
     *
     */
    protected final void decrementNumberOfKeys()
    {
	this.writeInteger(MTreeNode.sizeOfHeader(), this.readNumberOfKeys() - 1);
    }

    /**
     *
     * @return
     */
    protected final int freeSpace()
    {
	int total = this.readNumberOfKeys();

	return total == 0
		? this.sizeOfArray() // Total
		- MTreeNode.sizeOfHeader() // Header
		- this.sizeOfFeatures() // Features

		: this.sizeOfArray() // Total
		- MTreeNode.sizeOfHeader() // Header
		- this.sizeOfFeatures() // Features
		- (total * this.sizeOfEntry()) // Entries
		- (this.sizeOfArray() - this.readOffset(total - 1)); // Keys
    }

    /**
     *
     */
    protected final void incrementNumberOfKeys()
    {
	this.writeInteger(MTreeNode.sizeOfHeader(), this.readNumberOfKeys() + 1);
    }

    /**
     *
     * @param idx
     * @return
     */
    public final double readDistanceToParent(int idx)
    {
	int pos = MTreeNode.sizeOfHeader()
		+ this.sizeOfFeatures()
		+ ((idx + 1) * this.sizeOfEntry())
		- MTreeNode.sizeOfInteger // off
		- MTreeNode.sizeOfDouble; // parent distance

	return this.readDouble(pos);
    }

    /**
     *
     * @return
     */
    @Override
    public final int readNumberOfKeys()
    {
	return this.readInteger(MTreeNode.sizeOfHeader());
    }

    /**
     *
     * @param idx
     * @return
     */
    protected final int readOffset(int idx)
    {
	int pos = MTreeNode.sizeOfHeader()
		+ this.sizeOfFeatures()
		+ ((idx + 1) * this.sizeOfEntry())
		- MTreeNode.sizeOfInteger; // offset

	return this.readInteger(pos);
    }

    /**
     *
     * @param idx
     * @return return true if key was removed.
     */
    public abstract boolean remove(int idx);

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
     * @param dist
     */
    protected void writeDistanceToParent(int idx, double dist)
    {
	int pos = MTreeNode.sizeOfHeader()
		+ this.sizeOfFeatures()
		+ ((idx + 1) * this.sizeOfEntry())
		- MTreeNode.sizeOfInteger // off
		- MTreeNode.sizeOfDouble; // parent distance

	this.writeDouble(pos, dist);
    }

    /**
     *
     * @param idx
     * @param off
     */
    protected final void writeOffset(int idx, int off)
    {
	int pos = MTreeNode.sizeOfHeader()
		+ this.sizeOfFeatures()
		+ ((idx + 1) * this.sizeOfEntry())
		- MTreeNode.sizeOfInteger; // offset

	this.writeInteger(pos, off);
    }
}
