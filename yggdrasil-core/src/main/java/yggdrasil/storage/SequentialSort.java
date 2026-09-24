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

import yggdrasil.block.SequentialDescriptor;
import yggdrasil.block.SequentialSortNode;
import yggdrasil.meta.Sort;
import yggdrasil.meta.Uuid;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 *
 * @param <S>
 */

public abstract class SequentialSort<S extends Sort<S> & Comparable<S>>
        extends AbstractKeyStructure<S> {

    protected final PerformanceMeasurement averageForAdd = new AveragePerformance();
    protected final PerformanceMeasurement averageForFind = new AveragePerformance();

    /**
     *
     * @param workspace
     */
    public SequentialSort(Workspace workspace) {
        super(workspace);

        Session se = this.getWorkspace().openSession();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        new SequentialDescriptor(se.load(pageIdDescriptor));

        se.close();
    }

    @Override
    public boolean add(S key) {
        long time = System.nanoTime();
        SequentialSortNode<S> newNode;
        SequentialSortNode<S> end;

        Session se = this.getWorkspace().openSession();
        long diskAccess = se.getBlockAccess();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        SequentialDescriptor descriptor = new SequentialDescriptor(se.load(pageIdDescriptor));

        if (descriptor.readBeginPageId() == 0) {
            //create SequentialSortNode
            newNode = new SequentialSortNode<>(se.create(), this.getObjectClass());
            // Circularly link
            newNode.writePreviousPageId(newNode.getPageId());
            newNode.writeNextPageId(newNode.getPageId());

            descriptor.writeBeginPageId(newNode.getPageId());
            descriptor.writeEndPageId(newNode.getPageId());
        }//endif
        end = new SequentialSortNode<>(se.load(descriptor.readEndPageId()), this.getObjectClass());

        //adding object in end node
        if (end.addKey(key) == false) {
            // node is full, creating new SequentialSortNode
            newNode = new SequentialSortNode<>(se.create(), this.getObjectClass());
            //adding object
            newNode.addKey(key);
            // Circularly link
            newNode.writeNextPageId(descriptor.readBeginPageId());
            newNode.writePreviousPageId(end.getPageId());
            end.writeNextPageId(newNode.getPageId());
            SequentialSortNode<S> beginNode = new SequentialSortNode<>(se.load(descriptor.readBeginPageId()), this.getObjectClass());
            beginNode.writePreviousPageId(newNode.getPageId());
            //ajust new end node
            descriptor.writeEndPageId(newNode.getPageId());
        }

        //clean home		
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
    public long getRootPageId() {
        Session se = this.getWorkspace().openSession();
        long rootPageId = 0;
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        SequentialDescriptor descriptor = new SequentialDescriptor(se.load(pageIdDescriptor));
        rootPageId = descriptor.readBeginPageId();

        se.close();

        return rootPageId;
    }

    public S find(Uuid uuid) {
        long time = System.nanoTime();
        S key = null;
        Session se = this.getWorkspace().openSession();
        long diskAccess = se.getBlockAccess();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        SequentialDescriptor descriptor = new SequentialDescriptor(se.load(pageIdDescriptor));
        long actualPageId = descriptor.readBeginPageId();
        long firstPageId = actualPageId;

        if (actualPageId != 0) {

            do {
                SequentialSortNode<S> actualSeqNode = new SequentialSortNode<>(se.load(actualPageId), this.getObjectClass());

                key = actualSeqNode.findUuid(uuid);
                averageForFind.incrementVerification(actualSeqNode.getVerifications());
                actualPageId = actualSeqNode.readNextPageId();
            } while (key == null && actualPageId != firstPageId);

        }
        se.close();
        //statistic for add
        diskAccess = se.getBlockAccess() - diskAccess;
        averageForFind.incrementDiskAccess(diskAccess);
        time = System.nanoTime() - time;
        averageForFind.incrementTime(time);
        averageForFind.incrementMeasurement();
        return key;
    }

    /**
     *
     * @param key
     * @return
     */
    public Uuid find(S key) {
        long time = System.nanoTime();
        Uuid uuid = null;

        Session se = this.getWorkspace().openSession();
        long diskAccess = se.getBlockAccess();
        long pageIdDescriptor = se.findPageIdDescriptor(this.getClassUuid());
        SequentialDescriptor descriptor = new SequentialDescriptor(se.load(pageIdDescriptor));
        long actualPageId = descriptor.readBeginPageId();
        long firstPageId = actualPageId;

        if (actualPageId != 0) {
            do {
                SequentialSortNode<S> actualSeqNode = new SequentialSortNode<>(se.load(actualPageId), this.getObjectClass());

                uuid = actualSeqNode.findKey(key);
                averageForFind.incrementVerification(actualSeqNode.getVerifications());
                actualPageId = actualSeqNode.readNextPageId();
            } while (uuid == null && actualPageId != firstPageId);

        }
        se.close();
        //statistic for add
        diskAccess = se.getBlockAccess() - diskAccess;
        averageForFind.incrementDiskAccess(diskAccess);
        time = System.nanoTime() - time;
        averageForFind.incrementTime(time);
        averageForFind.incrementMeasurement();
        return uuid;
    }

    public PerformanceMeasurement getAverageForAdd() {
        return averageForAdd;
    }

    public PerformanceMeasurement getAverageForFind() {
        return averageForFind;
    }

    @Override
    public boolean remove(S key) {
        return false;
    }
}
