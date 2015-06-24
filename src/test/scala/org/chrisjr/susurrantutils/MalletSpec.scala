package org.chrisjr.susurrantutils

import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers

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

}

class MalletSpec extends FunSpec with ShouldMatchers {
  describe("toInstances") {
    it("should convert tracks to readable Mallet instances") {
      val instanceFile = mkTemp()
      val (tracksFile, tokens) = MalletSpec.randomTracks()
      MalletUtil.toInstances(tracksFile, None, instanceFile)
      
      val instances = cc.mallet.types.InstanceList.load(new java.io.File(instanceFile))
      val alphaSize = instances.getAlphabet.size
      alphaSize should be <= 3000
      alphaSize should be > 2000
    }

  }
}