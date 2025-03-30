package au.platypus.quarkus.bookmark.services

import au.platypus.quarkus.bookmark.models.Bookmark
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.persistence.PersistenceException
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.validation.ValidationException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger
import java.util.*

@ApplicationScoped
class BookmarkService {

    companion object {
        private val LOGGER = Logger.getLogger(BookmarkService::class.java.name)
    }

    @Throws(InterruptedException::class)
    fun onStart(@Observes ev: StartupEvent?) {
    }

    @Transactional
    fun createBookmark(@Valid bookmark: Bookmark): Bookmark {
        try {
            bookmark.persist()
            return bookmark
        } catch (e: PersistenceException) {
            LOGGER.error("Error creating bookmark", e)
            throw BookmarkServiceException("Failed to create bookmark: ${e.message}", e)
        }
    }

    fun getBookmarkById(id: Long): Bookmark? {
        return Bookmark.findById(id)
    }

    fun getAllBookmarks(): List<Bookmark> {
        return Bookmark.listAll()
    }

    @Transactional
    fun updateBookmark(id: Long, updatedBookmark: Bookmark): Bookmark? {
        val existingBookmark = Bookmark.findById(id)
        return existingBookmark?.let {
            it.name = updatedBookmark.name
            it.url = updatedBookmark.url
            it.displayOrder = updatedBookmark.displayOrder
            it.description = updatedBookmark.description
            it.persist()
            it
        }
    }

    @Transactional
    fun deleteBookmark(id: Long): Boolean {
        return Bookmark.deleteById(id)
    }

    fun findByName(name: String): Bookmark? {
        return Bookmark.findByName(name)
    }

    fun listByName(name: String): List<Bookmark> {
        return Bookmark.listByName(name)
    }

    fun listByNameLike(name: String): List<Bookmark> {
        return Bookmark.listByNameLike(name)
    }

    fun search(query: String): List<Bookmark> {
        // Sanitize and validate input
        val sanitizedQuery = sanitizeSearchQuery(query)
        if (!isValidSearchQuery(sanitizedQuery)) {
            throw ValidationException("Invalid search query")
        }

        // Use parameterized query with bound parameters
        return Bookmark.list(
            "lower(name) like lower(:query) or lower(url) like lower(:query) or lower(description) like lower(:query) ORDER BY displayOrder",
            mapOf("query" to "%$sanitizedQuery%")
        )
    }

    private fun sanitizeSearchQuery(query: String): String {
        return query.replace(Regex("[^a-zA-Z0-9\\s-]"), "")
    }

    private fun isValidSearchQuery(query: String): Boolean {
        return query.length in 1..100 && !query.contains(";")
    }

    @Transactional
    fun reorderBookmark(id: Long, newIndex: Int): Bookmark {
        val bookmark = Bookmark.findById(id)
            ?: throw NotFoundException("Bookmark with id $id not found")

        val maxIndex = Bookmark.count().toInt()
        if (newIndex < 1 || newIndex > maxIndex) {
            throw ValidationException("New displayOrder must be between 1 and $maxIndex")
        }

        val oldIndex = bookmark.displayOrder
        if (newIndex > oldIndex) {
            // Moving down: decrease displayOrder for bookmarks between old and new position
            Bookmark.update(
                "displayOrder = displayOrder - 1 where displayOrder > ?1 and displayOrder <= ?2",
                oldIndex, newIndex
            )
        } else if (newIndex < oldIndex) {
            // Moving up: increase displayOrder for bookmarks between new and old position
            Bookmark.update(
                "displayOrder = displayOrder + 1 where displayOrder >= ?1 and displayOrder < ?2",
                newIndex, oldIndex
            )
        }

        bookmark.displayOrder = newIndex
        return bookmark
    }

    // Custom Exception
    class BookmarkServiceException(message: String, cause: Throwable) : RuntimeException(message, cause)

    @Provider
    class ErrorMapper : ExceptionMapper<Exception> {

        @Inject
        lateinit var objectMapper: ObjectMapper

        override fun toResponse(exception: Exception): Response {
            LOGGER.error("Failed to handle request", exception)
            var code = when (exception) {
                is WebApplicationException -> exception.response.status
                is BookmarkServiceException -> 500 // Or a more specific code if appropriate
                else -> 500
            }
            val exceptionJson = objectMapper.createObjectNode()
            exceptionJson.put("exceptionType", exception.javaClass.name)
            exceptionJson.put("code", code)
            exceptionJson.put("error", exception.message ?: "Internal Server Error") // Provide a default message
            return Response.status(code)
                .entity(exceptionJson)
                .build()
        }
    }
}