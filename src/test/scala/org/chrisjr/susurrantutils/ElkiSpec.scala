package org.chrisjr.susurrantutils

import org.scalatest.FunSpec

class ElkiSpec extends FunSpec {
  describe("The Elki object") {
    it("should be able to convert HDF5 file with dataset '/X' to Elki bundles") {
      val (f, xs) = Hdf5.randomHdf5(group = "/", dataset = "X")
      val tmp2 = mkTemp()
      Elki.hdf5ToBundle(f, tmp2)
    }
  }
}