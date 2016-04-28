package org.scalacoin.marshallers.rpc.bitcoincore.blockchain

import org.scalacoin.marshallers.MarshallerUtil
import org.scalacoin.marshallers.rpc.bitcoincore.blockchain.softforks.SoftForkMarshaller
import org.scalacoin.rpc.bitcoincore.blockchain.softforks.SoftForks
import org.scalacoin.rpc.bitcoincore.blockchain.{BlockChainInfoImpl, BlockchainInfo}
import spray.json._
import SoftForkMarshaller._

/**
 * Created by Tom on 1/11/2016.
 */
object BlockchainInfoMarshaller extends DefaultJsonProtocol with MarshallerUtil{
  val chainKey = "chain"
  val blockCountKey = "blocks"
  val headerCountKey = "headers"
  val bestBlockHashKey = "bestblockhash"
  val difficultyKey = "difficulty"
  val verificationProgressKey = "verificationprogress"
  val chainWorkKey = "chainwork"
  //val softForksKey = "softforks"

  implicit object BlockchainInfoFormatter extends RootJsonFormat[BlockchainInfo] {
    override def read (value : JsValue) : BlockchainInfo = {
      val obj = value.asJsObject
      val chain = obj.fields(chainKey).convertTo[String]
      val blockCount = obj.fields(blockCountKey).convertTo[Int]
      val headerCount = obj.fields(headerCountKey).convertTo[Int]
      val bestBlockHash = obj.fields(bestBlockHashKey).convertTo[String]
      val difficulty = obj.fields(difficultyKey).convertTo[Double]
      val verificationProgress = obj.fields(verificationProgressKey).convertTo[Double]
      val chainWork = obj.fields(chainWorkKey).convertTo[String]
      //val softForks : Seq[SoftForks] = convertToSoftForksList(obj.fields(softForksKey))
      BlockChainInfoImpl(chain, blockCount, headerCount, bestBlockHash, difficulty, verificationProgress, chainWork) //softForks
    }

    override def write (detail : BlockchainInfo) : JsValue = {
      //val softForks : JsArray = convertToJsArray(detail.softForks)


      val m : Map[String, JsValue] = Map (
        chainKey -> JsString(detail.chain),
        blockCountKey -> JsNumber(detail.blockCount),
        headerCountKey -> JsNumber(detail.headerCount),
        bestBlockHashKey -> JsString(detail.bestBlockHash),
        difficultyKey -> JsNumber(detail.difficulty),
        verificationProgressKey -> JsNumber(detail.verificationProgress),
        chainWorkKey -> JsString(detail.chainWork)
        //softForksKey -> softForks
        )
      JsObject(m)
    }
  }
}