package org.chrisjr

package object susurrantutils {
  def mkTemp(): String = {
    val f = java.io.File.createTempFile("tmp", null)
    f.deleteOnExit()
    f.getPath
  }
}