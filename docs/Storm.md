## Storm: Program Reduction for Probabilistic Programs

Given a probabilistic program, data, and inference algorithm parameters which expose a bug in the underlying probabilistic programming system, Storm generates the minimal sized version of the code, data, and inference parameters which triggers the same bug.

### Prerequisites

```
java
maven
```

If you installed the Storm framework, nothing else is required
 
### Setting up benchmarks

All the benchmarks are located in the `bugs` folder. To setup any one benchmark, cd to the directory and run `run.sh .`

For example, run: `cd bugs/templates/stan723; ./run.sh .`

To setup all benchmarks: `cd bugs; bash setup_benchmarks.sh`

#### Choosing benchmarks to run

The specification of all benchmarks to run is present in `src/main/resources/testfiles_*.json`

To run a single benchmark, add the specs to  `testfiles_single.json`

Update `TESTSET` variable in `src/main/resources/config.properties` to the suffix of the json file. 

Possible options are:

1. `stan` (for benchmarks obtained from stan issues) 
2. `stan_em` (for benchmarks from stan example models), 
3. `pyro` (for pyro benchmarks),
4. `stan_cov` (for coverage based reduction), 
5. `single` (for custom set of benchmarks)


### Configuration parameters

There are several options which determine how to run Storm. All these options are available in `src/main/resources/config.properties`

1. `MAX_THREADS`: Number of threads to run. Each thread runs a single benchmark
2. `TESTSET`: the set of benchmarks to use
3. `BASIC`: turns off domain specific information for reduction
4. `DistributionChoice`: strategy to choose distribution replacement. Options are  TIMING,EXPONFAMILY,RANDOM.
5. `ORDER`: order of applying transformations. Options are static,random,size,analysis,basic,domain.
6. `RUNCOV`: records coverage for reduction



### Running Storm

From the root directory, run: `java -cp target/grammar-1.0.jar tool.testmin.TMMain`

This will generate the outputs in `out/` folder, which contains the following files:

```
1. Minimized data, stan code, and number of samples for each benchmark
2. Report.txt: Detailed report on transformations used, timings, and reduction obtained
3. Report.csv: Summarized report of all benchmark
```




If you use our framework for your research, please cite as:

```
@article{dutta2019storm,
  title={Storm: Program Reduction for Testing and Debugging Probabilistic Programming Systems},
  author={Dutta, Saikat and Zhang, Wenxian and Huang, Zixin and Misailovic, Sasa},
  year={2019}
}
```
