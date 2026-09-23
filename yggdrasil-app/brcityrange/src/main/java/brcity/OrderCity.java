package brcity;

import yggdrasil.block.Page;
import yggdrasil.block.PullPage;
import yggdrasil.block.PushPage;
import yggdrasil.meta.Sort;
import yggdrasil.meta.Uuid;

public class OrderCity extends EntityCity implements Sort<OrderCity>, Comparable<OrderCity> {
 
    public static Uuid classId = Uuid.fromString("2f10da29-5d5f-4ff4-b9ab-7f1267924caf");

    @Override
    public boolean hasSameKey(OrderCity obj) {
        return compareTo(obj)==0;
    }

    @Override
    public boolean pullKey(byte[] array, int position) {
        PullPage pull = new PullPage(array, position);
        this.setName(pull.pullString());
        return true;
    }

    @Override
    public void pushKey(byte[] array, int position) {
        PushPage push = new PushPage(array, position);
        push.pushString(this.getName());
    }

    @Override
    public int sizeOfKey() {
        return Page.sizeOfString(this.getName());
    }

    @Override 
    public int compareTo(OrderCity o){
        return this.getName().compareTo(o.getName());
    }
}