package model

import dto.*
import dto.posttypes.PostType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PostModel(
    var id: Long = 0L,
    val author: String,
    val content: String,
    var likeCount: Long = 0L,
    var commentCount: Long = 0L,
    var shareCount: Long = 0L,
    var likedByMe: Boolean = false,
    var commentedByMe: Boolean = false,
    var sharedByMe: Boolean = false,
    val postType: PostType,
    var source: Post? = null,
    var url: String? = null,
    var address: String? = null,
    var coordinates: Coordinates? = Coordinates(),
    var created: String = ""
) {

    init {
        created = createdDateTime()
    }

    fun createdDateTime(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

    fun getProperPostObject(): Post = when (postType) {
        PostType.POST -> Post(
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
        PostType.EVENT -> Event(
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
            source,
            address,
            coordinates
        )
        PostType.VIDEO -> Video(
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
            source,
            url
        )
        PostType.ADD -> Add(
            id,
            author,
            content,
            created,
            postType,
            url
        )
        PostType.REPOST -> Repost(
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
        else -> throw IllegalArgumentException("Нет такого типа поста!!!")
    }
}

fun PostModel.copy() = PostModel(
    this.id,
    this.author,
    this.content,
    this.likeCount,
    this.commentCount,
    this.shareCount,
    this.likedByMe,
    this.commentedByMe,
    this.sharedByMe,
    this.postType,
    this.source,
    this.url,
    this.address,
    this.coordinates,
    createdDateTime()
)