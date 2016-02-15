package org.scalacoin.util

import org.scalacoin.currency.CurrencyUnits
import org.scalacoin.protocol.VarIntImpl
import org.scalacoin.protocol.script.{ScriptSignatureImpl, ScriptPubKey, ScriptPubKeyImpl, ScriptSignature}
import org.scalacoin.protocol.transaction._
import org.scalacoin.script.constant.{OP_0, ScriptToken}

/**
 * Created by chris on 2/12/16.
 */
trait TransactionTestUtil {

  /**
   * Mimics the test utility found in bitcoin core
   * https://github.com/bitcoin/bitcoin/blob/605c17844ea32b6d237db6d83871164dc7d59dab/src/test/script_tests.cpp#L73
   * @param scriptSignature
   * @param tx
   */
  def buildSpendingTransaction(scriptSignature : ScriptSignature, tx : Transaction) : Transaction = {

    val outpoint = TransactionOutPointImpl(tx.txId,0)
    val input = TransactionInputImpl(outpoint,VarIntImpl(0,0),scriptSignature,0xFFFFFFFF)
    //empty script pubkey
    val scriptPubKey = ScriptPubKeyImpl(Seq(),"",Seq())
    val output = TransactionOutputImpl(CurrencyUnits.oneSatoshi,0,scriptPubKey)
    TransactionImpl(TransactionConstants.version,Seq(input),Seq(output),TransactionConstants.lockTime)
  }


  /**
   * Mimics this test utility found in bitcoin core
   * https://github.com/bitcoin/bitcoin/blob/605c17844ea32b6d237db6d83871164dc7d59dab/src/test/script_tests.cpp#L57
   * @param scriptPubKey
   * @return
   */
  def buildCreditingTransaction(scriptPubKey : ScriptPubKey) : Transaction = {
    val outpoint = TransactionOutPointImpl("",0)

    val scriptSignature = ScriptSignatureImpl(Seq(OP_0,OP_0),"0000")
    val input = TransactionInputImpl(outpoint,VarIntImpl(0,0),scriptSignature,0xFFFFFFFF)
    val output = TransactionOutputImpl(CurrencyUnits.oneSatoshi,0,scriptPubKey)

    TransactionImpl(TransactionConstants.version,Seq(input),Seq(output),TransactionConstants.lockTime)
  }
}
