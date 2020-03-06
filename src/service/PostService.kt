package service

import dto.Post
import io.ktor.features.NotFoundException
import io.ktor.util.KtorExperimentalAPI
import repository.PostRepository

@KtorExperimentalAPI
class PostService(private val repo: PostRepository) {
    suspend fun getAll(): List<Post> {
        return repo.getAll().map { it.getProperPostObject() }
    }

    suspend fun getById(id: Long): Post {
        val model = repo.getById(id) ?: throw NotFoundException()
        return model.getProperPostObject()
    }

    suspend fun save(input: PostRequestDto): PostResponseDto {
        val model = PostModel(id = input.id, author = input.author, content = input.content)
        return PostResponseDto.fromModel(repo.save(model))
    }
}