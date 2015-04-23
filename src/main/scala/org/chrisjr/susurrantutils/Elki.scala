package org.chrisjr.susurrantutils

import de.lmu.ifi.dbs.elki.application.ConvertToBundleApplication
import de.lmu.ifi.dbs.elki.data.FloatVector
import de.lmu.ifi.dbs.elki.data.`type`.VectorFieldTypeInformation
import de.lmu.ifi.dbs.elki.datasource.AbstractDatabaseConnection
import de.lmu.ifi.dbs.elki.logging.Logging
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter
import java.io.File
import scala.collection.JavaConversions._
import scala.reflect.ClassTag

object Elki {
  def hdf5ToBundle(h5filename: String, outFilename: String): Unit = {
    val conn = new Hdf5DatabaseConnection(List(), new File(h5filename))
    val outFile = new java.io.File(outFilename)
    val app = new ConvertToBundleApplication(conn, outFile)
    app.run()
  }
  
  def vecToArray(vec: FloatVector): Array[Float] = (for {
    i <- 0 until vec.getDimensionality
  } yield vec.getValue(i).toFloat).toArray

  def getBundleData(bundle: MultipleObjectsBundle): Array[Array[Float]] = {
    val col = bundle.getColumn(0)
    (for {
      o <- col
      vec = o.asInstanceOf[FloatVector]
    } yield vecToArray(vec)).toArray
  }
}