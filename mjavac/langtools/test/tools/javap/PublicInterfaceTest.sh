#!/bin/sh

#
# Copyright 2003-2007 Sun Microsystems, Inc.  All Rights Reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
# CA 95054 USA or visit www.sun.com if you need additional information or
# have any questions.
#

# @test
# @bug 4866831
# @build NotPackagePrivateInterface
# @run shell PublicInterfaceTest.sh
# @summary Verify that javap marks public interfaces as public
# @author Joseph D. Darcy

# Verify directory context variables are set
if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${TESTCLASSES}" = "" ]
then
  echo "TESTCLASSES not set.  Test cannot execute.  Failed."
  exit 1
fi

JAVAP="${TESTJAVA}/bin/javap"

"$JAVAP" ${TESTTOOLVMOPTS} -classpath "${TESTCLASSES}" NotPackagePrivateInterface | grep public

# If the exit status of grep is 0, then "public" was correctly found
# in the output of javap.

RESULT=$?
case "$RESULT" in
        0  )
	exit 0;
        ;;

        * )
        echo "The javap tool did not output \"public\" for a public interface."
        exit 1
esac
