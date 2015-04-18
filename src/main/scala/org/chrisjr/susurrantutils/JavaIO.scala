package org.chrisjr.susurrantutils

import java.io._

object JavaIO {
  def pickle[T](filename: String, t: T) = {
    val fos = new FileOutputStream(filename)
    val oos = new ObjectOutputStream(fos)
    oos.writeObject(t)
    oos.close()
  }

  def unpickle[T](filename: String): T = {
    val fis = new FileInputStream(filename)
    val ois = new ObjectInputStream(fis)
    val obj = ois.readObject().asInstanceOf[T]
    ois.close()
    obj
  }
  
  class Print(val writer: PrintWriter) {
    def print(s: String) = writer.print(s)
    def println() = writer.println()
    def close() = writer.close()
  }
  object Print {
    def apply(filename: String) =
      new Print(new PrintWriter(filename, "UTF-8"))
  }
}