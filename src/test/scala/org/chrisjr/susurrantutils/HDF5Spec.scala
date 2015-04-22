package org.chrisjr.susurrantutils

import org.scalatest.FunSpec
import ch.systemsx.cisd.hdf5._
import java.io.File
import scala.util.Random

class HDF5Spec extends FunSpec {
  describe("Writing an HDF5 file") {
    it("should be readable") {
      val (f, xs) = Hdf5.randomHdf5()
      
      val reader = Hdf5.hdf5Reader(f)
      val members = Hdf5.getGroupMembers(reader, "/groupA")
      assert(members === Set("a"))
      assert(reader.readFloatMatrix("/groupA/a") === xs)
    }
  }
  describe("A PagedSeq") {
    it("should be created from an HDF5 file") {
      val (f, xs) = Hdf5.randomHdf5(16384)
      val reader = Hdf5.hdf5Reader(f)
      val paged = Hdf5.pagedFloats(reader, "/groupA/a")
      assert(paged.reduce(_ ++ _) === xs)
    }
  }
}

