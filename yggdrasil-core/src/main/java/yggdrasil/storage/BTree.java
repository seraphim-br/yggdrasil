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
package yggdrasil.storage;

import yggdrasil.block.BTreeDescriptor;
import yggdrasil.block.BTreeIndex;
import yggdrasil.block.BTreeLeaf;
import yggdrasil.block.Node;
import yggdrasil.meta.Entity;
import yggdrasil.meta.Uuid;
import yggdrasil.meta.Sort;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 *
 * @param <K>
 */
public abstract class BTree<K extends Sort<K> & Comparable<K>> extends AbstractKeyStructure<K> {

    private PerformanceMeasurement averageForAdd = new AveragePerformance();
    private PerformanceMeasurement averageForFind = new AveragePerformance();

    /**
     *
     * @param workspace
     */
    public BTree(Workspace workspace) {
        super(workspace);

        Session se = this.getWorkspace().openSession();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        new BTreeDescriptor(se.load(pageIdDescriptor));
        se.close();
    }

    @Override
    public boolean add(K key) {
        long time = System.nanoTime();
        // Open descriptor
        Session se = this.getWorkspace().openSession();
        long diskAccess = se.getBlockAccess();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        BTreeDescriptor descriptor = new BTreeDescriptor(se.load(pageIdDescriptor));
        long rootPageId = descriptor.readRootPageId();
        long parent;
        // If root does not exist
        if (rootPageId == 0) {
            // Add first leaf
            BTreeLeaf<K> newLeaf = new BTreeLeaf<>(se.create(), this.getObjectClass());
            newLeaf.addFirstKey(key, key.getUuid());
            // Circularly link
            newLeaf.writePreviousPageId(newLeaf.getPageId());
            newLeaf.writeNextPageId(newLeaf.getPageId());
            // Adjust rootPageId
            descriptor.writeRootPageId(newLeaf.getPageId());
            averageForAdd.incrementVerification(newLeaf.getCalculatedComparisons());
            descriptor.incTreeHeight();
        } else {
            long[] path = new long[descriptor.readTreeHeight()];
            Node node = se.load(rootPageId);
            BTreeIndex<K> index;

            int level = 1;
            while (BTreeIndex.matchNodeType(node)) {
                index = new BTreeIndex<>(node, this.getObjectClass());
                path[level] = index.getPageId();
                level++;
                long sub = index.readSubPageId(index.indexOfQualify(key));
                node = se.load(sub);
                averageForAdd.incrementVerification(index.getCalculatedComparisons());
            }

            BTreeLeaf<K> leaf = new BTreeLeaf<>(node, this.getObjectClass());

//            if (leaf.indexOfKey(key) == -1) {
            if (leaf.addKey(key, key.getUuid()) == false) {
                // Split

                boolean promote = true;
                BTreeIndex<K> newIndex;
                node = leaf;
                // Split leaf
                BTreePromotion objPromote = splitLeaf(leaf, key, key.getUuid(), se);

                level = path.length - 1;

                while (level > 0) {
                    parent = path[level];
                    node = se.load(parent);

                    if (promote == true) {
                        //add in index
                        index = new BTreeIndex<>(node, this.getObjectClass());
                        if (index.addKey(objPromote.getKey(), objPromote.getSubPageId()) == false) {
                            // Split index
                            objPromote = this.splitIndex(index, objPromote.getKey(), objPromote.getSubPageId(), se);
                            promote = true;
                        } else {
                            promote = false;
                        }
                        averageForAdd.incrementVerification(index.getCalculatedComparisons());
                    }
                    level--;
                }

                if (promote == true) {
                    // Promote
                    newIndex = new BTreeIndex<>(se.create(), this.getObjectClass());
                    newIndex.addFirstKey(objPromote.getKey(), node.getPageId(), objPromote.getSubPageId());
                    // Circularly link
                    newIndex.writePreviousPageId(newIndex.getPageId());
                    newIndex.writeNextPageId(newIndex.getPageId());
                    descriptor.writeRootPageId(newIndex.getPageId());
                    descriptor.incTreeHeight();
                    averageForAdd.incrementVerification(newIndex.getCalculatedComparisons());
                }
            }
            averageForAdd.incrementVerification(leaf.getCalculatedComparisons());
//            } else {
//                return false;
//            }
        }

        se.close();
        //statistic for add
        diskAccess = se.getBlockAccess() - diskAccess;
        this.averageForAdd.incrementDiskAccess(diskAccess);
        time = System.nanoTime() - time;
        this.averageForAdd.incrementTime(time);
        this.averageForAdd.incrementMeasurement();
        return true;
    }

    @Override
    public Uuid find(K key) {
        long time = System.nanoTime();
        Session se = this.getWorkspace().openSession();
        long diskAccess = se.getBlockAccess();

        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        BTreeDescriptor descriptor = new BTreeDescriptor(se.load(pageIdDescriptor));
        long rootPageId = descriptor.readRootPageId();
        Uuid uuidFinded = null;
        BTreeLeaf<K> leaf;
        BTreeIndex<K> index;
        Node node;
        if (rootPageId != 0) {
            node = se.load(rootPageId);
            while (BTreeIndex.matchNodeType(node)) {
                index = new BTreeIndex<>(node, this.getObjectClass());
                long sub = index.readSubPageId(index.indexOfQualify(key));
                node = se.load(sub);
                averageForFind.incrementVerification(index.getCalculatedComparisons());
            }//endwhile
            leaf = new BTreeLeaf<>(node, this.getObjectClass());
            int idx = leaf.indexOfKey(key);
            if (idx >= 0) {
                uuidFinded = leaf.readEntityUuid(idx);
            }
            averageForFind.incrementVerification(leaf.getCalculatedComparisons());
        }
        se.close();
        //statistic for add
        diskAccess = se.getBlockAccess() - diskAccess;
        this.averageForFind.incrementDiskAccess(diskAccess);
        time = System.nanoTime() - time;
        this.averageForFind.incrementTime(time);
        this.averageForFind.incrementMeasurement();
        return uuidFinded;
    }

    /**
     *
     * @return
     */
    public PerformanceMeasurement getAverageForAdd() {
        return averageForAdd;
    }

    /**
     *
     * @return
     */
    public PerformanceMeasurement getAverageForFind() {
        return averageForFind;
    }

    @Override
    public long getRootPageId() {
        Session se = this.getWorkspace().openSession();
        long rootPageId = 0;

        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        BTreeDescriptor descriptor = new BTreeDescriptor(se.load(pageIdDescriptor));
        rootPageId = descriptor.readRootPageId();

        se.close();

        return rootPageId;
    }

    /**
     *
     * Remove key that has the same uuid. Review this method.
     *
     * @param key
     * @return
     */
    @Override
    public boolean remove(K key) {
        Node node;
        boolean result = false;
        BTreeLeaf<K> leaf;
        BTreeIndex<K> index;

        Session se = this.getWorkspace().openSession();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        BTreeDescriptor descriptor = new BTreeDescriptor(se.load(pageIdDescriptor));
        long rootPageId = descriptor.readRootPageId();

        if (rootPageId != 0) {

            node = se.load(descriptor.readRootPageId());

            while (BTreeIndex.matchNodeType(node)) {
                index = new BTreeIndex<>(node, this.getObjectClass());
                long sub = index.readSubPageId(index.indexOfQualify(key));
                node = se.load(sub);

            }//endwhile
            leaf = new BTreeLeaf<>(node, this.getObjectClass());
            int idx = leaf.indexOfKey(key);
            Uuid uuidObjKey = leaf.readEntityUuid(idx);
            if (((Entity<?>) key).getUuid().equals(uuidObjKey)) {
                result = leaf.remove(idx);
            }
        }
        se.close();
        return result;
    }

    public int height() {
        int height = 0;
        Session se = this.getWorkspace().openSession();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        BTreeDescriptor descriptor = new BTreeDescriptor(se.load(pageIdDescriptor));
        height = descriptor.readTreeHeight();
        se.close();
        return height;
    }

    private BTreePromotion splitIndex(BTreeIndex<K> fullIndex, K key, long pageId, Session se) {
        int i;
        int total = fullIndex.readNumberOfKeys() + 1;
        int half = ((int) (total / 2));
        BTreeIndex<K> newIndex = new BTreeIndex<>(se.create(), this.getObjectClass());

        // Copy orded of keys and subPageId
        long vectSub[] = new long[total + 1];
        @SuppressWarnings("unchecked")
        K vectKey[] = (K[]) new Sort[total];
        // Copy key and subPageId
        for (i = 0; i < total - 1; i++) {
            vectKey[i] = fullIndex.buildKey(i);
            vectSub[i] = fullIndex.readSubPageId(i);
        }
        // Last subPageId
        vectSub[i] = fullIndex.readSubPageId(i);
        // Moving for find position of the key
        while (i > 0) {
            averageForAdd.incrementVerification(1);
            if (key.compareTo(vectKey[i - 1]) < 0) {
                // Copy the key and pageId in the last position
                vectKey[i] = vectKey[i - 1];
                vectSub[i + 1] = vectSub[i];
                i--;
            } else {
                break;
            }
        }
        // Copy key and pageId in the correct position
        vectKey[i] = key;
        // Copy the pageId in the correct position
        vectSub[i + 1] = pageId;
        // Copy lower half of keys and subPageId
        fullIndex.addFirstKey(vectKey[0], vectSub[0], vectSub[1]);
        for (i = 1; i < half; i++) {
            fullIndex.addKey(vectKey[i], vectSub[i + 1]);
        }
        // Copy upper half of keys and subPageId
        newIndex.addFirstKey(vectKey[half + 1], vectSub[half + 1], vectSub[half + 2]);

        for (i = half + 1; i < total; i++) {
            newIndex.addKey(vectKey[i], vectSub[i + 1]);
        }

        BTreePromotion promote = new BTreePromotion(vectKey[half], newIndex.getPageId());

        // Circularly link
        newIndex.writeNextPageId(fullIndex.readNextPageId());
        fullIndex.writeNextPageId(newIndex.getPageId());
        newIndex.writePreviousPageId(fullIndex.getPageId());
        long nextPageId = newIndex.readNextPageId();
        if (nextPageId != fullIndex.getPageId()) {
            BTreeIndex<K> nextIndex = new BTreeIndex<>(se.load(nextPageId), this.getObjectClass());
            nextIndex.writePreviousPageId(newIndex.getPageId());
        } else {
            fullIndex.writePreviousPageId(newIndex.getPageId());
        }

        return promote;
    }

    private BTreePromotion splitLeaf(BTreeLeaf<K> fullLeaf, K key, Uuid uuid, Session se) {
        int i;
        int total = fullLeaf.readNumberOfKeys() + 1;
        int half = ((int) (total / 2));
        BTreeLeaf<K> newLeaf = new BTreeLeaf<>(se.create(), this.getObjectClass());

        // Copy all keys and uuid
        Uuid vecUuid[] = new Uuid[total];
        @SuppressWarnings("unchecked")
        K vectKey[] = (K[]) new Sort[total];
        // Copy key and uuid
        for (i = 0; i < total - 1; i++) {
            vectKey[i] = fullLeaf.buildKey(i);
            vecUuid[i] = fullLeaf.readEntityUuid(i);
        }
        // Moving for find position of the key
        while (i > 0) {
            averageForAdd.incrementVerification(1);
            if (key.compareTo(vectKey[i - 1]) < 0) {
                // Copy the key and pageId in the last position
                vectKey[i] = vectKey[i - 1];
                vecUuid[i] = vecUuid[i - 1];
                i--;
            } else {
                break;
            }
        }
        // Copy key and pageId in the correct position
        vectKey[i] = key;
        // Copy the pageId in the correct position
        vecUuid[i] = uuid;
        // Copy lower half of keys and uuid		
        fullLeaf.addFirstKey(vectKey[0], vecUuid[0]);
        for (i = 1; i < half; i++) {
            fullLeaf.addKey(vectKey[i], vecUuid[i]);
        }
        // Copy upper half of keys and uuid
        newLeaf.addFirstKey(vectKey[half], vecUuid[half]);
        for (i = half + 1; i < total; i++) {
            newLeaf.addKey(vectKey[i], vecUuid[i]);
        }

        BTreePromotion promote = new BTreePromotion(vectKey[half], newLeaf.getPageId());

        // Circularly link
        newLeaf.writeNextPageId(fullLeaf.readNextPageId());
        fullLeaf.writeNextPageId(newLeaf.getPageId());
        newLeaf.writePreviousPageId(fullLeaf.getPageId());
        long nextPageId = newLeaf.readNextPageId();
        if (nextPageId != fullLeaf.getPageId()) {
            BTreeLeaf<K> nextLeaf = new BTreeLeaf<>(se.load(nextPageId), this.getObjectClass());
            nextLeaf.writePreviousPageId(newLeaf.getPageId());
        } else {
            fullLeaf.writePreviousPageId(newLeaf.getPageId());
        }

        return promote;
    }

    class BTreePromotion {

        private K key;
        private long subPageId;

        public BTreePromotion(K key, long subPageId) {
            this.key = key;
            this.subPageId = subPageId;
        }

        public K getKey() {
            return key;
        }

        public long getSubPageId() {
            return subPageId;
        }
    }
}
