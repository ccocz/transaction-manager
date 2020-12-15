package cp1.tests;

import cp1.base.Resource;
import cp1.base.ResourceId;

final class ResourceImpl extends Resource {
    private volatile long value = 0;

    public ResourceImpl(ResourceId id) {
        super(id);
    }

    public void incValue() {
        long x = this.value;
        ++x;
        this.value = x;
    }

    public void decValue() {
        long x = this.value;
        --x;
        this.value = x;
    }

    public long getValue() {
        return this.value;
    }
}
