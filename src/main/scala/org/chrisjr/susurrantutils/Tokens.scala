package org.chrisjr.susurrantutils

import ch.systemsx.cisd.hdf5.IHDF5Reader
import com.lambdaworks.jacks.JacksMapper
import play.api.libs.json.Json
import java.io._
import scala.io.Source
import scala.collection.JavaConversions._

object Tokens {
  type Token = (Option[Int], Int, Int)
//  case class Token(beat_coef: Option[Int], chroma: Int, gfcc: Int)

  def groupby[T](iter: Iterator[T])(startsGroup: T => Boolean): Iterator[Iterator[T]] =
    new Iterator[Iterator[T]] {
      val base = iter.buffered
      override def hasNext = base.hasNext
      override def next() = Iterator(base.next()) ++ new Iterator[T] {
        override def hasNext = base.hasNext && !startsGroup(base.head)
        override def next() = if (hasNext) base.next() else Iterator.empty.next()
      }
    }

  def combineTokens(tokens: Map[String, Array[Int]]): Array[Token] = {
    for {
      i <- (0 until (tokens("gfccs").length)).toArray;
      beat_coefs = tokens("beat_coefs");
      beat_i = i / 256
      beat = if (beat_i < beat_coefs.length) Some(beat_coefs(beat_i)) else None
    } yield (beat, tokens("chroma")(i), tokens("gfccs")(i))
  }

  def saveTracks(tokenFile: String, outDir: String): Unit = {
    val reader = Hdf5.hdf5Reader(tokenFile)
    val segIterator = Hdf5.mapTracks(reader, { (reader, track) =>
      val segData = (for {
        dtype <- Hdf5.validDataTypes;
        data = reader.readIntArray(s"/${track}/${dtype}")
      } yield (dtype, data)).toMap
      (track, combineTokens(segData))
    })
    val trackIterator = groupby(segIterator)(_._1.endsWith("0"))

    for (group <- trackIterator) {
      var trackName: Option[String] = None
      var allTokens: Iterator[Token] = Iterator.empty
      for ((seg, tokens) <- group) {
        if (trackName.isEmpty) {
          trackName = Some(seg.split('.')(0))
        }
        allTokens = allTokens ++ tokens.toIterator
      }

      val track = trackName.getOrElse(
        throw new IllegalStateException("No track name found!"))
      val out = new FileOutputStream(
        new File(outDir, s"$track.json"))
      JacksMapper.writeValue(out, allTokens.toSeq)
    }
  }
  
  case class Comment(body: String, username: String, user_url: String,
      track_id: Int, link: String, segment: String)

  class CommentReader(inFile: File) {
    implicit val commentFmt = Json.format[Comment]
    val data = {
      val source = Source.fromFile(inFile)
      val input = source.getLines.mkString("\n")
      val json = Json.parse(input)
      json.as[Map[String, Seq[Comment]]]
    }
    
    def getSegments: Seq[String] = data.keys.toSeq

    def getCommentsFor(segment: String): Map[String, Int] = {
      import scalaz._
      import Scalaz._

      val comments = data.getOrElse(segment, Seq())
      val tokens = comments.flatMap(_.body.toLowerCase.split(" "))
      tokens.map(x => Map(x -> 1)).reduce(_ |+| _)
    }
  }
  
  def commentReader(commentFile: String): CommentReader = {
    new CommentReader(new File(commentFile))
  }
}