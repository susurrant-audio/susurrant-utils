package org.chrisjr.susurrantutils

import org.scalatest.FunSpec

class TokenSpec extends FunSpec {
  describe("A CommentReader") {
    it("should retrieve comments for a track") {
      val commentReader = Tokens.commentReader("/Users/chrisjr/Development/susurrant_prep/parsed_comments.json")
      val availableSegments = commentReader.getSegments
      val segment = availableSegments(0)
      val comments = commentReader.getCommentsFor(segment)
      println(comments)
    }
    
  }
}