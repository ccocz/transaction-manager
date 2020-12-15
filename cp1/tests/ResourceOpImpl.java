package cp1.tests;

import cp1.base.Resource;
import cp1.base.ResourceOperation;

final class ResourceOpImpl extends ResourceOperation {
    private final static ResourceOpImpl singleton = new ResourceOpImpl();

    public static ResourceOperation get() {
        return singleton;
    }

    private ResourceOpImpl() {
    }

    @Override
    public String toString() {
        return "OP_" + super.toString();
    }

    @Override
    public void execute(Resource r) {
        if (!(r instanceof ResourceImpl)) {
            throw new AssertionError("Unexpected resource type " +
                    r.getClass().getCanonicalName());
        }
        ((ResourceImpl) r).incValue();
    }

    @Override
    public void undo(Resource r) {
        if (!(r instanceof ResourceImpl)) {
            throw new AssertionError("Unexpected resource type " +
                    r.getClass().getCanonicalName());
        }
        ((ResourceImpl) r).decValue();
    }
}
