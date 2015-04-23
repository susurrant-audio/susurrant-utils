package org.chrisjr.susurrantutils

import org.scalatest.FunSpec
import org.scalatest.MustMatchers._

import java.io.File
import scala.collection.JavaConversions._

import de.lmu.ifi.dbs.elki.datasource.filter.normalization
import normalization.columnwise.AttributeWiseMinMaxNormalization


class ElkiSpec extends FunSpec {
  describe("Hdf5DatabaseConnection") {
    it("should be able to read an HDF5 file") {
      val (f, xs) = Hdf5.randomHdf5(group = "/", dataset = "X")
      val hdbc = new Hdf5DatabaseConnection(List(), new File(f))
      val bundle = hdbc.loadData()
      val xs_ = Elki.getBundleData(bundle)
      xs mustEqual xs_
    }
    it("should be able to apply filters") {
      val (f, xs) = Hdf5.randomHdf5(group = "/", dataset = "X")
      val filt = new AttributeWiseMinMaxNormalization(Array(), Array())
      val hdbc = new Hdf5DatabaseConnection(List(filt), new File(f))
      val bundle = hdbc.loadData()
      val xs_ = Elki.getBundleData(bundle)
      assert (xs_.forall(_.forall(x => x >= 0.0 && x <= 1.0)))
    }    
  }
  describe("The Elki object") {
    it("should be able to convert HDF5 file with dataset '/X' to Elki bundles") {
      val (f, xs) = Hdf5.randomHdf5(group = "/", dataset = "X")
      val tmp2 = mkTemp()
      Elki.hdf5ToBundle(f, tmp2)
    }
  }
}