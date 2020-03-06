package dto

import dto.posttypes.PostType

open class Post(
    val id: Long = 0,
    val author: String,
    val content: String,
    val created: String,
    var likeCount: Long = 0L,
    var commentCount: Long = 0L,
    var shareCount: Long = 0L,
    var likedByMe: Boolean = false,
    var commentedByMe: Boolean = false,
    var sharedByMe: Boolean = false,
    val postType: PostType,
    val source: Post? = null
)