package org.chrisjr.susurrantutils

import ch.systemsx.cisd.hdf5._
import scala.collection.immutable.PagedSeq
import scala.collection.JavaConversions._
import de.lmu.ifi.dbs.elki.data.FloatVector

object Hdf5 {
  val validDataTypes = Set("gfccs", "beat_coefs", "chroma")
  class Hdf5PagedSeq(reader: IHDF5Reader, group: String) extends IndexedSeq[Array[Array[Float]]] {
    val paged = new PagedHdf5(reader, group)
    def apply(idx: Int): Array[Array[Float]] = paged.get(idx)
    def length: Int = paged.size()
  }

  def hdf5Writer(filename: String): IHDF5Writer =
    HDF5Factory.open(filename)
  def hdf5Reader(filename: String): IHDF5Reader =
    HDF5Factory.openForReading(filename)
  def getGroupMembers(reader: IHDF5Reader, group: String): Set[String] =
    reader.`object`().getGroupMembers(group).toSet
  def pagedFloats(reader: IHDF5Reader, group: String): IndexedSeq[Array[Array[Float]]] = {
    new Hdf5PagedSeq(reader, group)
  }
  def mapTracks[T](reader: IHDF5Reader, f: (IHDF5Reader, String) => T): Iterator[T] = {
    val objInfo = reader.`object`()
    for {
      (track, i) <- objInfo.getGroupMembers("/").iterator().zipWithIndex
      _ = if (i % 10 == 0) { println(i) }
      dtypes = objInfo.getGroupMembers(s"/${track}").toSet
      if dtypes == validDataTypes
    } yield f(reader, track)
  }

  def trackLengthsByType(filename: String, outFile: String) = {
    val dtypes = validDataTypes.toArray
    val allLengths =
      mapTracks(hdf5Reader(filename), { (reader, track) =>
        for {
          dtype <- dtypes
          objInfo = reader.`object`()
          dims = objInfo.getDimensions(s"/${track}/${dtype}")
        } yield dims.head
      }).toArray
    val writer = hdf5Writer(outFile)
    writer.writeLongMatrix("/lengths", allLengths)
    writer.close()
  }
  
  def readH5dset(h5file: String, dataset: String = "/X"): java.util.List[FloatVector] = {
    val reader = hdf5Reader(h5file)
    val dim = reader.`object`().getDimensions(dataset)(1).toInt
    val vecs = for {
        ary <- new Hdf5FloatIterator(reader, dataset)
      } yield new FloatVector(ary)
    vecs.toList
  }

  def randomHdf5(rows: Int = 1000, cols: Int = 10,
                 group: String = "/groupA/", dataset: String = "a"): (String, Array[Array[Float]]) = {
    val f = mkTemp()
    val xs = Array.fill(rows, 10) { scala.util.Random.nextFloat }
    val writer = Hdf5.hdf5Writer(f)
    if (group != "/") {
      writer.`object`().createGroup(group)
    }
    writer.writeFloatMatrix(group ++ dataset, xs)
    writer.close()
    (f, xs)
  }
}