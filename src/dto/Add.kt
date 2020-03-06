package dto

import dto.posttypes.PostType

class Add(
    id: Long,
    author: String,
    content: String,
    created: String, postType: PostType,
    var url: String?
) : Post(id, author, content, created, postType = postType)