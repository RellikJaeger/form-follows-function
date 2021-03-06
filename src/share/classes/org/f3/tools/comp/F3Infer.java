/*
 * Copyright 1999-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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

package org.f3.tools.comp;

import com.sun.tools.mjavac.util.*;
import com.sun.tools.mjavac.util.List;
import com.sun.tools.mjavac.code.*;
import com.sun.tools.mjavac.code.Type.*;

import static com.sun.tools.mjavac.code.Flags.*;
import static com.sun.tools.mjavac.code.Kinds.*;
import static com.sun.tools.mjavac.code.TypeTags.*;
import org.f3.tools.code.*;
/** Helper class for type parameter inference, used by the attribution phase.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class F3Infer {
    protected static final Context.Key<F3Infer> inferKey =
        new Context.Key<F3Infer>();

    /** A value for prototypes that admit any type, including polymorphic ones. */
    public static final Type anyPoly = new Type(NONE, null);

    Symtab syms;
    F3Types types;

    public static F3Infer instance(Context context) {
        F3Infer instance = context.get(inferKey);
        if (instance == null)
            instance = new F3Infer(context);
        return instance;
    }

    protected F3Infer(Context context) {
        context.put(inferKey, this);
        syms = F3Symtab.instance(context);
        types = F3Types.instance(context);
    }

    public static class NoInstanceException extends RuntimeException {
        private static final long serialVersionUID = 0;

        public boolean isAmbiguous; // exist several incomparable best instances?

        JCDiagnostic diagnostic;

        NoInstanceException(boolean isAmbiguous) {
            this.diagnostic = null;
            this.isAmbiguous = isAmbiguous;
        }
        NoInstanceException setMessage(String key) {
            this.diagnostic = JCDiagnostic.fragment(key);
            return this;
        }
        NoInstanceException setMessage(String key, Object arg1) {
            this.diagnostic = JCDiagnostic.fragment(key, arg1);
            return this;
        }
        NoInstanceException setMessage(String key, Object arg1, Object arg2) {
            this.diagnostic = JCDiagnostic.fragment(key, arg1, arg2);
            return this;
        }
        NoInstanceException setMessage(String key, Object arg1, Object arg2, Object arg3) {
            this.diagnostic = JCDiagnostic.fragment(key, arg1, arg2, arg3);
            return this;
        }
        public JCDiagnostic getDiagnostic() {
            return diagnostic;
        }
    }
    private final NoInstanceException ambiguousNoInstanceException =
        new NoInstanceException(true);
    private final NoInstanceException unambiguousNoInstanceException =
        new NoInstanceException(false);

/***************************************************************************
 * Auxiliary type values and classes
 ***************************************************************************/

    /** A mapping that turns type variables into undetermined type variables.
     */
    Mapping fromTypeVarFun = new Mapping("fromTypeVarFun") {
            public Type apply(Type t) {
                Type result = applyx(t);
                //System.err.println("from type var "+t.getClass()+" "+t+" => "+result);
                return result;
            }

            public Type applyx(Type t) {
                if (t.tag == TYPEVAR) {
		    UndetVar v = new UndetVar(t);
		    TypeVar tv = (TypeVar)t;
		    if (tv.lower != syms.botType && tv.lower != tv.bound) {
			v.lobounds = v.lobounds.append(tv.lower);
		    }
                    //System.err.println("lobounds="+v.lobounds);
		    Type upper = tv.getUpperBound();
		    tv.bound = upper == null ? tv: upper;
		    return v;
		}
                else return t.map(this);
            }
        };

    /** A mapping that returns its type argument with every UndetVar replaced
     *  by its `inst' field. Throws a NoInstanceException
     *  if this not possible because an `inst' field is null.
     */
    Mapping getInstFun = new Mapping("getInstFun") {
            public Type apply(Type t) {
                switch (t.tag) {
                case UNKNOWN:
                    throw ambiguousNoInstanceException
                        .setMessage("undetermined.type");
                case UNDETVAR:
                    UndetVar that = (UndetVar) t;
                    if (that.inst == null)
                        throw ambiguousNoInstanceException
                            .setMessage("type.variable.has.undetermined.type",
                                        that.qtype);
                    return apply(that.inst);
                default:
                    return t.map(this);
                }
            }
        };

/***************************************************************************
 * Mini/Maximization of UndetVars
 ***************************************************************************/

    /** Instantiate undetermined type variable to its minimal upper bound.
     *  Throw a NoInstanceException if this not possible.
     */
    void maximizeInst(UndetVar that, Warner warn) throws NoInstanceException {
	//System.err.println("maximize inst: "+ that);
        if (that.inst == null) {
            if (that.hibounds.isEmpty())
                that.inst = syms.objectType;
            else if (that.hibounds.tail.isEmpty())
                that.inst = that.hibounds.head;
            else {
                for (List<Type> bs = that.hibounds;
                     bs.nonEmpty() && that.inst == null;
                     bs = bs.tail) {
                    // System.out.println("hibounds = " + that.hibounds);//DEBUG
                    if (isSubClass(bs.head, that.hibounds))
                        that.inst = types.fromUnknownFun.apply(bs.head);
                }
                if (that.inst == null || !types.isSubtypeUnchecked(that.inst, that.hibounds, warn)) {
		    //System.err.println("not a subtype': "+ that.inst +" " +that.hibounds);
                    throw ambiguousNoInstanceException
                        .setMessage("no.unique.maximal.instance.exists",
                                    that.qtype, that.hibounds);
		}
            }
        }
    }
    //where
        private boolean isSubClass(Type t, final List<Type> ts) {
            t = t.baseType();
            if (t.tag == TYPEVAR) {
                List<Type> bounds = types.getBounds((TypeVar)t);
                for (Type s : ts) {
                    if (!types.isSameType(t, s.baseType())) {
                        for (Type bound : bounds) {
                            if (!isSubClass(bound, List.of(s.baseType())))
                                return false;
                        }
                    }
                }
            } else {
                for (Type s : ts) {
                    if (!t.tsym.isSubClass(s.baseType().tsym, types))
                        return false;
                }
            }
            return true;
        }

    /** Instaniate undetermined type variable to the lub of all its lower bounds.
     *  Throw a NoInstanceException if this not possible.
     */
    void minimizeInst(UndetVar that, Warner warn) throws NoInstanceException {
	///System.err.println("minimize inst: "+that.qtype +" low="+that.lobounds + " inst="+that.inst);
	if (that.inst != null) {
	    if (that.lobounds.size() > 0) {
		that.lobounds = that.lobounds.append(that.inst);
		that.inst = null;
	    }
	}
        if (that.inst == null) {
            if (that.lobounds.isEmpty())
                that.inst = syms.botType;
            else if (that.lobounds.tail.isEmpty())
                that.inst = that.lobounds.head;
            else {
                that.inst = types.lub(that.lobounds);
                if (that.inst == null) {
		    //System.err.println("no minimal instance: "+that.qtype+": "+that.lobounds);
                    throw ambiguousNoInstanceException
                        .setMessage("no.unique.minimal.instance.exists",
                                    that.qtype, that.lobounds);
		}
            }
            // VGJ: sort of inlined maximizeInst() below.  Adding
            // bounds can cause lobounds that are above hibounds.
            if (that.hibounds.isEmpty())
                return;
            Type hb = null;
            if (that.hibounds.tail.isEmpty())
                hb = that.hibounds.head;
            else for (List<Type> bs = that.hibounds;
                      bs.nonEmpty() && hb == null;
                      bs = bs.tail) {
                if (isSubClass(bs.head, that.hibounds))
                    hb = types.fromUnknownFun.apply(bs.head);
            }
            if (hb == null ||
                !types.isSubtypeUnchecked(hb, that.hibounds, warn)
		//||!types.isSubtypeUnchecked(that.inst, hb, warn) 
		) {
		//System.err.println("inst="+that.inst);
		//System.err.println("hb="+hb);
		//System.err.println("hibounds="+that.hibounds);
                throw ambiguousNoInstanceException;
	    }
        }
    }

/***************************************************************************
 * Exported Methods
 ***************************************************************************/

    /** Try to instantiate expression type `that' to given type `to'.
     *  If a maximal instantiation exists which makes this type
     *  a subtype of type `to', return the instantiated type.
     *  If no instantiation exists, or if several incomparable
     *  best instantiations exist throw a NoInstanceException.
     */
    public Type instantiateExpr(ForAll that,
                                Type to,
                                Warner warn) throws NoInstanceException {
	return instantiateExpr1(that, to, warn).qtype;
    }

    public ForAll instantiateExpr1(ForAll that,
				   Type to,
				   Warner warn) throws NoInstanceException {
        List<Type> undetvars = Type.map(that.tvars, fromTypeVarFun);
	//System.err.println("undetvars="+undetvars);
        //System.err.println("instantiateExpr1: "+ that + " to "+ to);
        for (List<Type> l = undetvars; l.nonEmpty(); l = l.tail) {
            UndetVar v = (UndetVar) l.head;
            ListBuffer<Type> hibounds = new ListBuffer<Type>();
	    //System.err.println("v="+v);
	    //System.err.println("v.qtype="+v.qtype);
            for (List<Type> l1 = types.getBounds((TypeVar) v.qtype); l1.nonEmpty(); l1 = l1.tail) {
		//System.err.println("l1.head="+l1.head);
		//System.err.println("v.qtype="+(v.qtype));
		//System.err.println("that.tvars="+that.tvars);
                if (!l1.head.containsSome(that.tvars)) {
                    hibounds.append(l1.head);
                }
            }
            v.hibounds = hibounds.toList();
	    //System.err.println("v="+v);
	    //System.err.println("v.hibounds="+v.hibounds);
        }
        Type qtype1 = types.subst(that.qtype, that.tvars, undetvars);
	//System.err.println("qtype1="+qtype1+", to="+to);
        if (!types.isSubtype(qtype1, to)) {
            throw unambiguousNoInstanceException
                .setMessage("no.conforming.instance.exists",
                            that.tvars, that.qtype, to);
        }
        for (List<Type> l = undetvars; l.nonEmpty(); l = l.tail)
            maximizeInst((UndetVar) l.head, warn);
        // System.out.println(" = " + qtype1.map(getInstFun));//DEBUG

        // check bounds
        List<Type> targs = Type.map(undetvars, getInstFun);
        targs = types.subst(targs, that.tvars, targs);
        checkWithinBounds(that.tvars, targs, warn);
        return new ForAll(targs, getInstFun.apply(qtype1));
    }

    /** Instantiate method type `mt' by finding instantiations of
     *  `tvars' so that method can be applied to `argtypes'.
     */
    public Type instantiateMethod(List<Type> tvars,
                                  MethodType mt,
                                  List<Type> argtypes,
                                  boolean allowBoxing,
                                  boolean useVarargs,
                                  Warner warn) throws NoInstanceException {
	if (false) {
	    System.err.println("instantiateMethod(" + tvars + ", " + mt + ", " + argtypes + ")"); //DEBUG
	    System.err.println("allowBoxing+"+allowBoxing+ ", varargs="+useVarargs);
	}
        List<Type> undetvars = Type.map(tvars, fromTypeVarFun);
        List<Type> formals = mt.argtypes;
        // instantiate all polymorphic argument types and
        // set up lower bounds constraints for undetvars
        Type varargsFormal = useVarargs ? formals.last() : null;
        while (argtypes.nonEmpty() && formals.head != varargsFormal) {
            Type ft = formals.head;
            Type at = argtypes.head.baseType();
            if (at.tag == FORALL) {
		Type bt = at;
                at = instantiateArg((ForAll) at, ft, tvars, warn);
		//System.err.println("instantiated: "+ bt + " to "+ at);
	    }
            Type sft = types.subst(ft, tvars, undetvars);
            //System.err.println("subst "+ft+" "+tvars+" "+undetvars+" => "+sft);
            types.isSuperType(at, sft); // Hack!!! insane but true: f3infer depends on this being called (types.isSuperType sets bounds on undetvar's)

            boolean works = allowBoxing
                ? types.isConvertible(at, sft, warn)
                : types.isSubtypeUnchecked(at, sft, warn);
            if (!works) {
		if (false) {
		    System.err.println("ft="+ft);
		    System.err.println("at="+at);
		    System.err.println("sft="+sft);
		}
                throw unambiguousNoInstanceException
                    .setMessage("no.conforming.assignment.exists",
                                tvars, at, ft);
            }
            formals = formals.tail;
            argtypes = argtypes.tail;
        }
        if (formals.head != varargsFormal || // not enough args
            !useVarargs && argtypes.nonEmpty()) { // too many args
            // argument lists differ in length
            throw unambiguousNoInstanceException
                .setMessage("arg.length.mismatch");
        }

        // for varargs arguments as well
        if (useVarargs) {
            Type elt = types.elemtype(varargsFormal);
            Type sft = types.subst(elt, tvars, undetvars);
            while (argtypes.nonEmpty()) {
                Type ft = sft;
                Type at = argtypes.head.baseType();
                if (at.tag == FORALL)
                    at = instantiateArg((ForAll) at, ft, tvars, warn);
                boolean works = types.isConvertible(at, sft, warn);
                if (!works) {
                    throw unambiguousNoInstanceException
                        .setMessage("no.conforming.assignment.exists",
                                    tvars, at, ft);
                }
                argtypes = argtypes.tail;
            }
        }

        // minimize as yet undetermined type variables
        for (Type t : undetvars)
            minimizeInst((UndetVar) t, warn);

        /** Type variables instantiated to bottom */
        ListBuffer<Type> restvars = new ListBuffer<Type>();

        /** Instantiated types or TypeVars if under-constrained */
        ListBuffer<Type> insttypes = new ListBuffer<Type>();

        /** Instantiated types or UndetVars if under-constrained */
        ListBuffer<Type> undettypes = new ListBuffer<Type>();

        for (Type t : undetvars) {
            UndetVar uv = (UndetVar)t;
            if (uv.inst.tag == BOT) {
                restvars.append(uv.qtype);
                insttypes.append(uv.qtype);
                undettypes.append(uv);
                uv.inst = null;
            } else {
                insttypes.append(uv.inst);
                undettypes.append(uv.inst);
            }
        }

        checkWithinBounds(tvars, undettypes.toList(), warn);

        if (!restvars.isEmpty()) {
            //System.err.println("tvars="+tvars);
            //System.err.println("insttypes="+insttypes.toList());
            // if there are uninstantiated variables,
            // quantify result type with them
            mt = new MethodType(mt.argtypes,
                                new ForAll(restvars.toList(), mt.restype),
                                mt.thrown, syms.methodClass);
            //System.err.println("couldn't instantiate: "+restvars.toList());
            //System.err.println("returning: "+mt);
            //throw new NoInstanceException(false);
        }

        // return instantiated version of method type
        return types.subst(mt, tvars, insttypes.toList());
    }
    //where

        /** Try to instantiate argument type `that' to given type `to'.
         *  If this fails, try to insantiate `that' to `to' where
         *  every occurrence of a type variable in `tvars' is replaced
         *  by an unknown type.
         */
        private Type instantiateArg(ForAll that,
                                    Type to,
                                    List<Type> tvars,
                                    Warner warn) throws NoInstanceException {
            List<Type> targs;
            try {
                return instantiateExpr(that, to, warn);
            } catch (NoInstanceException ex) {
                Type to1 = to;
                for (List<Type> l = tvars; l.nonEmpty(); l = l.tail)
                    to1 = types.subst(to1, List.of(l.head), List.of(syms.unknownType));
                return instantiateExpr(that, to1, warn);
            }
        }

    /** check that type parameters are within their bounds.
     */
    private void checkWithinBounds(List<Type> tvars,
                                   List<Type> arguments,
                                   Warner warn)
        throws NoInstanceException {
	//System.err.println("check within bounds: "+tvars + ": "+arguments);
        for (List<Type> tvs = tvars, args = arguments;
             tvs.nonEmpty();
             tvs = tvs.tail, args = args.tail) {
            if (args.head instanceof UndetVar) continue;
            List<Type> bounds = types.subst(types.getBounds((TypeVar)tvs.head), tvars, arguments);
            if (!types.isSubtypeUnchecked(args.head, bounds, warn)) {
		//System.err.println("not a subtype: "+ args.head +" " +bounds);
                throw unambiguousNoInstanceException
                    .setMessage("inferred.do.not.conform.to.bounds",
                                arguments, tvars);
	    }
        }
    }
}
