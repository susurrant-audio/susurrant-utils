package org.chrisjr.susurrantutils

import java.nio.file.{ Paths, Files }
import java.io._
import scala.collection.JavaConversions._
import JavaIO._

object SusurrantUtils {
  def convert(args: Array[String]) = {
    val h5filename = args.headOption
    h5filename.fold(throw new IllegalArgumentException("No filename passed")) { h5 =>
      val reader = Hdf5.hdf5Reader(h5)
      val blocks = new Hdf5FloatIterator(reader, "/X");
      val outFile = h5 ++ ".seq"
      ArrayFileUtil.write(outFile, blocks)
      outFile
    }
  }

  def convertH5(args: Array[String]) = {
    val h5filename = args.headOption
    h5filename.fold(throw new IllegalArgumentException("No filename passed")) { h5 =>
      val dataType = args.drop(1).headOption.getOrElse("gfccs")
      val reader = Hdf5.hdf5Reader(h5)
      val tracks = Hdf5.getGroupMembers(reader, "/")
      var it = Array.empty[Array[Float]].toIterator
      for (
        track <- tracks;
        address = "/" ++ track ++ "/" ++ dataType;
        if reader.exists(address)
      ) {
        val mat = reader.readFloatMatrix(address)
        it = it ++ (if (dataType == "gfccs") (mat.toIterator.map(_.drop(1))) else mat.toIterator)
      }
      val outFile = dataType ++ ".seq"
      ArrayFileUtil.write(outFile, it)
      outFile
    }
  }

  def convertToKryo(args: Array[String]) = {
    import org.apache.spark.SparkConf

    val conf = new SparkConf()
    val h5filename = args.headOption
    h5filename.fold(throw new IllegalArgumentException("No filename passed")) { h5 =>
      val reader = Hdf5.hdf5Reader(h5)
      val blocks = Hdf5.pagedFloats(reader, "/X")
      val outFile = h5 ++ ".kryo"
      val vecs = blocks.iterator
      KryoRdd.saveOne(vecs, outFile, blocks.length)
      outFile
    }
  }

  def convertToRDD(args: Array[String]) = {
    import org.apache.hadoop.io._

    import org.apache.spark.SparkContext
    import org.apache.spark.SparkContext._
    import org.apache.spark.SparkConf
    import org.apache.spark.SparkFiles

    val conf = new SparkConf()
      .setAppName("K-Means")
      .setMaster("local[4]")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    val sc = new SparkContext(conf)

    val h5filename = args.headOption
    h5filename.fold(throw new IllegalArgumentException("No filename passed")) { h5 =>
      val reader = Hdf5.hdf5Reader(h5)
      val blocks = Hdf5.pagedFloats(reader, "/X")
      val outFile = h5 ++ ".kryo"
      val vecs = sc.parallelize(blocks, 16)
      KryoRdd.saveAsObjectFile(vecs, outFile)
      outFile
    }
  }

  def pickledToH5(filename: String = System.getProperty("user.home") ++ "/train/clusters.obj"): Array[Array[Double]] = {
    val centers = unpickle[Array[Array[Double]]](filename)
    val writer = Hdf5.hdf5Writer(filename.replace(".obj", ".h5"))
    writer.writeDoubleMatrix("/centers", centers)
    writer.close()
    centers
  }

  def kmeans(dataFile: String = System.getProperty("user.home") ++ "/train/data") = {
    import org.apache.hadoop.io._

    import org.apache.spark.SparkContext
    import org.apache.spark.SparkContext._
    import org.apache.spark.SparkConf
    import org.apache.spark.SparkFiles

    val master = System.getenv("MASTER")
    val conf = new SparkConf()
      .setAppName("K-Means")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    if (master != null) {
      conf.setMaster(master)
    }

    val sc = new SparkContext(conf)

    import org.apache.spark.mllib.clustering.KMeans
    import org.apache.spark.mllib.linalg.Vectors

    // Load and parse the data
    val data = KryoRdd.objectFile[Array[Array[Float]]](sc, dataFile)
    val parsedData = data.flatMap(_.map(a => Vectors.dense(a.map(_.toDouble))))
      .persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK_SER)
    // .persist(org.apache.spark.storage.StorageLevel.MEMORY_ONLY_SER)

    // Cluster the data into classes using KMeans
    val numClusters = 5000
    val numIterations = 20
    val clusters = KMeans.train(parsedData, numClusters, numIterations)

    // Evaluate clustering by computing Within Set Sum of Squared Errors
    val WSSSE = clusters.computeCost(parsedData)
    println("Within Set Sum of Squared Errors = " + WSSSE)

    val centers = clusters.clusterCenters.map(_.toArray)
    pickle(System.getProperty("user.home") ++ "/train/clusters.obj", centers)
  }

  import scopt._

  sealed trait Mode
  case object TokensToVW extends Mode

  case class Config(mode: Mode = TokensToVW, tokensIn: File = new File("."),
      vwOut: File = new File("."))

  val parser = new scopt.OptionParser[Config]("susurrant") {
    head("susurrant", "0.0.1")
    cmd("to_vw") action { (_, c) =>
      c.copy(mode = TokensToVW)
    } text ("convert tokens to vw format") children(
      opt[File]('i', "tokens-in") required() valueName("<file>") action { (x, c) =>
        c.copy(tokensIn = x) } validate { x => if (x.exists() && x.isFile()) success else failure("Input file must exist") }
        text("tokensIn is an H5 file with token data"),
      opt[File]('o', "out") required() valueName("<file>") action { (x, c) =>
        c.copy(vwOut = x) } text("out will be filled with VW data")
    )
    help("help") text ("prints this usage text")
  }

  def main(args: Array[String]) {
    parser.parse(args, Config()).fold() { conf =>
      conf.mode match {
        case TokensToVW =>
          MalletUtil.toVW(conf.tokensIn.toString, conf.vwOut.toString)
      }
    }
    //    MalletUtil.train()
    //    Tokens.saveTracks("../vocab/tokens.h5", "../susurrant_elm/data/tracks")
    //    Hdf5.trackLengthsByType("../tracks.h5", "../lengths.h5")
    //    if (dataFile.isDefined) kmeans(dataFile.get) else kmeans()
    //      convertToRDD(args)
  }
}