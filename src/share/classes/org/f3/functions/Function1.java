/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package org.f3.functions;

import org.f3.runtime.F3Object;
import org.f3.runtime.Functor;
import org.f3.runtime.Monad;
import org.f3.runtime.Either;

public class Function1<R, A1> extends Function<R> 
    implements Monad<Function1, R> // , Comonad<Function1, R>
{
    /*
    public <A2> 
	Function1<? super R, ? super Either<? extends A1, ? extends A2> >
	or(final Function1<? extends R, ? super A2> g) 
    {
	final Function1<R,A1> f = this;
	return new Function1<R, Either<? extends A1, ? extends A2> >() {
	    public R invoke(Either<? extends A1, ? extends A2> x) {
		return x.either(f, g);
	    }
	};
    }
    */
    public <B> Function0<? extends R> mul(final A1 x) {
	final Function1<R, A1> self = this;	
	return new Function0<R>() {
	    public R invoke() {
		return self.invoke(x);
	    }
	    public String toString() {
		return self + " * " +x;
	    }
	};
    }
    
    public <B> Function1<? extends R, ? super B> 
        composeWith(final Function1<? extends A1, ? super B> f) {
	final Function1<R, A1> self = this;
	return new Function1<R, B>() {
	    public R invoke(final B b) {
		return self.invoke(f.invoke(b));
	    }
	    public String toString() {
		return "("+self +") * ("+ f + ")";
	    }
	};
    }

    public Function2<R, A1, Void> uncurry() {
	final Function1<R, A1> self = this;	
	return new Function2<R, A1, Void>() {
	    public R invoke(A1 x1,Void x2) {
		return self.invoke(x1);
	    }
	    public String toString() {
		return self +".uncurry()";
	    }
	};
    }

    public Function0<R> apply(final A1 x1) {
	final Function1<R, A1> self = this;
	return new Function0<R>() {
	    public R invoke() {
		return self.invoke(x1);
	    }
	    public String toString() {
		return self+".apply("+x1+")";
	    }
	};
    }

    public <Y> Function1<Y, A1> map(final Function1<? extends Y, ? super R> f) {
	final Function1<R, A1> self = this;
	return new Function1<Y, A1>() {
	    public Y invoke(final A1 a1) {
		return f.invoke(self.invoke(a1));
	    }
	    public String toString() {
		return self+".map("+f+")";
	    }
	};
    }

    public <Y> Function1<Y, A1> 
	flatmap(final Function1<? extends Function1<? extends Y, ? super A1>, ? super R> f) 
	{
	    final Function1<R, A1> self = this;
	    return new Function1<Y, A1>() {
		public Y invoke(A1 x1) {
		    final R r = self.invoke(x1);
		    Function1<? extends Y, ? super A1> g = f.invoke(r);
		    return g.invoke(x1);
		}
		public String toString() {
		    return self+".flatmap("+f+")";
		}
	    };
	}

    public Function1() {}
    
    public Function1(final F3Object implementor, final int number) {
        super(implementor, number);
    }
    
    // Get the implementor to invoke the function.
    // Don't override this.
    public Object invoke$(Object arg1, Object arg2, Object[] rargs) {
	//System.err.println("this="+this+", arg1="+arg1);
        return invoke((A1)arg1);
    }

    // Override this
    public R invoke(A1 x1) {
        if (implementor != null) {
            return (R) implementor.invoke$(number, x1, null, null);
        } else {
            throw new RuntimeException("invoke function missing in "+this);
        }
    }

    static public <a,b,c> Function1<? extends c, ? super a>
	compose(Function1<? extends c, ? super b> f,
		Function1<? extends b, ? super a> g) 
	{
	    return f.composeWith(g);
	}
}
