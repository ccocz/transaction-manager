package cp1.tests;

import cp1.base.ResourceId;

final class ResourceIdImpl implements ResourceId {
    private static volatile int next = 0;

    public static synchronized ResourceId generate() {
        return new ResourceIdImpl(next++);
    }

    private final int value;

    private ResourceIdImpl(int value) {
        this.value = value;
    }

    @Override
    public int compareTo(ResourceId other) {
        if (!(other instanceof ResourceIdImpl)) {
            throw new RuntimeException("Comparing incompatible resource IDs");
        }
        ResourceIdImpl second = (ResourceIdImpl) other;
        return Integer.compare(this.value, second.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceIdImpl)) {
            return false;
        }
        ResourceIdImpl second = (ResourceIdImpl) obj;
        return this.value == second.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.value);
    }

    @Override
    public String toString() {
        return "R" + this.value;
    }
}
