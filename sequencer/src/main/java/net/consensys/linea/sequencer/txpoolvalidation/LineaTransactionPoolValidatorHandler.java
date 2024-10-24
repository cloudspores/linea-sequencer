package net.consensys.linea.sequencer.txpoolvalidation;

import java.util.Optional;

import org.hyperledger.besu.datatypes.Transaction;

public class LineaTransactionPoolValidatorHandler {

  private Transaction currentTransaction;

  public void onTransactionAdded(Transaction addedTransaction) {
    this.currentTransaction = addedTransaction;
  }

  public Optional<Transaction> getCurrentTransaction() {
    return Optional.ofNullable(currentTransaction);
  }
}
