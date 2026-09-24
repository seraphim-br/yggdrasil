package brcity;

import yggdrasil.block.Page;
import yggdrasil.block.PullPage;
import yggdrasil.block.PushPage;
import yggdrasil.meta.Entity;
import yggdrasil.meta.Uuid;

public class EntityCity extends City implements Entity<EntityCity>{
    protected Uuid uuid = Uuid.generator();
    public static Uuid classId = Uuid.fromString("26126211-C189-161E-9885-3580B7768D57");

    @Override
    public boolean isEqual(EntityCity obj) {
        return (((this.getName() == null) && (obj.getName() == null))
                || ((this.getName() != null) && (obj.getName() != null) && (this.getName().equals(obj.getName()))))
                && (this.getLatitude() == obj.getLatitude()) && (this.getLongitude() == obj.getLongitude());
    }

    @Override
    public Uuid getUuid() {
        return this.uuid;
    }

    @Override
    public void setUuid(Uuid uuid) {
        this.uuid=uuid;
    }

    @Override
    public boolean pullEntity(byte[] array, int position) {
        PullPage pull = new PullPage(array, position);
        Uuid storedClass = pull.pullUuid();
        if (PointCity.classId.equals(storedClass) == true) {
            uuid = pull.pullUuid();
            this.setName(pull.pullString());
            this.setLatitude(pull.pullDouble());
            this.setLongitude(pull.pullDouble());
            return true;
        }
        return false;
    }

    @Override
    public void pushEntity(byte[] array, int position) {
        PushPage push = new PushPage(array, position);
        push.pushUuid(PointCity.classId);
        push.pushUuid(uuid);
        push.pushString(this.getName());
        push.pushDouble(this.getLatitude());
        push.pushDouble(this.getLongitude());
    }

    @Override
    public int sizeOfEntity() {
        return Page.sizeOfUuid + Page.sizeOfUuid + Page.sizeOfString(this.getName()) + Page.sizeOfDouble
                + Page.sizeOfDouble;
    }
    
}
