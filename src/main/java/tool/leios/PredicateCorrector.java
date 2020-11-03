package tool.leios;
import grammar.AST;
import grammar.cfg.*;
import java.util.*;
import static grammar.transformations.util.CFGUtil.*;

public class PredicateCorrector {

    CFGBuilder cfg;
    int holeNumber = 0;
    ArrayList<String> changedVars;

    public PredicateCorrector(CFGBuilder cfg_, ArrayList<String> changedVars_){
        cfg = cfg_;
        changedVars = changedVars_;
    }

    public void correctPredicates(){
        for(Section  section : cfg.getSections()){
            if(section.sectionType == SectionType.FUNCTION){
               if(section.sectionName.equals("main")) {
                   Set<BasicBlock> visited = new HashSet<>();
                   for (BasicBlock basicBlock : section.basicBlocks) {
                       BasicBlock curBlock = basicBlock;
                       while(!visited.contains(curBlock)) {
                           visited.add(curBlock);
                           if (curBlock.getParent().sectionName.equalsIgnoreCase("main")) {
                               analyze_block(curBlock);
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


    private void analyze_block(BasicBlock bBlock) {
        if (bBlock.getStatements().size() != 0) {
            for (Statement statement : bBlock.getStatements()) {
                //if it is a predicate
                if (statement.statement instanceof AST.IfStmt) {
                    AST.Expression cond = ((AST.IfStmt) statement.statement).condition;
                    cond = apply_mutations(cond);
                    ((AST.IfStmt) statement.statement).condition = cond;
                }
            }
        }
    }


    private AST.Expression apply_mutations(AST.Expression E){
        if (E instanceof AST.MinusOp){
            ((AST.MinusOp) E).op1 = apply_mutations(((AST.MinusOp) E).op1);
            ((AST.MinusOp) E).op2 = apply_mutations(((AST.MinusOp) E).op2);
            return E;
        }
        else if (E instanceof AST.AddOp){
            ((AST.AddOp) E).op1 = apply_mutations(((AST.AddOp) E).op1);
            ((AST.AddOp) E).op2 = apply_mutations(((AST.AddOp) E).op2);
            return E;
        }

        else if (E instanceof AST.MulOp){
            ((AST.MulOp) E).op1 = apply_mutations(((AST.MulOp) E).op1);
            ((AST.MulOp) E).op2 = apply_mutations(((AST.MulOp) E).op2);
            return E;
        }

        else if (E instanceof AST.DivOp){
            ((AST.DivOp) E).op1 = apply_mutations(((AST.DivOp) E).op1);
            ((AST.DivOp) E).op2 = apply_mutations(((AST.DivOp) E).op2);
            return E;
        }

        else if (E instanceof AST.AndOp){
            ((AST.AndOp) E).op1 = apply_mutations(((AST.AndOp) E).op1);
            ((AST.AndOp) E).op2 = apply_mutations(((AST.AndOp) E).op2);
            return E;
        }

        else if (E instanceof AST.OrOp){
            ((AST.OrOp) E).op1 = apply_mutations(((AST.OrOp) E).op1);
            ((AST.OrOp) E).op1 = apply_mutations(((AST.OrOp) E).op2);
            return E;
        }

        //Most important case!
        else if (E instanceof AST.EqOp){
            AST.Expression LHS = ((AST.EqOp) E).op1;
            AST.Expression RHS = ((AST.EqOp) E).op2;
            AST.Id hole1Name = makeHole();
            AST.Id hole2Name = makeHole();

            AST.MinusOp lowerBound = new AST.MinusOp(RHS,hole1Name);
            AST.AddOp upperBound = new AST.AddOp(RHS,hole2Name);

            AST.GtOp lower = new AST.GtOp(LHS,lowerBound);
            AST.LtOp upper = new AST.LtOp(LHS,upperBound);

            AST.AndOp andOp = new AST.AndOp(lower,upper);
            return andOp;
        }


        else if (E instanceof AST.GtOp){
            AST.Expression LHS = ((AST.GtOp) E).op1;
            AST.Expression RHS = ((AST.GtOp) E).op2;

            if (isApproximatedVar(LHS) || isApproximatedVar(RHS)){
                AST.Id hole1Name = makeHole();
                AST.AddOp upperBound = new AST.AddOp(RHS,hole1Name);
                ((AST.GtOp) E).op2 = upperBound;
                return E;
            }
        }


        else if (E instanceof AST.LtOp){
            AST.Expression LHS = ((AST.LtOp) E).op1;
            AST.Expression RHS = ((AST.LtOp) E).op2;

            if (isApproximatedVar(LHS) || isApproximatedVar(RHS)){
                AST.Id hole1Name = makeHole();
                AST.AddOp upperBound = new AST.AddOp(RHS,hole1Name);
                ((AST.LtOp) E).op2 = upperBound;
                return E;
            }
        }


        return E;
    }





    AST.Id makeHole(){
        String holeName = "Hole" + Integer.toString(holeNumber);
        AST.Id id = new AST.Id(holeName);
        holeNumber++;
        return id;
    }

    boolean isApproximatedVar(AST.Expression E){
        if (E instanceof AST.Id){
            if (changedVars.contains(((AST.Id) E).id)){
                return true;
            }
        }
        return false;
    }

}
