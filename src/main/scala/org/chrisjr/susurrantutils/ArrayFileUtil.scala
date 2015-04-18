package org.chrisjr.susurrantutils

import org.apache.hadoop.io._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.RawLocalFileSystem

object ArrayFileUtil {
  val conf = new Configuration()
  val fs = new RawLocalFileSystem()
  
  def write[T](fname: String, mat: TraversableOnce[Array[T]]): Unit = {
    val writer = new ArrayFile.Writer(conf, fs, fname, classOf[ArrayPrimitiveWritable])
    for (xs <- mat) {
      val xsw = new ArrayPrimitiveWritable(xs)
      writer.append(xsw)
    }
    writer.close()
  }

  def read[T](fname: String): Iterator[Array[T]] = {
    val reader = new ArrayFile.Reader(fs, fname, conf)
    new Iterator[Array[T]]() {
      var current: Writable = null
      private def tryAdvance = {
        val xsw = new ArrayPrimitiveWritable()
        current = reader.next(xsw)
      }
      def hasNext = {
        tryAdvance
        current != null
      }
      def next = {
        current.asInstanceOf[ArrayPrimitiveWritable].get().asInstanceOf[Array[T]]
      }
    }
  }
}