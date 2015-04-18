package org.chrisjr.susurrantutils

import org.scalatest.FunSpec
import ch.systemsx.cisd.hdf5._
import java.io.File
import scala.util.Random

class HDF5Spec extends FunSpec {
  def mkTemp(): String = {
    val f = File.createTempFile("tmp", null)
    f.deleteOnExit()
    f.getPath
  }
  def randomHdf5(rows: Int = 1000): (String, Array[Array[Float]]) = {
      val f = mkTemp()
      val xs = Array.fill(rows, 10) { Random.nextFloat }
      val writer = Hdf5.hdf5Writer(f)
      writer.`object`().createGroup("groupA")
      writer.writeFloatMatrix("/groupA/a", xs)
      writer.close()
      (f, xs)
  }
  describe("Writing an HDF5 file") {
    it("should be readable") {
      val (f, xs) = randomHdf5()
      
      val reader = Hdf5.hdf5Reader(f)
      val members = Hdf5.getGroupMembers(reader, "/groupA")
      assert(members === Set("a"))
      assert(reader.readFloatMatrix("/groupA/a") === xs)
    }
  }
  describe("A PagedSeq") {
    it("should be created from an HDF5 file") {
      val (f, xs) = randomHdf5(16384)
      val reader = Hdf5.hdf5Reader(f)
      val paged = Hdf5.pagedFloats(reader, "/groupA/a")
      assert(paged.reduce(_ ++ _) === xs)
    }
  }
}

