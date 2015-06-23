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
    val trackIterator = Hdf5.mapTracks(reader, { (reader, track) =>
      val trackData = (for {
        dtype <- Hdf5.validDataTypes;
        data = reader.readIntArray(s"/${track}/${dtype}")
      } yield (dtype, data)).toMap
      (track, combineTokens(trackData))
    })

    for ((track, tokens) <- trackIterator) {
      val track_id = track.split('.')(0)
      val out = new FileOutputStream(
        new File(outDir, s"$track.json"))
      JacksMapper.writeValue(out, tokens.toSeq)
    }

  }

  case class Comment(body: String, username: String, user_url: String,
                     track_id: Int, link: String, segment: String)

  trait CommentExtractor {
    def getSegments: Seq[String]
    def getCommentsFor(segment: String): Map[String, Int]
  }

  class CommentReader(inFile: File) extends CommentExtractor {
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
      val tokens = for {
        comment <- comments
        token <- comment.body.toLowerCase.split(" ")
        token0 = token.filter(Character.isLetter)
        if token0.length > 3
      } yield Map(token0 -> 1)
      if (tokens.length > 0) tokens.reduce(_ |+| _) else Map()
    }
  }

  def commentReader(commentFile: String): CommentExtractor = {
    new CommentReader(new File(commentFile))
  }
  object EmptyCommentReader extends CommentExtractor {
    def getSegments = Seq()
    def getCommentsFor(segment: String) = Map()
  }
}