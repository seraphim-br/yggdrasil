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

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import yggdrasil.block.MTreeDescriptor;
import yggdrasil.block.MTreeIndex;
import yggdrasil.block.MTreeLeaf;
import yggdrasil.block.MTreeNode;
import yggdrasil.block.Node;
import yggdrasil.meta.Entity;
import yggdrasil.meta.Metric;
import yggdrasil.meta.Uuid;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 *
 * @param <K>
 */
public abstract class MTree<K extends Metric<K>> extends AbstractKeyStructure<K> {

    public static final double precisionError = 0.0001d; //incrementa nona casa
    protected final Random rand = new Random(100);
    protected final PerformanceMeasurement averageForAdd = new AveragePerformance();
    protected final PerformanceMeasurement averageForFind = new AveragePerformance();

    /**
     *
     * @param workspace
     */
    public MTree(Workspace workspace) {
        super(workspace);
        Session se = this.getWorkspace().openSession();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        new MTreeDescriptor(se.load(pageIdDescriptor));
        se.close();
    }

    class MTreePromotion {

        private final K firstKey;
        private double firstCoveringRadius;
        private final long fistSubPageId;
        private final K secondKey;
        private double secondCoveringRadius;
        private final long secondSubPageId;

        public MTreePromotion(K firstKey, double firstCoveringRadius, long fistSubPageId, K secondKey, double secondCoveringRadius, long secondSubPageId) {
            this.firstKey = firstKey;
            this.firstCoveringRadius = firstCoveringRadius;
            this.fistSubPageId = fistSubPageId;
            this.secondKey = secondKey;
            this.secondCoveringRadius = secondCoveringRadius;
            this.secondSubPageId = secondSubPageId;
        }

        public double getFirstCoveringRadius() {
            return firstCoveringRadius;
        }

        public K getFirstKey() {
            return firstKey;
        }

        public long getFistSubPageId() {
            return fistSubPageId;
        }

        public double getSecondCoveringRadius() {
            return secondCoveringRadius;
        }

        public K getSecondKey() {
            return secondKey;
        }

        public long getSecondSubPageId() {
            return secondSubPageId;
        }

        public void setFirstCoveringRadius(double firstCoveringRadius) {
            this.firstCoveringRadius = firstCoveringRadius;
        }

        public void setSecondCoveringRadius(double secondCoveringRadius) {
            this.secondCoveringRadius = secondCoveringRadius;
        }
    }

    /**
     *
     */
    public class FindQualify implements Comparable<FindQualify> {

        private final long pageId;
        private final double distanceParentToPointQuery;

        /**
         *
         * @param pageId
         * @param distanceParentToPointQuery
         */
        public FindQualify(long pageId, double distanceParentToPointQuery) {
            this.pageId = pageId;
            this.distanceParentToPointQuery = distanceParentToPointQuery;
        }

        /**
         *
         * @return
         */
        public double getDistanceParentToPointQuery() {
            return distanceParentToPointQuery;
        }

        /**
         *
         * @return
         */
        public long getPageId() {
            return pageId;
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public int compareTo(FindQualify obj) {
            if (this.getDistanceParentToPointQuery() < obj.getDistanceParentToPointQuery()) {
                return -1;
            } else if (this.getDistanceParentToPointQuery() > obj.getDistanceParentToPointQuery()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public long getRootPageId() {
        Session se = this.getWorkspace().openSession();

        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        MTreeDescriptor descriptor = new MTreeDescriptor(se.load(pageIdDescriptor));
        long rootPageId = descriptor.readRootPageId();

        se.close();

        return rootPageId;
    }

    @Override
    public boolean add(K key) {
        long time = System.nanoTime();
        Session se = this.getWorkspace().openSession();
        long diskAccess = se.getBlockAccess();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        MTreeDescriptor descriptor = new MTreeDescriptor(se.load(pageIdDescriptor));
        long rootPageId = descriptor.readRootPageId();

        if (rootPageId == 0) // tree is empty
        {
            MTreeLeaf<K> leaf = new MTreeLeaf<>(se.create(), this.getObjectClass());
            leaf.addKey(key, Double.MAX_VALUE, key.getUuid());
            // Circularly link
            leaf.writePreviousPageId(leaf.getPageId());
            leaf.writeNextPageId(leaf.getPageId());
            descriptor.writeRootPageId(leaf.getPageId());
            descriptor.incTreeHeight();
            averageForAdd.incrementVerification(leaf.getCalculatedDistance());
        } else {
            long[] path = new long[descriptor.readTreeHeight()];
            Node node = se.load(rootPageId);
            double dist = Double.MAX_VALUE; // Distance to parent

            int level = 1;
            while (MTreeIndex.matchNodeType(node)) {
                MTreeIndex<K> index = new MTreeIndex<>(node, this.getObjectClass());
                path[level] = index.getPageId();
                level++;
                int qualify = index.indexOfInsertion(key);
                long subPageId = index.readSubPageId(qualify);
                dist = index.distanceToQualify(qualify, key);
                node = se.load(subPageId);
                averageForAdd.incrementVerification(index.getCalculatedDistance());
            }

            MTreeLeaf<K> leaf = new MTreeLeaf<>(node, this.getObjectClass());

            if (leaf.addKey(key, dist, key.getUuid()) == false) {
                int idxSub, idxParent;
                long pageIdSub;
                long pageIdActual;
                double distPromoParent1, distPromoParent2;
                double coveringSub = 0;
                boolean promote = true;

                MTreeIndex<K> actualIndex, parentIndex, newRoot;

                K parentKey;

                //split leaf
                MTreePromotion objPromote = splitLeaf(se, leaf, key, key.getUuid());

                level = path.length - 1;
                long parent = path[level];

                if (parent != 0) {
                    //pageId of the sub node
                    pageIdSub = leaf.getPageId();
                    actualIndex = new MTreeIndex<>(se.load(parent), this.getObjectClass());

                    //while (actualIndex != null)
                    while (level > 0) {
                        if (path[level - 1] != 0) {
                            //pageId of actual node
                            pageIdActual = actualIndex.getPageId();
                            parentIndex = new MTreeIndex<>(se.load(path[level - 1]), this.getObjectClass());

                            //calc dist
                            idxParent = parentIndex.indexOfSubPageId(pageIdActual);
                            parentKey = parentIndex.buildKey(idxParent);
                            distPromoParent1 = objPromote.getFirstKey().distanceTo(parentKey);
                            distPromoParent2 = objPromote.getSecondKey().distanceTo(parentKey);

                            averageForAdd.incrementVerification(parentIndex.getCalculatedDistance() + 2);
                        } else {
                            parentIndex = null;
                            distPromoParent1 = Double.MAX_VALUE;
                            distPromoParent2 = Double.MAX_VALUE;
                        }

                        //search the index 
                        idxSub = actualIndex.indexOfSubPageId(pageIdSub);
                        if (promote == true) {
                            //remove old promoted key
                            actualIndex.remove(idxSub);

                            if (actualIndex.addKey(objPromote.getFirstKey(), distPromoParent1, objPromote.getFistSubPageId(), objPromote.getFirstCoveringRadius(),
                                    objPromote.getSecondKey(), distPromoParent2, objPromote.getSecondSubPageId(), objPromote.getSecondCoveringRadius())) {
                                promote = false;
                            } else {
                                //split index
                                objPromote = this.splitIndex(se, actualIndex,
                                        objPromote.getFirstKey(), objPromote.getFistSubPageId(), objPromote.getFirstCoveringRadius(),
                                        objPromote.getSecondKey(), objPromote.getSecondSubPageId(), objPromote.getSecondCoveringRadius());

                                promote = true;
                            }//endif

                            double cov1 = distPromoParent1 + objPromote.getFirstCoveringRadius();
                            double cov2 = distPromoParent2 + objPromote.getSecondCoveringRadius();
                            if (cov1 > cov2) {
                                coveringSub = cov1;
                            } else {
                                coveringSub = cov2;
                            }
                        } else {
                            if (actualIndex.readCoveringRadius(idxSub) < coveringSub) {
                                actualIndex.writeCoveringRadius(idxSub, coveringSub);
                            }
                            coveringSub = actualIndex.readDistanceToParent(idxSub)
                                    + actualIndex.readCoveringRadius(idxSub);
                        }
                        pageIdSub = actualIndex.getPageId();

                        averageForAdd.incrementVerification(actualIndex.getCalculatedDistance());

                        //update actual
                        actualIndex = parentIndex;
                        level--;
                    }
                }
                if (promote == true) {
                    //promote
                    newRoot = new MTreeIndex<>(se.create(), this.getObjectClass());
                    newRoot.addKey(objPromote.getFirstKey(), Double.MAX_VALUE, objPromote.getFistSubPageId(), objPromote.getFirstCoveringRadius());
                    newRoot.addKey(objPromote.getSecondKey(), Double.MAX_VALUE, objPromote.getSecondSubPageId(), objPromote.getSecondCoveringRadius());
                    // Circularly link
                    newRoot.writePreviousPageId(newRoot.getPageId());
                    newRoot.writeNextPageId(newRoot.getPageId());
                    //ajust rootPageId										
                    descriptor.writeRootPageId(newRoot.getPageId());
                    descriptor.incTreeHeight();
                    averageForAdd.incrementVerification(newRoot.getCalculatedDistance());
                }

            }
            averageForAdd.incrementVerification(leaf.getCalculatedDistance());
        }
        se.close();
        //statistic for add
        diskAccess = se.getBlockAccess() - diskAccess;
        averageForAdd.incrementDiskAccess(diskAccess);
        time = System.nanoTime() - time;
        averageForAdd.incrementTime(time);
        averageForAdd.incrementMeasurement();
        return true;
    }

    @Override
    public Uuid find(K key) {
        long time = System.nanoTime();
        Session se = this.getWorkspace().openSession();
        long diskAccess = se.getBlockAccess();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        MTreeDescriptor descriptor = new MTreeDescriptor(se.load(pageIdDescriptor));
        int total;
        double distParentToQuery, dist, cov;
        FindQualify objQualify;
        K storedKey;
        Uuid uuidFinded = null;

        PriorityQueue<FindQualify> qualifies = new PriorityQueue<>();
        qualifies.add(new FindQualify(descriptor.readRootPageId(), Double.MAX_VALUE));
        do {
            //objQualify = qualifies.pop();
            objQualify = qualifies.poll();

            distParentToQuery = objQualify.getDistanceParentToPointQuery();

            Node node = se.load(objQualify.getPageId());

            if (MTreeIndex.matchNodeType(node)) {
                MTreeIndex<K> index = new MTreeIndex<>(node, this.getObjectClass());
                total = index.readNumberOfKeys();

                for (int i = 0; i < total; i++) {
                    cov = index.readCoveringRadius(i);

                    //triangle inequality 
                    if (distParentToQuery - index.readDistanceToParent(i) <= cov) {
                        storedKey = index.buildKey(i); // Key stored in the node.

                        averageForFind.incrementVerification(1);
                        dist = storedKey.distanceTo(key); // Distance from key to storedKey: d(Or, On)

                        if (dist <= cov) {
                            qualifies.add(new FindQualify(index.readSubPageId(i), dist));
                        }
                    }
                }

                averageForFind.incrementVerification(index.getCalculatedDistance());
            } else {
                MTreeLeaf<K> leaf = new MTreeLeaf<>(node, this.getObjectClass());

                total = leaf.readNumberOfKeys();

                for (int i = 0; i < total; i++) {
                    //triangle inequality 
                    if (leaf.readDistanceToParent(i) <= distParentToQuery) {
                        K objKey = leaf.buildKey(i);
                        averageForFind.incrementVerification(2);
                        dist = key.distanceTo(objKey);
                        if ((dist == 0) && (objKey.distanceTo(key) == 0)) {
                            uuidFinded = leaf.readEntityUuid(i);
                            break;
                        }
                    }

                }

                averageForFind.incrementVerification(leaf.getCalculatedDistance());
            }
        } while ((!qualifies.isEmpty()) && (uuidFinded == null));
        se.close();
        //statistic for add
        diskAccess = se.getBlockAccess() - diskAccess;
        averageForFind.incrementDiskAccess(diskAccess);
        time = System.nanoTime() - time;
        averageForFind.incrementTime(time);
        averageForFind.incrementMeasurement();
        return uuidFinded;
    }

    public PerformanceMeasurement getAverageForAdd() {
        return averageForAdd;
    }

    public PerformanceMeasurement getAverageForFind() {
        return averageForFind;
    }

    public int height() {
        Session se = this.getWorkspace().openSession();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        MTreeDescriptor descriptor = new MTreeDescriptor(se.load(pageIdDescriptor));
        int height = descriptor.readTreeHeight();
        se.close();
        return height;

    }

    protected MTreePromotion splitIndex(Session se, MTreeIndex<K> fullIndex,
            K firstKey, long firstSubId, double firstCov,
            K secondKey, long secondSubId, double secondCov) {
        int i, k1, k2;
        double dist1, dist2;
        boolean failAddKey;
        int total = fullIndex.readNumberOfKeys() + 2;
        MTreePromotion objPromote = null;
        MTreeIndex<K> newIndex = new MTreeIndex<>(se.create(), this.getObjectClass());

        //copy orded of keys and subPageId
        K vectKey[] = (K[]) new Metric[total];
        long vectSub[] = new long[total];
        double vectCoverage[] = new double[total];
        //coping key and subPageId
        for (i = 0; i < total - 2; i++) {
            vectKey[i] = fullIndex.buildKey(i);
            vectSub[i] = fullIndex.readSubPageId(i);
            vectCoverage[i] = fullIndex.readCoveringRadius(i);
        }//endfor
        //copy key, subPageId and coverage
        vectKey[i] = firstKey;
        vectSub[i] = firstSubId;
        vectCoverage[i] = firstCov;
        vectKey[i + 1] = secondKey;
        vectSub[i + 1] = secondSubId;
        vectCoverage[i + 1] = secondCov;

        do {
            k1 = rand.nextInt(total);
            do {
                k2 = rand.nextInt(total);
            } while (k1 == k2);
            //zering
            fullIndex.clear();
            newIndex.clear();
            //choose promotion
            failAddKey = false;

            objPromote = new MTreePromotion(vectKey[k1], 0d, fullIndex.getPageId(), vectKey[k2], 0d, newIndex.getPageId());

            //distributing keys
            for (i = 0; i < total; i++) {
                averageForAdd.incrementVerification(2);
                dist1 = objPromote.getFirstKey().distanceTo(vectKey[i]);
                dist2 = objPromote.getSecondKey().distanceTo(vectKey[i]);
                if (dist1 < dist2) {
                    if (fullIndex.addKey(vectKey[i], dist1, vectSub[i], vectCoverage[i])) {
                        //adjusting covering radius of the promoted Key1
                        if (objPromote.getFirstCoveringRadius() < (dist1 + vectCoverage[i])) {
                            objPromote.setFirstCoveringRadius(dist1 + vectCoverage[i]);
                        }
                    } else {
                        failAddKey = true;
                        break;
                    }
                } else if (newIndex.addKey(vectKey[i], dist2, vectSub[i], vectCoverage[i])) {
                    //adjusting covering radius of the promoted Key2
                    if (objPromote.getSecondCoveringRadius() < (dist2 + vectCoverage[i])) {
                        objPromote.setSecondCoveringRadius(dist2 + vectCoverage[i]);
                    }
                } else {
                    failAddKey = true;
                    break;
                }
            }//endfor
        } while (failAddKey);

        // Circularly linked list
        newIndex.writeNextPageId(fullIndex.readNextPageId());
        fullIndex.writeNextPageId(newIndex.getPageId());
        newIndex.writePreviousPageId(fullIndex.getPageId());
        long nextPageId = newIndex.readNextPageId();
        if (nextPageId != fullIndex.getPageId()) {
            MTreeIndex<K> nextIndex = new MTreeIndex<>(se.load(nextPageId), this.getObjectClass());
            nextIndex.writePreviousPageId(newIndex.getPageId());
            averageForAdd.incrementVerification(nextIndex.getCalculatedDistance());
        } else {
            fullIndex.writePreviousPageId(newIndex.getPageId());
        }
        averageForAdd.incrementVerification(newIndex.getCalculatedDistance());
        objPromote.setFirstCoveringRadius(objPromote.getFirstCoveringRadius() + MTree.precisionError);
        objPromote.setSecondCoveringRadius(objPromote.getSecondCoveringRadius() +  MTree.precisionError);

/*        
        int tot = fullIndex.readNumberOfKeys();
        K build;
        double distRep;
        double dToParente;
        double cov;
        for (i = 0; i < tot; i++) {
            build = fullIndex.buildKey(i);
            distRep = objPromote.getFirstKey().distanceTo(build);
            dToParente = fullIndex.readDistanceToParent(i);
            cov = fullIndex.readCoveringRadius(i);
            if(distRep != dToParente){
                System.out.println("[error-old] distante to parente in index");
            }
            if(objPromote.getFirstCoveringRadius() < distRep + cov){
                System.out.println("[error-old] covering radius in index");
            }
            if(distRep - dToParente > cov){
                System.out.println("[error-old] triangle inequality in index");
            }
        }
        tot = newIndex.readNumberOfKeys();
        for (i = 0; i < tot; i++) {
            build = newIndex.buildKey(i);
            distRep = objPromote.getSecondKey().distanceTo(build);
            dToParente = newIndex.readDistanceToParent(i);
            cov = newIndex.readCoveringRadius(i);
            if(distRep != dToParente){
                System.out.println("[error-new] distante to parente in index");
            }
            if(objPromote.getSecondCoveringRadius() < distRep + cov){
                System.out.println("[error-new] covering radius in index");
            }
            if(distRep - dToParente > cov){
                System.out.println("[error-new] triangle inequality in index");
            }
        }
*/        
        return objPromote;
    }
    
    private MTreePromotion splitLeaf(Session se, MTreeLeaf<K> fullLeaf, K key, Uuid uuid) {
        int i, k1, k2;
        double dist1, dist2;
        boolean failAddKey;
        int total = fullLeaf.readNumberOfKeys() + 1;
        MTreePromotion objPromote = null;
        MTreeLeaf<K> newLeaf = new MTreeLeaf<>(se.create(), this.getObjectClass());

        //copy all keys and subPageId
        Uuid[] vectUuid = new Uuid[total];
        K vectKey[] = (K[]) new Metric[total];
        //coping key and subPageId
        for (i = 0; i < total - 1; i++) {
            vectKey[i] = fullLeaf.buildKey(i);
            vectUuid[i] = fullLeaf.readEntityUuid(i);
        }//endfor
        //copy key and pageId in the correct position
        vectKey[i] = key;
        //copy the pageId in the correct position
        vectUuid[i] = uuid;

        do {
            k1 = rand.nextInt(total);
            do {
                k2 = rand.nextInt(total);
            } while (k1 == k2);
            //zering
            fullLeaf.clear();
            newLeaf.clear();
            //choose promotion
            failAddKey = false;

            objPromote = new MTreePromotion(vectKey[k1], 0d, fullLeaf.getPageId(), vectKey[k2], 0d, newLeaf.getPageId());

            //distributing keys
            for (i = 0; i < total; i++) {
                averageForAdd.incrementVerification(2);
                dist1 = objPromote.getFirstKey().distanceTo(vectKey[i]);
                dist2 = objPromote.getSecondKey().distanceTo(vectKey[i]);
                if (dist1 < dist2) {
                    if (fullLeaf.addKey(vectKey[i], dist1, vectUuid[i])) {
                        if (objPromote.getFirstCoveringRadius() < dist1) {
                            objPromote.setFirstCoveringRadius(dist1);
                        }
                    } else {
                        failAddKey = true;
                        break;
                    }
                } else if (newLeaf.addKey(vectKey[i], dist2, vectUuid[i])) {
                    if (objPromote.getSecondCoveringRadius() < dist2) {
                        objPromote.setSecondCoveringRadius(dist2);
                    }
                } else {
                    failAddKey = true;
                    break;
                }
            }//endfor
        } while (failAddKey);

        // Circularly link
        newLeaf.writeNextPageId(fullLeaf.readNextPageId());
        fullLeaf.writeNextPageId(newLeaf.getPageId());
        newLeaf.writePreviousPageId(fullLeaf.getPageId());
        long nextPageId = newLeaf.readNextPageId();
        if (nextPageId != fullLeaf.getPageId()) {
            MTreeLeaf<K> nextLeaf = new MTreeLeaf<>(se.load(nextPageId), this.getObjectClass());
            nextLeaf.writePreviousPageId(newLeaf.getPageId());
            averageForAdd.incrementVerification(nextLeaf.getCalculatedDistance());
        } else {
            fullLeaf.writePreviousPageId(newLeaf.getPageId());
        }
        averageForAdd.incrementVerification(newLeaf.getCalculatedDistance());
        objPromote.setFirstCoveringRadius(objPromote.getFirstCoveringRadius() + MTree.precisionError);
        objPromote.setSecondCoveringRadius(objPromote.getSecondCoveringRadius() + MTree.precisionError);

/*
        int tot = fullLeaf.readNumberOfKeys();
        K build;
        double distRep;
        double dToParente;
        for (i = 0; i < tot; i++) {
            build = fullLeaf.buildKey(i);
            distRep = objPromote.getFirstKey().distanceTo(build);
            dToParente = fullLeaf.readDistanceToParent(i);
            if(distRep != dToParente){
                System.out.println("[error-old] distante to parente in leaf");
            }
            if(objPromote.getFirstCoveringRadius() < distRep){
                System.out.println("[error-old] covering radius in leaf");
            }
            if(distRep > dToParente){
                System.out.println("[error-old] triangle inequality in leaf");
            }
        }
        tot = newLeaf.readNumberOfKeys();
        for (i = 0; i < tot; i++) {
            build = newLeaf.buildKey(i);
            distRep = objPromote.getSecondKey().distanceTo(build);
            dToParente = newLeaf.readDistanceToParent(i);
            if(distRep != dToParente){
                System.out.println("[error-new] distante to parente in leaf");
            }
            if(objPromote.getSecondCoveringRadius() < distRep){
                System.out.println("[error-new] covering radius in leaf");
            }
            if(distRep > dToParente){
                System.out.println("[error-new] triangle inequality in leaf");
            }
        }
*/
        return objPromote;
    }

    /**
     *
     */
    public void bfs() {
        LinkedList<MTreeNode<K>> fila = new LinkedList<>();

        Session se = this.getWorkspace().openSession();

        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        MTreeDescriptor descriptor = new MTreeDescriptor(se.load(pageIdDescriptor));

        Node nod = se.load(descriptor.readRootPageId());
        MTreeNode<K> node;

        if (MTreeIndex.matchNodeType(nod)) {
            node = new MTreeIndex<>(nod, this.getObjectClass());
        } else {
            node = new MTreeLeaf<>(nod, this.getObjectClass());
        }

        fila.addLast(node);

        while (!fila.isEmpty()) {
            MTreeNode<K> n = fila.removeFirst();

            int total = n.readNumberOfKeys();

            System.out.println("===== Page ID: " + n.getPageId() + " =====");
            for (int i = 0; i < total; i++) {
                K obj = (K) n.buildKey(i);
                System.out.print("Chave: " + obj);

                if (MTreeIndex.matchNodeType(n)) {
                    MTreeIndex<K> index = new MTreeIndex<>(n, this.getObjectClass());
                    System.out.print(" \tCovRad.: " + index.readCoveringRadius(i));
                    long sub = index.readSubPageId(i);
                    Node nod2 = se.load(sub);
                    MTreeNode<K> node2;

                    if (MTreeIndex.matchNodeType(nod2)) {
                        node2 = new MTreeIndex<>(nod2, this.getObjectClass());
                    } else {
                        node2 = new MTreeLeaf<>(nod2, this.getObjectClass());
                    }
                    fila.addLast(node2);

                    System.out.print(" \tDist.: " + index.readDistanceToParent(i)
                            + " \tSub.: " + index.readSubPageId(i) + "\n");
                } else {
                    MTreeLeaf<K> leaf = new MTreeLeaf<>(n, this.getObjectClass());

                    System.out.print(" \tDist.: " + leaf.readDistanceToParent(i)
                            + " \tUUID.: " + leaf.readEntityUuid(i) + "\n");
                }
            }
        }

        se.close();
    }

    /**
     * Remove key that has the same uuid.
     *
     * @param key
     * @return
     */
    @Override
    public boolean remove(K key) {
        Session se = this.getWorkspace().openSession();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        MTreeDescriptor descriptor = new MTreeDescriptor(se.load(pageIdDescriptor));
        int total;
        double distParentToQuery, dist, cov;
        FindQualify objQualify;
        K storedKey;
        boolean result = false;

        PriorityQueue<FindQualify> qualifies = new PriorityQueue<>();
        qualifies.add(new FindQualify(descriptor.readRootPageId(), Double.MAX_VALUE));
        do {
            objQualify = qualifies.poll();

            distParentToQuery = objQualify.getDistanceParentToPointQuery();

            Node node = se.load(objQualify.getPageId());

            if (MTreeIndex.matchNodeType(node)) {
                MTreeIndex<K> index = new MTreeIndex<>(node, this.getObjectClass());
                total = index.readNumberOfKeys();

                for (int i = 0; i < total; i++) {
                    cov = index.readCoveringRadius(i);

                    //triangle inequality 
                    if (distParentToQuery - index.readDistanceToParent(i) <= cov) {
                        storedKey = index.buildKey(i); // Key stored in the node.

                        dist = storedKey.distanceTo(key); // Distance from key to storedKey: d(Or, On)

                        if (dist <= cov) {
                            qualifies.add(new FindQualify(index.readSubPageId(i), dist));
                        }
                    }
                }
            } else {
                MTreeLeaf<K> leaf = new MTreeLeaf<>(node, this.getObjectClass());

                total = leaf.readNumberOfKeys();

                for (int i = 0; i < total; i++) {
                    //triangle inequality 
                    if (leaf.readDistanceToParent(i) == distParentToQuery) {
                        K objKey = leaf.buildKey(i);
                        if (key.hasSameKey(objKey)) {
                            Uuid uuidObjKey = leaf.readEntityUuid(i);
                            if (((Entity<?>) key).getUuid().equals(uuidObjKey)) {
                                result = leaf.remove(i);
                                if (result == true) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } while ((!qualifies.isEmpty()) && (!result));
        se.close();
        return result;
    }
}
