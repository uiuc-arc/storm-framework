# Storm-Framework

Storm-Framework is a unified framework for testing, analyis, and transformation of probabilistic programs.  At its core, Storm consists of four main components: the Storm-IR language, a Translator, a Transformer, and a Static Analysis engine. The key advantage of Storm is the common intermediate language, which allows the programmer to write any kind of analysis/transformation once, and then translate it into any PPL of their choice. This rids the programmer from having to deal with the intricacies of individual PPSs. In this documentation, we provide the details of each tool that can be implemented using Storm-Framework and show how the developers can easily adopt and extend our infrastructure for their own use-case. 


## Setup

To setup Storm and all the related tools, run: `sudo ./setup.sh`

Build the framework using: `mvn package -DskipTests`


## Project Structure

The project is organized in the following way:
```
ROOT
|   docs                        # documentation of tools
|   bugs                        # benchmarks for storm: program reduction tool
|   python3                     # python3 grammar file
|   src                         # all source code
|   |   main/java/grammar       # core infrastructure for Storm 
|   |   |   analyses            # static analysis code (Data Flow/Control Flow Analysis)           
|   |   |   cfg                 # control flow graph utilities
|   |   |   transformations     # AST/CFG based code transformations
|   |   main/java/tool          # implementation of all tools
|   |   |   leios               # code for leios
|   |   |   probfuzz            # code for probfuzz
|   |   |   testmin             # code for storm: program reduction tool
|   |   main/java/translators   # translators from Storm-IR to other languages
|   |   main/java/utils         # code utilities
|   |   main/resources
|   |   |   config.properties   # configuration options for Storm and other tools
|   |   |   models.json         # specifications for all distributions 
|   |   |   testfiles*.json     # benchmarks for storm: program reduction tool
|   template                    # grammar for Storm-IR
|   README.md                   # readme file
|   setup.sh                    # setup code for Storm
```

## Tools

Storm-framework can be used to implement a variety of tools. Below we describe some tools that we have implemented using Storm-framework and how to use them. 

### ProbFuzz

ProbFuzz is test generation tool for testing probabilistic programming systems. Find the detailed documentation on how to run it [here](docs/ProbFuzz.md). You can find the details of our technique in our [paper](http://misailo.web.engr.illinois.edu/papers/probfuzz-fse18.pdf) published at FSE 2018.

### Storm: Program Reduction

Storm is a program reduction tool for probabilistic programs which reduces given code, data, and inference parameters while preserving the same fault-exposing behaviour of the input. Find the detailed documentation [here](docs/Storm.md). You can find the details of our technique in our [paper](http://misailo.web.engr.illinois.edu/papers/storm-fse19.pdf) published at FSE 2019.

### Leios

Leios is a tool for continualization of discrete and mixed discrete-continous probabilistic programs. You can a detailed description of our technique in our paper [here](https://jsl1994.github.io/papers/ESOP2020Final.pdf) (ESOP 2020).
For details about how to run Leios, check the documentation [here](docs/LEIOS.md).

