package tool.leios;
import grammar.AST;
import grammar.cfg.*;
import java.util.*;
import static grammar.transformations.util.CFGUtil.*;

public class DistributionMutator {

    CFGBuilder cfg;
    ArrayList<String> changedVars = new ArrayList<>();

    public DistributionMutator(CFGBuilder cfg_){
        cfg = cfg_;
    }

    public void applyApproximations(){
        for(Section  section : cfg.getSections()){

            //change type of data from int to float
            if(section.sectionType == SectionType.DATA) {
                for (BasicBlock basicBlock : section.basicBlocks){
                    for (AST.Data data : basicBlock.getData()){
                        if (data.decl.dtype.primitive == AST.Primitive.INTEGER){
                            data.decl.dtype.primitive = AST.Primitive.FLOAT;
                        }
                    }
                }
            }

            //apply distribution approximations
            if(section.sectionType == SectionType.FUNCTION){
                if(section.sectionName.equals("main")) {
                    Set<BasicBlock> visited = new HashSet<>();
                    for (BasicBlock basicBlock : section.basicBlocks) {
                        BasicBlock curBlock = basicBlock;
                        while(!visited.contains(curBlock)) {
                            visited.add(curBlock);
                            if (curBlock.getParent().sectionName.equalsIgnoreCase("main")) {
                                analyzeModelBlock(curBlock);
                            }

                            BasicBlock nextBlock = getNextBlock(curBlock);
                            if (nextBlock != null) {
                                curBlock = nextBlock;
                            }
                        }
                    }
                }
            }
        }
    }




    private void analyzeModelBlock(BasicBlock bBlock) {
        if (bBlock.getStatements().size() != 0) {
            for (Statement statement : bBlock.getStatements()) {

                //change type declarations from int to float
                if (statement.statement instanceof AST.Decl){
                    if (((AST.Decl) statement.statement).dtype.primitive == AST.Primitive.INTEGER){
                        ((AST.Decl) statement.statement).dtype.primitive = AST.Primitive.FLOAT;
                        //changedVars.add(((AST.Decl) statement.statement).id.id);
                    }
                }

                //if it is an assignment
                if (statement.statement instanceof AST.AssignmentStatement) {
                    AST.Expression RHS = ((AST.AssignmentStatement) statement.statement).rhs;
                    ((AST.AssignmentStatement) statement.statement).rhs = apply_approximations(RHS);
                    if (((AST.AssignmentStatement) statement.statement).rhs.transformed){
                        if ( ((AST.AssignmentStatement) statement.statement).lhs instanceof AST.Id){
                            String id = ((AST.Id) ((AST.AssignmentStatement) statement.statement).lhs).id;
                            changedVars.add(id);
                        }
                    }
                }
            }
        }
    }

    private AST.Expression apply_approximations(AST.Expression rhs){
        if (rhs instanceof AST.FunctionCall){
            if (((AST.FunctionCall) rhs).toString().contains("poisson")){
                return poisson_approximation(((AST.FunctionCall) rhs));
            }
            if (((AST.FunctionCall) rhs).toString().contains("binomial")){
                return binomial_approximation(((AST.FunctionCall) rhs));
            }
            if (((AST.FunctionCall) rhs).toString().contains("bernoulli")){
                return bernoulli_approximation(((AST.FunctionCall) rhs));
            }
        }
        else if (rhs instanceof AST.Number){
            return constant_approximation((AST.Number) rhs);
        }
        return rhs;
    }


    private AST.Expression poisson_approximation(AST.FunctionCall poissonDist){
        assert poissonDist.toString().contains("poisson");
        poissonDist.id = new AST.Id("normal");
        AST.Expression mu = poissonDist.parameters.get(0);
        AST.Expression sigmaSquared = poissonDist.parameters.get(0);

        //take the sqrt
        AST.FunctionCall sqrtFunc = new AST.FunctionCall();
        sqrtFunc.id = new AST.Id("sqrt");
        ArrayList<AST.Expression> sqrtParams = new ArrayList<>();
        sqrtParams.add(sigmaSquared);
        sqrtFunc.parameters = sqrtParams;

        ArrayList<AST.Expression> params = new ArrayList<>();
        params.add(mu);
        params.add(sqrtFunc);
        poissonDist.parameters = params;
        poissonDist.transformed = true;
        return poissonDist;
    }

    private AST.Expression binomial_approximation(AST.FunctionCall binomialDist){
        assert binomialDist.toString().contains("binomial");
        binomialDist.id = new AST.Id("normal");
        AST.Expression N = binomialDist.parameters.get(0);
        AST.Expression p = binomialDist.parameters.get(1);
        AST.MulOp mu = new AST.MulOp(N,p);
        AST.MinusOp oneMinusP = new AST.MinusOp(new AST.Integer("1"),p);
        AST.MulOp sigmaSquared = new AST.MulOp(mu,oneMinusP);

        //take the sqrt
        AST.FunctionCall sqrtFunc = new AST.FunctionCall();
        sqrtFunc.id = new AST.Id("sqrt");
        ArrayList<AST.Expression> sqrtParams = new ArrayList<>();
        sqrtParams.add(sigmaSquared);
        sqrtFunc.parameters = sqrtParams;

        ArrayList<AST.Expression> params = new ArrayList<>();
        params.add(mu);
        params.add(sqrtFunc);
        binomialDist.parameters = params;
        binomialDist.transformed = true;
        return binomialDist;
    }


    private AST.Expression bernoulli_approximation(AST.FunctionCall bernoulliDist){
        assert bernoulliDist.toString().contains("bernoulli");
        bernoulliDist.id = new AST.Id("beta");
        AST.Expression p = bernoulliDist.parameters.get(0);
        AST.MinusOp oneMinusP = new AST.MinusOp(new AST.Integer("1"),p);
        AST.DivOp ratio = new AST.DivOp(oneMinusP,p);
        AST.Number beta = new AST.Double("0.1");
        AST.MulOp prod = new AST.MulOp(beta,ratio);

        ArrayList<AST.Expression> params = new ArrayList<>();
        params.add(beta);
        params.add(prod);

        bernoulliDist.parameters = params;
        bernoulliDist.transformed = true;
        return bernoulliDist;
    }




    private AST.Expression constant_approximation(AST.Number c){

        AST.Double mu = new AST.Double(c.toString());

        AST.FunctionCall smoothedConstant = new AST.FunctionCall();
        smoothedConstant.id = new AST.Id("normal");

        AST.Number beta = new AST.Double("0.1");

        ArrayList<AST.Expression> params = new ArrayList<>();
        params.add(mu);
        params.add(beta);
        smoothedConstant.parameters = params;

        smoothedConstant.transformed = true;
        return smoothedConstant;
    }

    public ArrayList<String> getChangedVars() {
        return changedVars;
    }
}
