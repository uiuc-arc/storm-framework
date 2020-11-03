package translators.listeners;

import grammar.AST;
import grammar.cfg.Statement;

public interface StatementListener extends CFGBaseListener{
    void enterAssignmentStatement();

    void enterForLoopStatement();

    void enterIfStmt();

    void enterDeclStatement(Statement statement);

    void enterFunctionCallStatement();

    void enterData(AST.Data data);
}
