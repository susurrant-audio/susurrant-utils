package org.chrisjr.susurrantutils

import ch.systemsx.cisd.hdf5.IHDF5Reader
import cc.mallet.types._
import cc.mallet.pipe._
import cc.mallet.topics.tui.TopicTrainer
import JavaIO._
import java.io.File
import java.util.ArrayList
import scala.collection.JavaConversions._

object MalletUtil {
  type LdaOpts = Map[String, String]
  def toArgs(opts: LdaOpts): Array[String] =
    (for {
      (k, v) <- opts.toArray
    } yield Array(s"--$k", v)).flatten

  def toInstances(h5file: String, commentJson: Option[String], instanceFile: String): Unit = {
    import scalaz._
    import Scalaz._

    val reader = Hdf5.hdf5Reader(h5file)
    val commentReader: Tokens.CommentExtractor =
      if (commentJson.nonEmpty) Tokens.commentReader(commentJson.get)
      else Tokens.EmptyCommentReader

    val ts2fs = new TokenSequence2FeatureSequence()
    val instances = new InstanceList(ts2fs)
    val instanceIterator = Hdf5.mapTracks(reader, { (reader, track) =>
      val trackData = (for {
        dtype <- Hdf5.validDataTypes.toArray;
        data = reader.readIntArray(s"/${track}/${dtype}");
        datum <- data
      } yield Map(s"${dtype}${datum}" -> 1))
      val commentData = commentReader.getCommentsFor(track)
      val tokenData = (trackData.reduce(_ |+| _)) |+| commentData
      val tokens = (for {
        (word, count) <- tokenData
        _ <- 1 to count
      } yield new Token(word)) 
      val ts = new TokenSequence(tokens)
      new Instance(ts, None, track, None)
    })

    instances.addThruPipe(instanceIterator)

    instances.save(new java.io.File(instanceFile))
  }

  def writeMalletText(txtFile: String, instances: Iterable[(String, Map[String, Int])]): Unit = {
    val writer = Print(txtFile)
    val label = "0"
    for ((name, counts) <- instances) {
      val wordCounts = counts.flatMap { case (word, count) => Seq.fill(count)(s"$word") }
      writer.print(s"$name\t$label\t")
      writer.print(wordCounts.mkString(" "))
      writer.println()
    }
    writer.close()
  }

  def writeVW(vw: String, instances: Iterable[(String, Map[String, Int])]): Unit = {
    val writer = Print(vw)
    for ((name, counts) <- instances) {
      val wordCounts = counts.map { case (word, count) => s"$word:$count" }
      val target = 1
      writer.print(s"$target 1.0 $name| ")
      writer.print(wordCounts.mkString(" "))
      writer.println()
    }
    writer.close()
  }

  def instancesToVW(instanceFile: String, vw: String): Unit = {
    val instances = InstanceList.load(new java.io.File(instanceFile))
    val it = for {
      instance <- instances
      name = instance.getName().toString
      fs = instance.getData.asInstanceOf[FeatureSequence]
      vec = new FeatureVector(fs)
      dict = vec.getAlphabet()
      counts = (for {
        loc <- (0 until vec.numLocations()).toIterator
        wordObj = dict.lookupObject(vec.indexAtLocation(loc))
        word = wordObj.toString()
        count = vec.valueAtLocation(loc).toInt
      } yield (word -> count)).toMap
    } yield (name, counts)

    writeVW(vw, it)
  }

  def toInstanceIterator(h5file: String, commentJson: Option[String]): Iterator[(String, Map[String, Int])] = {
    import scalaz._
    import Scalaz._
    
    val commentReader: Tokens.CommentExtractor =
      if (commentJson.nonEmpty) Tokens.commentReader(commentJson.get)
      else Tokens.EmptyCommentReader

    val reader = Hdf5.hdf5Reader(h5file)
    val instanceIterator = Hdf5.mapTracks(reader, { (reader, track) =>
      val trackData = (for {
        dtype <- Hdf5.validDataTypes.toArray;
        data = reader.readIntArray(s"/${track}/${dtype}");
        datum <- data
      } yield Map(s"${dtype}${datum}" -> 1))
      val commentData = commentReader.getCommentsFor(track)
      val counts = (trackData.reduce(_ |+| _)) |+| commentData
      (track, counts)
    })
    instanceIterator
  }

  def toVW(h5file: String, commentJson: Option[String], vw: String): Unit = {
    val instanceIterator = toInstanceIterator(h5file, commentJson)

    writeVW(vw, instanceIterator.toIterable)
  }

  def toMalletText(h5file: String, commentJson: Option[String], txtFile: String): Unit = {
    val instanceIterator = toInstanceIterator(h5file, commentJson)

    writeMalletText(txtFile, instanceIterator.toIterable)
  }
  
  def defaultOpts(malletDir: File, topics: Int = 100) = Map(
    "input" -> new File(malletDir, "instances.mallet").toString(),
    "num-topics" -> topics.toString,
    "num-iterations" -> 1000.toString,
    "num-threads" -> 4.toString,
    "optimize-interval" -> 10.toString,
    "optimize-burn-in" -> 200.toString,
    "use-symmetric-alpha" -> false.toString,
    "alpha" -> 50.0.toString,
    "beta" -> 0.01.toString,
    "output-state" -> new File(malletDir, "topic-state.gz").toString(),
    "output-doc-topics" -> new File(malletDir, "doc-topics.txt").toString(),
    "output-topic-keys" -> new File(malletDir, "topic-keys.txt").toString(),
    "word-topic-counts-file" -> new File(malletDir, "word-topics.txt").toString,
    "diagnostics-file" -> new File(malletDir, "diagnostics-file.txt").toString,
    "xml-topic-phrase-report" -> new File(malletDir, "topic-phrases.xml").toString)

  def train(maybeOpts: Option[LdaOpts] = None): Unit = {
    val opts = maybeOpts.getOrElse(defaultOpts(new File("../lda")))
    TopicTrainer.main(toArgs(opts))
  }
}