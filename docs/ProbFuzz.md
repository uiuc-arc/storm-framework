## ProbFuzz

ProbFuzz is a tool for testing Probabilistic Programming Systems(PPS). It encodes a probabilistic language grammar from which it systematically generates probabilistic programs. ProbFuzz has language specific translators to transform the generated program into versions which use the API of individual systems. Then it finds bugs by differential testing of the given systems. The developer writes the templates of probabilistic models in an intermediate language with holes, which represent missing distributions, parameters or data. ProbFuzz generates the probabilistic models by completing the holes in the template with specific choices. Currently, ProbFuzz has backends for Stan, Edward2, Pyro, and PSI, which translate the completed program into a valid program for each system. Finally, it runs them and computes metrics to reveal bugs.

### Running ProbFuzz
From the root directory, run:

`java -cp target/grammar-1.0.jar tool.probfuzz.Main [template-path] [#programs]`

This will generate several programs by fuzzing on the given template and produce output programs in `output/` folder. For each program, it will contain the translated program in each system and also its output.  

Several templates are available in `src/main/java/tool/probfuzz/templates/`. Each template is a partial specification(in Storm-IR) of a probabilistic program. You can also add your own template for fuzzing. Refer to `template/Template3.g4` for the complete language specification.


### Citation

If you use our framework, please cite us using:
```
@inproceedings{dutta2018testing,
   title={Testing probabilistic programming systems},
   author={Dutta, Saikat and Legunsen, Owolabi and Huang, Zixin and Misailovic, Sasa},
   booktitle={Proceedings of the 2018 26th ACM Joint Meeting on European Software Engineering Conference and Symposium on the Foundations of Software Engineering},
   pages={574--586},
   year={2018}
 }
``` 




