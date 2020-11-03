package tool.leios;

import grammar.cfg.CFGBuilder;
import translators.Edward2Translator;
import translators.PyroTranslator;
import translators.StanTranslator;

import java.util.ArrayList;

public class Continualizer {

    CFGBuilder builder;
    String code = null;
    public Continualizer(CFGBuilder builder_){
        builder = builder_;
    }

    public void continualize(){
        //continualize the distributions
        DistributionMutator Cont = new DistributionMutator(builder);
        Cont.applyApproximations();
        ArrayList<String> changedVars = Cont.getChangedVars();

        //correct predicates
        PredicateCorrector PC = new PredicateCorrector(builder,changedVars);
        PC.correctPredicates();
    }

    public CFGBuilder getBuilder(){
        return builder;
    }

    public String getCode(){
        return code;
    }

    public String getStanCode(){
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(builder.getSections());
        } catch (Exception e) {
            e.printStackTrace();
        }
        code = stanTranslator.getCode();
        return code;
    }

    public String getPyroCode(){
        PyroTranslator pyroTranslator = new PyroTranslator();
        try {
            pyroTranslator.translate(builder.getSections());
        } catch (Exception e) {
            e.printStackTrace();
        }
        code = pyroTranslator.getCode();
        return code;
    }


    public String getEdwardCode(){
        Edward2Translator edward2Translator = new Edward2Translator();
        try {
            edward2Translator.translate(builder.getSections());
        } catch (Exception e) {
            e.printStackTrace();
        }
        code = edward2Translator.getCode();
        return code;
    }

}

