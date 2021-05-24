package com.cenboomh.commons.ojdbc.wrapper;

import java.sql.SQLException;
import java.sql.Wrapper;

/**
 * @author wuwen
 */
public class AbstractWrapper implements Wrapper {

    private final Object delegate;

    protected AbstractWrapper(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        final Object result;

        if (iface.isAssignableFrom(getClass())) {
            result = this;
        } else if (iface.isAssignableFrom(delegate.getClass())) {
            result = delegate;
        } else if (Wrapper.class.isAssignableFrom(delegate.getClass())) {
            result = ((Wrapper) delegate).unwrap(iface);
        } else {
            throw new SQLException("Can not unwrap to " + iface.getName());
        }

        return iface.cast(result);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {

        if (iface.isAssignableFrom(getClass())) {
            return true;
        } else if (iface.isAssignableFrom(delegate.getClass())) {
            return true;
        } else if (Wrapper.class.isAssignableFrom(delegate.getClass())) {
            return ((Wrapper) delegate).isWrapperFor(iface);
        }

        return false;
    }
}
