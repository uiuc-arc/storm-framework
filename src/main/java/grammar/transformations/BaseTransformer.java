package grammar.transformations;

import grammar.cfg.Section;

import java.util.ArrayList;
import java.util.Queue;

public abstract class BaseTransformer {

    public abstract void transform() throws Exception;
    public abstract void undo() throws Exception;
    public abstract void availTransformers(ArrayList<Section> sections, Queue<BaseTransformer> availTrans) throws Exception;
    public abstract  boolean isTransformed();

    public abstract boolean statementFilterFunction();
//    public abstract AST.AssignmentStatement getCorrespondingAssignStatement();
}
