package org.chrisjr.susurrantutils

import org.scalatest.FunSpec
import java.nio.file._
import java.nio.file.attribute._
import scala.util.Random

class ArrayFileSpec extends FunSpec {
  def delTree(start: Path) = {
         Files.walkFileTree(start, new SimpleFileVisitor[Path]() {
         override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
             Files.delete(file);
             FileVisitResult.CONTINUE
         }
         override def postVisitDirectory(dir: Path, e: java.io.IOException): FileVisitResult = {
             if (e == null) {
                 Files.delete(dir);
                 return FileVisitResult.CONTINUE;
             } else {
                 // directory iteration failed
                 throw e;
             }
         }
     });

  }
  def withTemp[T](t: String => T) = {
    val f = Files.createTempDirectory("tmp")
    t(f.toString())
    delTree(f)
  }

  describe("An ArrayFile") {
    it("should be readable with array of double arrays") (withTemp { f =>
      val mat = Array.fill(10, 1000) { Random.nextDouble }
      ArrayFileUtil.write(f, mat)
      assert(ArrayFileUtil.read[Double](f).toArray === mat)
    })
    it("should be readable with array of float arrays") (withTemp { f =>
      val mat = Array.fill(10, 1000) { Random.nextFloat }
      ArrayFileUtil.write(f, mat)
      assert(ArrayFileUtil.read[Float](f).toArray === mat)
    })

  }
}