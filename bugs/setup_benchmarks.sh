#!/usr/bin/env bash
# sets up all the benchmarks
for d in `ls -d templates/stan*`;
do
echo "$d"
dd=`find $d -name "cmdstan*"`
pyv=`find $d -name "pyvirtual*"`
if [  -z "$dd" ] && [ -z "$pyv" ]; then
  (cd $d; ./run.sh .)
fi
done

for d in `ls -d templates/probfuzz/*`;
do
echo "$d"
pyv=`find $d -name "pyvirtual*"`
if [ -z "$pyv" ]; then
  (cd $d; ./run.sh .)
fi
done

for d in `ls -d templates/pyro/*`;
do
echo "$d"
pyv="find $d -name \"pyvirtual*\""
if [ -z "$pyv" ]; then
  (cd $d; ./run.sh .)
fi
done