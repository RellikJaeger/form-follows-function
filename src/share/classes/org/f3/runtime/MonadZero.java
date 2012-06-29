package org.f3.runtime;
import org.f3.functions.*;

public interface MonadZero<This, X> extends Monad<This, X> {
    public MonadZero<This, X> mzero();
}