#!/usr/bin/env bash
# run: ./stan_runner.sh [stan file] [data file] [algo:nuts/hmc]
# update cmdstan path as needed

CMDSTAN_PATH='/home/saikat/projects/cmdstan-2.19.1'
cd $CMDSTAN_PATH

file=$1
data=$2
basefile=`basename $file`
basedata=`basename $data`
basefilename=`echo $basefile | cut -d"." -f1`
filepath=`dirname $1`
cd $CMDSTAN_PATH
rm -rf $basefilename
mkdir -p $basefilename

cp $1 $2 $basefilename/

echo "making..."

make $basefilename/$basefilename
cd ./$basefilename/

if [ ! -z "$3" ]; then
./$basefilename sample algorithm=$3 num_samples=1000 num_warmup=200 data file=$basedata > stanout 2>&1
else
./$basefilename sample num_samples=1000 num_warmup=200 data file=$basedata > stanout 2>&1
fi
../bin/stansummary output.csv > stanout 2>&1
cp stanout $filepath/




