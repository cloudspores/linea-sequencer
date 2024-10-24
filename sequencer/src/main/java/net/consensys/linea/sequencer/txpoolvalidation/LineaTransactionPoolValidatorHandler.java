package net.consensys.linea.sequencer.txpoolvalidation;

import org.hyperledger.besu.datatypes.Transaction;

public class LineaTransactionPoolValidatorHandler {
  public void handle(Transaction addedTransactionContext) {
    var gasLimit = addedTransactionContext.getGasLimit();
    var gasPrice = addedTransactionContext.getGasPrice();
    var maxPriorityFeePerGas = addedTransactionContext.getMaxPriorityFeePerGas();
    var maxFeePerGas = addedTransactionContext.getMaxFeePerGas();
    var value = addedTransactionContext.getValue();
    var hash = addedTransactionContext.getHash();
  }
}
