package repository

import dto.*
import dto.posttypes.PostType
import io.ktor.features.NotFoundException
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import model.PostModel
import model.copy

interface PostRepository {
    suspend fun getAll(): List<PostModel>
    suspend fun getById(id: Long): PostModel?
    suspend fun create(item: PostModel): PostModel
    suspend fun update(id: Long, item: PostModel): PostModel
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long): PostModel?
    suspend fun dislikeById(id: Long): PostModel?
    suspend fun repost(item: PostModel): PostModel?
    suspend fun shareById(id: Long): PostModel?
}

class PostRepositoryImpl : PostRepository {
    private val mutex = Mutex()
    private val items = mutableListOf<PostModel>()
    private var lastKnownId: Long = 0L

    init {
        items.addAll(testData())
        updateLastKnownId()
    }

    private fun updateLastKnownId() {
        lastKnownId = items.maxBy { it.id }?.id ?: 0L
    }

    override suspend fun getAll(): List<PostModel> {
        mutex.withLock { return items }
    }

    override suspend fun getById(id: Long): PostModel? {
        mutex.withLock { return items.find { it.id == id } }
    }

    override suspend fun create(item: PostModel): PostModel {
        mutex.withLock {
            item.id = ++lastKnownId
            val finalPostModel = item.copy()
            if (items.add(finalPostModel)) {
                updateLastKnownId()
                return finalPostModel
            } else throw Throwable("Не получилось сохранить элемент $item")
        }
    }

    @KtorExperimentalAPI
    override suspend fun update(id: Long, item: PostModel): PostModel {
        mutex.withLock {
            return when (val index = items.indexOfFirst { it.id == id }) {
                -1 -> {
                    throw NotFoundException("Не существует такого элемента как $item, вероятно необходимо создать этот элемент")
                }
                else -> {
                    item.id = id
                    val finalPostModel = item.copy()
                    items[index] = finalPostModel
                    finalPostModel
                }
            }
        }
    }

    override suspend fun removeById(id: Long) {
        mutex.withLock { items.removeIf { it.id == id } }
    }

    override suspend fun likeById(id: Long): PostModel? {
        mutex.withLock {
            return when (val index = items.indexOfFirst { it.id == id }) {
                -1 -> null
                else -> {
                    val item = items[index]
                    if (!item.likedByMe) {
                        item.likeCount = ++item.likeCount
                        item.likedByMe = true
                    }
                    item
                }
            }
        }
    }

    override suspend fun dislikeById(id: Long): PostModel? {
        mutex.withLock {
            return when (val index = items.indexOfFirst { it.id == id }) {
                -1 -> null
                else -> {
                    val item = items[index]
                    if (item.likedByMe && item.likeCount > 0) {
                        item.likeCount = --item.likeCount
                        item.likedByMe = false
                    }
                    item
                }
            }
        }
    }

    override suspend fun repost(item: PostModel): PostModel? {
        //1. Проверить поле source, чтобы оно не было пустым
        //2. Проверить id поста из поля source, поста с таким id нет, то и репост делать не надо
        //3. Если всё ок, то сохраняем получившийся репост с нужным id и временем в created
        mutex.withLock {
            var result: PostModel? = null
            with(item) {
                val properSourcePost = source?.id?.let { getById(it) }?.getProperPostObject()
                if (source != null && properSourcePost != null) {
                    id = -1
                    source = properSourcePost
                    result = create(this)
                }
            }
            return result
        }
    }

    override suspend fun shareById(id: Long): PostModel? {
        mutex.withLock {
            return when (val index = items.indexOfFirst { it.id == id }) {
                -1 -> null
                else -> {
                    val item = items[index]
                    if (!item.sharedByMe) {
                        item.shareCount = ++item.shareCount
                        item.sharedByMe = true
                    }
                    item
                }
            }
        }
    }
}

fun testData() = listOf(
    PostModel(
        1, "CATS", "All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = true,
        sharedByMe = false,
        postType = PostType.POST
    ),
    PostModel(
        2, "CATS", "Event: All your base are belong to us",
        likeCount = 3,
        commentCount = 31,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.EVENT,
        address = "Shimizu, Suginami City, Tokyo, Japan",
        coordinates = Coordinates(35.7135292, 139.6134291)
    ),
    PostModel(
        3, "CATS", "Repost 1 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Post(
            234, "CATS", "Source post for repost. All your base are belong to us", "1992",
            likeCount = 3,
            commentCount = 1,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.POST
        )
    ),
    PostModel(
        348032,
        "CATS",
        "Реклама: All your base are belong to us",
        postType = PostType.ADD,
        url = "https://duckduckgo.com/?q=herbalife&atb=v127-3bd&ia=web"

    ),
    PostModel(
        4, "CATS", "Repost 2 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Event(
            523,
            "CATS",
            "Source event post for repost. Event: All your base are belong to us",
            "1992",
            likeCount = 3,
            commentCount = 31,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.EVENT,
            address = "Shimizu, Suginami City, Tokyo, Japan",
            coordinates = Coordinates(35.7135292, 139.6134291)
        )
    ), PostModel(
        5, "CATS", "All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = true,
        sharedByMe = false,
        postType = PostType.POST
    ),
    PostModel(
        6, "CATS", "Event: All your base are belong to us",
        likeCount = 3,
        commentCount = 31,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.EVENT,
        address = "Shimizu, Suginami City, Tokyo, Japan",
        coordinates = Coordinates(35.7135292, 139.6134291)
    ),
    PostModel(
        7, "CATS", "Repost 1 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Post(
            1234, "CATS", "Source post for repost. All your base are belong to us", "1992",
            likeCount = 3,
            commentCount = 1,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.POST
        )
    ),
    PostModel(
        8, "CATS", "All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = true,
        sharedByMe = false,
        postType = PostType.POST
    ),
    PostModel(
        9, "CATS", "Repost 2 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Event(
            1323,
            "CATS",
            "Source event post for repost. Event: All your base are belong to us",
            "1992",
            likeCount = 3,
            commentCount = 31,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.EVENT,
            address = "Shimizu, Suginami City, Tokyo, Japan",
            coordinates = Coordinates(35.7135292, 139.6134291)
        )
    ), PostModel(
        10, "CATS", "All your base are belong to us",
        likeCount = 25,
        commentCount = 8,
        shareCount = 12,
        likedByMe = true,
        commentedByMe = true,
        sharedByMe = false,
        postType = PostType.VIDEO,
        url = "https://www.youtube.com/watch?v=jQE66WA2s-A"
    ),
    PostModel(
        11, "CATS", "Event: All your base are belong to us",
        likeCount = 3,
        commentCount = 31,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.EVENT,
        address = "Shimizu, Suginami City, Tokyo, Japan",
        coordinates = Coordinates(35.7135292, 139.6134291)
    ),
    PostModel(
        12, "CATS", "Repost Video All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Video(
            33344, "CATS", "All your base are belong to us", "1992",
            likeCount = 25,
            commentCount = 8,
            shareCount = 12,
            likedByMe = true,
            commentedByMe = true,
            sharedByMe = false,
            postType = PostType.VIDEO,
            url = "https://www.youtube.com/watch?v=jQE66WA2s-A"
        )
    ),
    PostModel(
        13, "CATS", "Repost 1 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Post(
            21034, "CATS", "Source post for repost. All your base are belong to us", "1992",
            likeCount = 3,
            commentCount = 1,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.POST
        )
    ),
    PostModel(
        14, "CATS", "All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = true,
        sharedByMe = false,
        postType = PostType.POST
    ),
    PostModel(
        15, "CATS", "Repost 2 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Event(
            32312,
            "CATS",
            "Source event post for repost. Event: All your base are belong to us",
            "1992",
            likeCount = 3,
            commentCount = 31,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.EVENT,
            address = "Shimizu, Suginami City, Tokyo, Japan",
            coordinates = Coordinates(35.7135292, 139.6134291)
        )
    ), PostModel(
        16, "CATS", "All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = true,
        sharedByMe = false,
        postType = PostType.POST
    ),
    PostModel(
        17, "CATS", "Event: All your base are belong to us",
        likeCount = 3,
        commentCount = 31,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.EVENT,
        address = "Shimizu, Suginami City, Tokyo, Japan",
        coordinates = Coordinates(35.7135292, 139.6134291)
    ),
    PostModel(
        18, "CATS", "Repost 1 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Post(
            14234, "CATS", "Source post for repost. All your base are belong to us", "1992",
            likeCount = 3,
            commentCount = 1,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.POST
        )
    ),
    PostModel(
        19, "CATS", "All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = true,
        sharedByMe = false,
        postType = PostType.POST
    ),
    PostModel(
        20, "CATS", "Repost 2 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Event(
            33823,
            "CATS",
            "Source event post for repost. Event: All your base are belong to us",
            "1992",
            likeCount = 3,
            commentCount = 31,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.EVENT,
            address = "Shimizu, Suginami City, Tokyo, Japan",
            coordinates = Coordinates(35.7135292, 139.6134291)
        )
    ), PostModel(
        21, "CATS", "All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = true,
        sharedByMe = false,
        postType = PostType.POST
    ),
    PostModel(
        22, "CATS", "Event: All your base are belong to us",
        likeCount = 3,
        commentCount = 31,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.EVENT,
        address = "Shimizu, Suginami City, Tokyo, Japan",
        coordinates = Coordinates(35.7135292, 139.6134291)
    ),
    PostModel(
        23, "CATS", "Repost 1 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Post(
            19234, "CATS", "Source post for repost. All your base are belong to us", "1992",
            likeCount = 3,
            commentCount = 1,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.POST
        )
    ),
    PostModel(
        24, "CATS", "All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = true,
        sharedByMe = false,
        postType = PostType.POST
    ),
    PostModel(
        25, "CATS", "Repost 2 All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.REPOST,
        source = Event(
            29323,
            "CATS",
            "Source event post for repost. Event: All your base are belong to us",
            "1992",
            likeCount = 3,
            commentCount = 31,
            shareCount = 0,
            likedByMe = true,
            commentedByMe = false,
            sharedByMe = false,
            postType = PostType.EVENT,
            address = "Shimizu, Suginami City, Tokyo, Japan",
            coordinates = Coordinates(35.7135292, 139.6134291)
        )
    ),
    PostModel(
        26, "CATS", "All your base are belong to us",
        likeCount = 3,
        commentCount = 1,
        shareCount = 0,
        likedByMe = true,
        commentedByMe = false,
        sharedByMe = false,
        postType = PostType.POST
    )
)