package com.auberer.compilerdesignlectureproject.codegen;

import com.auberer.compilerdesignlectureproject.ast.*;
import com.auberer.compilerdesignlectureproject.codegen.instructions.CallInstruction;
import com.auberer.compilerdesignlectureproject.codegen.instructions.Instruction;
import com.auberer.compilerdesignlectureproject.codegen.instructions.PrintInstruction;
import lombok.Getter;

public class IRGenerator extends ASTVisitor<IRExprResult> {

  // IR module, which represents the whole program
  @Getter
  private final Module module;
  // The basic block, which is currently the insert point for new instructions
  private BasicBlock currentBlock = null;

  public IRGenerator(String moduleName) {
    module = new Module(moduleName);
  }

  @Override
  public IRExprResult visitEntry(ASTEntryNode node) {
    // We did not enter a function yet, so the current block has to be null
    assert currentBlock == null;

    // Visit children
    visitChildren(node);

    assert currentBlock == null;
    return null;
  }

  @Override
  public IRExprResult visitPrintBuiltin(ASTPrintBuiltinCallNode node) {
    // Create print instruction and append it to the current BasicBlock
    PrintInstruction printInstruction = new PrintInstruction(node);
    pushToCurrentBlock(printInstruction);

    return new IRExprResult(null, node, null);
  }

  // ToDo: Insert other visit methods here

  /**
   * Can be used to set the instruction insert point to a specific block
   *
   * @param targetBlock Block to switch to
   */
  private void switchToBlock(BasicBlock targetBlock) {
    assert targetBlock != null;

    // Check if the old block was terminated
    assert currentBlock == null || isBlockTerminated(currentBlock);
    // Set insert point to the new basic block
    currentBlock = targetBlock;
  }

  /**
   * Finalizes the IR of the current function by setting the current block to null
   */
  private void finalizeFunction() {
    assert currentBlock != null;
    assert isBlockTerminated(currentBlock);
    currentBlock = null;
  }

  /**
   * Appends the given instruction to the current block
   *
   * @param instruction Instruction to append
   */
  private void pushToCurrentBlock(Instruction instruction) {
    assert instruction != null;
    assert currentBlock != null;
    assert isBlockTerminated(currentBlock);

    // Push to the back of the current block
    currentBlock.pushInstruction(instruction);
  }

  /**
   * Checks if the given block is terminated
   *
   * @param block Block to check
   * @return True if the block is terminated
   */
  private boolean isBlockTerminated(BasicBlock block) {
    return !block.getInstructions().isEmpty() && block.getInstructions().getLast().isTerminator();
  }

  @Override
  public IRExprResult visitFctCall(ASTFctCallNode node) {
    CallInstruction callInstruction = new CallInstruction(node, module.getFunction(node.getName()), node.getCallParams());
    pushToCurrentBlock(callInstruction);
    return new IRExprResult(node.getValue(), node, null);
  }

  @Override
  public IRExprResult visitFctDef(ASTFctDefNode node) {

    BasicBlock body = new BasicBlock("body");
    BasicBlock params = new BasicBlock("params");

    switchToBlock(params);
    visitParamLst(node.getParams());

    Function function = new Function(node.getName());
    function.setEntryBlock(body);
    module.addFunction(function);
    visitLogic(node.getBody());

    switchToBlock(new BasicBlock("exit"));
    return new IRExprResult(node.getValue(), node, null);
  }

  @Override
  public IRExprResult visitParamLst(ASTParamLstNode node){
    int i = 0;
    for (ASTParamNode paramNode: node.getParamNodes()){
      BasicBlock block = new BasicBlock("param" + i);
      switchToBlock(block);
      i++;
      visitParam(paramNode);
    }
    return new IRExprResult(null, node, null);
  }

  @Override
  public IRExprResult visitParam(ASTParamNode node){
    visitType(node.getDataType());
    return new IRExprResult(null, node, null);
  }
}


