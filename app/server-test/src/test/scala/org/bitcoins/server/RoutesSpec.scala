package org.bitcoins.server

import java.time.{ZoneId, ZonedDateTime}

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.server.ValidationRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.bitcoins.chain.api.ChainApi
import org.bitcoins.core.Core
import org.bitcoins.core.api.CoreApi
import org.bitcoins.core.crypto.DoubleSha256DigestBE
import org.bitcoins.core.currency.{Bitcoins, CurrencyUnit}
import org.bitcoins.core.protocol.BitcoinAddress
import org.bitcoins.core.protocol.BlockStamp.{
  BlockHash,
  BlockHeight,
  BlockTime,
  InvalidBlockStamp
}
import org.bitcoins.core.protocol.transaction.{EmptyTransaction, Transaction}
import org.bitcoins.core.psbt.PSBT
import org.bitcoins.core.util.FutureUtil
import org.bitcoins.core.wallet.fee.FeeUnit
import org.bitcoins.node.Node
import org.bitcoins.wallet.MockUnlockedWalletApi
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import ujson.Value.InvalidData
import ujson._

import scala.concurrent.Future

class RoutesSpec
    extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with MockFactory {

  // the genesis address
  val testAddressStr = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
  val testAddress = BitcoinAddress(testAddressStr).get

  val mockWalletApi = mock[MockUnlockedWalletApi]

  val mockChainApi = mock[ChainApi]

  val mockNode = mock[Node]

  val chainRoutes = ChainRoutes(mockChainApi)

  val nodeRoutes = NodeRoutes(mockNode)

  val walletRoutes = WalletRoutes(mockWalletApi, mockNode)

  val coreRoutes: CoreRoutes = CoreRoutes(Core)

  "The server" should {

    "combine PSBTs" in {
      val psbt1 = PSBT(
        "70736274ff01003f0200000001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000ffffffff010000000000000000036a0100000000000a0f0102030405060708090f0102030405060708090a0b0c0d0e0f000a0f0102030405060708090f0102030405060708090a0b0c0d0e0f000a0f0102030405060708090f0102030405060708090a0b0c0d0e0f00")
      val psbt2 = PSBT(
        "70736274ff01003f0200000001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000ffffffff010000000000000000036a0100000000000a0f0102030405060708100f0102030405060708090a0b0c0d0e0f000a0f0102030405060708100f0102030405060708090a0b0c0d0e0f000a0f0102030405060708100f0102030405060708090a0b0c0d0e0f00")
      val expected = PSBT(
        "70736274ff01003f0200000001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000ffffffff010000000000000000036a0100000000000a0f0102030405060708090f0102030405060708090a0b0c0d0e0f0a0f0102030405060708100f0102030405060708090a0b0c0d0e0f000a0f0102030405060708090f0102030405060708090a0b0c0d0e0f0a0f0102030405060708100f0102030405060708090a0b0c0d0e0f000a0f0102030405060708090f0102030405060708090a0b0c0d0e0f0a0f0102030405060708100f0102030405060708090a0b0c0d0e0f00")

      val route =
        coreRoutes.handleCommand(
          ServerCommand("combinepsbts",
                        Arr(Arr(Str(psbt1.base64), Str(psbt2.base64)))))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual s"""{"result":"${expected.base64}","error":null}"""
      }

      val joinRoute =
        coreRoutes.handleCommand(
          ServerCommand("joinpsbts",
                        Arr(Arr(Str(psbt1.base64), Str(psbt2.base64)))))

      Get() ~> joinRoute ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual s"""{"result":"${expected.base64}","error":null}"""
      }
    }

    "finalize a PSBT" in {
      val psbt = PSBT(
        "70736274ff01009a020000000258e87a21b56daf0c23be8e7070456c336f7cbaa5c8757924f545887bb2abdd750000000000ffffffff838d0427d0ec650a68aa46bb0b098aea4422c071b2ca78352a077959d07cea1d0100000000ffffffff0270aaf00800000000160014d85c2b71d0060b09c9886aeb815e50991dda124d00e1f5050000000016001400aea9a2e5f0f876a588df5546e8742d1d87008f00000000000100bb0200000001aad73931018bd25f84ae400b68848be09db706eac2ac18298babee71ab656f8b0000000048473044022058f6fc7c6a33e1b31548d481c826c015bd30135aad42cd67790dab66d2ad243b02204a1ced2604c6735b6393e5b41691dd78b00f0c5942fb9f751856faa938157dba01feffffff0280f0fa020000000017a9140fb9463421696b82c833af241c78c17ddbde493487d0f20a270100000017a91429ca74f8a08f81999428185c97b5d852e4063f6187650000002202029583bf39ae0a609747ad199addd634fa6108559d6c5cd39b4c2183f1ab96e07f473044022074018ad4180097b873323c0015720b3684cc8123891048e7dbcd9b55ad679c99022073d369b740e3eb53dcefa33823c8070514ca55a7dd9544f157c167913261118c01220202dab61ff49a14db6a7d02b0cd1fbb78fc4b18312b5b4e54dae4dba2fbfef536d7483045022100f61038b308dc1da865a34852746f015772934208c6d24454393cd99bdf2217770220056e675a675a6d0a02b85b14e5e29074d8a25a9b5760bea2816f661910a006ea01010304010000000104475221029583bf39ae0a609747ad199addd634fa6108559d6c5cd39b4c2183f1ab96e07f2102dab61ff49a14db6a7d02b0cd1fbb78fc4b18312b5b4e54dae4dba2fbfef536d752ae2206029583bf39ae0a609747ad199addd634fa6108559d6c5cd39b4c2183f1ab96e07f10d90c6a4f000000800000008000000080220602dab61ff49a14db6a7d02b0cd1fbb78fc4b18312b5b4e54dae4dba2fbfef536d710d90c6a4f0000008000000080010000800001012000c2eb0b0000000017a914b7f5faf40e3d40a5a459b1db3535f2b72fa921e887220203089dc10c7ac6db54f91329af617333db388cead0c231f723379d1b99030b02dc473044022062eb7a556107a7c73f45ac4ab5a1dddf6f7075fb1275969a7f383efff784bcb202200c05dbb7470dbf2f08557dd356c7325c1ed30913e996cd3840945db12228da5f012202023add904f3d6dcf59ddb906b0dee23529b7ffb9ed50e5e86151926860221f0e73473044022065f45ba5998b59a27ffe1a7bed016af1f1f90d54b3aa8f7450aa5f56a25103bd02207f724703ad1edb96680b284b56d4ffcb88f7fb759eabbe08aa30f29b851383d2010103040100000001042200208c2353173743b595dfb4a07b72ba8e42e3797da74e87fe7d9d7497e3b2028903010547522103089dc10c7ac6db54f91329af617333db388cead0c231f723379d1b99030b02dc21023add904f3d6dcf59ddb906b0dee23529b7ffb9ed50e5e86151926860221f0e7352ae2206023add904f3d6dcf59ddb906b0dee23529b7ffb9ed50e5e86151926860221f0e7310d90c6a4f000000800000008003000080220603089dc10c7ac6db54f91329af617333db388cead0c231f723379d1b99030b02dc10d90c6a4f00000080000000800200008000220203a9a4c37f5996d3aa25dbac6b570af0650394492942460b354753ed9eeca5877110d90c6a4f000000800000008004000080002202027f6399757d2eff55a136ad02c684b1838b6556e5f1b6b34282a94b6b5005109610d90c6a4f00000080000000800500008000")
      val expected = PSBT(
        "70736274ff01009a020000000258e87a21b56daf0c23be8e7070456c336f7cbaa5c8757924f545887bb2abdd750000000000ffffffff838d0427d0ec650a68aa46bb0b098aea4422c071b2ca78352a077959d07cea1d0100000000ffffffff0270aaf00800000000160014d85c2b71d0060b09c9886aeb815e50991dda124d00e1f5050000000016001400aea9a2e5f0f876a588df5546e8742d1d87008f00000000000100bb0200000001aad73931018bd25f84ae400b68848be09db706eac2ac18298babee71ab656f8b0000000048473044022058f6fc7c6a33e1b31548d481c826c015bd30135aad42cd67790dab66d2ad243b02204a1ced2604c6735b6393e5b41691dd78b00f0c5942fb9f751856faa938157dba01feffffff0280f0fa020000000017a9140fb9463421696b82c833af241c78c17ddbde493487d0f20a270100000017a91429ca74f8a08f81999428185c97b5d852e4063f6187650000000107da00473044022074018ad4180097b873323c0015720b3684cc8123891048e7dbcd9b55ad679c99022073d369b740e3eb53dcefa33823c8070514ca55a7dd9544f157c167913261118c01483045022100f61038b308dc1da865a34852746f015772934208c6d24454393cd99bdf2217770220056e675a675a6d0a02b85b14e5e29074d8a25a9b5760bea2816f661910a006ea01475221029583bf39ae0a609747ad199addd634fa6108559d6c5cd39b4c2183f1ab96e07f2102dab61ff49a14db6a7d02b0cd1fbb78fc4b18312b5b4e54dae4dba2fbfef536d752ae0001012000c2eb0b0000000017a914b7f5faf40e3d40a5a459b1db3535f2b72fa921e8870107232200208c2353173743b595dfb4a07b72ba8e42e3797da74e87fe7d9d7497e3b20289030108da0400473044022062eb7a556107a7c73f45ac4ab5a1dddf6f7075fb1275969a7f383efff784bcb202200c05dbb7470dbf2f08557dd356c7325c1ed30913e996cd3840945db12228da5f01473044022065f45ba5998b59a27ffe1a7bed016af1f1f90d54b3aa8f7450aa5f56a25103bd02207f724703ad1edb96680b284b56d4ffcb88f7fb759eabbe08aa30f29b851383d20147522103089dc10c7ac6db54f91329af617333db388cead0c231f723379d1b99030b02dc21023add904f3d6dcf59ddb906b0dee23529b7ffb9ed50e5e86151926860221f0e7352ae00220203a9a4c37f5996d3aa25dbac6b570af0650394492942460b354753ed9eeca5877110d90c6a4f000000800000008004000080002202027f6399757d2eff55a136ad02c684b1838b6556e5f1b6b34282a94b6b5005109610d90c6a4f00000080000000800500008000")

      val route =
        coreRoutes.handleCommand(
          ServerCommand("finalizepsbt", Arr(Str(psbt.hex))))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual s"""{"result":"${expected.base64}","error":null}"""

      }
    }

    "extract a transaction from a PSBT" in {
      val psbt = PSBT(
        "70736274ff01009a020000000258e87a21b56daf0c23be8e7070456c336f7cbaa5c8757924f545887bb2abdd750000000000ffffffff838d0427d0ec650a68aa46bb0b098aea4422c071b2ca78352a077959d07cea1d0100000000ffffffff0270aaf00800000000160014d85c2b71d0060b09c9886aeb815e50991dda124d00e1f5050000000016001400aea9a2e5f0f876a588df5546e8742d1d87008f00000000000100bb0200000001aad73931018bd25f84ae400b68848be09db706eac2ac18298babee71ab656f8b0000000048473044022058f6fc7c6a33e1b31548d481c826c015bd30135aad42cd67790dab66d2ad243b02204a1ced2604c6735b6393e5b41691dd78b00f0c5942fb9f751856faa938157dba01feffffff0280f0fa020000000017a9140fb9463421696b82c833af241c78c17ddbde493487d0f20a270100000017a91429ca74f8a08f81999428185c97b5d852e4063f6187650000000107da00473044022074018ad4180097b873323c0015720b3684cc8123891048e7dbcd9b55ad679c99022073d369b740e3eb53dcefa33823c8070514ca55a7dd9544f157c167913261118c01483045022100f61038b308dc1da865a34852746f015772934208c6d24454393cd99bdf2217770220056e675a675a6d0a02b85b14e5e29074d8a25a9b5760bea2816f661910a006ea01475221029583bf39ae0a609747ad199addd634fa6108559d6c5cd39b4c2183f1ab96e07f2102dab61ff49a14db6a7d02b0cd1fbb78fc4b18312b5b4e54dae4dba2fbfef536d752ae0001012000c2eb0b0000000017a914b7f5faf40e3d40a5a459b1db3535f2b72fa921e8870107232200208c2353173743b595dfb4a07b72ba8e42e3797da74e87fe7d9d7497e3b20289030108da0400473044022062eb7a556107a7c73f45ac4ab5a1dddf6f7075fb1275969a7f383efff784bcb202200c05dbb7470dbf2f08557dd356c7325c1ed30913e996cd3840945db12228da5f01473044022065f45ba5998b59a27ffe1a7bed016af1f1f90d54b3aa8f7450aa5f56a25103bd02207f724703ad1edb96680b284b56d4ffcb88f7fb759eabbe08aa30f29b851383d20147522103089dc10c7ac6db54f91329af617333db388cead0c231f723379d1b99030b02dc21023add904f3d6dcf59ddb906b0dee23529b7ffb9ed50e5e86151926860221f0e7352ae00220203a9a4c37f5996d3aa25dbac6b570af0650394492942460b354753ed9eeca5877110d90c6a4f000000800000008004000080002202027f6399757d2eff55a136ad02c684b1838b6556e5f1b6b34282a94b6b5005109610d90c6a4f00000080000000800500008000")
      val expected = Transaction(
        "0200000000010258e87a21b56daf0c23be8e7070456c336f7cbaa5c8757924f545887bb2abdd7500000000da00473044022074018ad4180097b873323c0015720b3684cc8123891048e7dbcd9b55ad679c99022073d369b740e3eb53dcefa33823c8070514ca55a7dd9544f157c167913261118c01483045022100f61038b308dc1da865a34852746f015772934208c6d24454393cd99bdf2217770220056e675a675a6d0a02b85b14e5e29074d8a25a9b5760bea2816f661910a006ea01475221029583bf39ae0a609747ad199addd634fa6108559d6c5cd39b4c2183f1ab96e07f2102dab61ff49a14db6a7d02b0cd1fbb78fc4b18312b5b4e54dae4dba2fbfef536d752aeffffffff838d0427d0ec650a68aa46bb0b098aea4422c071b2ca78352a077959d07cea1d01000000232200208c2353173743b595dfb4a07b72ba8e42e3797da74e87fe7d9d7497e3b2028903ffffffff0270aaf00800000000160014d85c2b71d0060b09c9886aeb815e50991dda124d00e1f5050000000016001400aea9a2e5f0f876a588df5546e8742d1d87008f000400473044022062eb7a556107a7c73f45ac4ab5a1dddf6f7075fb1275969a7f383efff784bcb202200c05dbb7470dbf2f08557dd356c7325c1ed30913e996cd3840945db12228da5f01473044022065f45ba5998b59a27ffe1a7bed016af1f1f90d54b3aa8f7450aa5f56a25103bd02207f724703ad1edb96680b284b56d4ffcb88f7fb759eabbe08aa30f29b851383d20147522103089dc10c7ac6db54f91329af617333db388cead0c231f723379d1b99030b02dc21023add904f3d6dcf59ddb906b0dee23529b7ffb9ed50e5e86151926860221f0e7352ae00000000")

      val route =
        coreRoutes.handleCommand(
          ServerCommand("extractfrompsbt", Arr(Str(psbt.hex))))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual s"""{"result":"${expected.hex}","error":null}"""

      }
    }

    "convert a transaction to a PSBT" in {
      val tx = Transaction(
        "020000000258e87a21b56daf0c23be8e7070456c336f7cbaa5c8757924f545887bb2abdd750000000000ffffffff838d0427d0ec650a68aa46bb0b098aea4422c071b2ca78352a077959d07cea1d0100000000ffffffff0270aaf00800000000160014d85c2b71d0060b09c9886aeb815e50991dda124d00e1f5050000000016001400aea9a2e5f0f876a588df5546e8742d1d87008f00000000")
      val expected = PSBT(
        "70736274ff01009a020000000258e87a21b56daf0c23be8e7070456c336f7cbaa5c8757924f545887bb2abdd750000000000ffffffff838d0427d0ec650a68aa46bb0b098aea4422c071b2ca78352a077959d07cea1d0100000000ffffffff0270aaf00800000000160014d85c2b71d0060b09c9886aeb815e50991dda124d00e1f5050000000016001400aea9a2e5f0f876a588df5546e8742d1d87008f000000000000000000")

      val route =
        coreRoutes.handleCommand(
          ServerCommand("converttopsbt", Arr(Str(tx.hex))))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual s"""{"result":"${expected.base64}","error":null}"""

      }
    }

    "return the block count" in {
      (mockChainApi.getBlockCount: () => Future[Int])
        .expects()
        .returning(Future.successful(1234567890))

      val route =
        chainRoutes.handleCommand(ServerCommand("getblockcount", Arr()))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":1234567890,"error":null}"""
      }
    }

    "return the filter count" in {
      (mockChainApi.getFilterCount: () => Future[Int])
        .expects()
        .returning(Future.successful(1234567890))

      val route =
        chainRoutes.handleCommand(ServerCommand("getfiltercount", Arr()))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":1234567890,"error":null}"""
      }
    }

    "return the filter header count" in {
      (mockChainApi.getFilterHeaderCount: () => Future[Int])
        .expects()
        .returning(Future.successful(1234567890))

      val route =
        chainRoutes.handleCommand(ServerCommand("getfilterheadercount", Arr()))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":1234567890,"error":null}"""
      }
    }

    "return the best block hash" in {
      (mockChainApi.getBestBlockHash: () => Future[DoubleSha256DigestBE])
        .expects()
        .returning(Future.successful(DoubleSha256DigestBE.empty))

      val route =
        chainRoutes.handleCommand(ServerCommand("getbestblockhash", Arr()))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":"0000000000000000000000000000000000000000000000000000000000000000","error":null}"""
      }
    }

    "return the wallet's balance" in {
      (mockWalletApi.getBalance: () => Future[CurrencyUnit])
        .expects()
        .returning(Future.successful(Bitcoins(50)))

      val route =
        walletRoutes.handleCommand(ServerCommand("getbalance", Arr()))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":50,"error":null}"""
      }
    }

    "return a new address" in {
      (mockWalletApi.getNewAddress: () => Future[BitcoinAddress])
        .expects()
        .returning(Future.successful(testAddress))

      val route =
        walletRoutes.handleCommand(ServerCommand("getnewaddress", Arr()))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":"""" + testAddressStr + """","error":null}"""
      }
    }

    "send to an address" in {
      // positive cases

      (mockWalletApi
        .sendToAddress(_: BitcoinAddress, _: CurrencyUnit, _: FeeUnit))
        .expects(testAddress, Bitcoins(100), *)
        .returning(Future.successful(EmptyTransaction))

      (mockNode.broadcastTransaction _)
        .expects(EmptyTransaction)
        .returning(FutureUtil.unit)
        .anyNumberOfTimes()

      val route = walletRoutes.handleCommand(
        ServerCommand("sendtoaddress", Arr(Str(testAddressStr), Num(100))))

      Post() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":"0000000000000000000000000000000000000000000000000000000000000000","error":null}"""
      }

      // negative cases

      val route1 = walletRoutes.handleCommand(
        ServerCommand("sendtoaddress", Arr(Null, Null)))

      Post() ~> route1 ~> check {
        rejection shouldEqual ValidationRejection(
          "failure",
          Some(InvalidData(Null, "Expected ujson.Str")))
      }

      val route2 = walletRoutes.handleCommand(
        ServerCommand("sendtoaddress", Arr("Null", Null)))

      Post() ~> route2 ~> check {
        rejection shouldEqual ValidationRejection(
          "failure",
          Some(InvalidData("Null", "Expected a valid address")))
      }

      val route3 = walletRoutes.handleCommand(
        ServerCommand("sendtoaddress", Arr(Str(testAddressStr), Null)))

      Post() ~> route3 ~> check {
        rejection shouldEqual ValidationRejection(
          "failure",
          Some(InvalidData(Null, "Expected ujson.Num")))
      }

      val route4 = walletRoutes.handleCommand(
        ServerCommand("sendtoaddress", Arr(Str(testAddressStr), Str("abc"))))

      Post() ~> route4 ~> check {
        rejection shouldEqual ValidationRejection(
          "failure",
          Some(InvalidData("abc", "Expected ujson.Num")))
      }

    }

    "return the peer list" in {
      val route =
        nodeRoutes.handleCommand(ServerCommand("getpeers", Arr()))

      Get() ~> route ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":"TODO implement getpeers","error":null}"""
      }
    }

    "run wallet rescan" in {
      // positive cases

      (mockWalletApi.discoveryBatchSize: () => Int)
        .expects()
        .returning(100)
        .atLeastOnce()
      (mockWalletApi.isEmpty: () => Future[Boolean])
        .expects()
        .returning(Future.successful(false))
      (mockWalletApi.rescanNeutrinoWallet _)
        .expects(None, None, 100)
        .returning(FutureUtil.unit)

      val route1 =
        walletRoutes.handleCommand(
          ServerCommand("rescan", Arr(Arr(), Null, Null, true)))

      Post() ~> route1 ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":"scheduled","error":null}"""
      }

      (mockWalletApi.isEmpty: () => Future[Boolean])
        .expects()
        .returning(Future.successful(false))
      (mockWalletApi.rescanNeutrinoWallet _)
        .expects(
          Some(BlockTime(
            ZonedDateTime.of(2018, 10, 27, 12, 34, 56, 0, ZoneId.of("UTC")))),
          None,
          100)
        .returning(FutureUtil.unit)

      val route2 =
        walletRoutes.handleCommand(
          ServerCommand("rescan",
                        Arr(Arr(), Str("2018-10-27T12:34:56Z"), Null, true)))

      Post() ~> route2 ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":"scheduled","error":null}"""
      }

      (mockWalletApi.isEmpty: () => Future[Boolean])
        .expects()
        .returning(Future.successful(false))
      (mockWalletApi.rescanNeutrinoWallet _)
        .expects(None, Some(BlockHash(DoubleSha256DigestBE.empty)), 100)
        .returning(FutureUtil.unit)

      val route3 =
        walletRoutes.handleCommand(
          ServerCommand(
            "rescan",
            Arr(Null, Null, Str(DoubleSha256DigestBE.empty.hex), true)))

      Post() ~> route3 ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":"scheduled","error":null}"""
      }

      (mockWalletApi.isEmpty: () => Future[Boolean])
        .expects()
        .returning(Future.successful(false))
      (mockWalletApi.rescanNeutrinoWallet _)
        .expects(Some(BlockHeight(12345)), Some(BlockHeight(67890)), 100)
        .returning(FutureUtil.unit)

      val route4 =
        walletRoutes.handleCommand(
          ServerCommand("rescan", Arr(Arr(), Str("12345"), Num(67890), true)))

      Post() ~> route4 ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":"scheduled","error":null}"""
      }

      // negative cases

      val route5 =
        walletRoutes.handleCommand(
          ServerCommand("rescan", Arr(Null, Str("abcd"), Str("efgh"), true)))

      Post() ~> route5 ~> check {
        rejection shouldEqual ValidationRejection(
          "failure",
          Some(InvalidBlockStamp("abcd")))
      }

      val route6 =
        walletRoutes.handleCommand(
          ServerCommand("rescan",
                        Arr(Arr(55), Null, Str("2018-10-27T12:34:56"), true)))

      Post() ~> route6 ~> check {
        rejection shouldEqual ValidationRejection(
          "failure",
          Some(InvalidBlockStamp("2018-10-27T12:34:56")))
      }

      val route7 =
        walletRoutes.handleCommand(
          ServerCommand("rescan", Arr(Null, Num(-1), Null, true)))

      Post() ~> route7 ~> check {
        rejection shouldEqual ValidationRejection(
          "failure",
          Some(InvalidData(Num(-1), "Expected a positive integer")))
      }

      (mockWalletApi.isEmpty: () => Future[Boolean])
        .expects()
        .returning(Future.successful(false))
      (mockWalletApi.rescanNeutrinoWallet _)
        .expects(None, None, 55)
        .returning(FutureUtil.unit)

      val route8 =
        walletRoutes.handleCommand(
          ServerCommand("rescan", Arr(Arr(55), Arr(), Arr(), Bool(true))))

      Post() ~> route8 ~> check {
        contentType shouldEqual `application/json`
        responseAs[String] shouldEqual """{"result":"scheduled","error":null}"""
      }
    }

  }
}
