package org.bitcoins.node.serializers.messages.data

import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.node.util.NodeTestUtil
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 6/2/16.
  */
class RawTransactionMessageSerializerTest extends FlatSpec with MustMatchers {

  "RawTransactionMessageSerializer" must "read a TransactionMessage from a sequence of bytes" in {
    val txMessage =
      RawTransactionMessageSerializer.read(NodeTestUtil.rawTransaction)
    txMessage.transaction.txId.hex must be(
      BitcoinSUtil.flipEndianness(
        "44e504f5b7649d215be05ad9f09026dee95201244a3b218013c504a6a49a26ff"))
  }

  it must "write a TransactionMessage to its hex format" in {
    val txMessage =
      RawTransactionMessageSerializer.read(NodeTestUtil.rawTransaction)
    RawTransactionMessageSerializer.write(txMessage).toHex must be(
      NodeTestUtil.rawTransaction)
  }
}