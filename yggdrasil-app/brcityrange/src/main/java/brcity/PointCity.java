package brcity;

import yggdrasil.block.Page;
import yggdrasil.block.PullPage;
import yggdrasil.block.PushPage;
import yggdrasil.meta.DistanceUtil;
import yggdrasil.meta.Point;
import yggdrasil.meta.Uuid;

public class PointCity extends EntityCity implements Point<PointCity> {
 
    protected Uuid uuid = Uuid.generator();
    public static Uuid classId = Uuid.fromString("26126211-C189-161E-9885-3580B7768D57");
    private double preservedDistance;

    @Override
    public Uuid getUuid() {
        return this.uuid;
    }

    @Override
    public boolean hasSameKey(PointCity obj) {
        int i = 0;
        while ((i < this.numberOfDimensions()) && (this.getOrigin(i) == obj.getOrigin(i))) {
            i++;
        }
        return i == this.numberOfDimensions();
    }

    @Override
    public double getOrigin(int axis) {
        switch (axis) {
            case 0:
                return getLatitude();
            case 1:
                return getLongitude();
            default:
                return 0;
        }
    }

    @Override
    public void setOrigin(int axis, double value) {
        switch (axis) {
            case 0:
                setLatitude(value);
                break;
            case 1:
                setLongitude(value);
                break;
        }
    }

    @Override
    public double getPreservedDistance() {
        return this.preservedDistance;
    }

    @Override
    public int numberOfDimensions() {
        return 2;
    }

    @Override
    public void setPreservedDistance(double distance) {
        this.preservedDistance = distance;
    }

    @Override
    public double distanceTo(PointCity obj) {
        return DistanceUtil.euclidean(this, obj);
    }

    @Override
    public boolean pullKey(byte[] array, int position) {
        PullPage pull = new PullPage(array, position);
        for (int i = 0; i < this.numberOfDimensions(); i++) {
            this.setOrigin(i, pull.pullDouble());
        }
        return true;
    }

    @Override
    public void pushKey(byte[] array, int position) {
        PushPage push = new PushPage(array, position);
        for (int i = 0; i < this.numberOfDimensions(); i++) {
            push.pushDouble(this.getOrigin(i));
        }
    }

    @Override
    public int sizeOfKey() {
        return Page.sizeOfDouble * this.numberOfDimensions();
    }
}