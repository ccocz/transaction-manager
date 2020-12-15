package cp1.tests;

import cp1.base.LocalTimeProvider;

final class LocalTimeProviderImpl implements LocalTimeProvider {
    @Override
    public long getTime() {
        return System.currentTimeMillis();
    }
}
