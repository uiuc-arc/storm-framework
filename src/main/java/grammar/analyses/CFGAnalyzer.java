
package grammar.analyses;
import grammar.cfg.*;
import grammar.AST;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.*;

public class CFGAnalyzer {


    public static void labelGenAndKill(Graph<BasicBlock,Edge> origCFG){
        Map <String, Set<Integer>> definitionLocations = new HashMap<>();
        AST.Statement astStatement;
        int statementLocation;
        Set<Integer> locations;

        ArrayList<Pair<Statement,String>> assignmentStatements = new ArrayList<>();
        ArrayList<Pair<Statement,String>> arrayAssignStatements = new ArrayList<>();
        ArrayList<Statement> nonAssignmentStatements = new ArrayList<>();

        //iterate through each basic block
        for (BasicBlock basicBlock : origCFG.vertexSet()){
            //iterate through each statement in the basic block
            for (Statement statement : basicBlock.getStatements()){
                statementLocation = statement.id;
                astStatement = statement.statement;

                //if the statement is an Assignment
                if (isAssignment(astStatement)){
                    AST.Expression lhs = ((AST.AssignmentStatement) astStatement).lhs; //get the LHS of the assignment

                    //if it is a regular assignment such as "a := 5"
                    if (lhs instanceof AST.Id){
                        String id = ((AST.Id) lhs).id;   //fetch the id of the variable being assigned to
                        Pair<Statement,String> stmtIdPair = new Pair<>(statement,id);
                        assignmentStatements.add(stmtIdPair);
                        //if the variable is not already in the Locations Map...
                        if (!definitionLocations.containsKey(id)){
                            locations = new HashSet<Integer>(); //create a new set to map the loc to if there is none
                            locations.add(statementLocation);
                            definitionLocations.put(id,locations);
                        }
                        //when the variable IS already in the Locations Map
                        else {
                            locations = definitionLocations.get(id);  //otherwise update/add to the existing set
                            locations.add(statementLocation);
                            definitionLocations.put(id,locations);
                        }
                    }
                    //if it is an array element assignment such as "a[0] := 4"
                    else if (lhs instanceof AST.ArrayAccess){
                        String id = ((AST.ArrayAccess) lhs).id.id;
                        Pair<Statement,String> stmtIdPair = new Pair<>(statement,id);
                        arrayAssignStatements.add(stmtIdPair);

                        //if the variable is not already in the Locations Map...
                        if (!definitionLocations.containsKey(id)){
                            locations = new HashSet<Integer>(); //create a new set to map the loc to if there is none
                            locations.add(statementLocation);
                            definitionLocations.put(id,locations);
                        }
                        //when the variable IS already in the Locations Map
                        else {
                            locations = definitionLocations.get(id);  //otherwise update/add to the existing set
                            locations.add(statementLocation);
                            definitionLocations.put(id,locations);
                        }
                    }

                    //Shouldn't ever reach this point
                    else{
                        throw new RuntimeException("lhs id was neither a variable nor array access");
                    }
                }

                //non assignment statements
                else{
                    nonAssignmentStatements.add(statement);
                }
            }
        }


        //for Regular (Non-Array) assignments
        for (Pair<Statement,String> regularAssignStatementPair :  assignmentStatements){

            Statement regularAssignStatement = regularAssignStatementPair.getKey();
            String id = regularAssignStatementPair.getValue();

            //create GEN set and Abstract State
            Map<String,Set<Integer>> genMap = new HashMap<>();
            Set<Integer> genLocs = new HashSet<>();
            genLocs.add(regularAssignStatement.id); //statement id is NOT the same as the lhs variable's id
            genMap.put(id,genLocs);

            ReachingDefinitionsState genAbsState = new ReachingDefinitionsState();
            genAbsState.setVarLocations(genMap);
            regularAssignStatement.dataflowFacts.put("GEN",genAbsState);

            //create KILL set and Abstract State
            Set<Integer> killLocs = new HashSet<>();
            Map<String,Set<Integer>> killMap = new HashMap<>();
            killLocs.addAll(definitionLocations.get(id));
            killLocs.removeAll(genLocs);
            killMap.put(id,killLocs);

            ReachingDefinitionsState killAbsState = new ReachingDefinitionsState();
            killAbsState.setVarLocations(killMap);
            regularAssignStatement.dataflowFacts.put("KILL",killAbsState);

        }

        //for Array-indexed assignments
        for (Pair<Statement,String> arrayAccAssignStatementPair : arrayAssignStatements){
            Statement arrayAccAssignStmt = arrayAccAssignStatementPair.getKey();
            String id = arrayAccAssignStatementPair.getValue();

            //create GEN set and Abstract State
            Map<String,Set<Integer>> genMap = new HashMap<>();
            Set<Integer> genLocs = new HashSet<>();
            genLocs.add(arrayAccAssignStmt.id);
            genMap.put(id,genLocs);

            ReachingDefinitionsState genAbsState = new ReachingDefinitionsState();
            genAbsState.setVarLocations(genMap);
            arrayAccAssignStmt.dataflowFacts.put("GEN",genAbsState);

            //make the kill set empty
            ReachingDefinitionsState emptyState = new ReachingDefinitionsState();
            arrayAccAssignStmt.dataflowFacts.put("KILL",emptyState);
        }

        for (Statement nonAssignStatement : nonAssignmentStatements){
            labelNonAssignGenAndKill(nonAssignStatement);
        }
    }

//    public static void labelArrayAccessAssignGenAndKill(Statement arrAccessAssignStmt){
//        assert (arrAccessAssignStmt.statement instanceof AST.AssignmentStatement);
//
//    }

    public static void labelNonAssignGenAndKill(Statement nonAssignStmt){
        assert (!(nonAssignStmt.statement instanceof AST.AssignmentStatement));
        ReachingDefinitionsState emptyState = new ReachingDefinitionsState();
        nonAssignStmt.dataflowFacts.put("GEN",emptyState);
        nonAssignStmt.dataflowFacts.put("KILL",emptyState);
    }

    public static void initCFGLabels(Graph<BasicBlock,Edge> origCFG, AbstractDomain AD){
        Set<String> allUsedVars = getAllDefinedVars(origCFG);
        for (BasicBlock bb : origCFG.vertexSet()){
            for (Statement stmt : bb.getStatements()){
                AbstractState init = AD.getInitialVal(allUsedVars);
                stmt.dataflowFacts.put("OUT",init);
            }
        }
    }


    public static boolean isAssignment(Statement s){
        AST.Statement statement = s.statement;
        return (statement instanceof AST.AssignmentStatement);
    }

    public static boolean isAssignment(AST.Statement statement){
        return (statement instanceof AST.AssignmentStatement);
    }

    public static Set<String> getAllDefinedVars(Graph<BasicBlock,Edge> origCFG){
        Set<String> allDefinedVars = new HashSet<>();
        for (BasicBlock basicBlock : origCFG.vertexSet()){
            for (Statement statement : basicBlock.getStatements()){
                AST.Statement stmt = statement.statement;
                if (stmt instanceof AST.AssignmentStatement){
                    AST.AssignmentStatement assignStmt = (AST.AssignmentStatement) stmt;
                    AST.Expression lhs = assignStmt.lhs;

                    if (lhs instanceof AST.Id){
                        String id = ((AST.Id) lhs).id;
                        allDefinedVars.add(id);
                    }
                    else if (lhs instanceof AST.ArrayAccess){
                        AST.Id arrayAccessId = ((AST.ArrayAccess) lhs).id;
                        String id = arrayAccessId.id;
                        allDefinedVars.add(id);
                    }
                }
            }
        }
        return allDefinedVars;
    }

    void markTransformedStmts(Graph<BasicBlock,Edge> CFG, ArrayList<AST.Statement> changedStmts){
        for (BasicBlock bb : CFG.vertexSet()) {
            for (Statement st : bb.getStatements()) {
                if (changedStmts.contains(st.statement)){
                    st.isTransformed=true;
                }
            }
        }
    }

    Statement getStatement(Graph<BasicBlock,Edge> CFG, int key){
        for (BasicBlock bb : CFG.vertexSet()) {
            for (Statement st : bb.getStatements()) {
                if (st.id==key){
                    return st;
                }
            }
        }

        return null;
    }


    Graph<Statement,Edge> UseDefAnalysis(Graph<BasicBlock,Edge> CFG) {
        Graph<Statement,Edge> UseDefGraph = new DefaultDirectedGraph<>(Edge.class);

        for (BasicBlock bb : CFG.vertexSet()) {
            for (Statement st : bb.getStatements()) {

                ArrayList<String> vars;
                Map<String,AbstractState> dataflowFact = st.dataflowFacts;

                if (st.statement instanceof AST.IfStmt) {
                    //get Used Vars
                    AST.Expression cond = ((AST.IfStmt) st.statement).condition;
                    vars = getVars(cond);
                    Set<Integer> deps = Collections.emptySet();

                    for (String var : vars){
                        if (dataflowFact.keySet().contains(var)){
                            Set<Integer> reachingStmtIds = ((ReachingDefinitionsState) dataflowFact).VarLocations.get(var);
                            deps.addAll(reachingStmtIds);
                        }
                    }

                    for (Integer dep : deps){
                        Statement s = getStatement(CFG,dep);
                    }

                }
                else if (st.statement instanceof AST.AssignmentStatement) {
                    //get Used Vars
                    AST.Expression rhs = ((AST.AssignmentStatement) st.statement).rhs;
                    vars = getVars(rhs);

                    Set<Integer> deps = Collections.emptySet();

                    for (String var : vars){
                        if (dataflowFact.keySet().contains(var)){
                            Set<Integer> reachingStmtIds = ((ReachingDefinitionsState) dataflowFact).VarLocations.get(var);
                            deps.addAll(reachingStmtIds);
                        }
                    }
                    for (Integer dep : deps){
                        Statement s = getStatement(CFG,dep);
                    }

                }
            }
        }
        return null;
    }

    ArrayList<String> getVarsAux(AST.Expression E, ArrayList<String> vars){
        if (E instanceof AST.MinusOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.MinusOp) E).op1,vars);
            return getVarsAux(((AST.MinusOp) E).op2,op1_vars);
        }
        else if (E instanceof AST.AddOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.AddOp) E).op1,vars);
            return getVarsAux(((AST.AddOp) E).op2,op1_vars);
        }

        else if (E instanceof AST.MulOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.MulOp) E).op1,vars);
            return getVarsAux(((AST.MulOp) E).op2,op1_vars);
        }

        else if (E instanceof AST.DivOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.DivOp) E).op1,vars);
            return getVarsAux(((AST.DivOp) E).op2,op1_vars);
        }

        else if (E instanceof AST.AndOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.AndOp) E).op1,vars);
            return getVarsAux(((AST.AndOp) E).op2,op1_vars);
        }

        else if (E instanceof AST.OrOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.OrOp) E).op1,vars);
            return getVarsAux(((AST.OrOp) E).op2,op1_vars);
        }

        else if (E instanceof AST.EqOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.EqOp) E).op1,vars);
            return getVarsAux(((AST.EqOp) E).op2,op1_vars);
        }

        else if (E instanceof AST.GeqOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.GeqOp) E).op1,vars);
            return getVarsAux(((AST.GeqOp) E).op2,op1_vars);
        }

        else if (E instanceof AST.GtOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.GtOp) E).op1,vars);
            return getVarsAux(((AST.GtOp) E).op2,op1_vars);
        }

        else if (E instanceof AST.LeqOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.LeqOp) E).op1,vars);
            return getVarsAux(((AST.LeqOp) E).op2,op1_vars);
        }
        else if (E instanceof AST.NeOp){
            ArrayList<String> op1_vars = getVarsAux(((AST.NeOp) E).op1,vars);
            return getVarsAux(((AST.NeOp) E).op2,op1_vars);
        }

        /*else if (E instanceof AST.FunctionCall){
            ArrayList<String> arg_vars = new ArrayList<>();
            for ()
        }*/

        //one of the base cases
        else if (E instanceof AST.Number){
            return vars;
        }

        //one of the base cases
        else if (E instanceof AST.Id){
            vars.add(((AST.Id) E).id);
            return vars;
        }
        return null;
    }


    ArrayList<String> getVars(AST.Expression E){
        ArrayList<String> vars = new ArrayList<>();
        return getVarsAux(E,vars);
    }
}
