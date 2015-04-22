package org.chrisjr.susurrantutils

import de.lmu.ifi.dbs.elki.application.ConvertToBundleApplication
import de.lmu.ifi.dbs.elki.data.FloatVector
import de.lmu.ifi.dbs.elki.data.`type`.VectorFieldTypeInformation
import de.lmu.ifi.dbs.elki.datasource.AbstractDatabaseConnection
import de.lmu.ifi.dbs.elki.logging.Logging
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter
import java.io.InputStream

import scala.collection.JavaConversions._

object Elki {
  val LOG = Logging.getLogger(classOf[Hdf5DatabaseConnection])
  class Hdf5DatabaseConnection(filename: String, filters: List[ObjectFilter] = List())
    extends AbstractDatabaseConnection(filters) {
    def getLogger(): de.lmu.ifi.dbs.elki.logging.Logging = LOG
    def loadData(): MultipleObjectsBundle = {
      val reader = Hdf5.hdf5Reader(filename)
      val dim = reader.`object`().getDimensions("/X")(1).toInt
      val vtype = new VectorFieldTypeInformation(FloatVector.FACTORY, dim)
      val vecs = for {
        ary <- new Hdf5FloatIterator(reader, "/X")
      } yield new FloatVector(ary)
      MultipleObjectsBundle.makeSimple(vtype, vecs.toList)
    }
  }
  def hdf5ToBundle(h5filename: String, outFilename: String): Unit = {
    val conn = new Hdf5DatabaseConnection(h5filename)
    val outFile = new java.io.File(outFilename)
    val app = new ConvertToBundleApplication(conn, outFile)
    app.run()
  }
}