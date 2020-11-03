package translators;

import grammar.AST;
import grammar.cfg.*;
import org.apache.commons.lang3.tuple.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import translators.visitors.Edward2Visitor;
import utils.CommonUtils;

import java.io.FileNotFoundException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static tool.probfuzz.Utils.EDWARD2RUNNER;

public class Edward2Translator implements ITranslator {


    private static final String edward2TemplateFile = "src/main/resources/edward2Template";
    private String modelCode = "";
    private String dataArgs = "";
    private String dataStr = "";
    private String paramArgs = "";
    private String paramStr = "";
    private String initStr = "";
    private String dataSection = "";

    public String getModelCode() {
        return modelCode;
    }

    private String readTemplate(){
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(edward2TemplateFile));
            String str = new String(bytes);
            return str;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void translate(ArrayList<Section> sections) throws Exception {
        Set<BasicBlock> visited = new HashSet<>();
        for(Section section:sections){
            if(section.sectionType == SectionType.DATA){
                dataSection += getDataString(section.basicBlocks.get(0).getData());

            }
            else if (section.sectionType == SectionType.FUNCTION){
                if(section.sectionName.equals("main")){
                    for(BasicBlock basicBlock:section.basicBlocks){
                        BasicBlock curBlock = basicBlock;
                        while(!visited.contains(curBlock)){
                            visited.add(curBlock);
                            String block_text = translate_block(curBlock);

                            if(curBlock.getIncomingEdges().containsKey("true")){
                                block_text = "{\n" +block_text;
                            }

                            if(curBlock.getOutgoingEdges().containsKey("back")){
                                block_text = block_text +"}\n";
                            }

                            if(curBlock.getParent().sectionName.equalsIgnoreCase("main")){
                                modelCode+=block_text;
                            }
                            else if(curBlock.getParent().sectionName.equalsIgnoreCase("transformedparam")){
                                modelCode += block_text;
                            }
                            else if (curBlock.getParent().sectionName.equalsIgnoreCase("transformeddata")) {
                                modelCode += block_text;
                            } else if (curBlock.getParent().sectionName.equalsIgnoreCase("generatedquantities")) {
                                modelCode += block_text;
                            }

                            if(curBlock.getEdges().size() > 0){
                                BasicBlock prevBlock = curBlock;
                                if(curBlock.getEdges().size() == 1){
                                    curBlock = curBlock.getEdges().get(0).getTarget();
                                }
                                else{
                                    String label = curBlock.getEdges().get(0).getLabel();
                                    if(label != null && label.equalsIgnoreCase("false") && !visited.contains(curBlock.getEdges().get(1).getTarget())){
                                        curBlock = curBlock.getEdges().get(1).getTarget();
                                    }
                                    else
                                        curBlock = curBlock.getEdges().get(0).getTarget();
                                }
                                //if next is meet block
                                if(curBlock.getIncomingEdges().containsKey("meet") && !isIfNode(prevBlock)){
                                    modelCode += "}\n";

                                }

                            }
                        }
                    }
                }
                else{
                    throw new Exception("Unknown Function! ");
                }
            }
        }
    }

    private boolean isIfNode(BasicBlock basicBlock){
        return basicBlock.getStatements().size() == 1 && basicBlock.getStatements().get(0).statement instanceof AST.IfStmt;
    }

    private String translate_block(BasicBlock basicBlock){
        SymbolTable symbolTable = basicBlock.getSymbolTable();
        String output = "";
        if(basicBlock.getStatements().size() == 0)
            return output;

        for(Statement statement:basicBlock.getStatements()){
            if(statement.statement instanceof AST.AssignmentStatement){
                AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                output += new Edward2Visitor(basicBlock, false).evaluate(assignmentStatement.lhs) + "=" +
                        new Edward2Visitor(basicBlock, true, assignmentStatement.lhs).evaluate(assignmentStatement.rhs) + "\n";

                if(assignmentStatement.rhs instanceof AST.FunctionCall && ((AST.FunctionCall) assignmentStatement.rhs).isDistribution){
                    output = output.substring(0, output.length()-2) + ",name = \"" + assignmentStatement.lhs.toString() + "\")\n";
                    if(CommonUtils.isPrior(statement, assignmentStatement.lhs)) {
                        this.paramStr += assignmentStatement.lhs.toString() + "=" + assignmentStatement.lhs.toString() + ",";
                        this.initStr += String.format("tf.random_normal([%s]),", SymbolInfo.getDimsString(basicBlock.getSymbolTable(), assignmentStatement.lhs) );
                    }
                    else {
                        this.paramStr += assignmentStatement.lhs.toString() + "=" + "data['" + assignmentStatement.lhs.toString() + "'],";

                    }
                }
            }
            else if(statement.statement instanceof AST.ForLoop){
//                AST.ForLoop loop = (AST.ForLoop) statement.statement;
//                output += "for(" + loop.toString() + ")\n";
            }
            else if(statement.statement instanceof AST.Decl){
                    AST.Decl declaration = (AST.Decl) statement.statement;
                    if(CommonUtils.isPrior(statement, declaration.id)){
                        this.paramArgs += declaration.id +",";
                        //this.paramStr += declaration.id + "=" + declaration.id+",";
                    }
            }
        }

        if(basicBlock.getIncomingEdges().containsKey("true") || basicBlock.getIncomingEdges().containsKey("false")){
            return "{\n" + output + "}\n";
        }


        return output ;
    }

    private String getDataString(ArrayList<AST.Data> datasets){
        String d = "";
        for(AST.Data data: datasets){
            d += "data['" +  data.decl.id  + "'] =";
            dataArgs += data.decl.id + ",";
            dataStr += "data['" + data.decl.id +"'],";

            Edward2Visitor.dataItems.add(data.decl.id.toString());

            if(data.expression != null){
                d+=data.expression.toString();
            }
            else if(data.array != null){
                INDArray arr = CommonUtils.parseArray(data.array, data.decl.dtype.primitive == AST.Primitive.INTEGER);
                d+="np.array(" + arr.toString().replaceAll("\\s", "") + ").astype(np.float32)";
            }
            else if(data.vector != null){
                INDArray arr = CommonUtils.parseVector(data.vector, data.decl.dtype.primitive == AST.Primitive.INTEGER);
                d+="np.array(" + arr.toString().replaceAll("\\s", "") + ").astype(np.float32)";
            }

            d+="\n";
        }

        return d;
    }

    public String getDataSection() {
        return dataSection;
    }

    public String getCode(){
        String template = readTemplate();
        template = template.replace("$(data)", this.dataSection);
        //template = template.replace("$(data_str)", this.dataStr.substring(0, this.dataStr.length() - 1));
        template = template.replace("$(data_str)", "data");
        template = template.replace("$(model)", "def model(data):\n    " + this.modelCode.replaceAll("\n","\n    "));
        template = template.replace("$(params_list)", this.paramArgs.substring(0, this.paramArgs.length() - 1));
        template = template.replace("$(params)", this.paramStr.substring(0, this.paramStr.length() -1));
        template = template.replace("$(init)",  this.initStr.substring(0, this.initStr.length() -1));
        return template;
    }

    @Override
    public Pair run() {
        return run("/tmp/edward2code.py");
    }

    public Pair run(String codeFileName){
        System.out.println("Running Edward...");
        try {
            FileWriter fileWriter = new FileWriter(codeFileName);
            fileWriter.write(this.getCode());
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Pair results = CommonUtils.runCode(codeFileName, EDWARD2RUNNER);
        System.out.println(results.getLeft());
        System.out.println(results.getRight());
        return results;
    }
}
