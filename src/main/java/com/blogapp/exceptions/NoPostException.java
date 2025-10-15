package com.blogapp.exceptions;

/** Exception indicating a requested post could not be located. */
public class NoPostException extends RuntimeException {

  private final long postId;

  public NoPostException(String message, long postId) {
    super(message);
    this.postId = postId;
  }

  public long getPostId() {
    return postId;
  }
}
