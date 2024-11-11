package au.platypus.quarkus.bookmark.services

import au.platypus.quarkus.bookmark.models.Bookmark
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.validation.ValidationException
import jakarta.ws.rs.NotFoundException
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
    fun createBookmark(@Valid product: Bookmark): Bookmark {
        product.persist()
        return product
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
        return Bookmark.list(
            "lower(name) like lower(?1) or lower(url) like lower(?1) or lower(description) like lower(?1) ORDER BY displayOrder",
            "%${query}%"
        )
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
}