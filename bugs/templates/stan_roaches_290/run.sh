#!/bin/bash
# first argument is the directory where everthing should be setup

# cd ../TestMin/bugs/stan_roaches_290
cd $1
echo $pwd
if [ ! -d "cmdstan-2.9.0" ]; then
    mkdir -p cmdstan-2.9.0
    wget https://github.com/stan-dev/cmdstan/releases/download/v2.9.0/cmdstan-2.9.0.tar.gz
    tar -xf cmdstan-2.9.0.tar.gz -C cmdstan-2.9.0/ --strip-components 1
fi
initcontent=`cat stan_roaches_290.stan`

cd cmdstan-2.9.0
make build
if [ ! -d "stan_roaches_290" ]; then
    mkdir stan_roaches_290
fi
echo $pwd
here=`realpath ../`
echo $here
(cd ../../../../translators/ && ./teststan.py -o $here/stan_roaches_290.template && cp stan_roaches_290.stan $here/stan_roaches_290.stan && cp stan_roaches_290.data.R $here/stan_roaches_290.data.R)

if [ $? -ne 0 ]; then
echo "Translate failed"
echo "Failed"
exit 2
fi
curcontent=`cat ../stan_roaches_290.stan`
#diff ../stan_roaches_290.stan stan_roaches_290/stan_roaches_290.stan
diff <(echo "$initcontent") <(echo "$curcontent")
#if [ $? -ne 0 ]; then
cp ../stan_roaches_290.data.R ../*.stan stan_roaches_290/
#fi
pwd
echo "making..."
rm -f stan_roaches_290/stan_roaches_290
make stan_roaches_290/stan_roaches_290
cd ./stan_roaches_290/
pwd
if [ -z $2 ]
then
timeout 120 ./stan_roaches_290 variational elbo_samples=10 eval_elbo=10 iter=100 tol_rel_obj=0.001 random seed=1468072299 data file=stan_roaches_290.data.R > stanout 2>&1
else
echo "Iterations : $2"
timeout 120 ./stan_roaches_290 variational elbo_samples=10 eval_elbo=10 iter=$2 tol_rel_obj=0.001 random seed=1468072299 data file=stan_roaches_290.data.R > stanout 2>&1
fi
nans=`grep -i "stan::variational::advi::adapt_eta: Cannot compute ELBO using the initial variational distribution" stanout | wc -l`
if [ $nans -gt 0 ]; then
echo "Passed"
else
echo "Failed"
fi
