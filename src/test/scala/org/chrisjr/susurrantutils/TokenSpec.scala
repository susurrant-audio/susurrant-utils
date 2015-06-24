package org.chrisjr.susurrantutils

import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers

class TokenSpec extends FunSpec with ShouldMatchers {
  describe("A CommentReader") {
    it("should retrieve comments for a track") {
      val commentReader = Tokens.commentReader("/Users/chrisjr/Development/susurrant/parsed_comments.json")
      val availableSegments = commentReader.getSegments
      val segment = availableSegments(0)
      val comments = commentReader.getCommentsFor(segment)
      comments shouldBe Map("song" -> 1, "what" -> 1, "this" -> 1, "cheeky" -> 1)
    }
    
  }
}