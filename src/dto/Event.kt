package dto

import dto.posttypes.PostType

class Event(
    id: Long,
    author: String,
    content: String,
    created: String,
    likeCount: Long,
    commentCount: Long,
    shareCount: Long,
    likedByMe: Boolean,
    commentedByMe: Boolean,
    sharedByMe: Boolean,
    postType: PostType,
    source: Post? = null,
    var address: String?,
    var coordinates: Coordinates? = Coordinates()
) : Post(
    id,
    author,
    content,
    created,
    likeCount,
    commentCount,
    shareCount,
    likedByMe,
    commentedByMe,
    sharedByMe,
    postType,
    source
)

data class Coordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)