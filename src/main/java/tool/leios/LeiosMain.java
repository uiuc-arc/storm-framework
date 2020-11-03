package tool.leios;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors
import grammar.cfg.CFGBuilder;

public class LeiosMain {

    public static void outputFile(String translator,String output_file,Continualizer Cont){

        try {
            String preTransformOutputCode = null;
            if (translator.equals("stan")){
                preTransformOutputCode = Cont.getStanCode();
            } else if (translator.equals("pyro")){
                preTransformOutputCode = Cont.getPyroCode();
            } else if (translator.equals("edward")){
                preTransformOutputCode = Cont.getEdwardCode();
            }
            FileWriter myWriter = new FileWriter(output_file);
            myWriter.write(preTransformOutputCode);
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {

        String file_name = null;
        if (args.length > 0) {
            file_name = args[0];



            //translator
            String translator = "stan"; //default
            if (args.length > 1){
                translator = args[1];
                translator = translator.toLowerCase();
                assert (!(translator.equals("stan") || translator.equals("edward")  || translator.equals("pyro") )) : "language not supported!";
            }
            String preTransformOutput = null;
            String postTransformOutput = null;


            if (translator.equals("stan")){
                preTransformOutput = "preTransformOutput.stan";
                postTransformOutput = "postTransformOutput.stan";
            }
            else if (translator.equals("pyro") || translator.equals("edward")){
                preTransformOutput = "preTransformOutput.py";
                postTransformOutput = "postTransformOutput.py";
            }



            CFGBuilder builder = new CFGBuilder(file_name,"continualization.png");
            Continualizer Cont = new Continualizer(builder);

            //output the ORIGINAL CODE BEFORE DOING ANYTHING!
            outputFile(translator,preTransformOutput,Cont);

            Cont.continualize();

            //printout/output the transformed Source code
            outputFile(translator,postTransformOutput,Cont);

        }
    }
}
