package org.chrisjr.susurrantutils

import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers
import scala.collection.JavaConversions._
import cc.mallet.types._

object MalletSpec {
  def tokensForTypes(tokens: Int): Map[String, Array[Int]] = (for {
    dtype <- Hdf5.validDataTypes;
    tokensForType = Array.fill(tokens) { scala.util.Random.nextInt(1000) }
  } yield dtype -> tokensForType).toMap

  def randomTracks(tracksN: Int = 200, tokens: Int = 500): (String, Map[String, Map[String, Array[Int]]]) = {
    val f = mkTemp()
    val writer = Hdf5.hdf5Writer(f)
    val xs = (for {
      trackID <- 0 until tracksN
    } yield s"$trackID" -> tokensForTypes(tokens)).toMap

    for ((group, dtypeTokens) <- xs) {
      writer.`object`().createGroup(s"/$group")
      for ((dtype, tokenVals) <- dtypeTokens) {
        writer.writeIntArray(s"/$group/$dtype", tokenVals)        
      }
    }
    writer.close()
    (f, xs)
  }
  
  val tokenRegex = """([a-z_]+)(\d+)""".r
  
  def toDtypeMap(fs: cc.mallet.types.FeatureSequence): Map[String, Map[Int, Int]] = {
    val alpha = fs.getAlphabet
    val tokens = (for {
      tokenID <- fs.getFeatures
      token = alpha.lookupObject(tokenID)
    } yield token)
    tokens.foldLeft(Map[String, Map[Int, Int]]()) { (acc, token) =>
      token match {
        case tokenRegex(dtype, index) =>
          val m = acc.getOrElse(dtype, Map())
          val i = index.toInt
          acc + (dtype -> (m.updated(i, m.getOrElse(i, 0) + 1)))
      }
    }
  }

}

class MalletSpec extends FunSpec with ShouldMatchers {
  describe("toInstances") {
    it("should convert tracks to readable Mallet instances") {
      val instanceFile = mkTemp()
      val (tracksFile, tokens) = MalletSpec.randomTracks()
      MalletUtil.toInstances(tracksFile, None, instanceFile)
      
      val instances = InstanceList.load(new java.io.File(instanceFile))
      val alphaSize = instances.getAlphabet.size
      alphaSize should be <= 3000
      alphaSize should be > 2000
      val instanceValues = instances.iterator.map { inst =>
        val fs = inst.getData.asInstanceOf[FeatureSequence]
        inst.getName.toString -> MalletSpec.toDtypeMap(fs)
      }.toMap
      instanceValues.foreach { case (track, dtypeTokens) =>
        val tokenCounts = tokens.getOrElse(track, Map()).transform { case (_, array) =>
          array.groupBy(identity).mapValues(_.length)
        }
        tokenCounts shouldEqual dtypeTokens
      }
//      instanceValues shouldBe tokens
    }

  }
}